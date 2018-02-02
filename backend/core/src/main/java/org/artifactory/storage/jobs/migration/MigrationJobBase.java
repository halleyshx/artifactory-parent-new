package org.artifactory.storage.jobs.migration;

import org.apache.commons.lang.StringUtils;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.HaAddon;
import org.artifactory.api.context.ArtifactoryContext;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.repo.AsyncWorkQueueProviderService;
import org.artifactory.aql.AqlConverts;
import org.artifactory.aql.AqlService;
import org.artifactory.aql.result.AqlEagerResult;
import org.artifactory.aql.result.rows.AqlItem;
import org.artifactory.common.ConstantValues;
import org.artifactory.common.config.db.ArtifactoryDbProperties;
import org.artifactory.exception.CancelException;
import org.artifactory.fs.FileInfo;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.rest.resource.task.BackgroundTask;
import org.artifactory.rest.resource.task.BackgroundTasks;
import org.artifactory.schedule.quartz.QuartzCommand;
import org.artifactory.storage.db.InternalDbService;
import org.artifactory.storage.db.fs.dao.NodesDao;
import org.artifactory.storage.db.servers.model.ArtifactoryServer;
import org.artifactory.storage.db.servers.service.ArtifactoryServersCommonService;
import org.artifactory.storage.db.util.JdbcHelper;
import org.artifactory.storage.db.version.converter.DBSqlConverter;
import org.artifactory.storage.spring.StorageContextHelper;
import org.artifactory.version.ArtifactoryVersion;
import org.jfrog.storage.DbType;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.artifactory.schedule.TaskBase.TaskState.*;
import static org.artifactory.schedule.TaskUtils.pauseOrBreak;

/**
 * Common logic for the v550 migration jobs.
 *
 * @author Dan Feldman
 */
public abstract class MigrationJobBase extends QuartzCommand {
    private static final Logger log = LoggerFactory.getLogger(MigrationJobBase.class);

    //Configurable job params
    protected final long sleepInterval = ConstantValues.migrationJobSleepIntervalMillis.getLong();
    protected final int batchThreshold = ConstantValues.migrationJobBatchSize.getInt();
    protected final int queryLimit = ConstantValues.migrationJobDbQueryLimit.getInt();

    protected long lastNodeId = 0;  // Last node id returned by the db query, used for offsetting the next query

    @Override
    protected void onExecute(JobExecutionContext callbackContext) throws JobExecutionException {
        try {
            if (!jobRunsOnOneNodeOnly()) {
                return;
            }
            waitForClusterUpgrade();
            if (!init()) {
                return;
            }
            runMigration();
        } catch (CancelException e) {
            log().info("Caught stop signal in main loop, {} is exiting.", jobName());
        } catch (Exception e) {
            log().error("Caught unexpected exception during " + jobName() + " job, operation will break.", e);
        }
    }

    private void runMigration() {
        migrationLoop();
        additionalSteps();
        if (hasErrors()) {
            //we had errors for sure, time to stop.
            finishedWithErrors();
        } else if (stateOk()) { //run verification on counters before final db conversion
            markCompletion();
        } else {
            // There are still nulls in the db, retry the entire op another time.
            retry();
        }
    }

    protected final void migrationLoop() {
        // As long as calculation tasks are submitted there may be recoverable errors that require a retry
        boolean tasksWereSubmitted = true;
        while (tasksWereSubmitted) {
            tasksWereSubmitted = false;
            //Starting from the second loop - give a chance to run again on nodes that had errors we can recover from.
            //Unrecoverable nodes are filtered out of the query results when running the db query.
            lastNodeId = 0;
            while (lastNodeId > -1) { //-1 is set by the db query mechanism when no more nodes are returned
                waitForWorkQueue(1000);
                tasksWereSubmitted = migrationLogic();
                waitBetweenBatchesIfNeeded();
                logProgress();
                log().debug("Finished one loop, last returned node id: {}", lastNodeId);
            }
            // Wait here for all tasks to at least be clear of the queue (maybe some are still working though) before
            // trying the db query a second (or more) time - lest the next db query will return stuff that's still in work.
            waitForWorkQueue(0);
        }
        waitForWorkQueue(0);
        logProgress();
        //TODO [by dan]: perhaps wait here a bit more hardcoded to let the nodes table complete for sure?
    }

    /**
     * @return the minimal {@link ArtifactoryVersion} required for the migration job to run.
     * The job will hold until all nodes in the cluster are of that version at least.
     */
    protected abstract ArtifactoryVersion getMinimalVersion();

    /**
     * @return true if the job is setup to run
     */
    protected abstract boolean jobEnabled();

    /**
     * The init phase makes sure all prerequisites for the job's execution hold and inits all required states.
     */
    protected abstract boolean init();

    /**
     * Actual logic the job needs to execute inside the {@link #migrationLoop}
     */
    protected abstract boolean migrationLogic();

    /**
     * Allow any additional logic the job may want to run
     */
    protected abstract void additionalSteps();

    /**
     * Lets the job specify if it had any errors
     */
    protected abstract boolean hasErrors();

    /**
     * Lets the job report errors, perform extra logic if needed
     */
    protected abstract void finishedWithErrors();

    /**
     * Lets the job determine if all went well.
     */
    protected abstract boolean stateOk();

    /**
     * Lets the job report success, perform extra logic if needed
     */
    protected abstract void markCompletion();

    /**
     * Lets the job execute retry logic.
     */
    protected abstract void retry();

    /**
     * Returns the job's log.
     */
    protected abstract Logger log();

    protected abstract void logProgress();

    protected abstract String jobName();

    /**
     * How many calculations already done in current batch
     */
    protected abstract AtomicInteger currentBatchCount();

    /**
     * Workqueue name that is attached to this job.
     */
    protected abstract String workQueueCallbackName();

    /**
     * Waits for {@link ConstantValues#migrationJobSleepIntervalMillis} if the current batch count has
     * exceeded the {@link ConstantValues#migrationJobBatchSize}.
     * The wait here is an approximation as all tasks that update the counter are async and it makes no sense to put
     * each of them to sleep, instead no new tasks will be submitted if the counter is pass the threshold
     */
    protected void waitBetweenBatchesIfNeeded() {
        if (currentBatchCount().get() >= batchThreshold) {
            pauseOrBreakIfNeeded();
            try {
                log().trace("Current batch reached sleep threshold, going to sleep for {} millis", sleepInterval);
                Thread.sleep(sleepInterval);
            } catch (Exception e) {
                log().trace("{} job thread interrupted while waiting between batches: {}, resuming...", jobName(), e.getMessage());
            }
            //reset batch counter.
            currentBatchCount().set(0);
            logProgress();
        }
    }

    /**
     * Checks if this job needs to pause or break, if pause was signaled this method blocks until released by the task service.
     * @throws CancelException if job was signaled to stop.
     */
    protected void pauseOrBreakIfNeeded() throws CancelException {
        boolean needToStop = false;
        try {
            log().trace("Testing if current task execution should pause, current time: {}", new Date().toString());
            needToStop = pauseOrBreak();
            log().trace("Done Testing for pause state, current time: {}", new Date().toString());
        } catch (Exception e) {
            log().debug("Caught exception trying to verify if current task should stop: ", e);
        }
        if (needToStop) {
            log().warn("{} job received stop signal, aborting.", jobName());
            throw new CancelException("STOP!", 777);
        }
    }

    protected List<FileInfo> mapResultsToFileInfo(AqlEagerResult<AqlItem> aqlResults) {
        return aqlResults.getResults().stream()
                .filter(Objects::nonNull)
                .map(AqlConverts.toFileInfo)
                .collect(Collectors.toList());
    }

    /**
     * Polls on the job's work queue (approximately) until there are {@param targetSize} (or less) tasks in it.
     */
    private void waitForWorkQueue(int targetSize) {
        pauseOrBreakIfNeeded();
        int workQueuePendingSize = getWorkQueuePendingSize();
        while (workQueuePendingSize > targetSize) {
            pauseOrBreakIfNeeded();
            try {
                log().debug("There are still {} pending calculation tasks, waiting for {} millis until queue size is {}",
                        workQueuePendingSize, sleepInterval, targetSize);
                Thread.sleep(sleepInterval);
            } catch (Exception e) {
                log().debug("Interrupted while waiting for tasks to finish, resuming...", e);
            }
            workQueuePendingSize = getWorkQueuePendingSize();
        }
    }

    private int getWorkQueuePendingSize() {
        int size = 0;
        try {
            size = ContextHelper.get().beanForType(AsyncWorkQueueProviderService.class)
                    .getEstimatedPendingTasksSize(workQueueCallbackName());
        } catch (Exception e) {
            //work queue is inited only when a task is first pushed, this might get called too soon on the first few times
            log().debug("Work queue for callback {} still not inited, will try next time.", workQueueCallbackName());
        }
        return size;
    }

    /**
     * Queries all other nodes in the cluster (when run in HA env) and verifies all nodes are on the minimal
     * required version, execution waits here until condition is met.
     */
    private void waitForClusterUpgrade() throws CancelException {
        int sleepInterval = ConstantValues.migrationJobWaitForClusterSleepIntervalMillis.getInt();
        String sleepMsg = "'{}' waiting for all other nodes in the cluster to upgrade to minimal required version {}. " +
                "Sleep interval set to {} milliseconds";
        while(nonReadyNodesExist()) {
            log.info(sleepMsg, currentTaskToken(), getMinimalVersion().getValue(), sleepInterval);
            log().info(sleepMsg, currentTaskToken(), getMinimalVersion().getValue(), sleepInterval);
            try {
                log().trace("{}: going to sleep for {}ms", currentTaskToken(), sleepInterval);
                Thread.sleep(sleepInterval);
            } catch (InterruptedException e) {
                log().debug(currentTaskToken() + " interrupted while waiting for other nodes to upgrade, resuming...", e);
            }
        }
        log().info("{}: all nodes reached minimal version '{}', continuing execution", currentTaskToken(), getMinimalVersion().getValue());
    }

    private boolean nonReadyNodesExist() {
        ArtifactoryServersCommonService serversService = serversService();
        return serversService != null && serversService.getOtherActiveMembers()
                .stream()
                .map(ArtifactoryServer::getArtifactoryVersion)
                .filter(Objects::nonNull)
                .map(ArtifactoryVersion::fromVersionString)
                .filter(Objects::nonNull)
                .anyMatch(version -> version.before(getMinimalVersion()));
    }

    /**
     * In HA mode, system prop with force node id may have been set on multiple nodes by mistake - verify it runs only on one.
     */
    private boolean jobRunsOnOneNodeOnly() {
        if (jobEnabled() && haAddon().isHaEnabled()) {
            String otherNodeWithThisJobRunning = getOtherNodeWithThisJobRunning();
            if (StringUtils.isNotBlank(otherNodeWithThisJobRunning)) {
                String warnMsg = "{} job was found active on another node in the cluster (ID: '{}'), although it was " +
                        "setup to run on this node ('{}') as well. it will be aborted on this node.";
                log().warn(warnMsg, jobName(), otherNodeWithThisJobRunning, ContextHelper.get().getServerId());
                log.warn(warnMsg, jobName(), otherNodeWithThisJobRunning, ContextHelper.get().getServerId());
                return false;
            }
        }
        return true;
    }

    /**
     * @return the first node id found running this job, if any. null otherwise.
     */
    @Nullable
    private String getOtherNodeWithThisJobRunning() {
        return haAddon().propagateTasksList(serversService().getOtherRunningHaMembers(), BackgroundTasks.class)
                .stream()
                .filter(Objects::nonNull)
                .map(BackgroundTasks::getTasks)
                .flatMap(Collection::stream)
                .filter(this::taskIsActive)
                .filter(task -> this.getClass().getName().equalsIgnoreCase(task.getType()))
                .map(BackgroundTask::getNodeId)
                .findAny().orElse(null);
    }

    private boolean taskIsActive(BackgroundTask task) {
        String taskState = task.getState();
        return RUNNING.name().equalsIgnoreCase(taskState) ||
                VIRGIN.name().equalsIgnoreCase(taskState) ||
                PAUSING.name().equalsIgnoreCase(taskState) ||
                PAUSED.name().equalsIgnoreCase(taskState) ||
                SCHEDULED.name().equalsIgnoreCase(taskState);
    }

    protected void executeDbConversion(String conversionName) throws SQLException {
        JdbcHelper jdbcHelper = jdbcHelper();
        DbType dbType = ContextHelper.get().beanForType(ArtifactoryDbProperties.class).getDbType();
        new DBSqlConverter(conversionName).convert(jdbcHelper, dbType);
    }

    protected HaAddon haAddon() {
        return ContextHelper.get().beanForType(AddonsManager.class).addonByType(HaAddon.class);
    }

    protected AqlService aqlService() {
        return ContextHelper.get().beanForType(AqlService.class);
    }

    protected NodesDao nodesDao() {
        return ContextHelper.get().beanForType(NodesDao.class);
    }

    protected InternalDbService dbService() {
        return StorageContextHelper.get().beanForType(InternalDbService.class);
    }

    protected InternalRepositoryService repoService() {
        return ContextHelper.get().beanForType(InternalRepositoryService.class);
    }

    private ArtifactoryServersCommonService serversService() {
        ArtifactoryContext context = ContextHelper.get();
        return context != null ? context.beanForType(ArtifactoryServersCommonService.class) : null;
    }

    private JdbcHelper jdbcHelper() {
        return StorageContextHelper.get().beanForType(JdbcHelper.class);
    }
}

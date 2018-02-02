package org.artifactory.lock.service;

import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.HaAddon;
import org.artifactory.api.context.ArtifactoryContext;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.common.ConstantValues;
import org.artifactory.lock.LockingProviderTypeEnum;
import org.artifactory.schedule.*;
import org.artifactory.schedule.quartz.QuartzCommand;
import org.artifactory.storage.db.locks.service.DbLocksService;
import org.artifactory.storage.db.servers.service.ArtifactoryServersCommonService;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

/**
 * @author gidis
 */
@Service
public class LockServiceImpl implements LockService {

    private TaskService taskService;

    @Autowired
    public LockServiceImpl(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostConstruct
    private void init() {
        //TODO [by shayb]: check if we can remove this service as we will only need the job to run on HA with db/optimistic locking provider anyway and for that we already have a service
        if (LockingProviderTypeEnum.isDb() || LockingProviderTypeEnum.isOptimistic()) {
            TaskBase task = TaskUtils.createRepeatingTask(ReleaseExpiredLocksJob.class,
                    TimeUnit.MINUTES.toMillis(ConstantValues.dbLockCleanupJobIntervalSec.getLong()),
                    TimeUnit.MINUTES.toMillis(ConstantValues.dbLockCleanupJobStaleIntervalSec.getLong()));
            taskService.startTask(task, true);
        }
    }

    @JobCommand(singleton = true, runOnlyOnPrimary = false, description = "Release expired and orphan locks job",
            schedulerUser = TaskUser.SYSTEM, manualUser = TaskUser.SYSTEM)
    public static class ReleaseExpiredLocksJob extends QuartzCommand {
        private static final Logger log = LoggerFactory.getLogger(ReleaseExpiredLocksJob.class);

        @Override
        protected void onExecute(JobExecutionContext callbackContext) throws JobExecutionException {
            try {
                ArtifactoryContext context = ContextHelper.get();
                if (context == null) {
                    log.warn("Context is not bound.");
                    return;
                }
                releaseExpiredLocks(context);
            } catch (Exception e) {
                log.error("Expired locks cleanup job could not be completed. {}", e.getMessage());
                log.debug("Expired locks cleanup job could not be completed.", e);
            }
        }

        private void releaseExpiredLocks(ArtifactoryContext context) {
            ArtifactoryServersCommonService serversService = context.beanForType(ArtifactoryServersCommonService.class);
            DbLocksService dbLocksService = context.beanForType(DbLocksService.class);
            AddonsManager addonsManager = context.beanForType(AddonsManager.class);
            HaAddon haAddon = addonsManager.addonByType(HaAddon.class);
            if (haAddon.isHaEnabled() && haAddon.isPrimary() || serversService.getActiveRunningHaPrimary() == null) {
                try {
                    dbLocksService.cleanDbExpiredLocks();
                } catch (Exception e) {
                    log.warn("Failed cleaning expired locks. {}", e.getMessage());
                    log.debug("Failed cleaning expired locks.", e);
                }
            }
            try {
                dbLocksService.cleanCachedExpiredLocks();
            } catch (Exception e) {
                log.warn("Failed cleaning cached orphan locks. {}", e.getMessage());
                log.debug("Failed cleaning cached  orphan locks.", e);
            }
        }
    }

}

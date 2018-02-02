/*
 *
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2016 JFrog Ltd.
 *
 * Artifactory is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 * Artifactory is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Artifactory.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.artifactory.support.core.bundle;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.ha.HaCommonAddon;
import org.artifactory.addon.ha.semaphore.SemaphoreWrapper;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.common.ConstantValues;
import org.artifactory.support.config.analysis.ThreadDumpConfiguration;
import org.artifactory.support.config.bundle.BundleConfiguration;
import org.artifactory.support.config.configfiles.ConfigFilesConfiguration;
import org.artifactory.support.config.descriptor.ConfigDescriptorConfiguration;
import org.artifactory.support.config.security.SecurityInfoConfiguration;
import org.artifactory.support.config.storage.StorageSummaryConfiguration;
import org.artifactory.support.config.system.SystemInfoConfiguration;
import org.artifactory.support.config.systemlogs.SystemLogsConfiguration;
import org.artifactory.support.core.annotations.CollectService;
import org.artifactory.support.core.compression.SupportBundleCompressor;
import org.artifactory.support.core.exceptions.TempDirAccessException;
import org.artifactory.util.InternalStringUtils;
import org.jfrog.storage.common.ConflictGuard;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Nullable;
import javax.annotation.PreDestroy;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import static org.artifactory.common.ConstantValues.waitForSlotBeforeWithdraw;

/**
 * Provides generic bundle generation capabilities
 *
 * @author Michael Pasternak
 */
public abstract class AbstractSupportBundleService implements SupportBundleService {
    private static final Logger log = LoggerFactory.getLogger(AbstractSupportBundleService.class);

    private static final String SUPPORT_BUNDLE_TIMESTAMP_PATTERN = "yyyyMMdd-HHmmssS";

    private volatile ConflictGuard conflictGuard;
    private volatile ExecutorService executorService;
    private static ImmutableList<Method> collectServices = null;

    @Autowired
    private AddonsManager addonsManager;

    /**
     * @return CollectServices
     */
    private static ImmutableList<Method> getCollectServices() {
        if (collectServices == null) {
            synchronized (AbstractSupportBundleService.class) {
                if (collectServices == null) {
                    collectServices = identifyCollectServices();
                }
            }
        }
        return collectServices;
    }

    /**
     * Collects SystemLogs
     *
     * @param progressLatch pipe listening for worker progress events
     * @param tmpDir        output directory for produced content
     * @param configuration the runtime configuration
     * @return operation result
     */
    @CollectService(priority = CollectService.Priority.LOW)
    private boolean collectSystemLogs(CountDownLatch progressLatch, File tmpDir, BundleConfiguration configuration) {
        if (configuration.isCollectSystemLogs()) {
            try {
                return doCollectSystemLogs(
                        tmpDir,
                        configuration.getSystemLogsConfiguration()
                );
            } finally {
                postProcess(progressLatch);
            }
        }
        return false;
    }

    /**
     * Collects SystemInfo
     *
     * @param progressLatch pipe listening for worker progress events
     * @param tmpDir        output directory for produced content
     * @param configuration the runtime configuration
     * @return operation result
     */
    @CollectService
    private boolean collectSystemInfo(CountDownLatch progressLatch, File tmpDir, BundleConfiguration configuration) {
        if (configuration.isCollectSystemInfo()) {
            try {
                return doCollectSystemInfo(
                        tmpDir,
                        configuration.getSystemInfoConfiguration()
                );
            } finally {
                postProcess(progressLatch);
            }
        }
        return false;
    }

    /**
     * Collects SecurityConfig
     *
     * @param progressLatch pipe listening for worker progress events
     * @param tmpDir        output directory for produced content
     * @param configuration the runtime configuration
     * @return operation result
     */
    @CollectService
    private boolean collectSecurityConfig(CountDownLatch progressLatch, File tmpDir,
            BundleConfiguration configuration) {
        if (configuration.isCollectSecurityConfig()) {
            try {
                return doCollectSecurityConfig(
                        tmpDir,
                        configuration.getSecurityInfoConfiguration()
                );
            } finally {
                postProcess(progressLatch);
            }
        }
        return false;
    }

    /**
     * Collects ConfigDescriptor
     *
     * @param progressLatch pipe listening for worker progress events
     * @param tmpDir        output directory for produced content
     * @param configuration the runtime configuration
     * @return operation result
     */
    @CollectService
    private boolean collectConfigDescriptor(CountDownLatch progressLatch, File tmpDir,
            BundleConfiguration configuration) {
        if (configuration.isCollectConfigDescriptor()) {
            try {
                return doCollectConfigDescriptor(
                        tmpDir,
                        configuration.getConfigDescriptorConfiguration()
                );
            } finally {
                postProcess(progressLatch);
            }
        }
        return false;
    }

    /**
     * Collects ConfigurationFiles
     *
     * @param progressLatch pipe listening for worker progress events
     * @param tmpDir        output directory for produced content
     * @param configuration the runtime configuration
     * @return operation result
     */
    @CollectService
    private boolean collectConfigurationFiles(CountDownLatch progressLatch, File tmpDir,
            BundleConfiguration configuration) {
        if (configuration.isCollectConfigurationFiles()) {
            try {
                return doCollectConfigurationFiles(
                        tmpDir,
                        configuration.getConfigFilesConfiguration()
                );
            } finally {
                postProcess(progressLatch);
            }
        }
        return false;
    }

    /**
     * Collects ThreadDump
     *
     * @param progressLatch pipe listening for worker progress events
     * @param tmpDir        output directory for produced content
     * @param configuration the runtime configuration
     * @return operation result
     */
    @CollectService(priority = CollectService.Priority.HIGH)
    private boolean collectThreadDump(CountDownLatch progressLatch, File tmpDir, BundleConfiguration configuration) {
        if (configuration.isCollectThreadDump()) {
            try {
                return doCollectThreadDump(
                        tmpDir,
                        configuration.getThreadDumpConfiguration()
                );
            } finally {
                postProcess(progressLatch);
            }
        }
        return false;
    }

    /**
     * Collects StorageSummary
     *
     * @param progressLatch pipe listening for worker progress events
     * @param tmpDir        output directory for produced content
     * @param configuration the runtime configuration
     * @return operation result
     */
    @CollectService
    private boolean collectStorageSummary(CountDownLatch progressLatch, File tmpDir,
            BundleConfiguration configuration) {
        if (configuration.isCollectStorageSummary()) {
            try {
                return doCollectStorageSummary(
                        tmpDir,
                        configuration.getStorageSummaryConfiguration()
                );
            } finally {
                postProcess(progressLatch);
            }
        }
        return false;
    }

    /**
     * Invoked post {@link @CollectService}
     */
    private void postProcess(CountDownLatch progress) {
        synchronized (progress) {
            progress.countDown();
            if (progress.getCount() > 0) {
                getLog().debug(
                        Thread.currentThread().getName() + " has finished, " +
                                "still left '{}' tasks to break the latch",
                        progress.getCount()
                );
            } else {
                getLog().debug(
                        Thread.currentThread().getName() + " has finished, all tasks are done!"
                );
            }
        }
    }

    /**
     * Performs rolling up to {@link ConstantValues#maxBundles} bundles
     */
    private void rollBundles() {
        if (list().size() > ConstantValues.maxBundles.getInt()) {
            synchronized (this) {
                List<String> bundles = list();
                Collections.sort(bundles); // make sure it's sorted, so we'll delete only the earliest ones
                int bundleToDelete = bundles.size() - ConstantValues.maxBundles.getInt();
                for (int i = 0; i < bundleToDelete; i++) {
                    // Don't propagate to other nodes, this is a local operation.
                    delete(bundles.get(i), false, true);
                }
            }
        }
    }

    /**
     * Performs content collection of all {@link CollectService} products
     * and compresses output
     *
     * @param configuration the runtime configuration
     * @return compressed archives/s
     */
    @Override
    public final List<String> generate(BundleConfiguration configuration) {
        String alreadyRunningMsg = "Another support content collection process already running, " +
                "please try again in few moments";
        try {
            boolean lock = getConflictGuard().tryToLock(waitForSlotBeforeWithdraw.getInt(), TimeUnit.SECONDS);
            if (!lock) {
                getLog().warn(alreadyRunningMsg);
            } else {
                return doGenerate(configuration);

            }
        } catch (Exception e) {
            log.error("Failed to acquire lock for Support Bundle", e);
        } finally {
            rollBundles();
            getConflictGuard().unlock();
        }
        return Lists.newLinkedList();
    }

    /**
     * Performs content collection of all {@link CollectService} products
     * and compresses output
     *
     * @param configuration the runtime configuration
     * @return compressed archives/s
     */
    private List<String> doGenerate(BundleConfiguration configuration) {
        getLog().info("Initiating support content collection ...");
        getLog().debug("Creating temporary bundle directory");
        File sourceDirectory = createTempSourceDirectory();
        getLog().debug("Initiating content generation");
        generateContent(sourceDirectory, configuration);
        getLog().debug("Initiating content compression");
        List<File> compressedContent = compressAndMoveContent(sourceDirectory, getOutputDirectory());
        List<String> files = org.artifactory.support.utils.FileUtils.toFileNames(compressedContent);
        getLog().info("Support request content collection is done!, - " + files);
        return files;
    }

    /**
     * Creates output directory
     *
     * @return reference to output directory
     */
    private File createTempSourceDirectory() {
        File tmpDir = ArtifactoryHome.get().getTempWorkDir();
        File archiveTmpDir = new File(tmpDir,
                SUPPORT_BUNDLE_PREFIX +
                        DateTime.now().toString(SUPPORT_BUNDLE_TIMESTAMP_PATTERN)
                        + "-" + System.currentTimeMillis());
        try {
            FileUtils.forceMkdir(archiveTmpDir);
        } catch (IOException e) {
            throw new TempDirAccessException("Temp bundle directory creation has failed - " + e.getMessage(), e);
        }
        return archiveTmpDir;
    }

    /**
     * @return support bundle output directory
     */
    @Override
    public File getOutputDirectory() {
        return ArtifactoryHome.get().getSupportDir();
    }

    /**
     * Collects and compresses generated content
     *
     * @param archiveTmpDir   location of generated content
     * @param outputDirectory location to save the output archive
     * @return reference to     compressed archive
     */
    private List<File> compressAndMoveContent(File archiveTmpDir, File outputDirectory) {
        getLog().info("Compressing collected content ...");
        return SupportBundleCompressor.compress(archiveTmpDir, outputDirectory);
    }

    /**
     * Invokes asynchronously all eligible {@link CollectService}
     *
     * @param archiveTmpDir location for generated content
     * @param progress      progress signaling mechanism
     * @param configuration the runtime configuration
     */
    private void invokeContentCollection(File archiveTmpDir, CountDownLatch progress,
            BundleConfiguration configuration) {
        if (getCollectServices().size() > 0) {
            for (Method task : getCollectServices()) {
                getLog().debug("Checking task '{}' for execution", task.getName());

                if (isTaskEnabled(task, configuration)) {
                    getLog().info("Scheduling task '" + task.getName() + "'");
                    AbstractSupportBundleService owner = this;
                    try {
                        submitAsyncTask(() -> {
                            try {
                                task.setAccessible(true);
                                task.invoke(owner, progress, archiveTmpDir, configuration);
                            } catch (IllegalAccessException | IllegalArgumentException e) {
                                getLog().error("Task '" + task.getName() + "' has failed");
                                getLog().debug("Cause: {}", e);
                            } catch (InvocationTargetException e) {
                                getLog().error("Task '" + task.getName() + "' has failed, " + getCause(e));
                                getLog().debug("Cause: {}", e);
                            }
                        });
                    } catch (RejectedExecutionException e) {
                        getLog().error("Task '" + task.getName() + "' has failed");
                        getLog().debug("Cause: {}", e);
                    }
                } else {
                    getLog().debug("Task '{}' is not eligible for execution", task.getName());
                }
            }
        }
    }

    /**
     * Fetches Throwable cause
     *
     * @param e exception to check
     * @return actual exception cause
     */
    private String getCause(InvocationTargetException e) {
        return (e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
    }

    /**
     * Triggers async content generation and awaits for
     * all triggered tasks accomplishing
     *
     * @param archiveTmpDir location for generated content
     * @param configuration the runtime configuration
     * @return result
     */
    private boolean generateContent(File archiveTmpDir, BundleConfiguration configuration) {
        CountDownLatch progress = createProgressPipe(configuration);
        getLog().info("Awaiting for tasks collecting content ...");
        invokeContentCollection(archiveTmpDir, progress, configuration);
        try {
            progress.await(
                    ConstantValues.contentCollectionAwaitTimeout.getInt(),
                    TimeUnit.MINUTES
            );
            getLog().info("All collecting tasks were accomplished!");
            return true;
        } catch (Exception e) {
            getLog().error("Await for collecting tasks has ended with error, - " +
                    e.getMessage()
            );
            getLog().debug("cause: {}", e);
        }
        return false;
    }

    /**
     * Produces pipe listening for worker progress events
     *
     * @param configuration the runtime configuration
     * @return {@link CountDownLatch}
     */
    private CountDownLatch createProgressPipe(BundleConfiguration configuration) {
        return new CountDownLatch(getMembersCount(configuration));
    }

    /**
     * Collects all {@link CollectService}
     *
     * @return ImmutableList<Method>
     */
    private static ImmutableList<Method> identifyCollectServices() {
        return ImmutableList.copyOf(
                Arrays.stream(AbstractSupportBundleService.class.getDeclaredMethods())
                        .parallel()
                        .filter(m -> m.getAnnotation(CollectService.class) != null)
                        .sorted((m1, m2) -> {
                            CollectService c1 = m1.getAnnotation(CollectService.class);
                            CollectService c2 = m2.getAnnotation(CollectService.class);
                            if (c1 != null && c2 != null) {
                                return c2.priority().compareTo(c1.priority());
                            }
                            return 0;
                        })
                        .collect(Collectors.toList()
                        )
        );
    }

    /**
     * Check whether given {@link CollectService} is enabled
     *
     * @param task          task to check
     * @param configuration the runtime configuration
     * @return boolean
     */
    private boolean isTaskEnabled(Method task, BundleConfiguration configuration) {
        try {
            String isMethodName = "is" + InternalStringUtils.capitalize(task.getName());
            Method isMethod = configuration.getClass().getDeclaredMethod(isMethodName);
            if (isMethod != null) {
                return (boolean) isMethod.invoke(configuration);
            } else {
                // should not happen
                getLog().debug("Could not find corresponding verification method to '{}'", task.getName());
            }
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            getLog().debug("Cannot verify if task enabled: {}", e);
        }
        return false;
    }

    /**
     * Calculates amount of collect services eligible for execution
     *
     * @param configuration the runtime configuration
     * @return amount of {@link CollectService} eligible for execution
     */
    private int getMembersCount(BundleConfiguration configuration) {
        int workersCount = 0;
        for (Method method : getCollectServices()) {
            boolean enabled = isTaskEnabled(method, configuration);
            workersCount += (enabled ? 1 : 0);
        }
        return workersCount;
    }

    /**
     * @return {@link SemaphoreWrapper}
     */
    private ConflictGuard getConflictGuard() {
        if (conflictGuard == null) {
            synchronized (this) {
                if (conflictGuard == null) {
                    AddonsManager addonsManager = ContextHelper.get().beanForType(AddonsManager.class);
                    HaCommonAddon haCommonAddon = addonsManager.addonByType(HaCommonAddon.class);
                    conflictGuard = haCommonAddon.getConflictGuard(HaCommonAddon.SUPPORT_BUNDLE_SEMAPHORE_NAME);
                }
            }
        }
        return conflictGuard;
    }

    /**
     * @return {@link ExecutorService}
     */
    private ExecutorService getExecutorService() {
        if (executorService == null) {
            synchronized (this) {
                if (executorService == null) {
                    executorService = Executors.newFixedThreadPool(
                            Runtime.getRuntime().availableProcessors()
                    );
                }
            }
        }
        return executorService;
    }

    @PreDestroy
    private void destroy() {
        if (!getExecutorService().isShutdown()) {
            getExecutorService().shutdown();
        }
    }

    /**
     * Lists previously created bundles
     *
     * @return archive/s
     */
    @Override
    public final List<String> list() {
        List<String> bundles = listFromThisServer();
        HaCommonAddon haAddon = addonsManager.addonByType(HaCommonAddon.class);
        List<String> membersBundles = haAddon.propagateSupportBundleListRequest();
        bundles.addAll(membersBundles);

        // The same file might be exists in multiple servers due to the old implementations that it was shared, however,
        // the full String might contains different '?node=xyz' in the end of the string. For this, we take the base of
        // the string and ensure that there are no duplicates on the base (everything but the '?node=xyz'
        Map<String, String> uniqueBundleMap = Maps.newHashMap();
        bundles.forEach(item -> {
            String[] split = item.split("\\?");
            if (split.length == 2 && split[0] != null) {
                uniqueBundleMap.put(split[0], item);
            }
        });
        return uniqueBundleMap.values().stream()
                .sorted(Collections.reverseOrder())
                .collect(Collectors.toList());
    }

    @Override
    public List<String> listFromThisServer() {
        File[] files = getOutputDirectory().listFiles();
        if (files == null) {
            return Lists.newArrayList();
        }

        // Adding query param of the handling node, so the user who download the bundle file will reach to the correct node.
        // If the link does not contains query param with the handling node, meaning that this node should handle the download
        return Arrays.stream(files)
                .filter(File::isFile)
                .filter(this::isFileArchive)
                .map(File::getName)
                .map(item -> {
                    if (!item.contains("?node=")) {
                        item += "?node=" + ContextHelper.get().getServerId();
                    }
                    return item;
                })
                .collect(Collectors.toList());
    }

    private boolean isFileArchive(File file) {
        return FilenameUtils.getExtension(file.getName()).equals(SupportBundleCompressor.ARCHIVE_EXTENSION);
    }

    /**
     * Downloads support bundles
     *
     * @return {@link InputStream} to support bundle (user responsibility is to close stream upon consumption)
     */
    @Override
    public final InputStream download(String bundleName, String handlingNode) throws FileNotFoundException {
        assert !Strings.isNullOrEmpty(bundleName) : "bundleName cannot be empty";
        getLog().debug("Downloading support bundle '{}' with handling node: '{}'", bundleName, handlingNode);
        InputStream inputStream = null;
        if (handlingNodeIsOtherMember(handlingNode)) {
            getLog().debug("Handling node is other member: '{}'. Attempting to propagate download event from {}",
                    handlingNode, handlingNode);
            HaCommonAddon haCommonAddon = addonsManager.addonByType(HaCommonAddon.class);
            return haCommonAddon.propagateSupportBundleDownloadRequest(bundleName, handlingNode);
        } else {
            getLog().debug("Attempting to propagate download event", handlingNode);
            File bundleFile = getBundleFile(bundleName);
            if (bundleFile != null && bundleFile.exists()) {
                inputStream = new FileInputStream(bundleFile);
            } else {
                getLog().error("File not found: " + bundleName);
            }
        }
        return inputStream;
        // TODO: check if stream can be closed by server (if not closed by client)
    }

    private boolean handlingNodeIsOtherMember(String handlingNode) {
        String thisServerId = ContextHelper.get().getServerId();
        return StringUtils.isNotBlank(handlingNode) && !handlingNode.equals(thisServerId);
    }

    @Nullable
    private File getBundleFile(String bundleName) {
        File bundleFile = null;
        Matcher bundleMatcher = BUNDLE_PATTERN.matcher(bundleName);
        if (bundleMatcher.find()) {
            bundleFile = new File(ArtifactoryHome.get().getSupportDir(),
                    SUPPORT_BUNDLE_PREFIX + bundleMatcher.group(1) + ".zip");
        } else {
            getLog().error("Wrong bundle file name given: {}", bundleName);
        }
        return bundleFile;
    }

    /**
     * Deletes support bundles
     *
     * @param bundleName name of bundle to delete
     * @param async      whether delete should be performed asynchronously
     * @return result
     */
    @Override
    public final boolean delete(String bundleName, boolean shouldPropagate, boolean async) {
        File bundleFile = getBundleFile(bundleName);
        boolean deletedByOthers = false;
        boolean deletedByMe = false;
        if (shouldPropagate) {
            // Try to propagate in any case, other nodes might have this file due to the bug this commit fixes.
            // REST endpoint is async by default so no worries here.
            // Only propagate if this node is the first in the chain (governed by the x-originated header)
            deletedByOthers = propagateDeleteRequest(bundleName);
        }
        // File exists locally perform delete on current node.
        if (bundleFile != null && bundleFile.exists()) {
            if (async) {
                deletedByMe = deleteAsync(bundleName, bundleFile);
            } else {
                deletedByMe = deleteSync(bundleName, bundleFile);
            }
        }
        return deletedByMe || deletedByOthers;
    }

    private boolean propagateDeleteRequest(String bundleName) {
        boolean deletedByOthers = false;
        try {
            deletedByOthers = addonsManager.addonByType(HaCommonAddon.class).propagateDeleteSupportBundle(bundleName);
        } catch (Exception e) {
            getLog().warn("Failed to propagate delete support bundle request: {}", e.getMessage());
            getLog().debug("", e);
        }
        return deletedByOthers;
    }

    /**
     * Deletes bundle asynchronously
     *
     * @param bundleName name
     * @param file       bundle
     * @return true if scheduling has succeeded, otherwise false
     */
    private boolean deleteAsync(String bundleName, File file) {
        try {
            submitAsyncTask(() -> deleteSync(bundleName, file));
            return true;
        } catch (RejectedExecutionException e) {
            getLog().warn("Deleting '" + bundleName + "' has failed, see logs for more details");
            getLog().debug("Cause: {}", e);
            return false;
        }
    }

    /**
     * Deletes bundle synchronously
     *
     * @param bundleName name
     * @param file       bundle
     * @return success/failure
     */
    private boolean deleteSync(String bundleName, File file) {
        boolean deleted = FileUtils.deleteQuietly(file);
        if (deleted) {
            getLog().info("'" + bundleName + "' was successfully deleted");
        } else {
            getLog().warn("'" + bundleName + "' was not deleted");
        }
        return deleted;
    }

    /**
     * Submits async task for execution
     *
     * @param task {@link Runnable tp execute}
     */
    private void submitAsyncTask(Runnable task) {
        getExecutorService().submit(task);
    }

    /**
     * @return {@link Logger}
     */
    protected static Logger getLog() {
        return log;
    }

    /**
     * Collects SystemLogs
     *
     * @param tmpDir        output directory for produced content
     * @param configuration the runtime configuration
     * @return operation result
     */
    protected abstract boolean doCollectSystemLogs(File tmpDir, SystemLogsConfiguration configuration);

    /**
     * Collects SystemInfo
     *
     * @param tmpDir        output directory for produced content
     * @param configuration the runtime configuration
     * @return operation result
     */
    protected abstract boolean doCollectSystemInfo(File tmpDir, SystemInfoConfiguration configuration);

    /**
     * Collects SecurityConfig
     *
     * @param tmpDir        output directory for produced content
     * @param configuration the runtime configuration
     * @return operation result
     */
    protected abstract boolean doCollectSecurityConfig(File tmpDir, SecurityInfoConfiguration configuration);

    /**
     * Collects ConfigDescriptor
     *
     * @param tmpDir        output directory for produced content
     * @param configuration the runtime configuration
     * @return operation result
     */
    protected abstract boolean doCollectConfigDescriptor(File tmpDir, ConfigDescriptorConfiguration configuration);

    /**
     * Collects ConfigurationFiles
     *
     * @param tmpDir        output directory for produced content
     * @param configuration the runtime configuration
     * @return operation result
     */
    protected abstract boolean doCollectConfigurationFiles(File tmpDir, ConfigFilesConfiguration configuration);

    /**
     * Collects ThreadDump
     *
     * @param tmpDir        output directory for produced content
     * @param configuration the runtime configuration
     * @return operation result
     */
    protected abstract boolean doCollectThreadDump(File tmpDir, ThreadDumpConfiguration configuration);

    /**
     * Collects StorageSummary
     *
     * @param tmpDir        output directory for produced content
     * @param configuration the runtime configuration
     * @return operation result
     */
    protected abstract boolean doCollectStorageSummary(File tmpDir, StorageSummaryConfiguration configuration);
}

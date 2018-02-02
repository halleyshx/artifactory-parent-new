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

package org.artifactory.storage.db.servers.service;

import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.ha.ClusterOperationsService;
import org.artifactory.addon.license.ArtifactoryHaLicenseDetails;
import org.artifactory.addon.license.LicenseOperationStatus;
import org.artifactory.api.context.ArtifactoryContext;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.common.ConstantValues;
import org.artifactory.config.CentralConfigKey;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.post.jobs.CallHomeJob;
import org.artifactory.schedule.*;
import org.artifactory.schedule.quartz.QuartzCommand;
import org.artifactory.spring.ContextReadinessListener;
import org.artifactory.spring.Reloadable;
import org.artifactory.spring.ReloadableBean;
import org.artifactory.state.ArtifactoryServerState;
import org.artifactory.storage.binstore.service.BinaryService;
import org.artifactory.storage.db.servers.model.ArtifactoryServer;
import org.artifactory.version.CompoundVersionDetails;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.artifactory.addon.AddonsManager.ARTIFACTORY_PRODUCT_NAME;

/**
 * author: gidis
 */
@Service
@Reloadable(beanClass = ArtifactoryHeartbeatService.class, initAfter = {TaskService.class, BinaryService.class},
        listenOn = CentralConfigKey.none)
public class ArtifactoryHeartbeatServiceImpl implements ArtifactoryHeartbeatService, ReloadableBean,
        ContextReadinessListener {

    private static final Logger log = LoggerFactory.getLogger(ArtifactoryHeartbeatServiceImpl.class);

    @Autowired
    private TaskService taskService;

    @Autowired
    private ArtifactoryServersCommonService serversService;

    @Autowired
    private AddonsManager addonsManager;

    @Override
    public void init() {
    }

    @Override
    public void destroy() {
        taskService.cancelTasks(HeartbeatJob.class, true);
        taskService.cancelTasks(CallHomeJob.class, true);
    }

    /**
     * creates & starts HeartbeatJob
     */
    private void registersHeartbeatJob() {
        TaskBase heartbeatJob = TaskUtils.createRepeatingTask(HeartbeatJob.class,
                TimeUnit.SECONDS.toMillis(ConstantValues.haHeartbeatIntervalSecs.getLong()),
                TimeUnit.SECONDS.toMillis(ConstantValues.haHeartbeatIntervalSecs.getLong()));
        taskService.startTask(heartbeatJob, false);
    }

    /**
     * creates & starts CallHomeJob
     */
    private void registerCallHomeJob() {
        String callHomeQuarzExpression = CallHomeJob.buildRandomQuartzExp();
        TaskBase callHomeJob = TaskUtils.createCronTask(
                CallHomeJob.class,
                callHomeQuarzExpression
        );
        log.debug("Scheduling CallHomeJob to run at '{}'", callHomeQuarzExpression);
        taskService.startTask(callHomeJob, false);
    }

    @Override
    public void updateHeartbeat() {
        String serverId = "unknown server id";
        try {
            serverId = ContextHelper.get().getServerId();
            long heartbeat = System.currentTimeMillis();
            log.debug("Updating heartbeat for {} [{}]", serverId, heartbeat);
            String licenseKeyHash = addonsManager.getLicenseKeyHash();
            serversService.updateArtifactoryServerHeartbeat(serverId, heartbeat, licenseKeyHash);
            log.debug("Updated heartbeat for {} [{}]", serverId, heartbeat);
        } catch (Exception e) {
            log.error("Failed to update heartbeat for [" + serverId + "]", e);
        }
    }

    @Override
    public void activateLicenseIfNeeded() {
        String serverId = "unknown server id";
        try {
            boolean needToActivateLicense = false;
            boolean homeIsBound = ArtifactoryHome.isBound();
            if (homeIsBound) {
                boolean success = addonsManager.verifyExistingLicensesOnline(false);
                if (!success) {
                    needToActivateLicense = true;
                }
                if (ArtifactoryHome.get().isHaConfigured()) {
                    // Fist, verify all servers, remove license if there is duplicate and set to offline if needed.
                    addonsManager.verifyAllArtifactoryServers(true);
                    serverId = ContextHelper.get().getServerId();
                    log.trace("Attempting to perform internal activation if needed for {}", serverId);
                    boolean licenseInstalled = addonsManager.isLicenseInstalled();
                    //TODO [by shayb]: Consider do that only if we have available license
                    // Activate only if license is not activated already, or activated license is expired
                    if (!licenseInstalled ||
                            (licenseInstalled && addonsManager.isLicenseExpired(ARTIFACTORY_PRODUCT_NAME))) {
                        log.trace("Performing internal activation for '{}'", serverId);
                        needToActivateLicense = true;

                    } else {
                        log.trace("Skipping internal activation");
                    }
                }
                if (needToActivateLicense) {
                    LicenseOperationStatus status = new LicenseOperationStatus();
                    addonsManager.activateLicense(Collections.emptySet(), status, true, false);
               }
            } else {
                log.trace("Automatic activation is not required");
            }
        } catch (Exception e) {
            // Log entry should be vague, users shouldn't know that we are using the heartbeat to activate license,
            // otherwise, they can cheat and disable the job in order duplicate the license on multiple nodes
            log.debug("Failed to perform internal activation for {}.", serverId);
        }
    }

    @Override
    public void convert(CompoundVersionDetails source, CompoundVersionDetails target) {
    }

    @Override
    public void reload(CentralConfigDescriptor oldDescriptor) {
    }

    @Override
    public void onContextCreated() {
        // Topology listeners may be interested in the cluster's state before the first heartbeat runs
        // This must be here and not in the init, because the logic both the updateServiceClisterChange and the heartbeat
        // are triggering the online license activation, and the onlineLicenseActivaiton reloadableBean is not yet set
        // on the init phase
        HeartbeatJob.updateBinaryServiceClusterChanges(serversService);
        registersHeartbeatJob();
        registerCallHomeJob();
    }

    @Override
    public void onContextReady() {

    }

    @JobCommand(singleton = true, runOnlyOnPrimary = false, description = "Database Heartbeat",
            schedulerUser = TaskUser.SYSTEM, manualUser = TaskUser.SYSTEM)
    public static class HeartbeatJob extends QuartzCommand {

        @Override
        protected void onExecute(JobExecutionContext callbackContext) throws JobExecutionException {
            ArtifactoryContext context = ContextHelper.get();
            ArtifactoryServersCommonService serversService = context.beanForType(ArtifactoryServersCommonService.class);
            ArtifactoryServer currentMember = serversService.getCurrentMember();
            if (context.isReady() && ArtifactoryServerState.RUNNING == currentMember.getServerState()) {
                ArtifactoryHeartbeatService heartbeatService = context.beanForType(ArtifactoryHeartbeatService.class);
                heartbeatService.activateLicenseIfNeeded();
                heartbeatService.updateHeartbeat();
                updateBinaryServiceClusterChanges(serversService);
            }
        }

        static void updateBinaryServiceClusterChanges(ArtifactoryServersCommonService serversService) {
            try {
                ArtifactoryContext context = ContextHelper.get();
                if (ArtifactoryHome.get().isHaConfigured()) {
                    List<String> haLicensedNodes = getValidHaLicensedNodes();
                    //These are the currently running nodes that have proper HA licenses
                    List<ArtifactoryServer> runningLicensedNodes = serversService.getOtherRunningHaMembers().stream()
                            .filter(Objects::nonNull)
                            .filter(node -> haLicensedNodes.contains(node.getServerId()))
                            .collect(Collectors.toList());
                    if (runningLicensedNodes.size() > 0) {
                        log.trace("Notifying cluster topology listeners about current active nodes: {}", runningLicensedNodes);
                        context.beanForType(ClusterOperationsService.class).clusterTopologyChanged(runningLicensedNodes);
                    }
                }
            } catch (Exception e) {
                log.error("Failed to update Binary store about cluster changes", e);
            }
        }

        /**
         * @return Node ids of all nodes in the cluster that have a valid, HA compatible license
         */
        private static List<String> getValidHaLicensedNodes() {
            return ContextHelper.get().beanForType(AddonsManager.class).getClusterLicensesDetails()
                    .stream()
                    .filter(HeartbeatJob::HACompatibleLicense)
                    .map(ArtifactoryHaLicenseDetails::getNodeId)
                    .collect(Collectors.toList());
        }

        private static boolean HACompatibleLicense(ArtifactoryHaLicenseDetails license) {
            return "Enterprise".equalsIgnoreCase(license.getType()) || "Trial".equalsIgnoreCase(license.getType());
        }
    }
}

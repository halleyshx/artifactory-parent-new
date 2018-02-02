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

package org.artifactory.post.providers.features;

import org.apache.commons.lang.StringUtils;
import org.artifactory.api.callhome.FeatureGroup;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.descriptor.download.FolderDownloadConfigDescriptor;
import org.artifactory.descriptor.replication.LocalReplicationDescriptor;
import org.artifactory.descriptor.replication.RemoteReplicationDescriptor;
import org.artifactory.descriptor.repo.RealRepoDescriptor;
import org.artifactory.descriptor.repo.RemoteRepoDescriptor;
import org.artifactory.descriptor.repo.RepoLayout;
import org.artifactory.descriptor.repo.distribution.DistributionRepoDescriptor;
import org.artifactory.descriptor.trashcan.TrashcanConfigDescriptor;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.repo.virtual.VirtualRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * This class represent the repositories feature group of the CallHome feature
 *
 * @author Shay Bagants
 */
@Component
public class RepositoriesFeature implements CallHomeFeature {

    @Autowired
    private CentralConfigService configService;

    @Autowired
    private InternalRepositoryService repoService;

    @Override
    public FeatureGroup getFeature() {

        FeatureGroup repositoriesFeature = new FeatureGroup("repositories");
        FeatureGroup localRepositoriesFeature = new FeatureGroup("local repositories");
        FeatureGroup remoteRepositoriesFeature = new FeatureGroup("remote repositories");
        FeatureGroup distributionRepositoriesFeature = new FeatureGroup("distribution repositories");

        FeatureGroup trashCanFeature = new FeatureGroup("Trashcan");
        TrashcanConfigDescriptor trashcanConfig = configService.getDescriptor().getTrashcanConfig();
        trashCanFeature.addFeatureAttribute("enabled",
                trashcanConfig.isEnabled());
        trashCanFeature.addFeatureAttribute("retention_period",
                trashcanConfig.getRetentionPeriodDays());

        FeatureGroup folderDownloadFeature = new FeatureGroup("Folder Download");
        FolderDownloadConfigDescriptor folderDownloadConfig = configService.getDescriptor().getFolderDownloadConfig();
        folderDownloadFeature.addFeatureAttribute("enabled", folderDownloadConfig.isEnabled());
        folderDownloadFeature.addFeatureAttribute("enabledForAnonymous", folderDownloadConfig.isEnabledForAnonymous());
        folderDownloadFeature.addFeatureAttribute("max_size", folderDownloadConfig.getMaxDownloadSizeMb());
        folderDownloadFeature.addFeatureAttribute("max_files", folderDownloadConfig.getMaxFiles());
        folderDownloadFeature.addFeatureAttribute("max_parallel_downloads", folderDownloadConfig.getMaxConcurrentRequests());

        List<RealRepoDescriptor> localAndRemoteRepositoriesDescriptors = repoService.getLocalAndRemoteRepoDescriptors();
        List<DistributionRepoDescriptor> distributionRepositoriesDescriptors = repoService
                .getDistributionRepoDescriptors();

        localAndRemoteRepositoriesDescriptors.forEach(rr -> {
            if (rr.isLocal()) {
                addLocalRepoFeatures(localRepositoriesFeature, rr);
            } else {
                addRemoteRepoFeatures(remoteRepositoriesFeature, rr);
            }
        });
        distributionRepositoriesDescriptors.forEach(
                dr -> addDistributionRepoFeatures(distributionRepositoriesFeature, dr)
        );

        long localCount = localAndRemoteRepositoriesDescriptors.stream()
                .filter(RealRepoDescriptor::isLocal)
                .count();
        localRepositoriesFeature.addFeatureAttribute("number_of_repositories", localCount);
        remoteRepositoriesFeature.addFeatureAttribute("number_of_repositories",
                localAndRemoteRepositoriesDescriptors.size() - localCount);
        distributionRepositoriesFeature.addFeatureAttribute("number_of_repositories",
                distributionRepositoriesDescriptors.size());

        repositoriesFeature.addFeature(localRepositoriesFeature);
        repositoriesFeature.addFeature(remoteRepositoriesFeature);
        repositoriesFeature.addFeature(distributionRepositoriesFeature);

        // virtual repos
        FeatureGroup virtualRepositoriesFeature = new FeatureGroup("virtual repositories");
        List<VirtualRepo> virtualRepositories = repoService.getVirtualRepositories();
        virtualRepositoriesFeature
                .addFeatureAttribute("number_of_repositories", virtualRepositories.size());
        addVirtualRepoFeatures(virtualRepositoriesFeature, virtualRepositories);
        repositoriesFeature.addFeature(virtualRepositoriesFeature);

        repositoriesFeature.addFeature(trashCanFeature);
        repositoriesFeature.addFeature(folderDownloadFeature);

        return repositoriesFeature;
    }

    /**
     * Collects virtual repo metadata  {@see RTFACT-8412}
     */
    private void addLocalRepoFeatures(FeatureGroup localRepositoriesFeature,
            final RealRepoDescriptor localRepoDescriptor) {
        // local repos
        localRepositoriesFeature.addFeature(new FeatureGroup(localRepoDescriptor.getKey()) {{
            addFeatureAttribute("package_type", localRepoDescriptor.getType().name());
            RepoLayout repoLayout = localRepoDescriptor.getRepoLayout();
            if (repoLayout != null) {
                addFeatureAttribute("repository_layout", repoLayout.getName());
            }
            LocalReplicationDescriptor localReplication =
                    configService.getDescriptor().getEnabledLocalReplication(localRepoDescriptor.getKey());
            if (localReplication != null && localReplication.isEnabled()) {
                List<LocalReplicationDescriptor> repls =
                        configService.getDescriptor().getMultiLocalReplications(localRepoDescriptor.getKey());
                addFeatureAttribute("push_replication", (repls == null || repls.size() == 0 ? false :
                        repls.size() > 1 ? "multi" : "true"));
                addFeatureAttribute("event_replication", localReplication.isEnableEventReplication());
                addFeatureAttribute("sync_properties", localReplication.isSyncProperties());
                addFeatureAttribute("sync_deleted", localReplication.isSyncDeletes());
            } else if (localReplication == null) {
                addFeatureAttribute("push_replication", false);
                addFeatureAttribute("event_replication", false);
                addFeatureAttribute("sync_deleted", false);
            }
        }});
    }

    /**
     * Collects virtual repo metadata  {@see RTFACT-8412}
     */
    private void addRemoteRepoFeatures(FeatureGroup remoteRepositoriesFeature,
            final RealRepoDescriptor remoteRepoDescriptor) {
        // remote repos
        remoteRepositoriesFeature.addFeature(new FeatureGroup(remoteRepoDescriptor.getKey()) {{
            addFeatureAttribute("package_type", remoteRepoDescriptor.getType().name());
            RepoLayout repoLayout = remoteRepoDescriptor.getRepoLayout();
            if (repoLayout != null) {
                addFeatureAttribute("repository_layout", repoLayout.getName());
            }
            RemoteReplicationDescriptor remoteReplicationDescriptor =
                    configService.getDescriptor().getRemoteReplication(remoteRepoDescriptor.getKey());
            if (remoteReplicationDescriptor != null) {
                addFeatureAttribute("pull_replication", remoteReplicationDescriptor.isEnabled());
                if (remoteReplicationDescriptor.isEnabled()) {
                    addFeatureAttribute("pull_replication_url",
                            ((RemoteRepoDescriptor) remoteRepoDescriptor).getUrl());
                    addFeatureAttribute("event_replication", remoteReplicationDescriptor.isEnableEventReplication());
                }
            } else {
                addFeatureAttribute("pull_replication", false);
            }
        }});
    }

    /**
     * Collects virtual repo metadata  {@see RTFACT-10170}
     */
    private void addDistributionRepoFeatures(FeatureGroup distributionRepositoriesFeature,
            final DistributionRepoDescriptor distributionRepoDescriptor) {
        // distribution repos
        distributionRepositoriesFeature.addFeature(new FeatureGroup(distributionRepoDescriptor.getKey()) {{
            addFeatureAttribute("target_repo_license",
                    distributionRepoDescriptor.getDefaultNewRepoPremium() ? "premium" : "oss");
            addFeatureAttribute("visibility",
                    distributionRepoDescriptor.getDefaultNewRepoPrivate() ? "private" : "public");
            addFeatureAttribute("distribute_product",
                    StringUtils.isNotBlank(distributionRepoDescriptor.getProductName()));
        }});
    }

    /**
     * Collects virtual repo metadata  {@see RTFACT-8412}
     */
    private void addVirtualRepoFeatures(FeatureGroup virtualRepositoriesFeature, List<VirtualRepo> virtualRepositories) {
        virtualRepositories.forEach(vr -> {
            virtualRepositoriesFeature.addFeature(new FeatureGroup(vr.getKey()) {{
                addFeatureAttribute("number_of_included_repositories",
                        vr.getResolvedLocalRepos().size() + vr.getResolvedRemoteRepos().size());
                addFeatureAttribute("package_type", vr.getDescriptor().getType().name());
                if (vr.getDescriptor().getRepoLayout() != null) {
                    addFeatureAttribute("repository_layout", vr.getDescriptor().getRepoLayout().getName());
                }
                if (vr.getDescriptor().getDefaultDeploymentRepo() != null) {
                    addFeatureAttribute("configured_local_deployment",
                            vr.getDescriptor().getDefaultDeploymentRepo().getKey());
                }
            }});
        });
    }
}

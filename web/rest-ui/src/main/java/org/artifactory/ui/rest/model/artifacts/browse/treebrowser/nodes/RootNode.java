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

package org.artifactory.ui.rest.model.artifacts.browse.treebrowser.nodes;

import com.google.common.collect.Lists;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.descriptor.repo.*;
import org.artifactory.descriptor.repo.distribution.DistributionRepoDescriptor;
import org.artifactory.repo.trash.TrashService;
import org.artifactory.rest.common.model.RestModel;
import org.artifactory.ui.rest.model.artifacts.browse.treebrowser.nodes.repo.RepositoryNode;
import org.artifactory.ui.rest.model.artifacts.browse.treebrowser.nodes.repo.VirtualRemoteRepositoryNode;
import org.codehaus.jackson.annotate.JsonTypeName;

import java.util.*;

/**
 * This is the root node of the tree browser. It contains all the repository nodes.
 *
 * @author Chen Keinan
 */
@JsonTypeName("root")
class RootNode implements RestTreeNode {

    @Override
    public Collection<? extends RestModel> fetchItemTypeData(boolean isCompact) {
        List<RestModel> repoNodes = new ArrayList<>();
        //Add a tree node for each file repository and local cache repository
        RepositoryService repositoryService = ContextHelper.get().getRepositoryService();
        addDistributionRepoNodes(repoNodes, repositoryService);
        addLocalRepoNodes(repoNodes, repositoryService);
        addRemoteRepoNodes(repoNodes, repositoryService);
        addVirtualRepoNodes(repoNodes, repositoryService);
        addTrashRepo(repoNodes, repositoryService);
        return repoNodes;
    }

    private void addDistributionRepoNodes(List<RestModel> repoNodes, RepositoryService repositoryService) {
        List<DistributionRepoDescriptor> distributionDescriptors = repositoryService.getDistributionRepoDescriptors();
        removeNonPermissionRepositories(distributionDescriptors);
        Collections.sort(distributionDescriptors, new RepoComparator());
        repoNodes.addAll(getDistributionNodes(distributionDescriptors));
    }

    private void addLocalRepoNodes(List<RestModel> repoNodes, RepositoryService repositoryService) {
        List<LocalRepoDescriptor> localRepos = repositoryService.getLocalAndCachedRepoDescriptors();
        removeNonPermissionRepositories(localRepos);
        Collections.sort(localRepos, new LocalRepoAlphaComparator());
        repoNodes.addAll(getLocalNodes(localRepos));
    }

    private void addRemoteRepoNodes(List<RestModel> repoNodes, RepositoryService repositoryService) {
        List<RemoteRepoDescriptor> remoteDescriptors = repositoryService.getRemoteRepoDescriptors();
        removeNonPermissionRepositories(remoteDescriptors);
        Collections.sort(remoteDescriptors, new RepoComparator());
        repoNodes.addAll(getRemoteNodes(remoteDescriptors));
    }

    /**
     * add virtual repo nodes to repo list
     *
     * @param repoNodes         - repository nodes list
     * @param repositoryService - repository service
     */
    private void addVirtualRepoNodes(List<RestModel> repoNodes, RepositoryService repositoryService) {
        List<VirtualRepoDescriptor> virtualDescriptors = repositoryService.getVirtualRepoDescriptors();
        removeNonPermissionRepositories(virtualDescriptors);
        Collections.sort(virtualDescriptors, new RepoComparator());
        repoNodes.addAll(getVirtualNodes(virtualDescriptors));
    }

    private void addTrashRepo(List<RestModel> repoNodes, RepositoryService repositoryService) {
        AuthorizationService authService = ContextHelper.get().beanForType(AuthorizationService.class);
        if (authService.isAdmin()) {
            LocalRepoDescriptor trashDescriptor = repositoryService.localRepoDescriptorByKey(TrashService.TRASH_KEY);
            repoNodes.add(new RepositoryNode(trashDescriptor.getKey(), trashDescriptor.getType(), "trash"));
        }
    }

    private List<INode> getLocalNodes(List<LocalRepoDescriptor> repos) {
        List<INode> items = Lists.newArrayListWithCapacity(repos.size());
        repos.forEach(repo -> {
            String repoType = repo.getKey().endsWith("-cache") ? "cached" : "local";
            RepositoryNode itemNodes = new RepositoryNode(repo.getKey(), repo.getType(), repoType);
            items.add(itemNodes);
        });
        return items;
    }

    private List<INode> getRemoteNodes(List<RemoteRepoDescriptor> repos) {
        List<INode> items = Lists.newArrayListWithCapacity(repos.size());
        repos.forEach(repo -> {
            if (repo.isListRemoteFolderItems()) {
                VirtualRemoteRepositoryNode itemNodes = new VirtualRemoteRepositoryNode(repo.getKey(), repo.getType(),
                        "remote", false);
                items.add(itemNodes);
            }
        });
        return items;
    }

    private List<INode> getVirtualNodes(List<VirtualRepoDescriptor> repos) {
        List<INode> items = Lists.newArrayListWithCapacity(repos.size());
        repos.forEach(repo -> {
            VirtualRemoteRepositoryNode itemNodes = new VirtualRemoteRepositoryNode(repo.getKey(), repo.getType(),
                    "virtual", repo.getDefaultDeploymentRepo() != null);
            items.add(itemNodes);
        });
        return items;
    }

    private List<INode> getDistributionNodes(List<DistributionRepoDescriptor> repos) {
        List<INode> items = Lists.newArrayListWithCapacity(repos.size());
        repos.forEach(repo -> items.add(new RepositoryNode(repo.getKey(), repo.getType(), "distribution")));
        return items;
    }

    private void removeNonPermissionRepositories(List<? extends RepoDescriptor> repositories) {
        AuthorizationService authorizationService = ContextHelper.get().getAuthorizationService();
        Iterator<? extends RepoDescriptor> repoDescriptors = repositories.iterator();
        while (repoDescriptors.hasNext()) {
            RepoDescriptor repoDescriptor = repoDescriptors.next();
            if (!authorizationService.userHasPermissionsOnRepositoryRoot(repoDescriptor.getKey())) {
                repoDescriptors.remove();
            }
        }
    }

    private static class RepoComparator implements Comparator<RepoBaseDescriptor> {

        @Override
        public int compare(RepoBaseDescriptor descriptor1, RepoBaseDescriptor descriptor2) {

            //Local repositories can be either ordinary or caches
            if (descriptor1 instanceof LocalRepoDescriptor) {
                boolean repo1IsCache = ((LocalRepoDescriptor) descriptor1).isCache();
                boolean repo2IsCache = ((LocalRepoDescriptor) descriptor2).isCache();

                //Cache repositories should appear in a higher priority
                if (repo1IsCache && !repo2IsCache) {
                    return 1;
                } else if (!repo1IsCache && repo2IsCache) {
                    return -1;
                }
            }
            return descriptor1.getKey().compareTo(descriptor2.getKey());
        }
    }
}
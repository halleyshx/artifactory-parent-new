package org.artifactory.security;

import org.artifactory.api.context.ContextHelper;
import org.artifactory.descriptor.config.MutableCentralConfigDescriptor;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.RepoPathFactory;

import static org.artifactory.repo.config.RepoConfigDefaultValues.EXAMPLE_REPO_KEY;

/**
 * A helper that provides information about Artifactory's state - is it the first time Artifactory is starting
 *
 * @author Yuval Reches
 */
public class FirstStartupHelper {

    private FirstStartupHelper() {
        // Utility class
    }

    public static boolean isThereOnlyEmptyDefaultRepo(MutableCentralConfigDescriptor mutableDescriptor) {
        LocalRepoDescriptor localRepoDescriptor = mutableDescriptor.getLocalRepositoriesMap()
                .get(EXAMPLE_REPO_KEY);
        RepoPath exampleRepoPath = RepoPathFactory.create(EXAMPLE_REPO_KEY, "");
        long exampleRepoArtifactCount =
                ContextHelper.get().getRepositoryService().getArtifactCount(exampleRepoPath);
        int numOfRepos = mutableDescriptor.getLocalRepositoriesMap().size() +
                mutableDescriptor.getRemoteRepositoriesMap().size() +
                mutableDescriptor.getVirtualRepositoriesMap().size();
        return (localRepoDescriptor != null && numOfRepos == 1 && exampleRepoArtifactCount == 0);

    }

}

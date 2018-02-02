package org.artifactory.ui.rest.model.onboarding;

import com.google.common.collect.Lists;
import org.artifactory.descriptor.repo.RepoType;
import org.artifactory.rest.common.model.BaseModel;

import java.util.List;

/**
 * List of requested repo types to create deafult repos for
 * url /api/onboarding/createDefaultRepos
 *
 * @author nadavy
 */
public class CreateDefaultReposModel extends BaseModel {
    private List<RepoType> repoTypeList = Lists.newArrayList();
    private boolean fromOnboarding;

    public CreateDefaultReposModel(List<RepoType> repoTypeList, boolean fromOnboarding) {
        this.repoTypeList = repoTypeList;
        this.fromOnboarding = fromOnboarding;
    }

    public CreateDefaultReposModel() {

    }

    public boolean isFromOnboarding() {
        return fromOnboarding;
    }

    public List<RepoType> getRepoTypeList() {
        return repoTypeList;
    }
}

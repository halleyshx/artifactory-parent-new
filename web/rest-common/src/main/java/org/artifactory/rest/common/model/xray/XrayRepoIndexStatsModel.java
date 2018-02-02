package org.artifactory.rest.common.model.xray;

import org.artifactory.rest.common.model.BaseModel;

/**
 * Model that represent a repository index state. 'started' means artifacts that are tagged with status 'Indexing',
 * 'completed' is the num of artifacts with status 'Indexed', and potential is all the artifacts that can be indexed
 * (@see XrayHandler#extensionsForIndex)
 */
public class XrayRepoIndexStatsModel extends BaseModel {
    private long started = 0;
    private long completed = 0;
    private long potential = 0;

    public XrayRepoIndexStatsModel() {
    }

    public XrayRepoIndexStatsModel(long started, long completed, long potential) {
        this.started = started;
        this.completed = completed;
        this.potential = potential;
    }

    public long getStarted() {
        return started;
    }

    public void setStarted(long started) {
        this.started = started;
    }

    public long getCompleted() {
        return completed;
    }

    public void setCompleted(long completed) {
        this.completed = completed;
    }

    public long getPotential() {
        return potential;
    }

    public void setPotential(long potential) {
        this.potential = potential;
    }
}

package org.artifactory.addon.distribution;

import org.artifactory.addon.Addon;
import org.artifactory.api.bintray.distribution.FileSpec;
import org.artifactory.api.common.BasicStatusHolder;


public interface DistributionAddon extends Addon {

    BasicStatusHolder distributeArtifact(FileSpec fileSpec, String AuthToken);

}

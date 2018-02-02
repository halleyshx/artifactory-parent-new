package org.artifactory.addon.release.bundle;

import org.artifactory.addon.Addon;
import org.artifactory.api.rest.release.ReleaseBundleRequest;
import org.artifactory.api.rest.release.ReleaseBundleResult;

/**
 * @author Shay Bagants
 */
public interface ReleaseBundleAddon extends Addon {

    ReleaseBundleResult handleRequest(ReleaseBundleRequest bundleRequest);

}

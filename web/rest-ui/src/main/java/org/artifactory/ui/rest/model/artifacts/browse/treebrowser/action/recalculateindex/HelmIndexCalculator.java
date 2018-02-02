package org.artifactory.ui.rest.model.artifacts.browse.treebrowser.action.recalculateindex;

import org.artifactory.addon.helm.HelmAddon;
import org.codehaus.jackson.annotate.JsonTypeName;

/**
 * @author Yuval Reches
 */
@JsonTypeName("Helm")
public class HelmIndexCalculator extends BaseIndexCalculator {

    @Override
    public void calculateIndex() throws Exception {
        HelmAddon helmAddon = addonsManager.addonByType(HelmAddon.class);
        if (helmAddon != null) {
            helmAddon.requestReindexRepo(getRepoKey(), true);
        }
    }
}

package org.artifactory.ui.rest.model.artifacts.browse.treebrowser.action.recalculateindex;

import org.artifactory.addon.chef.ChefAddon;
import org.codehaus.jackson.annotate.JsonTypeName;

/**
 * @author Shay Bagants
 */
@JsonTypeName("Chef")
public class ChefIndexCalculator extends BaseIndexCalculator {

    @Override
    public void calculateIndex() throws Exception {
        ChefAddon chefAddon = addonsManager.addonByType(ChefAddon.class);
        if (chefAddon != null) {
            chefAddon.recalculateAll(getRepoKey(), true);
        }
    }
}

package org.artifactory.ui.rest.model.artifacts.browse.treebrowser.action.recalculateindex;

import org.artifactory.addon.puppet.PuppetAddon;
import org.codehaus.jackson.annotate.JsonTypeName;

/**
 * @author Shay Bagants
 */
@JsonTypeName("Puppet")
public class PuppetIndexCalculator extends BaseIndexCalculator {

    @Override
    public void calculateIndex() throws Exception {
        PuppetAddon puppetAddon = addonsManager.addonByType(PuppetAddon.class);
        if (puppetAddon != null) {
            puppetAddon.reindexPuppetRepo(getRepoKey());
        }
    }
}

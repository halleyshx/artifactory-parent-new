package org.artifactory.ui.rest.model.artifacts.browse.treebrowser.action.recalculateindex;

import org.artifactory.addon.composer.ComposerAddon;
import org.codehaus.jackson.annotate.JsonTypeName;

/**
 * @author Shay Bagants
 */
@JsonTypeName("Composer")
public class ComposerIndexCalculator extends BaseIndexCalculator {

    @Override
    public void calculateIndex() throws Exception {
        ComposerAddon composerAddon = addonsManager.addonByType(ComposerAddon.class);
        composerAddon.recalculateAll(getRepoKey(), true);
    }
}

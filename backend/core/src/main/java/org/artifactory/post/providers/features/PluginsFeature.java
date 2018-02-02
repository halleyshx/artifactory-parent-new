package org.artifactory.post.providers.features;

import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.plugin.PluginsAddon;
import org.artifactory.api.callhome.FeatureGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 *
 * This class represent the plugins feature group of the CallHome feature
 *
 * Created by royz on 11/05/2017.
 */
@Component
public class PluginsFeature implements CallHomeFeature {

    @Autowired
    private AddonsManager addonsManager;

    @Override
    public FeatureGroup getFeature() {

        FeatureGroup pluginsFeature = new FeatureGroup("plugins");
        Map<String, String> plugins = addonsManager.addonByType(PluginsAddon.class).getPluginsStatus();
        if (!plugins.isEmpty()) {
            pluginsFeature.addFeatureAttribute("number_of_plugins", plugins.keySet().size());
            addPluginsStatus(pluginsFeature, plugins);
        }
        else
            pluginsFeature.addFeatureAttribute("number_of_plugins", 0);

        return pluginsFeature;
    }

    /**
     * Get plugins' statuses
     *
     * @param pluginsFeature that holds the entire security features
     */
    private void addPluginsStatus(FeatureGroup pluginsFeature, Map<String, String> plugins){

        for (String plugin : plugins.keySet()) {
            pluginsFeature.addFeature(new FeatureGroup(plugin) {{
                addFeatureAttribute("plugin_status", plugins.get(plugin));
            }});
        }
    }
}

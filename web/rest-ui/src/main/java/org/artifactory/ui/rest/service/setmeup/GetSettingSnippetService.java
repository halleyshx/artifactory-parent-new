package org.artifactory.ui.rest.service.setmeup;

import org.apache.commons.lang.StringUtils;
import org.artifactory.addon.filteredresources.FilteredResourcesAddon;
import org.artifactory.factory.InfoFactoryHolder;
import org.artifactory.rest.common.service.RestResponse;
import org.slf4j.Logger;

import java.io.StringReader;

public abstract class GetSettingSnippetService {

    protected abstract Logger getLog();

    /**
     * Replace place holders with values. If we couldn't retrieve the user's password we use the credentials that the user has manually inserted
     */
    String filterResource(RestResponse restResponse, FilteredResourcesAddon filteredResourcesWebAddon, String snippet, String password) {
        try {
            String filtered = filteredResourcesWebAddon.filterResource(null,
                    (org.artifactory.md.Properties) InfoFactoryHolder.get().createProperties(),
                    new StringReader(snippet));
            return addPasswordToSnippet(filtered, password);
        } catch (Exception e) {
            getLog().error("Unable to filter file: " + e.getMessage());
            restResponse.error(e.getMessage());
        }
        return snippet;
    }

    /**
     * If we couldn't retrieve the user's password we use the credentials that the user has manually inserted
     */
    String addPasswordToSnippet(String snippet, String password) {
        if (StringUtils.isBlank(password)) {
            return snippet;
        }
        return snippet.replace("*** Insert encrypted password here ***", password);
    }
}

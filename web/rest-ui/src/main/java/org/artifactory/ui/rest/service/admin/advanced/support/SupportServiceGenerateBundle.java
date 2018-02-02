/*
 *
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2016 JFrog Ltd.
 *
 * Artifactory is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 * Artifactory is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Artifactory.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.artifactory.ui.rest.service.admin.advanced.support;

import com.google.common.collect.Lists;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.support.SupportAddon;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.rest.common.service.ArtifactoryRestRequest;
import org.artifactory.rest.common.service.RestResponse;
import org.artifactory.rest.common.service.RestService;
import org.artifactory.support.config.bundle.BundleConfiguration;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import java.io.File;
import java.util.List;

/**
 * @author Michael Pasternak
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class SupportServiceGenerateBundle<T extends BundleConfigurationWrapper> implements RestService<T> {

    private static final String UI_SUPPORT_DOWNLOAD_BUNDLE = "/ui/userSupport/downloadBundle/";

    @Override
    public void execute(ArtifactoryRestRequest<T> request, RestResponse response) {
        AddonsManager addonsManager = ContextHelper.get().beanForType(AddonsManager.class);
        SupportAddon supportAddon = addonsManager.addonByType(SupportAddon.class);

        if (supportAddon.isSupportAddonEnabled()) {
            BundleConfigurationWrapper bundleConfigurationContainer = request.getImodel();
            List<String> resources = supportAddon.generate(
                    bundleConfigurationContainer.getBundleConfiguration()
            );

            if (resources != null) {
                response.iModel(
                        wrapResponse(
                                resources,
                                bundleConfigurationContainer.getHttpServletRequest()
                        )
                );
            } else {
                response.error("No content was collected, see log for more details");
            }
        }
    }

    /**
     * Wraps generated bundles to relative links
     *
     * @param bundles bundle names
     *
     * @param httpServletRequest
     * @return list of relative paths to download bundle/s
     */
    private List<String> wrapResponse(List<String> bundles, HttpServletRequest httpServletRequest) {
        if (bundles.size() > 0) {
            List<String> links = Lists.newArrayList();
            for(String item : bundles) {
                links.add(httpServletRequest.getContextPath() + UI_SUPPORT_DOWNLOAD_BUNDLE + item);
            }
            return links;
        }
        return bundles;
    }
}

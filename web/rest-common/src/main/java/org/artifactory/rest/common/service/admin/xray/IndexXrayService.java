package org.artifactory.rest.common.service.admin.xray;

import org.apache.commons.lang.StringUtils;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.xray.XrayAddon;
import org.artifactory.rest.common.service.ArtifactoryRestRequest;
import org.artifactory.rest.common.service.RestResponse;
import org.artifactory.rest.common.service.RestService;
import org.artifactory.security.AuthenticationHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * @author Shay Yaakov
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class IndexXrayService implements RestService {
    private static final Logger log = LoggerFactory.getLogger(IndexXrayService.class);

    @Autowired
    private AddonsManager addonsManager;

    @Override
    public void execute(ArtifactoryRestRequest request, RestResponse response) {
        XrayAddon xrayAddon = addonsManager.addonByType(XrayAddon.class);
        String remoteAddress = AuthenticationHelper.getRemoteAddress(AuthenticationHelper.getAuthentication());
        String err = "Failing Xray index request received from " + remoteAddress + ": ";
        if (!xrayAddon.isXrayEnabled()) {
            response.error("Xray configuration does not exist on this Artifactory instance or its disabled")
                    .responseCode(400);
            log.debug(err + " no Xray configuration found or is disabled.");
            return;
        }

        String repos = request.getQueryParamByKey("repos");
        if (StringUtils.isBlank(repos)){
            response.error("No repos were given to index.").responseCode(400);
            log.debug(err + "No repos were given to index.");
            return;
        }
        xrayAddon.indexRepos(Arrays.asList(repos.split("\\s*,\\s*")));
        response.responseCode(202);
    }
}

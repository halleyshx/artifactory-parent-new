package org.artifactory.rest.common.service.admin.xray;

import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.xray.XrayAddon;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.rest.common.service.ArtifactoryRestRequest;
import org.artifactory.rest.common.service.RestResponse;
import org.artifactory.rest.common.service.RestService;
import org.artifactory.security.AuthenticationHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static org.artifactory.addon.xray.XrayAddon.ARTIFACTORY_XRAY_USER;

/**
 * @author Chen Keinan
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class DeleteXrayConfigService implements RestService {
    private static final Logger log = LoggerFactory.getLogger(DeleteXrayConfigService.class);

    @Override
    public void execute(ArtifactoryRestRequest request, RestResponse response) {
        String remoteAddress = AuthenticationHelper.getRemoteAddress(AuthenticationHelper.getAuthentication());
        log.info("Deleting Xray config, request received from instance at: {}", remoteAddress);
        XrayAddon xrayAddon  = ContextHelper.get().beanForType(AddonsManager.class).addonByType(XrayAddon.class);
        // init xray data
        xrayAddon.removeXrayConfig();
        // delete xray user
        xrayAddon.deleteXrayUser(ARTIFACTORY_XRAY_USER);
    }
}

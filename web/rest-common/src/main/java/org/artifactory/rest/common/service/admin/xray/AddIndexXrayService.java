package org.artifactory.rest.common.service.admin.xray;

import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.xray.XrayAddon;
import org.artifactory.addon.xray.XrayRepo;
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

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Chen Keinan
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class AddIndexXrayService implements RestService {
    private static final Logger log = LoggerFactory.getLogger(AddIndexXrayService.class);

    @Override
    public void execute(ArtifactoryRestRequest request, RestResponse response) {
        String remoteAddress = AuthenticationHelper.getRemoteAddress(AuthenticationHelper.getAuthentication());
        log.info("Adding repos to Xray index, request received from instance at: {}", remoteAddress);
        List<XrayRepo> repos = request.getModels();
        log.debug("Requested repos: {}", repos.stream().map(XrayRepo::getName).collect(Collectors.toList()));
        AddonsManager addonsManager = ContextHelper.get().beanForType(AddonsManager.class);
        XrayAddon xrayAddon = addonsManager.addonByType(XrayAddon.class);
        xrayAddon.setXrayEnabledOnRepos(repos, true);
    }
}

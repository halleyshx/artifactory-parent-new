package org.artifactory.rest.common.service.admin.xray;

import org.apache.http.HttpStatus;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.xray.XrayAddon;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.descriptor.config.MutableCentralConfigDescriptor;
import org.artifactory.rest.common.model.xray.XrayConfigModel;
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

import javax.servlet.http.HttpServletResponse;

import static org.artifactory.addon.xray.XrayAddon.ARTIFACTORY_XRAY_USER;

/**
 * @author Chen Keinan
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class CreateXrayConfigService implements RestService {
    private static final Logger log = LoggerFactory.getLogger(CreateXrayConfigService.class);

    @Autowired
    private AddonsManager addonsManager;

    @Autowired
    private CentralConfigService configService;

    @Override
    public void execute(ArtifactoryRestRequest request, RestResponse response) {
        XrayAddon xrayAddon = addonsManager.addonByType(XrayAddon.class);
        if (!addonsManager.xrayTrialSupported()) {
            response.error("Artifactory license does not support xray").responseCode(HttpStatus.SC_FORBIDDEN);
        } else {
            if (!xrayAddon.isXrayConfigExist()) {
                // create xray Config
                createXrayConfig(response, xrayAddon, request);
            } else {
                response.error("Xray config already exists on this instance.").responseCode(HttpServletResponse.SC_CONFLICT);
            }
        }
    }

    private void createXrayConfig(RestResponse response, XrayAddon xrayAddon, ArtifactoryRestRequest request) {
        String remoteAddress = AuthenticationHelper.getRemoteAddress(AuthenticationHelper.getAuthentication());
        log.info("Creating Xray config, request received from instance at: {}", remoteAddress);
        XrayConfigModel xrayConfigModel = (XrayConfigModel) request.getImodel();
        boolean created = createXrayConfig(xrayConfigModel);
        if (created) {
            // create xray user
            String encryptedPass = xrayAddon.createXrayUser();
            // update response
            if (encryptedPass != null) {
                addUserDetailsToResponse(response, encryptedPass);
            }
        } else {
            String err = "Failed to create Xray config.";
            response.error(err).responseCode(HttpServletResponse.SC_BAD_REQUEST);
            log.warn(err);
        }
    }

    private void addUserDetailsToResponse(RestResponse response, String encryptedPass) {
        XrayConfigModel xrayConfigModel = new XrayConfigModel();
        xrayConfigModel.setArtUser(ARTIFACTORY_XRAY_USER);
        xrayConfigModel.setArtPass(encryptedPass);
        response.iModel(xrayConfigModel);
        response.responseCode(HttpServletResponse.SC_CREATED);
    }

    private boolean createXrayConfig(XrayConfigModel xrayModel) {
        if (xrayModel.validate(true)) {
            MutableCentralConfigDescriptor descriptor = configService.getMutableDescriptor();
            log.debug("Creating Xray config for instance {}", xrayModel.getXrayBaseUrl());
            descriptor.setXrayConfig(xrayModel.toDescriptor());
            configService.saveEditedDescriptorAndReload(descriptor);
            return true;
        }
        log.debug("Invalid Xray config model received!");
        return false;
    }
}

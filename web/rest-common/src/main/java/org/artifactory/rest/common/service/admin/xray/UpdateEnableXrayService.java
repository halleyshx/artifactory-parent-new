package org.artifactory.rest.common.service.admin.xray;

import org.artifactory.api.config.CentralConfigService;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.descriptor.config.MutableCentralConfigDescriptor;
import org.artifactory.rest.common.model.xray.XrayEnabledModel;
import org.artifactory.rest.common.service.ArtifactoryRestRequest;
import org.artifactory.rest.common.service.RestResponse;
import org.artifactory.rest.common.service.RestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * @author Nadav Yogev
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class UpdateEnableXrayService implements RestService {

    @Autowired
    private CentralConfigService configService;

    @Override
    public void execute(ArtifactoryRestRequest request, RestResponse response) {
        XrayEnabledModel enable = (XrayEnabledModel) request.getImodel();
        MutableCentralConfigDescriptor mutableDescriptor = configService.getMutableDescriptor();
        if (mutableDescriptor.getXrayConfig() != null) {
            mutableDescriptor.getXrayConfig().setEnabled(enable.isXrayEnabled());
        }
        configService.saveEditedDescriptorAndReload(mutableDescriptor);
    }
}

package org.artifactory.rest.common.service.admin.xray;

import com.google.common.collect.Lists;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.xray.XrayAddon;
import org.artifactory.addon.xray.XrayRepo;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.rest.common.service.ArtifactoryRestRequest;
import org.artifactory.rest.common.service.RestResponse;
import org.artifactory.rest.common.service.RestService;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author Chen Keinan
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class GetXrayConfiguredReposService implements RestService {

    @Override
    public void execute(ArtifactoryRestRequest request, RestResponse response) {
        AddonsManager addonsManager = ContextHelper.get().beanForType(AddonsManager.class);
        XrayAddon xrayAddon = addonsManager.addonByType(XrayAddon.class);
        if (xrayAddon.isXrayEnabled()) {
            // create xray Config
            List<XrayRepo> xrayIndexedAndNonIndexed = xrayAddon.getXrayIndexedAndNonIndexed(true);
            response.iModelList(xrayIndexedAndNonIndexed);
        } else {
            response.iModelList(Lists.newArrayList());
        }
    }
}

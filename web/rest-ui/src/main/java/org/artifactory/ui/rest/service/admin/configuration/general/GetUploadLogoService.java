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

package org.artifactory.ui.rest.service.admin.configuration.general;

import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.rest.common.service.ArtifactoryRestRequest;
import org.artifactory.rest.common.service.RestResponse;
import org.artifactory.rest.common.service.RestService;
import org.artifactory.rest.common.service.StreamRestResponse;
import org.artifactory.ui.rest.model.admin.configuration.generalconfig.LogoFileUpload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * @author Chen keinan
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class GetUploadLogoService implements RestService {

    @Autowired
    CentralConfigService centralConfigService;

    @Override
    public void execute(ArtifactoryRestRequest request, RestResponse response) {
        String logoDir = ContextHelper.get().getArtifactoryHome().getLogoDir().getAbsolutePath();
        // update response with logo file
        updateResponseWithLogoFile("logo", logoDir, response);
    }

    /**
     * update response with logo file
     * @param  name - save logo file list
     * @param logoDir - logo dir
     */
    private void updateResponseWithLogoFile(String name, String logoDir, RestResponse response) {
        File sourceFile = new File(logoDir,name);
        boolean fileExist = sourceFile.canRead();
        if (fileExist) {
            LogoFileUpload fileResponse = new LogoFileUpload();
            fileResponse.setFile(sourceFile);
            ((StreamRestResponse) response).setFile(sourceFile);
            response.iModel(fileResponse);
        } else {
            response.error("no user logo found");
        }
    }
}

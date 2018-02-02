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

package org.artifactory.rest.resource.system;


import org.apache.commons.lang.StringUtils;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.rest.constant.ConfigRestConstants;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.descriptor.config.MutableCentralConfigDescriptor;
import org.artifactory.rest.common.exception.BadRequestException;
import org.artifactory.rest.resource.BaseResource;
import org.artifactory.rest.util.PATCH;
import org.artifactory.util.HttpUtils;
import org.artifactory.util.UrlValidator;
import org.jfrog.common.ConfigNewData;
import org.jfrog.common.MapToDiffConverter;
import org.jfrog.common.YamlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


/**
 * @author freds
 */
public class ConfigResource {
    private static final Logger log = LoggerFactory.getLogger(ConfigResource.class);

    private final CentralConfigService centralConfigService;
    private final HttpServletRequest request;
    private final UrlValidator urlValidator;

    public ConfigResource(CentralConfigService centralConfigService, HttpServletRequest httpServletRequest) {
        this.centralConfigService = centralConfigService;
        this.request = httpServletRequest;
        this.urlValidator = new UrlValidator("http", "https");
    }

    @GET
    @Produces(MediaType.APPLICATION_XML)
    public CentralConfigDescriptor getConfig() {
        return centralConfigService.getDescriptor();
        /*
        File configFile = ArtifactoryHome.getConfigFile();
        try {
            return org.apache.commons.io.Files.readFileToString(configFile, "utf-8");
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        */
    }

    @POST
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.TEXT_PLAIN)
    public String setConfig(String xmlContent) {
        log.debug("Received new configuration data.");
        centralConfigService.setConfigXml(xmlContent);
        CentralConfigDescriptor descriptor = centralConfigService.getDescriptor();
        int x = descriptor.getLocalRepositoriesMap().size();
        int y = descriptor.getRemoteRepositoriesMap().size();
        int z = descriptor.getVirtualRepositoriesMap().size();
        return "Reload of new configuration (" + x + " local repos, " + y + " remote repos, " + z +
                " virtual repos) succeeded";
    }

    @PUT
    @Consumes("application/xml")
    @Path(ConfigRestConstants.REMOTE_REPOSITORIES_PATH)
    public void useRemoteRepositories(String xmlContent) {
        //TODO: [by tc] do
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path(ConfigRestConstants.LOGO_URL_PATH)
    public String logoUrl() {
        String descriptorLogo = centralConfigService.getDescriptor().getLogo();
        if (StringUtils.isNotBlank(descriptorLogo)) {
            return descriptorLogo;
        }

        File logoFile = new File(ContextHelper.get().getArtifactoryHome().getLogoDir(), "logo");
        if (logoFile.exists()) {
            return HttpUtils.getServletContextUrl(request) + "/webapp/logo?" + logoFile.lastModified();
        }

        return null;
    }

    @PUT
    @Consumes({MediaType.TEXT_PLAIN})
    @RolesAllowed(AuthorizationService.ROLE_ADMIN)
    @Path(ConfigRestConstants.URL_BASE_PATH)
    public Response setUrlBase(String urlBase) {
        log.debug("Updating URL base: {}", urlBase);
        final String messageFailed = "Updating URL base has failed.\n";
        final String messageOk = "URL base has been successfully updated to \"%s\".\n";

        validateUrl(urlBase);
        persistUrl(urlBase);

        if(!centralConfigService.getMutableDescriptor().getUrlBase().equals(urlBase))
            return Response.serverError().entity(messageFailed).build();
        return Response.ok().entity(String.format(messageOk, urlBase)).build();
    }

    @PATCH
    @Consumes({BaseResource.MEDIA_TYPE_APPLICATION_YAML})
    @RolesAllowed(AuthorizationService.ROLE_ADMIN)
    public Response patchConfig(String patchYaml) {
        try {
            log.debug("Patch on current config with: {}", patchYaml);
            Map hashMap = YamlUtils.getInstance().readValue(patchYaml, HashMap.class);
            Set<ConfigNewData<?>> newDataDiffs = MapToDiffConverter.mapToListOfChanges(hashMap);
            centralConfigService.mergeAndSaveNewData(newDataDiffs);
            return Response.ok().entity(newDataDiffs.size() + " changes to config merged successfully").build();
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    /**
     * Saves new urlBase in configuration.
     *
     * @param urlBase url to save.
     */
    private void persistUrl(String urlBase) {
        MutableCentralConfigDescriptor descriptor = centralConfigService.getMutableDescriptor();
        descriptor.setUrlBase(urlBase);
        centralConfigService.saveEditedDescriptorAndReload(descriptor);
    }

    /**
     * Validates URL using UrlValidator.
     *
     * @see {@link org.artifactory.util.UrlValidator}
     *
     * @param urlBase url to validate.
     * @throws BadRequestException if URL is in illegal form.
     */
    private void validateUrl(String urlBase) {
        if (!StringUtils.isBlank(urlBase)) { // we allow removing custom urlBase
            try {
                urlValidator.validate(urlBase);
            } catch (UrlValidator.UrlValidationException ex) {
                throw new BadRequestException(ex.getMessage());
            }
        }
    }
}

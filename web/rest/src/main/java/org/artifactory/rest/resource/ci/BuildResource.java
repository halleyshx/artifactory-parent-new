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

package org.artifactory.rest.resource.ci;

import com.google.common.base.Function;
import com.google.common.collect.Sets;
import com.sun.jersey.api.core.ExtendedUriInfo;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.rest.AuthorizationRestException;
import org.artifactory.addon.rest.RestAddon;
import org.artifactory.addon.xray.XrayAddon;
import org.artifactory.api.bintray.BintrayService;
import org.artifactory.api.bintray.distribution.Distribution;
import org.artifactory.api.bintray.distribution.Distributor;
import org.artifactory.api.bintray.distribution.reporting.DistributionReporter;
import org.artifactory.api.build.BuildRunComparators;
import org.artifactory.api.build.BuildService;
import org.artifactory.api.common.BasicStatusHolder;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.repo.exception.ItemNotFoundRuntimeException;
import org.artifactory.api.rest.artifact.PromotionResult;
import org.artifactory.api.rest.build.BuildInfo;
import org.artifactory.api.rest.build.Builds;
import org.artifactory.api.rest.build.BuildsByName;
import org.artifactory.api.rest.constant.BintrayRestConstants;
import org.artifactory.api.rest.constant.BuildRestConstants;
import org.artifactory.api.rest.constant.RestConstants;
import org.artifactory.api.search.SearchService;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.build.BuildRun;
import org.artifactory.build.DetailedBuildRunImpl;
import org.artifactory.build.InternalBuildService;
import org.artifactory.common.ConstantValues;
import org.artifactory.exception.CancelException;
import org.artifactory.rest.common.exception.BadRequestException;
import org.artifactory.rest.common.exception.NotFoundException;
import org.artifactory.rest.common.exception.RestException;
import org.artifactory.rest.common.list.StringList;
import org.artifactory.rest.common.model.distribution.DistributionResponseBuilder;
import org.artifactory.rest.common.util.BintrayRestHelper;
import org.artifactory.rest.common.util.BuildResourceHelper;
import org.artifactory.rest.common.util.RestUtils;
import org.artifactory.rest.util.ResponseUtils;
import org.artifactory.sapi.common.RepositoryRuntimeException;
import org.artifactory.util.DoesNotExistException;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.BuildRetention;
import org.jfrog.build.api.Module;
import org.jfrog.build.api.dependency.BuildPatternArtifacts;
import org.jfrog.build.api.dependency.BuildPatternArtifactsRequest;
import org.jfrog.build.api.release.BintrayUploadInfoOverride;
import org.jfrog.build.api.release.Promotion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URLDecoder;
import java.text.ParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Set;


import static com.google.common.collect.Lists.transform;
import static org.artifactory.rest.common.util.BintrayRestHelper.createAggregatedResponse;
import static org.artifactory.rest.common.util.RestUtils.getServletContextUrl;

/**
 * A resource to manage the build actions
 *
 * @author Noam Y. Tenne
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
@Path(BuildRestConstants.PATH_ROOT)
@RolesAllowed({AuthorizationService.ROLE_ADMIN, AuthorizationService.ROLE_USER})
public class BuildResource {

    private static final Logger log = LoggerFactory.getLogger(BuildResource.class);
    @Autowired
    private AddonsManager addonsManager;
    @Autowired
    private BuildService buildService;
    @Autowired
    private AuthorizationService authorizationService;
    @Autowired
    private SearchService searchService;
    @Autowired
    private BintrayService bintrayService;
    @Autowired
    private Distributor distributor;
    @Context
    private HttpServletRequest request;
    @Context
    private HttpServletResponse response;
    @Context
    private ExtendedUriInfo uriInfo;

    public static final String anonAccessDisabledMsg = "Anonymous access to build info is disabled";
    private final String ERROR_MESSAGE="Errors have occurred while maintaining build retention. Please review the system " +
            "logs for further information.";
    private final String WARN_MESSAGE="Warnings have been produced while " +
            "maintaining build retention. Please review the system logs for further information.";
    /**
     * Assemble all, last created, available builds with the last
     *
     * @return Builds json object
     */
    @GET
    @Produces({BuildRestConstants.MT_BUILDS, MediaType.APPLICATION_JSON})
    public Builds getAllBuilds() throws IOException {
        if (authorizationService.isAnonUserAndAnonBuildInfoAccessDisabled()) {
            throw new AuthorizationRestException(anonAccessDisabledMsg);
        }
        Set<BuildRun> latestBuildsByName = searchService.getLatestBuilds();
        if (!latestBuildsByName.isEmpty()) {
            //Add our builds to the list of build resources
            Builds builds = new Builds();
            builds.slf = RestUtils.getBaseBuildsHref(request);

            for (BuildRun buildRun : latestBuildsByName) {
                String buildHref = RestUtils.getBuildRelativeHref(buildRun.getName());
                builds.builds.add(new Builds.Build(buildHref, buildRun.getStarted()));
            }
            return builds;

        }

        throw new NotFoundException("No builds were found");
    }

    /**
     * Get the build name from the request url and assemble all builds under that name.
     *
     * @return BuildsByName json object
     */
    @GET
    @Path("/{buildName: .+}")
    @Produces({BuildRestConstants.MT_BUILDS_BY_NAME, MediaType.APPLICATION_JSON})
    public BuildsByName getAllSpecificBuilds(@PathParam("buildName") String buildName) throws IOException {
        if (authorizationService.isAnonUserAndAnonBuildInfoAccessDisabled()) {
            throw new AuthorizationRestException(anonAccessDisabledMsg);
        }
        Set<BuildRun> buildsByName;
        try {
            buildsByName = buildService.searchBuildsByName(buildName);
        } catch (RepositoryRuntimeException e) {
            buildsByName = Sets.newHashSet();
        }
        if (!buildsByName.isEmpty()) {
            BuildsByName builds = new BuildsByName();
            builds.slf = RestUtils.getBaseBuildsHref(request) + RestUtils.getBuildRelativeHref(buildName);
            for (BuildRun buildRun : buildsByName) {
                String versionHref = RestUtils.getBuildNumberRelativeHref(buildRun.getNumber());
                builds.buildsNumbers.add(new BuildsByName.Build(versionHref, buildRun.getStarted()));
            }
            return builds;
        }

        throw new NotFoundException(String.format("No build was found for build name: %s", buildName));
    }

    /**
     * Get the build name and number from the request url and send back the exact build for those parameters
     *
     * @return BuildInfo json object
     */
    @GET
    @Path("/{buildName: .+}/{buildNumber: .+}")
    @Produces({BuildRestConstants.MT_BUILD_INFO, BuildRestConstants.MT_BUILDS_DIFF, MediaType.APPLICATION_JSON})
    public Response getBuildInfo(
            @PathParam("buildName") String buildName,
            @PathParam("buildNumber") String buildNumber,
            @QueryParam("started") String buildStarted,
            @QueryParam("diff") String diffNumber) throws IOException {

        if (authorizationService.isAnonUserAndAnonBuildInfoAccessDisabled()) {
            throw new AuthorizationRestException(anonAccessDisabledMsg);
        }
        if (!authorizationService.canDeployToLocalRepository()) {
            throw new AuthorizationRestException();
        }

        Build build = null;
        if (StringUtils.isNotBlank(buildStarted)) {
            BuildRun buildRun = buildService.getBuildRun(buildName, buildNumber, buildStarted);
            if (buildRun != null) {
                build = buildService.getBuild(buildRun);
            }
        } else {
            build = buildService.getLatestBuildByNameAndNumber(buildName, buildNumber);
        }

        if (build == null) {
            String msg = String.format("No build was found for build name: %s, build number: %s %s",
                    buildName, buildNumber,
                    StringUtils.isNotBlank(buildStarted) ? ", build started: " + buildStarted : "");
            throw new NotFoundException(msg);
        }

        if (queryParamsContainKey("diff")) {
            Build secondBuild = buildService.getLatestBuildByNameAndNumber(buildName, diffNumber);
            if (secondBuild == null) {
                throw new NotFoundException(String.format("No build was found for build name: %s , build number: %s ",
                        buildName, diffNumber));
            }
            BuildRun buildRun = buildService.getBuildRun(build.getName(), build.getNumber(), build.getStarted());
            BuildRun secondBuildRun = buildService.getBuildRun(secondBuild.getName(), secondBuild.getNumber(),
                    secondBuild.getStarted());
            Comparator<BuildRun> comparator = BuildRunComparators.getBuildStartDateComparator();
            if (comparator.compare(buildRun, secondBuildRun) < 0) {
                throw new BadRequestException(
                        "Build number should be greater than the build number to compare against.");
            }
            return prepareBuildDiffResponse(build, secondBuild, request);
        } else {
            return prepareGetBuildResponse(build);
        }
    }

    private Response prepareGetBuildResponse(Build build) throws IOException {
        BuildInfo buildInfo = new BuildInfo();
        buildInfo.slf = RestUtils.getBuildInfoHref(request, build.getName(), build.getNumber());
        buildInfo.buildInfo = build;

        return Response.ok(buildInfo).build();
    }

    private Response prepareBuildDiffResponse(Build firstBuild, Build secondBuild, HttpServletRequest request) {
        RestAddon restAddon = addonsManager.addonByType(RestAddon.class);
        return restAddon.getBuildsDiff(firstBuild, secondBuild, request);
    }

    /**
     * Returns the outputs of build matching the request
     *
     * @param buildPatternArtifactsRequests contains build name and build number or keyword
     * @return build outputs (build dependencies and generated artifacts).
     * The returned array will always be the same size as received, returning nulls on non-found builds.
     */
    @POST
    @Path("/patternArtifacts")
    @Consumes({BuildRestConstants.MT_BUILD_PATTERN_ARTIFACTS_REQUEST, RestConstants.MT_LEGACY_ARTIFACTORY_APP,
            MediaType.APPLICATION_JSON})
    @Produces({BuildRestConstants.MT_BUILD_PATTERN_ARTIFACTS_RESULT, MediaType.APPLICATION_JSON})
    public List<BuildPatternArtifacts> getBuildPatternArtifacts(
            final List<BuildPatternArtifactsRequest> buildPatternArtifactsRequests) {

        if (authorizationService.isAnonUserAndAnonBuildInfoAccessDisabled()) {
            throw new AuthorizationRestException(anonAccessDisabledMsg);
        }
        final RestAddon restAddon = addonsManager.addonByType(RestAddon.class);
        final String contextUrl = getServletContextUrl(request);
        return transform(buildPatternArtifactsRequests,
                new Function<BuildPatternArtifactsRequest, BuildPatternArtifacts>() {
                    @Override
                    public BuildPatternArtifacts apply(BuildPatternArtifactsRequest input) {
                        return restAddon.getBuildPatternArtifacts(input, contextUrl);
                    }
                }
        );
    }


    /**
     * Adds the given build information to the DB
     *
     * @param build Build to add
     */
    @PUT
    @Consumes({BuildRestConstants.MT_BUILD_INFO, RestConstants.MT_LEGACY_ARTIFACTORY_APP, MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response addBuild(Build build) throws Exception {
        log.info("Adding build '{} #{}'", build.getName(), build.getNumber());
        if (authorizationService.isAnonUserAndAnonBuildInfoAccessDisabled()) {
            throw new AuthorizationRestException(anonAccessDisabledMsg);
        }
        if (!authorizationService.canDeployToLocalRepository()) {
            throw new AuthorizationRestException();
        }

        String buildName = build.getName();
        String buildNumber = build.getNumber();
        String buildStarted = build.getStarted();

        if (StringUtils.isBlank(buildName) || StringUtils.isBlank(buildNumber) || StringUtils.isBlank(buildStarted)) {
            log.error("Build was not added: Build name, number or start date cannot be empty or null!");
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Build name, number or start date cannot be empty or null!").build();
        }

        try {
            buildService.addBuild(build);
        } catch (CancelException e) {
            if (log.isDebugEnabled()) {
                log.debug("An error occurred while adding the build '" + build.getName() + " #" + build.getNumber() +
                        "'.", e);
            }
            return Response.status(e.getErrorCode()).entity(e.getMessage()).build();
        }
        /* send event to xray interceptor */
        callXrayBuildInterceptor(build);
        log.info("Added build '{} #{}'", build.getName(), build.getNumber());
        // start retention policy if such exists
        BuildRetention retention = build.getBuildRetention();
        return executeRetentionPolicy(build.getName(), retention, false);
    }

    @POST
    @Path("/retention/{buildName: .+}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response retention(BuildRetention retention, @PathParam("buildName") String buildName,
            @QueryParam(BuildRestConstants.PARAM_ASYNC) boolean async) throws Exception {
        log.debug("Build retention endpoint is executed");
        boolean forceAsync = ConstantValues.buildRetentionAlwaysAsync.getBoolean();
        if (authorizationService.isAnonUserAndAnonBuildInfoAccessDisabled()) {
            throw new AuthorizationRestException(anonAccessDisabledMsg);
        }
        if (!authorizationService.canDeployToLocalRepository()) {
            throw new AuthorizationRestException();
        }
        if (retention == null || retention.getCount() == 0) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Max count retention needs to be a positive number")
                    .build();
        }
        return executeRetentionPolicy(buildName, retention,  async || forceAsync);
    }

    private Response executeRetentionPolicy(String buildName, BuildRetention retention, boolean async) {
        if (retention != null) {
            log.info("Start executing Retention Policy", buildName);
            if (async) {
                return executeRetentionPolicyAsync(buildName, retention);
            }
            return executeRetentionPolicySync(buildName, retention);
        }
        return Response.status(HttpStatus.SC_NO_CONTENT).build();
    }

    private Response executeRetentionPolicyAsync(String buildName, BuildRetention retention) {
        doRetentionPolicy(buildName, retention, true);
        log.info("Retention policy for build name: " + buildName + " is scheduled to run");
        return Response.status(HttpStatus.SC_NO_CONTENT).build();
    }

    private Response executeRetentionPolicySync(String buildName, BuildRetention retention) {
        BasicStatusHolder multiStatusHolder = doRetentionPolicy(buildName, retention, false);
        log.info("Retention policy for build name: " + buildName + " has been finished");
        if (multiStatusHolder.hasErrors()) {
            int code = multiStatusHolder.getMostImportantErrorStatusCode().getStatusCode();
            return Response.status(code)
                    .entity(ResponseUtils
                            .getResponseWithStatusCodeErrorAndErrorMassages(multiStatusHolder, ERROR_MESSAGE, code))
                    .build();
        } else if (multiStatusHolder.hasWarnings()) {
            int code = multiStatusHolder.getMostImportantWarningsStatusCode().getStatusCode();
            return Response.status(code)
                    .entity(ResponseUtils
                            .getResponseWithStatusCodeErrorAndErrorMassages(multiStatusHolder, WARN_MESSAGE, code))
                    .build();
        }
        return Response.status(HttpStatus.SC_NO_CONTENT).build();
    }

    private BasicStatusHolder doRetentionPolicy(String buildName, BuildRetention retention, boolean async) {
        RestAddon restAddon = this.addonsManager.addonByType(RestAddon.class);
        BasicStatusHolder multiStatusHolder = new BasicStatusHolder();
        restAddon.discardOldBuilds(buildName, retention, multiStatusHolder, async);
        return multiStatusHolder;
    }

    private void callXrayBuildInterceptor(Build build) {
        AddonsManager addonsManager = ContextHelper.get().beanForType(AddonsManager.class);
        XrayAddon xrayAddon = addonsManager.addonByType(XrayAddon.class);
        xrayAddon.callAddBuildInterceptor(build);
    }

    /**
     * Adds the given module information to an existing build
     *
     * @param buildName   The name of the parent build that should receive the module
     * @param buildNumber The number of the parent build that should receive the module
     * @param modules     Modules to add
     */
    @POST
    @Path("/append/{buildName: .+}/{buildNumber: .+}")
    @Consumes({BuildRestConstants.MT_BUILD_INFO_MODULE, RestConstants.MT_LEGACY_ARTIFACTORY_APP, MediaType.APPLICATION_JSON})
    public void addModule(@PathParam("buildName") String buildName,
            @PathParam("buildNumber") String buildNumber,
            @QueryParam("started") String buildStarted,
            List<Module> modules) throws Exception {
        log.info("Adding module to build '{} #{}'", buildName, buildNumber);
        if (authorizationService.isAnonUserAndAnonBuildInfoAccessDisabled()) {
            throw new AuthorizationRestException(anonAccessDisabledMsg);
        }
        if (!authorizationService.canDeployToLocalRepository()) {
            throw new AuthorizationRestException();
        }

        Build build = null;
        if (StringUtils.isNotBlank(buildStarted)) {
            BuildRun buildRun = buildService.getBuildRun(buildName, buildNumber, buildStarted);
            if (buildRun != null) {
                build = buildService.getBuild(buildRun);
            }
        } else {
            build = buildService.getLatestBuildByNameAndNumber(buildName, buildNumber);
        }

        if (build == null) {
            throw new NotFoundException("No builds were found");
        }

        build.getModules().addAll(modules);

        try {
            ((InternalBuildService) buildService).updateBuild(new DetailedBuildRunImpl(build));
        } catch (CancelException e) {
            if (log.isDebugEnabled()) {
                log.debug("An error occurred while adding a module to the build '" + build.getName() + " #" +
                        build.getNumber() + "'.", e);
            }
            throw new RestException(e.getErrorCode(), e.getMessage());
        }
    }

    /**
     * Promotes a build
     *
     * @param buildName   Name of build to promote
     * @param buildNumber Number of build to promote
     * @param promotion   Promotion settings
     * @return Promotion result
     */
    @POST
    @Path("/promote/{buildName: .+}/{buildNumber: .+}")
    @Consumes({BuildRestConstants.MT_PROMOTION_REQUEST, MediaType.APPLICATION_JSON})
    @Produces({BuildRestConstants.MT_PROMOTION_RESULT, MediaType.APPLICATION_JSON})
    public Response promote(
            @PathParam("buildName") String buildName,
            @PathParam("buildNumber") String buildNumber, Promotion promotion) throws IOException {

        if (authorizationService.isAnonUserAndAnonBuildInfoAccessDisabled()) {
            throw new AuthorizationRestException(anonAccessDisabledMsg);
        }
        RestAddon restAddon = addonsManager.addonByType(RestAddon.class);
        try {
            if (RestUtils.shouldDecodeParams(request)) {
                buildName = URLDecoder.decode(buildName, "UTF-8");
                buildNumber = URLDecoder.decode(buildNumber, "UTF-8");
            }
            PromotionResult promotionResult = restAddon.promoteBuild(buildName, buildNumber, promotion);
            int status = promotion.isFailFast() && promotionResult.errorsOrWarningHaveOccurred() ? HttpStatus.SC_BAD_REQUEST : HttpStatus.SC_OK;
            return Response.status(status).entity(promotionResult).build();
        } catch (IllegalArgumentException | ItemNotFoundRuntimeException iae) {
            throw new BadRequestException(iae.getMessage());
        } catch (DoesNotExistException dnee) {
            throw new NotFoundException(dnee.getMessage());
        } catch (ParseException pe) {
            throw new RestException("Unable to parse given build start date: " + pe.getMessage());
        }
    }

    /**
     * Renames structure, content and properties of build info objects
     *
     * @param to Replacement build name
     */
    @POST
    @Path("/rename/{buildName: .+}")
    public String renameBuild(
            @PathParam("buildName") String buildName,
            @QueryParam("to") String to) throws IOException {

        if (authorizationService.isAnonUserAndAnonBuildInfoAccessDisabled()) {
            throw new AuthorizationRestException(anonAccessDisabledMsg);
        }
        RestAddon restAddon = addonsManager.addonByType(RestAddon.class);
        try {
            String from;
            if (RestUtils.shouldDecodeParams(request)) {
                from = URLDecoder.decode(buildName, "UTF-8");
            } else {
                from = buildName;
            }
            restAddon.renameBuilds(from, to);

            response.setStatus(HttpStatus.SC_OK);

            return String.format("Build renaming of '%s' to '%s' was successfully started.\n", from, to);
        } catch (IllegalArgumentException iae) {
            throw new BadRequestException(iae.getMessage());
        } catch (DoesNotExistException dnne) {
            throw new NotFoundException(dnne.getMessage());
        }
    }

    /**
     * Removes the build with the given name and number
     *
     * @return Status message
     */
    @DELETE
    @Path("/{buildName: .+}")
    public void deleteBuilds(
            @PathParam("buildName") String buildName,
            @QueryParam("artifacts") int artifacts,
            @QueryParam("buildNumbers") StringList buildNumbers,
            @QueryParam("deleteAll") int deleteAll) throws IOException {
        RestAddon restAddon = addonsManager.addonByType(RestAddon.class);
        try {
            if (RestUtils.shouldDecodeParams(request)) {
                buildName = URLDecoder.decode(buildName, "UTF-8");
            }

            restAddon.deleteBuilds(response, buildName, buildNumbers, artifacts, deleteAll);
        } catch (IllegalArgumentException iae) {
            throw new BadRequestException(iae.getMessage());
        } catch (DoesNotExistException dnne) {
            throw new NotFoundException(dnne.getMessage());
        }
        response.flushBuffer();
    }

    /**
     * Pushes a build to Bintray, expects to find the bintrayBuildInfo.json as one of the build's artifacts
     *
     * @param buildName     Name of build to promote
     * @param buildNumber   Number of build to promote
     * @param gpgPassphrase (optional) the Passphrase to use in conjunction with the key stored in Bintray to
     *                      sign the version
     * @return result of the operation
     */
    @POST
    @Path("/pushToBintray/{buildName: .+}/{buildNumber: .+}")
    @Consumes({BuildRestConstants.MT_BINTRAY_DESCRIPTOR_OVERRIDE, MediaType.APPLICATION_JSON})
    @Produces({BintrayRestConstants.MT_BINTRAY_PUSH_RESPONSE, MediaType.APPLICATION_JSON})
    @Deprecated //use distribute
    public Response pushToBintray(@PathParam("buildName") String buildName,
            @PathParam("buildNumber") String buildNumber,
            @QueryParam("gpgPassphrase") @Nullable String gpgPassphrase,
            @QueryParam("gpgSign") @Nullable Boolean gpgSignOverride,
            @Nullable BintrayUploadInfoOverride override) throws IOException {

        BasicStatusHolder status = new BasicStatusHolder();
        String buildId = buildName + " #" + buildNumber;
        if (!BintrayRestHelper.isPushToBintrayAllowed(status, null)) {
            throw new AuthorizationRestException(status.getLastError().getMessage());
        }
        Build build = BuildResourceHelper.getBuild(buildName, buildNumber, status);
        if (!status.isError()) {
            status.merge(bintrayService.pushPromotedBuild(build, gpgPassphrase, gpgSignOverride, override));
        }
        return createAggregatedResponse(status, buildId, false);
    }

    @POST
    @Path("/distribute/{buildName: .+}/{buildNumber: .+}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response distributeBuild(Distribution distribution,
            @PathParam("buildName") String buildName,
            @PathParam("buildNumber") String buildNumber) throws IOException {
        //For validation, build search etc. must use status holder because interfaces are used by Bintray service as well
        BasicStatusHolder preOpStatus = new BasicStatusHolder();
        String buildId = buildName + " #" + buildNumber;
        DistributionReporter status = new DistributionReporter(!distribution.isDryRun());
        if (!BintrayRestHelper.isPushToBintrayAllowed(preOpStatus, distribution.getTargetRepo())) {
            throw new AuthorizationRestException(preOpStatus.getLastError().getMessage());
        }
        Build build = BuildResourceHelper.getBuild(buildName, buildNumber, preOpStatus);
        if (build == null) {
            preOpStatus.error("Invalid build {}:{} given.", HttpStatus.SC_BAD_REQUEST, log);
        } else {
            BuildResourceHelper.populateBuildPaths(build, distribution, status);
        }
        status.merge(preOpStatus);
        String response;
        if (status.isError()) {
            response = DistributionResponseBuilder.writeResponseBody(status, buildId, distribution.isAsync(),
                    distribution.isDryRun());
        } else {
            status = distributor.distribute(distribution);
            response = DistributionResponseBuilder.writeResponseBody(status, buildId, distribution.isAsync(),
                    distribution.isDryRun());
        }
        return Response.status(DistributionResponseBuilder.getResponseCode(status)).entity(response)
                .type(MediaType.APPLICATION_JSON_TYPE).build();
    }

    private boolean queryParamsContainKey(String key) {
        MultivaluedMap<String, String> queryParameters = queryParams();
        return queryParameters.containsKey(key);
    }

    private MultivaluedMap<String, String> queryParams() {
        return uriInfo.getQueryParameters();
    }
}

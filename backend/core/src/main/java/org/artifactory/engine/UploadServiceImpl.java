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

package org.artifactory.engine;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.RestCoreAddon;
import org.artifactory.api.repo.exception.FileExpectedException;
import org.artifactory.api.repo.exception.ItemNotFoundRuntimeException;
import org.artifactory.api.repo.exception.RepoRejectException;
import org.artifactory.api.repo.exception.maven.BadPomException;
import org.artifactory.api.request.ArtifactoryResponse;
import org.artifactory.api.request.UploadService;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.checksum.ChecksumInfo;
import org.artifactory.checksum.ChecksumType;
import org.artifactory.common.ConstantValues;
import org.artifactory.factory.InfoFactoryHolder;
import org.artifactory.fs.MutableFileInfo;
import org.artifactory.fs.RepoResource;
import org.artifactory.fs.StatsInfo;
import org.artifactory.md.MutablePropertiesInfo;
import org.artifactory.md.Properties;
import org.artifactory.md.PropertiesXmlProvider;
import org.artifactory.md.StatsInfoXmlProvider;
import org.artifactory.mime.MavenNaming;
import org.artifactory.mime.NamingUtils;
import org.artifactory.model.common.RepoPathImpl;
import org.artifactory.model.xstream.fs.PropertiesImpl;
import org.artifactory.repo.InternalRepoPathFactory;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.SaveResourceContext;
import org.artifactory.repo.local.ValidDeployPathContext;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.request.ArtifactoryRequest;
import org.artifactory.resource.FileResource;
import org.artifactory.resource.MutableRepoResourceInfo;
import org.artifactory.resource.UnfoundRepoResource;
import org.artifactory.storage.binstore.service.BinaryInfo;
import org.artifactory.storage.binstore.service.BinaryService;
import org.artifactory.storage.fs.service.StatsService;
import org.artifactory.traffic.TrafficService;
import org.artifactory.util.HttpUtils;
import org.artifactory.webapp.servlet.DelayedHttpResponse;
import org.artifactory.webapp.servlet.HttpArtifactoryResponse;
import org.jfrog.storage.binstore.exceptions.BinaryNotFoundException;
import org.jfrog.storage.binstore.exceptions.BinaryRejectedException;
import org.jfrog.storage.binstore.exceptions.BinaryStorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;

import static org.apache.http.HttpStatus.*;
import static org.artifactory.util.HttpUtils.getSha1Checksum;
import static org.artifactory.util.HttpUtils.getSha256Checksum;
import static org.artifactory.util.UploadServiceUtils.*;

/**
 * Handles upload of a single item. The item can be file, directory, properties, checksum data etc.
 * This service validates the request and delegates the actual upload to the repo and repo service.
 *
 * @author Yoav Landman
 */
@Service
public class UploadServiceImpl implements UploadService {
    private static final Logger log = LoggerFactory.getLogger(UploadServiceImpl.class);

    @Autowired
    private AuthorizationService authService;

    @Autowired
    private InternalRepositoryService repoService;

    @Autowired
    @Qualifier("statsServiceImpl")
    private StatsService statsService;

    @Autowired
    private BinaryService binaryService;

    @Autowired
    private BasicAuthenticationEntryPoint authenticationEntryPoint;

    @Autowired
    private AddonsManager addonsManager;

    @Autowired
    private TrafficService trafficService;

    private SuccessfulDeploymentResponseHelper successfulDeploymentResponseHelper =
            new SuccessfulDeploymentResponseHelper();

    @Override
    public void upload(ArtifactoryRequest request, ArtifactoryResponse response) throws IOException, RepoRejectException {
        log.debug("Request: {}", request);
        addonsManager.interceptResponse(response);
        if (responseWasIntercepted(response)) {
            return;
        }
        validateRequestAndUpload(request, response);
    }

    private void validateRequestAndUpload(ArtifactoryRequest request, ArtifactoryResponse response) throws IOException {
        if (isRequestedRepoKeyInvalid(request)) {
            response.sendError(SC_NOT_FOUND, "No target local repository specified in deploy request.", log);
            return;
        }

        LocalRepo targetRepository = getTargetRepository(request, repoService);
        if (isTargetRepositoryInvalid(targetRepository)) {
            sendInvalidTargetRepositoryError(request, response);
            return;
        }

        if (NamingUtils.isProperties(request.getPath())) {
            validateAndUploadProperties(request, response, targetRepository);
            return;
        }

        if (NamingUtils.isStatistics(request.getPath())) {
            validateAndUploadStatistics(request, response, targetRepository);
            return;
        }

        try {
            long contentLength = request.getContentLength();
            RepoPath repoPath = InternalRepoPathFactory.create(targetRepository.getKey(), request.getPath(),
                    request.getRepoPath().isFolder());
            repoService.assertValidDeployPath(new ValidDeployPathContext.Builder(targetRepository, repoPath)
                    .contentLength(contentLength).requestSha1(getSha1Checksum(request))
                    .requestSha2(getSha256Checksum(request)).build());
        } catch (RepoRejectException e) {
            handleInvalidDeployPathError(request, response, e);
            return;
        }

        adjustResponseAndUpload(request, response, targetRepository);
    }

    private void sendInvalidTargetRepositoryError(ArtifactoryRequest request, ArtifactoryResponse response) throws IOException {
        int responseStatus;
        String responseMessage;
        String repoKey = request.getRepoKey();
        if (isVirtualRepoKey(repoKey, repoService)) {
            response.setHeader("Allow", "GET");
            responseStatus = HttpStatus.SC_METHOD_NOT_ALLOWED;
            responseMessage = "No local repository was configured as local deployment " +
                    "repository for the (" + repoKey + ") virtual repository.";
        } else {
            responseStatus = SC_NOT_FOUND;
            responseMessage = "Could not find a local repository named " + repoKey + " to deploy to.";
        }
        response.sendError(responseStatus, responseMessage, log);
    }

    private void adjustResponseAndUpload(ArtifactoryRequest request, ArtifactoryResponse response, LocalRepo targetRepository) throws IOException {
        if (processOriginatedExternally(response)) {
            response = new DelayedHttpResponse((HttpArtifactoryResponse) response);
        }
        try {
            if (request.getRepoPath().isFolder()) {
                createDirectory(request, response);
            } else if (request.isChecksum()) {
                validateAndUploadChecksum(request, response, targetRepository);
            } else if (NamingUtils.isMetadata(request.getPath())) {
                response.sendError(SC_CONFLICT, "Old metadata notation is not supported anymore: " +
                        request.getRepoPath(), log);
            } else {
                uploadArtifact(request, response, targetRepository);
            }
        } catch (RepoRejectException | BinaryRejectedException| BinaryStorageException e) {
            //Catch rejections on save
            response.sendError(e.getErrorCode(), e.getMessage(), log);
            return;
        }
        commitResponseIfDelayed(response);
    }

    private void handleInvalidDeployPathError(ArtifactoryRequest request, ArtifactoryResponse response,
            RepoRejectException rejectionException) throws IOException {
        if (rejectionSignifiesRequiredAuthorization(rejectionException, authService)) {
            consumeRequestBody(request);
            String realmName = authenticationEntryPoint.getRealmName();
            response.sendAuthorizationRequired(rejectionException.getMessage(), realmName);
        } else {
            response.sendError(rejectionException.getErrorCode(), rejectionException.getMessage(), log);
        }
    }

    private void createDirectory(ArtifactoryRequest request, ArtifactoryResponse response) throws IOException {
        RepoPath repoPath = request.getRepoPath();
        log.info("MKDir request to '{}'", request.getRepoPath());

        repoService.mkdirs(repoPath);
        annotateWithRequestPropertiesIfPermitted(request, repoPath);

        sendSuccessfulResponse(request, response, repoPath, true);
        log.info("Successfully created directory '{}'", request.getRepoPath());
    }

    private void annotateWithRequestPropertiesIfPermitted(ArtifactoryRequest request, RepoPath repoPath) {
        if (authService.canAnnotate(repoPath)) {
            Properties properties = request.getProperties();
            repoService.setProperties(repoPath, properties);
        }
    }

    private void validateAndUploadChecksum(ArtifactoryRequest request, ArtifactoryResponse response, LocalRepo repo) throws IOException {
        long length = request.getContentLength();
        if (isAbnormalChecksumContentLength(length)) {
            // something is fishy, checksum file should not be so big...
            response.sendError(SC_CONFLICT, "Suspicious checksum file, content length of " + length +
                    " bytes is bigger than allowed.", log);
            return;
        }
        log.info("Deploy to '{}' Content-Length: {}", request.getRepoPath(), length < 0 ? "unspecified" : length);
        String checksumPath = request.getPath();
        if (NamingUtils.isMetadataChecksum(checksumPath) || MavenNaming.isMavenMetadataChecksum(checksumPath)) {
            //Ignore request - we maintain our self-calculated checksums for metadata
            consumeContentAndRespondWithSuccess(request, response);
            return;
        }
        validatePathAndUploadChecksum(request, response, repo);
    }

    /**
     * @see <a href="http://wiki.jfrog.org/confluence/display/RTF30/Artifactory%27s+REST+API#Artifactory'sRESTAPI-SetItemProperties">Set Item Properties REST API</a>
     * @deprecated can use the set item properties REST API instead, see
     */
    private void validateAndUploadProperties(ArtifactoryRequest request, ArtifactoryResponse response, LocalRepo repo) throws IOException {
        //TORE: [by YS] this doesn't belong here
        long length = request.getContentLength();
        if (isAbnormalPropertiesContentLength(length)) {
            // something is fishy, properties file should not be so big...
            response.sendError(SC_CONFLICT, "Properties content length of " + length +
                    " bytes is bigger than allowed.", log);
            return;
        }
        log.info("Deploy properties to '{}' Content-Length: {}", request.getRepoPath(), length < 0 ? "unspecified" : length);
        String path = request.getPath();
        if (isMavenRepo(repo)) {
            path = adjustMavenSnapshotPath(repo, request);
        }
        RepoPathImpl itemRepoPath = new RepoPathImpl(request.getRepoKey(), NamingUtils.stripMetadataFromPath(path));
        try {
            String propertiesStr = IOUtils.toString(request.getInputStream());
            PropertiesXmlProvider propertiesXmlProvider = new PropertiesXmlProvider();
            MutablePropertiesInfo propertiesInfo = propertiesXmlProvider.fromXml(propertiesStr);
            Properties properties = new PropertiesImpl(propertiesInfo);
            boolean success = repoService.setProperties(itemRepoPath, properties);
            if (success) {
                response.setStatus(SC_CREATED);
            } else {
                response.sendError(SC_NOT_FOUND, "Failed to set properties on " + itemRepoPath, log);
            }
        } catch (Exception e) {
            log.error("Failed to deploy properties to '" + itemRepoPath + "'", e);
            response.sendError(SC_CONFLICT, "Failed to deploy properties : " + e.getMessage() +
                    " on path " + request.getRepoPath(), log);
        }
    }

    private void validateAndUploadStatistics(ArtifactoryRequest request, ArtifactoryResponse response, LocalRepo repo) throws IOException {
        long length = request.getContentLength();
        if (isAbnormalStatisticsContentLength(length)) {
            // something is fishy, statistics file should not be so big...
            response.sendError(SC_CONFLICT, "Statistics content length of " + length +
                    " bytes is bigger than allowed.", log);
            return;
        }
        log.info("Deploy statistics to '{}' Content-Length: {}", request.getRepoPath(), length < 0 ? "unspecified" : length);
        String path = request.getPath();
        if (isMavenRepo(repo)) {
            path = adjustMavenSnapshotPath(repo, request);
        }
        RepoPathImpl itemRepoPath = new RepoPathImpl(request.getRepoKey(), NamingUtils.stripMetadataFromPath(path));
        try {
            String statisticsStr = IOUtils.toString(request.getInputStream());
            StatsInfoXmlProvider statsInfoXmlProvider = new StatsInfoXmlProvider();
            StatsInfo statsInfo = statsInfoXmlProvider.fromXml(statisticsStr);
            boolean success = statsService.setStats(itemRepoPath, statsInfo);
            if (success) {
                response.setStatus(SC_CREATED);
            } else {
                response.sendError(SC_NOT_FOUND, "Failed to set statistics on " + itemRepoPath, log);
            }
        } catch (Exception e) {
            log.debug("Failed to deploy statistics to '" + itemRepoPath + "'", e);
            response.sendError(SC_CONFLICT, "Failed to deploy statistics : " + e.getMessage() +
                    " on path " + request.getRepoPath(), log);
        }
    }

    private void validatePathAndUploadChecksum(ArtifactoryRequest request, ArtifactoryResponse response, LocalRepo repo) throws IOException {
        String uploadedChecksum;
        try {
            uploadedChecksum = getChecksumContentAsString(request);
        } catch (IOException e) {
            response.sendError(SC_CONFLICT, "Failed to read checksum from file: " + e.getMessage() +
                    " for path " + request.getRepoPath(), log);
            return;
        }
        ChecksumType checksumType = ChecksumType.forFilePath(request.getPath());
        RepoPath targetFileRepoPath = adjustAndGetChecksumTargetRepoPath(request, repo);
        try {
            ChecksumInfo checksumInfo = repoService.setClientChecksum(repo, checksumType, targetFileRepoPath, uploadedChecksum);
            if (isChecksumValidAccordingToPolicy(uploadedChecksum, checksumInfo)) {
                sendUploadedChecksumResponse(request, response, targetFileRepoPath);
            } else {
                String message = String.format("Checksum error for '%s': received '%s' but actual is '%s'",
                        request.getPath(), uploadedChecksum, checksumInfo.getActual());
                sendInvalidUploadedChecksumResponse(request, response, repo, targetFileRepoPath, message);
            }
        } catch (ItemNotFoundRuntimeException e) {
            response.sendError(SC_NOT_FOUND, "Target file to set checksum on doesn't exist: " + targetFileRepoPath, log);
        } catch (FileExpectedException e) {
            response.sendError(SC_CONFLICT, "Checksum only supported for files (but found folder): " + targetFileRepoPath, log);
        }
    }

    private void uploadArtifact(ArtifactoryRequest request, ArtifactoryResponse response, LocalRepo repo)
            throws IOException, BinaryStorageException, BinaryRejectedException, RepoRejectException {
        if (isDeployArchiveBundle(request)) {
            RestCoreAddon restCoreAddon = addonsManager.addonByType(RestCoreAddon.class);
            restCoreAddon.deployArchiveBundle(request, response, repo);
            return;
        }
        long length = request.getContentLength();
        log.info("Deploy to '{}' Content-Length: {}", request.getRepoPath(), length < 0 ? "unspecified" : length);
        uploadFile(request, response, repo);
    }

    private void uploadFile(ArtifactoryRequest request, ArtifactoryResponse response, LocalRepo repo)
            throws RepoRejectException, BinaryRejectedException, BinaryStorageException, IOException {
        String path = request.getPath();
        if (isMavenRepo(repo)) {
            if (isRepoSnapshotPolicyNotDeployer(repo) && MavenNaming.isSnapshotMavenMetadata(path)) {
                // Skip the maven metadata deployment - use the metadata calculated after the pom is deployed
                consumeContentAndRespondAccepted(request, response);
                return;
            }
            path = adjustMavenSnapshotPath(repo, request);
        }
        RepoPath fileRepoPath = repo.getRepoPath(path);
        MutableFileInfo fileInfo = InfoFactoryHolder.get().createFileInfo(fileRepoPath);
        boolean isChecksumDeploy = isChecksumDeploy(request);
        setFileInfoChecksums(request, fileInfo, isChecksumDeploy);
        FileResource fileResource = new FileResource(fileInfo);
        uploadItem(request, response, repo, fileResource);
    }

    private void uploadItem(ArtifactoryRequest request, ArtifactoryResponse response, LocalRepo repo, RepoResource res)
            throws IOException, BinaryStorageException, RepoRejectException {
        if (isChecksumDeploy(request)) {
            uploadItemWithReusedContent(request, response, repo, res);
        } else if (ConstantValues.httpUseExpectContinue.getBoolean() && HttpUtils.isExpectedContinue(request)) {
            uploadItemWithReusedOrProvidedContent(request, response, repo, res);
        } else {
            uploadItemWithProvidedContent(request, response, repo, res);
        }
    }

    private void uploadItemWithReusedContent(ArtifactoryRequest request, ArtifactoryResponse response, LocalRepo repo,
            RepoResource res) throws IOException, RepoRejectException {
        String sha1 = getSha1Checksum(request);
        String sha2 = getSha256Checksum(request);
        if (StringUtils.isBlank(sha1) && StringUtils.isBlank(sha2)) {
                response.sendError(SC_NOT_FOUND, "Checksum deploy failed. no checksum header '" +
                        ArtifactoryRequest.CHECKSUM_SHA1 +"/"+ ArtifactoryRequest.CHECKSUM_SHA256 + "' was found.", log);
                return;
        }
        // At this point we verified we have one of them. Prefer sha2 deployment.
        if (StringUtils.isNotBlank(sha2)) {
            doChecksumDeploy(request, response, repo, res, ChecksumType.sha256, sha2);
        } else {
            doChecksumDeploy(request, response, repo, res, ChecksumType.sha1, sha1);
        }
    }

    private void doChecksumDeploy(ArtifactoryRequest request, ArtifactoryResponse response, LocalRepo repo,
            RepoResource res, ChecksumType checksumType, String checksum) throws IOException, RepoRejectException {
        log.debug("Checksum deploy to '{}' with {}: {}", res.getRepoPath(), checksumType.alg(), checksum);
        if (!checksumType.isValid(checksum)) {
            response.sendError(SC_NOT_FOUND, "Checksum deploy failed. Invalid " + checksumType.alg() + ": " + checksum, log);
            return;
        }
        InputStream inputStream = null;
        try {
            BinaryInfo binaryInfo = binaryService.findBinary(checksumType, checksum);
            if (binaryInfo != null) {
                inputStream = binaryService.getBinary(binaryInfo);
            }
            if (inputStream == null) {
                response.sendError(SC_NOT_FOUND, "Checksum deploy failed. No existing file with "
                        + checksumType.alg() + ": " + checksum, log);
            } else {
                uploadItemWithContent(request, response, repo, res, inputStream);
            }
        } catch (BinaryNotFoundException e) {
            response.sendError(SC_NOT_FOUND, "Checksum deploy failed. No existing file with "
                    + checksumType.alg() + ": " + checksum, log);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    private void uploadItemWithReusedOrProvidedContent(ArtifactoryRequest request, ArtifactoryResponse response,
            LocalRepo repo, RepoResource res) throws IOException, RepoRejectException {
        log.debug("Client '{}' supports Expect 100/continue", request.getHeader(HttpHeaders.USER_AGENT));
        String found = getValidChecksumFromRequest(request, res);
        if (found != null) {
            try (InputStream inputStream = binaryService.getBinary(found)) {
                uploadItemWithContent(request, response, repo, res, inputStream);
                return;
            } catch (BinaryNotFoundException e) {
                log.warn("Could not get original stream with {}: {}. Using content provided by request", found, e.getMessage());
            }
        }
        //no such binary as specified by header, continue normal deployment with incoming stream
        uploadItemWithProvidedContent(request, response, repo, res);
    }

    /**
     * Validates the checksum passed is valid and transforms sha1 to sha2 in case X-SHA256 header was passed.
     */
    private String getValidChecksumFromRequest(ArtifactoryRequest request, RepoResource res) {
        String found = null;
        String sha1 = getSha1Checksum(request);
        String sha2 = getSha256Checksum(request);
        // Allow uploads with either X-SHA1 or X-SHA256 headers
        if (StringUtils.isNotBlank(sha1) && ChecksumType.sha1.isValid(sha1)) {
            log.debug("Expect continue deploy to '{}' with SHA1: {}", res.getRepoPath(), sha1);
            found = sha1;
        } else if (StringUtils.isNotBlank(sha2) && ChecksumType.sha256.isValid(sha2)) {
            //TODO [by dan]: change back to sha2-compatible method when binarystore supports retrieve by sha2
            log.debug("Expect continue deploy to '{}' with SHA256: {}", res.getRepoPath(), sha2);
            try {
                BinaryInfo binary = binaryService.findBinary(ChecksumType.sha256, sha2);
                if (binary != null) {
                    found = binary.getSha1();
                } else {
                    log.warn("Cannot find binary with SHA256 '{}'", sha2);
                }
            } catch (Exception e) {
                log.warn("Cannot find binary with SHA256 '{}' : {}", sha2, e.getMessage());
                log.debug("", e);
            }
        }
        return found;
    }

    private void uploadItemWithProvidedContent(ArtifactoryRequest request, ArtifactoryResponse response, LocalRepo repo,
            RepoResource res) throws IOException, RepoRejectException, BinaryRejectedException {
        try (InputStream inputStream = request.getInputStream()) {
            long remoteUploadStartTime = System.currentTimeMillis();
            uploadItemWithContent(request, response, repo, res, inputStream);
            fireUploadTrafficEvent(res, remoteUploadStartTime, trafficService);
        }
    }

    private void uploadItemWithContent(ArtifactoryRequest request, ArtifactoryResponse response, LocalRepo repo,
            RepoResource res, InputStream inputStream) throws RepoRejectException, BinaryRejectedException, IOException {
        //Update the last modified
        long lastModified = request.getLastModified() > 0 ? request.getLastModified() : System.currentTimeMillis();
        ((MutableRepoResourceInfo) res.getInfo()).setLastModified(lastModified);
        RepoPath repoPath = res.getRepoPath();
        Properties properties = populateItemPropertiesFromRequest(request, inputStream, repoPath, authService);
        SaveResourceContext.Builder contextBuilder = new SaveResourceContext.Builder(res, inputStream).properties(properties);
        populateItemInfoFromHeaders(request, res, contextBuilder, authService);
        try {
            RepoResource resource = repoService.saveResource(repo, contextBuilder.build());
            if (!resource.isFound()) {
                response.sendError(SC_NOT_FOUND, ((UnfoundRepoResource) resource).getDetail(), log);
                return;
            }
            sendSuccessfulResponse(request, response, repoPath, false);
        } catch (BadPomException bpe) {
            response.sendError(SC_CONFLICT, bpe.getMessage(), log);
        }
    }

    private void sendSuccessfulResponse(ArtifactoryRequest request, ArtifactoryResponse response, RepoPath repoPath,
            boolean isDirectory) throws IOException {
        String url = buildArtifactUrl(request, repoPath);
        successfulDeploymentResponseHelper.writeSuccessfulDeploymentResponse(repoService, response, repoPath, url,
                isDirectory, includeSha256Value(request));
    }

    /**
     * Due to a bug in all of our ecosystem agents that can't parse extra info in the deployment response we have to
     * filter out the sha256 field out of our responses.
     * When the eco team fixes this we can add a specific version from which we support returning sha256 info back to
     * the clients.
     */
    private boolean includeSha256Value(ArtifactoryRequest request) {
        String userAgent = request.getHeader(HttpHeaders.USER_AGENT);
        //TODO [by dan]: revise this with actual version once eco team fixes on their end.
        return StringUtils.isBlank(userAgent) || !userAgent.contains("ArtifactoryBuildClient/");
    }
}
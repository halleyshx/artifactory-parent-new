package org.artifactory.logging.sumo;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.ha.HaCommonAddon;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.jackson.JacksonReader;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.common.ConstantValues;
import org.artifactory.common.crypto.CryptoHelper;
import org.artifactory.config.CentralConfigKey;
import org.artifactory.config.InternalCentralConfigService;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.descriptor.config.MutableCentralConfigDescriptor;
import org.artifactory.descriptor.repo.ProxyDescriptor;
import org.artifactory.descriptor.sumologic.SumoLogicConfigDescriptor;
import org.artifactory.logging.sumo.logback.SumoLogbackUpdater;
import org.artifactory.logging.sumo.logback.SumoLogbackUpdater.UpdateData;
import org.artifactory.logging.sumologic.SumoLogicException;
import org.artifactory.logging.sumologic.SumoLogicService;
import org.artifactory.spring.Reloadable;
import org.artifactory.util.HttpClientConfigurator;
import org.artifactory.util.MaskedValue;
import org.artifactory.version.CompoundVersionDetails;
import org.codehaus.jackson.JsonNode;
import org.jfrog.client.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * @author Shay Yaakov
 */
@Service
@Reloadable(beanClass = SumoLogicService.class, initAfter = InternalCentralConfigService.class,
        listenOn = { CentralConfigKey.serverName, CentralConfigKey.urlBase, CentralConfigKey.sumoLogicConfig })
public class SumoLogicServiceImpl implements SumoLogicService {
    private static final Logger log = LoggerFactory.getLogger(SumoLogicServiceImpl.class);

    @Autowired
    CentralConfigService centralConfigService;

    @Autowired
    SumoLogicTokenManager tokenManager;

    @Autowired
    SumoLogbackUpdater sumoLogbackUpdater;

    private CloseableHttpClient client;

    @Override
    public void init() {
        SumoLogicConfigDescriptor sumoLogicConfig = centralConfigService.getDescriptor().getSumoLogicConfig();
        createClient(sumoLogicConfig.getProxy());
    }

    @Override
    public void reload(CentralConfigDescriptor oldDescriptor) {
        SumoLogicConfigDescriptor oldSumoConfig = oldDescriptor.getSumoLogicConfig();
        CentralConfigDescriptor newDescriptor = centralConfigService.getDescriptor();
        SumoLogicConfigDescriptor newSumoConfig = newDescriptor.getSumoLogicConfig();

        boolean clientInitialized = (client != null);
        boolean proxyChanged = !EqualsBuilder.reflectionEquals(oldSumoConfig.getProxy(), newSumoConfig.getProxy());

        if (needToUpdateLogbackAppender(oldDescriptor, newDescriptor)) {
            // Update logback.xml with sumo logic appenders
            updateLogbackXml(newSumoConfig);
        }
        if (newSumoConfig.isEnabled() && (!clientInitialized || proxyChanged)) {
            init();
        } else if (!newSumoConfig.isEnabled() && clientInitialized) {
            closeClient();
        }
        boolean credentialsChanged = !StringUtils.equals(newSumoConfig.getClientId(), oldSumoConfig.getClientId()) ||
                !StringUtils.equals(newSumoConfig.getSecret(), oldSumoConfig.getSecret());
        if (credentialsChanged) {
            tokenManager.revokeAllTokens();
        }
    }

    private boolean needToUpdateLogbackAppender(CentralConfigDescriptor oldDescriptor, CentralConfigDescriptor newDescriptor) {
        SumoLogicConfigDescriptor oldSumoConfig = oldDescriptor.getSumoLogicConfig();
        SumoLogicConfigDescriptor newSumoConfig = newDescriptor.getSumoLogicConfig();
        if (oldSumoConfig == null) {
            return newSumoConfig != null && newSumoConfig.isEnabled();
        }
        boolean enabledChanged = oldSumoConfig.isEnabled() != newSumoConfig.isEnabled();
        if (enabledChanged) {
            return true;
        } else if (!newSumoConfig.isEnabled()) {
            return false;
        }
        boolean collectorUrlChanged = !StringUtils.equals(oldSumoConfig.getCollectorUrl(), newSumoConfig.getCollectorUrl());
        boolean proxyChanged = !EqualsBuilder.reflectionEquals(oldSumoConfig.getProxy(), newSumoConfig.getProxy());
        boolean serverNameChanged = !StringUtils.equals(oldDescriptor.getServerName(), newDescriptor.getServerName());
        boolean urlBaseChanged = !StringUtils.equals(oldDescriptor.getUrlBase(), newDescriptor.getUrlBase());
        boolean artifactoryHostChanged = urlBaseChanged || (serverNameChanged && StringUtils.isBlank(newDescriptor.getUrlBase()));
        return collectorUrlChanged || proxyChanged || artifactoryHostChanged;
    }

    @Override
    public String createToken(String username, String code, String baseUri) {
        log.debug("Create token for user {} - code={}, base uri={}", username, MaskedValue.of(code), baseUri);
        checkIntegrationWithSumoEnabled();
        return fetchOrRefreshToken(username, baseUri, () -> buildNewTokenRequest(code));
    }

    @Override
    public void registerApplication() {
        log.debug("Register application");
        checkIntegrationWithSumoEnabled();
        resetApplication(true);
        String url = ConstantValues.sumoLogicApiUrl.getString() + "/partner/oauth/register";
        HttpPost postMethod = new HttpPost(url);
        String jsonRequest = "{\"integration_name\":\"JFrogArtifactory\",\"client_name\":\"Artifactory\"}";
        postMethod.setEntity(new StringEntity(jsonRequest, ContentType.APPLICATION_JSON));
        try(CloseableHttpResponse response = client.execute(postMethod)) {
            int status = response.getStatusLine().getStatusCode();
            if (status == HttpStatus.SC_CREATED) {
                JsonNode jsonResponse = JacksonReader.streamAsTree(response.getEntity().getContent());
                MutableCentralConfigDescriptor mutableDescriptor = centralConfigService.getMutableDescriptor();
                SumoLogicConfigDescriptor sumoLogicConfig = mutableDescriptor.getSumoLogicConfig();
                sumoLogicConfig.setEnabled(true);
                sumoLogicConfig.setClientId(jsonResponse.get("client_id").asText());
                sumoLogicConfig.setSecret(jsonResponse.get("client_secret").asText());
                centralConfigService.saveEditedDescriptorAndReload(mutableDescriptor);
            } else {
                throw newExceptionFromErrorResponse("register application", "POST", url, response);
            }
        } catch (IOException e) {
            log.error("Failed registering Sumo Logic application: " + e.getMessage(), e);
            throw new SumoLogicException("Could not register application, see logs for more details.");
        }
    }

    @Override
    public void resetApplication(boolean enabled) {
        log.debug("Reset application, enabled={}", enabled);
        tokenManager.revokeAllTokens();
        MutableCentralConfigDescriptor mutableDescriptor = centralConfigService.getMutableDescriptor();
        SumoLogicConfigDescriptor sumoLogicConfig = new SumoLogicConfigDescriptor();
        sumoLogicConfig.setEnabled(enabled);
        mutableDescriptor.setSumoLogicConfig(sumoLogicConfig);
        centralConfigService.saveEditedDescriptorAndReload(mutableDescriptor);
    }

    @Override
    public void setupApplication(String username, String baseUri, boolean newSetup) {
        log.debug("Setup application - base uri={}", baseUri);
        checkIntegrationWithSumoEnabled();
        String url = PathUtils.addTrailingSlash(baseUri) + "json/v1/partners/app_integration_setup";
        HttpPost postMethod = new HttpPost(url);
        String request = buildAppIntegrationRequest(newSetup);
        postMethod.setEntity(new StringEntity(request, ContentType.APPLICATION_JSON));
        postMethod.addHeader("Accept", "application/json");
        try(CloseableHttpResponse response = client.execute(postMethod)) {
            int status = response.getStatusLine().getStatusCode();
            if (status == HttpStatus.SC_OK) {
                JsonNode jsonResponse = JacksonReader.streamAsTree(response.getEntity().getContent());
                String collectorUrl = jsonResponse.get("collection_uri").asText();
                String dashboardUrl = jsonResponse.get("dashboard_uri").asText();
                MutableCentralConfigDescriptor mutableDescriptor = centralConfigService.getMutableDescriptor();
                SumoLogicConfigDescriptor sumoLogicConfig = mutableDescriptor.getSumoLogicConfig();
                revokeAllOtherTokensIfConnectionChanged(username, sumoLogicConfig.getCollectorUrl(), collectorUrl);
                sumoLogicConfig.setEnabled(true);
                sumoLogicConfig.setBaseUri(baseUri);
                sumoLogicConfig.setCollectorUrl(collectorUrl);
                sumoLogicConfig.setDashboardUrl(dashboardUrl);
                centralConfigService.saveEditedDescriptorAndReload(mutableDescriptor);
            } else {
                throw newExceptionFromErrorResponse((newSetup ? "new" : "existing") + " application setup", "POST", url, response);
            }
        } catch (IOException e) {
            throw new SumoLogicException("Failed to setup " + (newSetup ? "new" : "existing") +
                    " Sumo Logic application, see logs for more details.", e);
        }
    }

    private void revokeAllOtherTokensIfConnectionChanged(String username, String oldCollectorUrl, String newCollectorUrl) {
        if (!StringUtils.equals(oldCollectorUrl, newCollectorUrl)) {
            log.debug("Revoking all Sumo Logic tokens, except for user {}", username);
            String refreshToken = tokenManager.getRefreshToken(username);
            String accessToken = tokenManager.getAccessToken(username);
            tokenManager.revokeAllTokens();
            tokenManager.updateTokens(username, refreshToken, accessToken);
        }
    }

    private void updateLogbackXml(SumoLogicConfigDescriptor sumoLogicConfig) {
        try {
            File etcDir = ArtifactoryHome.get().getEtcDir();
            File logbackConfigFile = new File(etcDir, ArtifactoryHome.LOGBACK_CONFIG_FILE_NAME);
            UpdateData updateData = new UpdateData(sumoLogicConfig, getArtifactoryHost(), getArtifactoryNode());
            sumoLogbackUpdater.update(logbackConfigFile, updateData);
        } catch (IOException e) {
            log.error("Failed to update logback xml with SumoLogic updated configuration", e);
        }
    }

    private String getArtifactoryHost() {
        String urlBase = centralConfigService.getDescriptor().getUrlBase();
        String host = null;
        if (StringUtils.isNotBlank(urlBase)) {
            try {
                URI uri = new URI(urlBase);
                host = uri.getHost();
                if (uri.getPort() > 0) {
                    host += ":" + uri.getPort();
                }
            } catch (URISyntaxException e) {
                log.debug("Could not extract artifactory host from custom URL base '{}': {}", urlBase, e.toString());
            }
        }
        if (host == null) {
            host = centralConfigService.getServerName();
        }
        return host;
    }

    private String getArtifactoryNode() {
        AddonsManager addonsManager = ContextHelper.get().beanForType(AddonsManager.class);
        HaCommonAddon haCommonAddon = addonsManager.addonByType(HaCommonAddon.class);
        if (haCommonAddon != null) {
            return haCommonAddon.getCurrentMemberServerId();
        }
        return null;
    }

    @Override
    public String refreshToken(String username) {
        log.debug("Refresh token for user {}", username);
        checkIntegrationWithSumoEnabled();
        SumoLogicConfigDescriptor sumoLogicConfig = centralConfigService.getDescriptor().getSumoLogicConfig();
        String refreshToken = tokenManager.getRefreshToken(username);
        String baseUri = sumoLogicConfig.getBaseUri();
        if (StringUtils.isBlank(baseUri)) {
            throw new SumoLogicException("Sumo Logic base URI is not configured, please setup the integration", 400);
        }
        if (StringUtils.isBlank(refreshToken)) {
            throw new SumoLogicException("Refresh token for user " + username + " is missing, please authenticate with Sumo Logic", 400);
        }
        return fetchOrRefreshToken(username, baseUri, () -> buildRefreshTokenRequest(refreshToken));
    }

    @Override
    public String getAccessToken(String username) {
        return tokenManager.getAccessToken(username);
    }

    private String fetchOrRefreshToken(String username, String baseUri, Supplier<String> requestSupplier) {
        log.trace("Fetch or refresh token for user {}", username);
        String url = PathUtils.addTrailingSlash(baseUri) + "oauth/token";
        HttpPost postMethod = new HttpPost(url);
        String request = requestSupplier.get();
        postMethod.setEntity(new StringEntity(request, ContentType.APPLICATION_JSON));
        tokenManager.revokeTokens(username);
        try(CloseableHttpResponse response = client.execute(postMethod)) {
            int status = response.getStatusLine().getStatusCode();
            if (status == HttpStatus.SC_OK) {
                JsonNode jsonResponse = JacksonReader.streamAsTree(response.getEntity().getContent());
                String accessToken = jsonResponse.get("access_token").asText();
                String refreshToken = jsonResponse.get("refresh_token").asText();
                tokenManager.updateTokens(username, refreshToken, accessToken);
                return accessToken;
            } else {
                throw newExceptionFromErrorResponse("token", "POST", url, response);
            }
        } catch (IOException e) {
            throw new SumoLogicException("Failed to fetch token from Sumo Logic for user " + username, e);
        }
    }

    private SumoLogicException newExceptionFromErrorResponse(String action, String method, String url, CloseableHttpResponse response) {
        String responseBody = tryReadResponseBodyAsString(response);
        int status = response.getStatusLine().getStatusCode();
        logResponseInDebug(method, url, status, responseBody);
        String responseMessage = tryExtractErrorMessage(responseBody);
        String message;
        if (StringUtils.isBlank(responseMessage)) {
            message = "Invalid response for " + action + " request: " + status;
        } else {
            message = "Error response for " + action + " request: " + responseMessage;
        }
        return new SumoLogicException(message, status);
    }

    @Nullable
    private String tryReadResponseBodyAsString(CloseableHttpResponse response) {
        if (response != null && response.getEntity() != null) {
            try {
                return IOUtils.toString(response.getEntity().getContent());
            } catch (Exception e) {
                log.warn("Could not read response body: {}", e.getMessage());
                log.debug("Could not read response body.", e);
            }
        }
        return null;
    }

    @Nullable
    private String tryExtractErrorMessage(@Nullable String responseBody) {
        if (StringUtils.isNotBlank(responseBody)) {
            try {
                JsonNode jsonResponse = JacksonReader.bytesAsTree(responseBody.getBytes("utf-8"));
                String message = Optional.ofNullable(jsonResponse.get("error_description")).map(JsonNode::getTextValue).orElse(null);
                if (StringUtils.isBlank(message)) {
                    message = Optional.ofNullable(jsonResponse.get("error")).map(JsonNode::getTextValue).orElse(null);
                }
                return StringUtils.isNotBlank(message) ? message : null;
            } catch (Exception e) {
                log.warn("Could not extract error message from response.");
                log.debug("Could not extract error message from response.", e);
            }
        }
        return null;
    }

    private void logResponseInDebug(String method, String url, int status, String responseBody) {
        if (log.isDebugEnabled()) {
            log.debug("{} {} returned {} with body:\n{}", method, url, status, responseBody);
        }
    }

    private void checkIntegrationWithSumoEnabled() {
        if (!centralConfigService.getDescriptor().getSumoLogicConfig().isEnabled()) {
            throw new SumoLogicException("Integration with Sumo Logic is disabled.", 400);
        }
    }

    private String buildNewTokenRequest(String code) {
        SumoLogicConfigDescriptor sumoLogicConfig = centralConfigService.getDescriptor().getSumoLogicConfig();
        String clientId = CryptoHelper.decryptIfNeeded(ArtifactoryHome.get(), sumoLogicConfig.getClientId());
        String secret = CryptoHelper.decryptIfNeeded(ArtifactoryHome.get(), sumoLogicConfig.getSecret());
        StringBuilder sb = new StringBuilder("{\"grant_type\":\"authorization_code\",");
        sb.append("\"code\":\"").append(code).append("\",");
        sb.append("\"client_id\":\"").append(clientId).append("\",");
        sb.append("\"client_secret\":\"").append(secret).append("\"}");
        log.debug("build new token request: grant_type={}, code={}, client_id={}, client_secret={}",
                "authorization_code", MaskedValue.of(code), MaskedValue.of(clientId), MaskedValue.of(secret));
        return sb.toString();
    }

    private String buildRefreshTokenRequest(String refreshToken) {
        SumoLogicConfigDescriptor sumoLogicConfig = centralConfigService.getDescriptor().getSumoLogicConfig();
        String clientId = CryptoHelper.decryptIfNeeded(ArtifactoryHome.get(), sumoLogicConfig.getClientId());
        String secret = CryptoHelper.decryptIfNeeded(ArtifactoryHome.get(), sumoLogicConfig.getSecret());
        StringBuilder sb = new StringBuilder("{\"grant_type\":\"refresh_token\",");
        sb.append("\"refresh_token\":\"").append(refreshToken).append("\",");
        sb.append("\"client_id\":\"").append(clientId).append("\",");
        sb.append("\"client_secret\":\"").append(secret).append("\"}");
        log.debug("build refresh token request: grant_type={}, refresh_token={}, client_id={}, client_secret={}",
                "refresh_token", MaskedValue.of(refreshToken), MaskedValue.of(clientId), MaskedValue.of(secret));
        return sb.toString();
    }

    private String buildAppIntegrationRequest(boolean newSetup) {
        String requestType = newSetup ? "setup_integration" : "retrieve_endpoints";
        SumoLogicConfigDescriptor sumoLogicConfig = centralConfigService.getDescriptor().getSumoLogicConfig();
        String clientId = CryptoHelper.decryptIfNeeded(ArtifactoryHome.get(), sumoLogicConfig.getClientId());
        String secret = CryptoHelper.decryptIfNeeded(ArtifactoryHome.get(), sumoLogicConfig.getSecret());
        StringBuilder sb = new StringBuilder("{\"request_type\":\"").append(requestType).append("\",");
        sb.append("\"client_id\":\"").append(clientId).append("\",");
        sb.append("\"client_secret\":\"").append(secret).append("\"}");
        log.debug("build app integration request: request_type={}, client_id={}, client_secret={}",
                requestType, MaskedValue.of(clientId), MaskedValue.of(secret));
        return sb.toString();
    }

    private void createClient(ProxyDescriptor proxy) {
        closeClient();
        log.trace("Creating http client");
        this.client = new HttpClientConfigurator()
                .proxy(proxy)
                .registerMBean(SumoLogicServiceImpl.class.getSimpleName())
                .socketTimeout(15000)
                .connectionTimeout(1500)
                .noRetry()
                .build();
    }

    private void closeClient() {
        if (this.client != null) {
            log.trace("Closing http client");
            IOUtils.closeQuietly(this.client);
            this.client = null;
        }
    }

    @Override
    public void destroy() {
        try {
            closeClient();
        } catch (Exception e) {
            log.error("Error while destroying Sumo Logic client.", e);
        }
    }

    @Override
    public void convert(CompoundVersionDetails source, CompoundVersionDetails target) {

    }
}
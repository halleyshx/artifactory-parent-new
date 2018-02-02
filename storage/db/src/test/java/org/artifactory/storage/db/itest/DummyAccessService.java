package org.artifactory.storage.db.itest;

import org.artifactory.api.security.access.CreatedTokenInfo;
import org.artifactory.api.security.access.TokenInfo;
import org.artifactory.api.security.access.TokenSpec;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.sapi.common.ExportSettings;
import org.artifactory.sapi.common.ImportSettings;
import org.artifactory.security.SecurityInfo;
import org.artifactory.security.access.AccessService;
import org.artifactory.version.CompoundVersionDetails;
import org.jfrog.access.client.AccessClient;
import org.jfrog.access.client.token.TokenResponse;
import org.jfrog.access.client.token.TokenVerifyResult;
import org.jfrog.access.common.ServiceId;
import org.jfrog.access.token.JwtAccessToken;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author Yinon Avraham.
 */
public class DummyAccessService implements AccessService {
    
    @Override
    public void initAccessService(boolean doBootstrap) {
        throw unexpectedMethodCallException();
    }

    @Nonnull
    @Override
    public CreatedTokenInfo createToken(@Nonnull TokenSpec tokenSpec) {
        throw unexpectedMethodCallException();
    }

    @Nonnull
    @Override
    public TokenResponse createTokenWithAccessAdminCredentials(@Nonnull String serviceId) {
        return null;
    }

    @Nonnull
    @Override
    public CreatedTokenInfo createNoPermissionToken(@Nonnull TokenSpec tokenSpec, @Nonnull String extraData) {
        throw unexpectedMethodCallException();
    }

    @Nonnull
    @Override
    public CreatedTokenInfo refreshToken(@Nonnull TokenSpec tokenSpec, @Nonnull String tokenValue, @Nonnull String refreshToken) {
        throw unexpectedMethodCallException();
    }

    @Nullable
    @Override
    public String extractSubjectUsername(@Nonnull JwtAccessToken accessToken) {
        throw unexpectedMethodCallException();
    }

    @Nonnull
    @Override
    public Collection<String> extractAppliedGroupNames(@Nonnull JwtAccessToken accessToken) {
        throw unexpectedMethodCallException();
    }

    @Override
    public void revokeToken(@Nonnull String tokenValue) {
        throw unexpectedMethodCallException();
    }

    @Override
    public void revokeTokenById(@Nonnull String tokenId) {
        throw unexpectedMethodCallException();
    }

    @Nonnull
    @Override
    public JwtAccessToken parseToken(@Nonnull String tokenValue) throws IllegalArgumentException {
        throw unexpectedMethodCallException();
    }

    @Override
    public boolean verifyToken(@Nonnull JwtAccessToken accessToken) {
        throw unexpectedMethodCallException();
    }

    @Override
    public TokenVerifyResult verifyAndGetResult(@Nonnull JwtAccessToken accessToken) {
        throw unexpectedMethodCallException();
    }

    @Nonnull
    @Override
    public ServiceId getArtifactoryServiceId() {
        throw unexpectedMethodCallException();
    }

    @Override
    public boolean isTokenAppliesScope(@Nonnull JwtAccessToken accessToken, @Nonnull String requiredScope) {
        throw unexpectedMethodCallException();
    }

    @Override
    public void registerAcceptedScopePattern(@Nonnull Pattern pattern) {
        throw unexpectedMethodCallException();
    }

    @Nonnull
    @Override
    public List<TokenInfo> getTokenInfos() {
        throw unexpectedMethodCallException();
    }

    @Override
    public AccessClient getAccessClient() {
        throw unexpectedMethodCallException();
    }

    @Override
    public void encryptOrDecrypt(boolean encrypt) {
        throw unexpectedMethodCallException();
    }

    @Override
    public void importSecurityEntities(SecurityInfo securityInfo) {
        throw unexpectedMethodCallException();
    }

    @Override
    public void ping() {
        throw unexpectedMethodCallException();
    }

    @Override
    public void afterImport(ImportSettings settings) {
        throw unexpectedMethodCallException();
    }

    @Override
    public void exportTo(ExportSettings settings) {
        throw unexpectedMethodCallException();
    }

    @Override
    public void importFrom(ImportSettings settings) {
        throw unexpectedMethodCallException();
    }

    private RuntimeException unexpectedMethodCallException() {
        return new UnsupportedOperationException("This is a dummy implementation, none of its methods should be called!");
    }

    @Override
    public void init() {

    }

    @Override
    public void reload(CentralConfigDescriptor oldDescriptor) {

    }

    @Override
    public void destroy() {

    }

    @Override
    public void convert(CompoundVersionDetails source, CompoundVersionDetails target) {

    }
}

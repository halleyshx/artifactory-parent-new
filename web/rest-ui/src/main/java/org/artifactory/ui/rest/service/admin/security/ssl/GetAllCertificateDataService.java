package org.artifactory.ui.rest.service.admin.security.ssl;

import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.webstart.ArtifactWebstartAddon;
import org.artifactory.addon.webstart.KeyStoreNotFoundException;
import org.artifactory.rest.common.service.ArtifactoryRestRequest;
import org.artifactory.rest.common.service.RestResponse;
import org.artifactory.rest.common.service.RestService;
import org.artifactory.rest.common.util.RestUtils;
import org.artifactory.ui.rest.model.admin.security.ssl.CertificateBaseDataModel;
import org.jfrog.security.ssl.CertificateHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.List;

/**
 * @author Shay Bagants
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class GetAllCertificateDataService implements RestService {
    private static final Logger log = LoggerFactory.getLogger(GetAllCertificateDataService.class);

    @Autowired
    private AddonsManager addonsManager;

    @Override
    public void execute(ArtifactoryRestRequest request, RestResponse response) {
        List<CertificateBaseDataModel> certificatesInfo = Lists.newArrayList();
        ArtifactWebstartAddon webstartAddon = addonsManager.addonByType(ArtifactWebstartAddon.class);
        KeyStore keyStore = null;
        try {
            log.debug("Attempting to retrieve keystore");
            keyStore = webstartAddon.getExistingKeyStore();
        } catch (KeyStoreNotFoundException e) {
            response.iModelList(certificatesInfo);
            return;
        }
        if (keyStore != null) {
            log.debug("Populating response with keystore results");
            populateCertificatesInformation(certificatesInfo, keyStore);
        }
        response.iModelList(certificatesInfo);
    }

    private void populateCertificatesInformation(List<CertificateBaseDataModel> certificatesInfo, KeyStore keyStore) {
        try {
            Enumeration<String> aliases = keyStore.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                if (StringUtils.isNotBlank(alias) && alias.startsWith(ArtifactWebstartAddon.SSL_CERT_ALIAS_PREFIX)) {
                    Certificate certificate = keyStore.getCertificate(alias);
                    if (certificate != null) {
                        CertificateBaseDataModel certInfo = getCertificateInfo(alias, (X509Certificate) certificate);
                        certificatesInfo.add(certInfo);
                    }
                }
            }
        } catch (KeyStoreException e) {
            log.warn("Unable to find existing certificates");
        }
    }

    private CertificateBaseDataModel getCertificateInfo(String alias, X509Certificate certificate) {
        String issuer = "Unknown";
        String subject = "Unknown";
        String validUntil = "Unknown";
        String fingerPrint = "Unknown";
        try {
            issuer = CertificateHelper.getCertificateIssuerCommonName(certificate);
            subject = CertificateHelper.getCertificateSubjectCommonName(certificate);
            validUntil = RestUtils.toIsoDateString(CertificateHelper.getValidUntil(certificate).getTime());
            fingerPrint = CertificateHelper.getCertificateFingerprint(certificate);
        } catch (CertificateEncodingException e) {
            log.debug("Could not get certificate details. {}", e.getMessage());
        }
        String viewableAlias = alias.substring(ArtifactWebstartAddon.SSL_CERT_ALIAS_PREFIX.length());
        return new CertificateBaseDataModel(viewableAlias,
                subject, issuer, fingerPrint, validUntil);
    }
}

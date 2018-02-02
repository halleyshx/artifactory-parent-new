package org.artifactory.ui.rest.service.admin.configuration.ha.license;

import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.license.AddRemoveLicenseVerificationResult;
import org.artifactory.addon.license.LicenseOperationStatus;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.rest.common.exception.ForbiddenException;
import org.artifactory.rest.common.model.artifactorylicense.AddClusterLicenseModel;
import org.artifactory.rest.common.model.artifactorylicense.BaseLicenseDetails;
import org.artifactory.rest.common.service.ArtifactoryRestRequest;
import org.artifactory.rest.common.service.RestResponse;
import org.artifactory.rest.common.service.RestService;
import org.artifactory.rest.common.util.AolUtils;
import org.artifactory.util.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.artifactory.addon.ArtifactoryRunningMode.HA;

/**
 * Service that adds licenses to the HA cluster license pool
 *
 * @author Shay Bagants
 */
@Component
public class AddClusterLicensesService implements RestService {

    private static final Logger log = LoggerFactory.getLogger(AddClusterLicensesService.class);

    @Autowired
    private AddonsManager addonsManager;

    @Autowired
    private AuthorizationService authService;

    @Override
    public void execute(ArtifactoryRestRequest request, RestResponse response) {
        // Block using on AOL and instances that are not HA configured
        AolUtils.assertNotAol("AddLicenseKeys");
        assertHaConfigured();
        if (addonsManager.isLicenseInstalled() && !authService.isAdmin()) {
            response.responseCode(HttpServletResponse.SC_FORBIDDEN).buildResponse();
            log.debug("Forbidden UI REST call from user " + authService.currentUsername());
            return;
        }

        AddClusterLicenseModel licensesDetails = (AddClusterLicenseModel) request.getImodel();
        if (licensesDetails != null && CollectionUtils.notNullOrEmpty(licensesDetails.getLicenses())) {
            Set<String> licenses = licensesDetails.getLicenses().stream()
                    .map(BaseLicenseDetails::getLicenseKey)
                    .collect(Collectors.toSet());
            LicenseOperationStatus status = addonsManager.addAndActivateLicenses(licenses, true, false);
            updateFeedbackMessage(response, status, licensesDetails.getLicenses().size());
        } else {
            response.error("Invalid input");
        }
    }

    private void updateFeedbackMessage(RestResponse response, LicenseOperationStatus status,
            int numOfLicensesFromUser) {
        // Final message to return
        String message;
        if (status.hasException()) {
            message = "Unable to add Artifactory license(s). See logs for further details.";
            log.error("Unable to add Artifactory license(s)", status.getException().getCause());
            response.error(message);
            return;
        }

        Map<String, AddRemoveLicenseVerificationResult> results = status.getAddRemoveLicenseVerificationResult();
        if (results.size() > 0) {
            // Collect all the license verification results to list of map(licenseKey,verificationStatus)
            List<Map.Entry<String, AddRemoveLicenseVerificationResult>> warnings = results.entrySet().stream()
                    .filter(entry -> !entry.getValue().isValid())
                    .collect(Collectors.toList());

            // If all licenses verification failed, lets return an error
            if (warnings.size() == numOfLicensesFromUser) {
                message = "Unable to add Artifactory license(s). See logs for further details.";
                response.error(message);
                warnings.forEach(warning ->
                        log.warn("Unable to add Artifactory license:\n'{}'.\n {} ", warning.getKey(),
                                warning.getValue().showMassage()));
                return;
            }

            // If partial licenses failed, lets return succeeded with error
            if (CollectionUtils.notNullOrEmpty(warnings)) {
                message = "Succeeded with errors: " + warnings.size() + " out of " + numOfLicensesFromUser +
                        " licenses were not added. See logs for further details.";
                response.info(message).buildResponse();
                warnings.forEach(warning ->
                        log.warn("Unable to add Artifactory license:\n'{}'. {} ", warning.getKey(),
                                warning.getValue().showMassage()));
                return;
            }
        }
        message = "Successfully added " + numOfLicensesFromUser + " license(s).";
        response.info(message).buildResponse();
    }

    /**
     * Ensure that HA configured instances (has ha node props) are not allowed to use this service
     */
    private void assertHaConfigured() {
        if (!addonsManager.getArtifactoryRunningMode().equals(HA)) {
            throw new ForbiddenException("In order to use this function, it is required to configure your Artifactory" +
                    "instance as HA.");
        }
    }
}

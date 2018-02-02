package org.artifactory.rest.common.service.admin.advance.sumologic;

import org.artifactory.logging.sumologic.SumoLogicException;
import org.artifactory.logging.sumologic.SumoLogicService;
import org.artifactory.rest.common.service.ArtifactoryRestRequest;
import org.artifactory.rest.common.service.RestResponse;
import org.artifactory.rest.common.service.RestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * @author Shay Yaakov
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class RegisterSumoLogicApplicationService implements RestService {
    private static final Logger log = LoggerFactory.getLogger(RegisterSumoLogicApplicationService.class);

    @Autowired
    private SumoLogicService sumoLogicService;

    @Override
    public void execute(ArtifactoryRestRequest request, RestResponse response) {
        try {
            sumoLogicService.registerApplication();
            response.info("Successfully registered a Sumo Logic application.");
        } catch (SumoLogicException e) {
            String msg = "Error registering application with Sumo Logic: " + e.getMessage();
            log.error(msg, e);
            response.iModel(msg).responseCode(e.getRelaxedStatus());
        } catch (Exception e) {
            String msg = "Error registering application with Sumo Logic: " + e.getMessage();
            log.error(msg, e);
            response.iModel(msg).responseCode(500);
        }
    }
}

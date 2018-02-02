package org.artifactory.webapp.servlet;

import org.artifactory.spring.ReloadableBean;
import org.springframework.session.MapSessionRepository;

/**
 * A relodable delegator for {@link MapSessionRepository}
 *
 * @author Shay Yaakov
 */
public interface ArtifactorySessionRepository extends ReloadableBean {
}

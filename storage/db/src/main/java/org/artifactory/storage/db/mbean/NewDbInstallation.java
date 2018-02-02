package org.artifactory.storage.db.mbean;

import org.springframework.context.ApplicationEvent;

/**
 * @author Noam Shemesh
 */
public class NewDbInstallation extends ApplicationEvent {
    public NewDbInstallation(Object source) {
        super(source);
    }
}

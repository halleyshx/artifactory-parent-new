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

package org.artifactory.layout;

import org.artifactory.config.ConfigurationChangesInterceptor;
import org.artifactory.config.ConfigurationException;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.descriptor.security.UserLockPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * This interceptor ensure that UserLockPolicy has correct configuration
 *
 * @author Michael Pasternak
 */
@Component
public class UserLockPolicyConfigurationInterceptor implements ConfigurationChangesInterceptor {

    private static final Logger log = LoggerFactory.getLogger(UserLockPolicyConfigurationInterceptor.class);

    @Override
    public void onBeforeSave(CentralConfigDescriptor newDescriptor) {
        UserLockPolicy newUserLockPolicy = newDescriptor.getSecurity().getUserLockPolicy();
        if (newUserLockPolicy.getLoginAttempts() > 100 || newUserLockPolicy.getLoginAttempts() < 1)  {
            log.debug("UserLockPolicy 'loginAttempts' value is illegal, valid range is 1 - 100, while specified {}", newUserLockPolicy.getLoginAttempts());
            throw new ConfigurationException("UserLockPolicy 'loginAttempts' value is illegal, valid range is 1 - 100");
        }
    }

    @Override
    public void onAfterSave(CentralConfigDescriptor newDescriptor, CentralConfigDescriptor oldDescriptor) {

    }
}
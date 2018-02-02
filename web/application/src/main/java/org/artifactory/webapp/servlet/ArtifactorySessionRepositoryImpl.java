/*
 * Copyright 2014-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.artifactory.webapp.servlet;

import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.ha.HaCommonAddon;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.common.ConstantValues;
import org.artifactory.config.CentralConfigKey;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.spring.Reloadable;
import org.artifactory.state.model.ArtifactoryStateManager;
import org.artifactory.version.CompoundVersionDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.session.ExpiringSession;
import org.springframework.session.MapSessionRepository;
import org.springframework.session.SessionRepository;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service(value = "sessionRepository")
@Reloadable(beanClass = ArtifactorySessionRepository.class, initAfter = {ArtifactoryStateManager.class},
        listenOn = CentralConfigKey.none)
public class ArtifactorySessionRepositoryImpl implements ArtifactorySessionRepository, SessionRepository<ExpiringSession> {
    private static final Logger log = LoggerFactory.getLogger(ArtifactorySessionRepositoryImpl.class);

    private MapSessionRepository delegate;

    @Override
    public void init() {
        AddonsManager addonsManager = ContextHelper.get().beanForType(AddonsManager.class);
        Map<String, ExpiringSession> sessionMap = addonsManager.addonByType(HaCommonAddon.class).getSessionMap("sessionMap");
        delegate = new MapSessionRepository(sessionMap);
        long timeoutInMinutes = ConstantValues.uiSessionTimeoutInMinutes.getLong();
        long timeoutInSeconds = TimeUnit.MINUTES.toSeconds(timeoutInMinutes);
        delegate.setDefaultMaxInactiveInterval((int) timeoutInSeconds);
    }

    @Override
    public ExpiringSession createSession() {
        return delegate.createSession();
    }

    @Override
    public void save(ExpiringSession session) {
        delegate.save(session);
    }

    @Override
    public ExpiringSession getSession(String id) {
        try {
            return delegate.getSession(id);
        } catch (RuntimeException e) {
            log.debug("Deleting session after an error in session's deserialization.", e);
            try {
                delete(id);
            } catch (RuntimeException ignored) {
                // this is expected, as if it throws an exception in getSession, it will throw the same exception in remove.
                // the item will be deleted as the serialization of the removed items happens after it was removed from the map
            }
            // if ExpiringSession can't be deserialized a runtime exception will be thrown.
            // (like in case of hazelcast map still saving an older version of the session authentication)
            //TODO [by nadav]: our session shouldn't include serializable object, but rather a JSON string
            if (!e.getMessage().contains("Serialization") && !e.getMessage().contains("serialVersionUID") &&
                    !e.getClass().getName().contains("Serialization")) {
                throw e;
            }
            return null;
        }
    }

    @Override
    public void delete(String id) {
        delegate.delete(id);
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

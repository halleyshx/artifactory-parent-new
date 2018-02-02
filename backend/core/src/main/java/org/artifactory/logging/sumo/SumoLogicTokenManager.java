/*
 *
 * Copyright 2016 JFrog Ltd. All rights reserved.
 * JFROG PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package org.artifactory.logging.sumo;

import org.artifactory.sapi.common.Lock;

/**
 * <p>Created on 19/07/16
 *
 * @author Yinon Avraham
 */
public interface SumoLogicTokenManager {

    String getRefreshToken(String username);

    String getAccessToken(String username);

    @Lock
    void updateTokens(String username, String refreshToken, String accessToken);

    void updateAccessToken(String username, String accessToken);

    @Lock
    void revokeTokens(String username);

    void revokeAccessToken(String username);

    @Lock
    void revokeAllTokens();
}

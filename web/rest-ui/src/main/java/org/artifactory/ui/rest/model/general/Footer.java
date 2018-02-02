/*
 *
 * Copyright 2016 JFrog Ltd. All rights reserved.
 * JFROG PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package org.artifactory.ui.rest.model.general;

/**
 * @author Shay Bagants
 */
public interface Footer {

    String getServerName();

    boolean isUserLogo();

    String getLogoUrl();

    boolean isHaConfigured();
}

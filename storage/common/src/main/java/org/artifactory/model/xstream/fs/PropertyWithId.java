/*
 *
 * Copyright 2016 JFrog Ltd. All rights reserved.
 * JFROG PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package org.artifactory.model.xstream.fs;

import java.io.Serializable;

/**
 * <p>Created on 24/07/16
 *
 * @author Yinon Avraham
 */
public interface PropertyWithId extends Serializable{

    long getPropId();

    String getPropKey();

    String getPropValue();

}

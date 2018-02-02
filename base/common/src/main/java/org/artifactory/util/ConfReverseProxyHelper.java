/*
 * Copyright (c) 2017. JFrog Ltd. All rights reserved. JFROG PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 */

package org.artifactory.util;

import org.artifactory.api.context.ArtifactoryContext;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.descriptor.repo.ReverseProxyDescriptor;
import org.artifactory.descriptor.repo.ReverseProxyMethod;
import org.artifactory.descriptor.repo.WebServerType;

public class ConfReverseProxyHelper {

    public static ReverseProxyDescriptor getReverseProxyDescriptor() {
        ArtifactoryContext artifactoryContext = ContextHelper.get();
        if(artifactoryContext == null){
            return null;
        }
        CentralConfigDescriptor res = artifactoryContext.getCentralConfig().getDescriptor();
        if (res==null) {
            return null;
        }
        return res.getCurrentReverseProxy();
    }

    /**
     * Default Docker repository path prefix - enabled.
     * unless we do have explicit method we will default to REPOPATHPREFIX
     * @return
     */
    public static ReverseProxyMethod getReverseProxyMethod() {
        ReverseProxyMethod defaultMethod = ReverseProxyMethod.SUBDOMAIN;
        ReverseProxyDescriptor currentReverseProxy = getReverseProxyDescriptor();
        if (currentReverseProxy == null) {
            return defaultMethod;
        }
        ReverseProxyMethod res = currentReverseProxy.getDockerReverseProxyMethod();
        if (ReverseProxyMethod.NOVALUE.equals(defaultMethod)) {
            return defaultMethod;
        }
        return res;
    }


    public static  WebServerType getReverseProxyType() {
        WebServerType defaultMethod = WebServerType.DIRECT;
        ReverseProxyDescriptor currentReverseProxy = getReverseProxyDescriptor();
        if (currentReverseProxy == null) {
            return defaultMethod;
        }
        return currentReverseProxy.getWebServerType();
    }

}

/*
 * Copyright (c) 2017. JFrog Ltd. All rights reserved. JFROG PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 */

package org.artifactory.util;

import org.apache.http.conn.util.InetAddressUtils;
import org.artifactory.descriptor.repo.ReverseProxyDescriptor;
import org.artifactory.descriptor.repo.ReverseProxyMethod;
import org.artifactory.descriptor.repo.WebServerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//import javax.ws.rs.core.UriBuilder;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.artifactory.util.ConfReverseProxyHelper.getReverseProxyDescriptor;

/**
 * @author saffih
 */
public class InternalRewrite {
    private static final Logger log = LoggerFactory.getLogger(InternalRewrite.class);
    final static String V2 = "/v2/";

    static String getLastSubdomain(String host) {
        int domainIdx = host.indexOf('.');
        if (domainIdx < 0) return null;
        int tldIdx = host.indexOf('.', domainIdx + 1);
        if (tldIdx > 0) {
            int forthIdx = host.indexOf('.', tldIdx + 1);
            if (forthIdx > 0) { // Either 4th level subdomain or IPv4.
                if (InetAddressUtils.isIPv4Address(host)) {
                    return null;
                }
            }
        }

        return host.substring(0, domainIdx);
    }


    /**
     * Do interanl rewrite of url into Docker v2 repositories
     *  /v2/{repo}/ ==> /api/docker/{repo}/v2/
     *  and for domain XXXX.*.*.* /v2/ ==> /api/docker/XXXX/v2/
     *  The domain rewrite is controled rewrite.config
     */
    public static String getInternalRewrite(HttpServletRequest request) {
        String relPath = getServletRelativePath(request);
        if (relPath == null) {
            return null;
        }
        String serverName = request.getServerName();
        ReverseProxyMethod dockerReverseProxyMethod = ConfReverseProxyHelper.getReverseProxyMethod();
        return getInternalRewrite(
                serverName,
                relPath,
                dockerReverseProxyMethod);
    }


    private static String getServletRelativePath(HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        String artPrefix = request.getContextPath();
        if (!requestURI.startsWith(artPrefix)){
            log.error("Request uri {} does not start with the request context {} !!! ", requestURI, artPrefix);
            return null;
        }
        return requestURI.substring(artPrefix.length());
    }

    /**
     * Calculate rewritten url based on the servername, relative path and rewrite method (provided by a getter )
     * @param serverName
     * @param relPath
     * @param chosenMethod
     * @return
     */
    static String getInternalRewrite(String serverName, String relPath, ReverseProxyMethod chosenMethod) {
        if (!relPath.startsWith(V2)) return null;
        // domain case ?
        String dockerPath = "/api/docker";
        String domainRepo = getLastSubdomain(serverName);
        boolean useSubDomain = (domainRepo!=null);
        boolean fallbackSingleRegistry = true;
        // by default we would like domain based - unless specified otherwith.
        if (chosenMethod!=null){
            // subdomain was not specified
            if (!chosenMethod.equals(ReverseProxyMethod.SUBDOMAIN)) useSubDomain=false;
            // repopath was not specified
            if (!chosenMethod.equals(ReverseProxyMethod.REPOPATHPREFIX)) fallbackSingleRegistry=false;
        }

        if (useSubDomain) {
            return dockerPath + "/" + domainRepo + relPath;
        }
        if (fallbackSingleRegistry) {
            int repoIndexStart = V2.length();
            int repoIndexEnd = relPath.indexOf("/", V2.length());
            if (repoIndexEnd==-1){
                return dockerPath + V2;
            }
            String repoKey = relPath.substring(repoIndexStart, repoIndexEnd);
            return dockerPath + "/" + repoKey + V2 + relPath.substring(repoIndexEnd+1);
        }

        // a method specified without internal rewrite.  we do not change the url. I.E. PORT Based
        return null;
    }

    public static URI rewriteBack(String repoKey, URI uri, Set<Map.Entry<String, List<String>>> headers) {
        ReverseProxyDescriptor currentReverseProxy = getReverseProxyDescriptor();
        if (currentReverseProxy==null) {
            // maintain old behaviour - until we support first use etc.
            return uri;
        }
        WebServerType proxy = ConfReverseProxyHelper.getReverseProxyType();
        return rewriteBack(repoKey, uri, proxy, headers);
    }


    static URI rewriteBack(String repoKey, URI uri, WebServerType proxy, Set<Map.Entry<String, List<String>>> headers) {
        ReverseProxyMethod reverseProxyMethod = ConfReverseProxyHelper.getReverseProxyMethod();
        return rewriteBack(repoKey, uri, proxy, reverseProxyMethod, headers);
    }

    static URI rewriteBack(String repoKey, URI uri, WebServerType proxy, ReverseProxyMethod reverseProxyMethod, Set<Map.Entry<String, List<String>>> headers) {
        String path = uri.getPath();
        // insert repo key into path.
        if (ReverseProxyMethod.REPOPATHPREFIX.equals(reverseProxyMethod)){
            if (path.startsWith(V2)){
                path = path.substring(V2.length());
            }else if (path.startsWith("v2/")){
                path = path.substring("v2/".length());
            }
            path = "/v2/"+repoKey+"/"+path;
        }
        // unless specified otherwise
        String scheme = "https";

        /**
         * Our default snippet provides this header with value of scheme (if the value was not already set)
         */
        String key = "x-forwarded-proto";
        String schemToUse = getSingleHeaderValue(headers, key);
        if (schemToUse==null){
            // This code would make NginX http settings fail and Https with Tomcat fail as well.
            // Only in DIRECT mode either keep the original
            if (WebServerType.DIRECT.equals(proxy)){
                scheme = "http";
                log.debug(" No  X-Forwarded-Proto using http schema");
            } else {
                // the default docker impl without the header was https
                log.debug(" No  X-Forwarded-Proto using https schema ");
                // should be changed to http with Nginx
                // scheme = "http";
            }
        }else{
            scheme = schemToUse;
        }

        // support port header
        String port = null;
        if (uri.getPort()>0){
            port=String.valueOf(uri.getPort());
        }


        // return UriBuilder.fromPath(path).scheme(scheme).host(uri.getHost()).port(port).build()
        try {
            URI constructedUri;
            if(port==null){
                constructedUri = new URI(scheme, uri.getHost(), path, null);
            } else {
                constructedUri = new URI(scheme, null, uri.getHost(), Integer.valueOf(port), path, null, null);
            }
            // existing code does not rewrites the base url in the docker repo handler.
            // it was not included. baseUrlOverrideKey = "x-artifactory-override-base-url";
            if (!constructedUri.equals(uri)){
                log.info("rewriteBack: {} to {} ",uri, constructedUri);
            }
            return constructedUri;
        } catch (URISyntaxException e) {
            log.warn("can't create URI ",e);
            return uri;
        }
    }

    /**
     * get single value header
     *
     * @param headers
     * @param key
     * @return
     */
    private static String getSingleHeaderValue(Set<Map.Entry<String, List<String>>> headers, String key) {
        String res = null;

        List<String> valueToUse = headers.stream().filter(entry -> entry.getKey().equalsIgnoreCase(key)).map(entry -> entry.getValue().get(0)).collect(Collectors.toList());
        if (!valueToUse.isEmpty()){
            res = valueToUse.get(0);
            if (valueToUse.size()>1){
                log.warn("multiple value header for {} : {} ", key, valueToUse);
            }
        }
        return res;
    }
}

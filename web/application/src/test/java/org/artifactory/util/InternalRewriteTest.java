/*
 * Copyright (c) 2017. JFrog Ltd. All rights reserved. JFROG PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 */

package org.artifactory.util;

import org.artifactory.descriptor.repo.ReverseProxyMethod;
import org.artifactory.descriptor.repo.WebServerType;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.artifactory.util.InternalRewrite.rewriteBack;
import static org.testng.Assert.*;

/**
 * @author saffih
 */
public class InternalRewriteTest {
    @Test
    public void testGetLastSubdomain() throws Exception {
         Assert.assertEquals(InternalRewrite.getLastSubdomain("registry.artifactory.jfrog.com"), "registry");
        assertEquals(InternalRewrite.getLastSubdomain("artifactory.jfrog.com"), "artifactory");
        assertEquals(InternalRewrite.getLastSubdomain("jfrog.com"), "jfrog");
        assertEquals(InternalRewrite.getLastSubdomain("com"), null);
        assertEquals(InternalRewrite.getLastSubdomain("127.0.0.1"), null);
        assertEquals(InternalRewrite.getLastSubdomain("muchtoomuch.toomany.host.sub.second.top.root"), "muchtoomuch");
        assertEquals(InternalRewrite.getLastSubdomain("toomany.host.sub.second.top.root"), "toomany");
        assertEquals(InternalRewrite.getLastSubdomain("host.sub.second.top.root"), "host");
        assertEquals(InternalRewrite.getLastSubdomain("host.sub.second.בעברית.root"), "host");
        assertEquals(InternalRewrite.getLastSubdomain("sub.second.top.root"), "sub");
        assertEquals(InternalRewrite.getLastSubdomain("second.top.root"), "second");
        assertEquals(InternalRewrite.getLastSubdomain("top.root"), "top");
        assertEquals(InternalRewrite.getLastSubdomain("root"), null);

    }

    @Test
    public void testUnspecifiedDockerInternalRewrite() throws Exception {
        assertEquals(InternalRewrite.getInternalRewrite(
                "registry.artifactory.jfrog.com",
                "/v2/ubuntu",
                null),
                "/api/docker/registry/v2/ubuntu");

        assertEquals(InternalRewrite.getInternalRewrite(
                "localhost",
                "/v2/registry/ubuntu",
                null),
                "/api/docker/registry/v2/ubuntu");
    }

    @Test
    void testRepoPathPrefixMethod() {
        ReverseProxyMethod method = ReverseProxyMethod.REPOPATHPREFIX;
        assertEquals(InternalRewrite.getInternalRewrite(
                "registry.artifactory.jfrog.com",
                "/api/docker/registry/v2/ubuntu",
                method),
                null);

        assertEquals(InternalRewrite.getInternalRewrite(
                "localhost",
                "/v2/registry/ubuntu",
                method),
                "/api/docker/registry/v2/ubuntu");

        assertEquals(InternalRewrite.getInternalRewrite(
                "localhost",
                "/v2/registry//ubuntu",
                method),
                "/api/docker/registry/v2//ubuntu");

        // how should we behave ?
        //assertEquals(InternalRewrite.getInternalRewrite(
        //        "localhost",
        //        "/v2//registry/ubuntu",
        //        method),
        //        "/api/docker//v2/registry/ubuntu");


    }

    @Test void testSubdomainRewrite() {
        ReverseProxyMethod method = ReverseProxyMethod.SUBDOMAIN;
        assertEquals(InternalRewrite.getInternalRewrite(
                "registry.artifactory.jfrog.com",
                "/v2/ubuntu",
                method),
                "/api/docker/registry/v2/ubuntu");

        assertEquals(InternalRewrite.getInternalRewrite(
                "localhost",
                "/v2/registry/ubuntu",
                method),
                null);

        assertEquals(InternalRewrite.getInternalRewrite(
                "registry.artifactory.jfrog.com",
                "/v2//// no matter what ////",
                method),
                "/api/docker/registry/v2//// no matter what ////");
    }

    @Test void testRejectedRewrite() {
        ReverseProxyMethod method = ReverseProxyMethod.SUBDOMAIN;
        assertEquals(InternalRewrite.getInternalRewrite(
                "registry.artifactory.jfrog.com",
                "//v2/ubuntu",
                method),
                null);


        assertEquals(InternalRewrite.getInternalRewrite(
                "registry.artifactory.jfrog.com",
                "/v2",
                method),
                null);

        assertEquals(InternalRewrite.getInternalRewrite(
                "registry.artifactory.jfrog.com",
                "/v",
                method),
                null);

        assertEquals(InternalRewrite.getInternalRewrite(
                "registry.artifactory.jfrog.com",
                "/v",
                method),
                null);
        assertEquals(InternalRewrite.getInternalRewrite(
                "registry.artifactory.jfrog.com",
                "/",
                method),
                null);
        assertEquals(InternalRewrite.getInternalRewrite(
                "registry.artifactory.jfrog.com",
                "",
                method),
                null);
        assertEquals(InternalRewrite.getInternalRewrite(
                "registry.artifactory.jfrog.com",
                " a%@r4ljeklhfakjlsh123dsjajdas a  jfdkjfkjadfsחלחלגגדדג ",
                method),
                null);
        assertEquals(InternalRewrite.getInternalRewrite(
                "registry.artifactory.jfrog.com",
                "/ v2 /v2/",
                method),
                null);

    }

    // explicit contruct.
    @Test void backRewriteCheckNginxSubDomain() throws URISyntaxException {

        URI uri = new URI("https", null,"me.art.com", 8081, "/v2/part1/image", null, null);
        URI uriHttp = new URI("http", null,"me.art.com", 8081, "/v2/part1/image", null, null);
        {
            String scheme = "https";
            HashMap<String, List<String>> m = schemeHeaders(scheme);
            URI ret = rewriteBack("key", uri, WebServerType.NGINX, ReverseProxyMethod.SUBDOMAIN, m.entrySet());
            assertEquals(uri, ret);
        }
        {
            String scheme = "http";
            HashMap<String, List<String>> m = schemeHeaders(scheme);
            URI ret = rewriteBack("key", uri, WebServerType.NGINX, ReverseProxyMethod.SUBDOMAIN, m.entrySet());
            assertEquals(scheme, ret.getScheme());
            assertEquals(uriHttp, ret);
        }
        // same as prev behaviour
        {
            HashMap<String, List<String>> m = headers(new HashMap<>());
            URI ret = rewriteBack("key", uri, WebServerType.NGINX, ReverseProxyMethod.SUBDOMAIN, m.entrySet());
            assertEquals(uri, ret);
        }
    }

    @Test void backRewriteCheckNginxPortShouldNotChange() throws URISyntaxException {
        URI uri = new URI("https", null,"me.art.com", 5555, "/v2/part1/image", null, null);
        URI uriHttp = new URI("http", null,"me.art.com", 5555, "/v2/part1/image", null, null);
        {
            String scheme = "https";
            HashMap<String, List<String>> m = schemeHeaders(scheme);
            URI ret = rewriteBack("key", uri, WebServerType.NGINX, ReverseProxyMethod.PORTPERREPO, m.entrySet());
            assertEquals(uri, ret);
        }
        {
            String scheme = "http";
            HashMap<String, List<String>> m = schemeHeaders(scheme);
            URI ret = rewriteBack("key", uri, WebServerType.NGINX, ReverseProxyMethod.PORTPERREPO, m.entrySet());
            assertEquals(scheme, ret.getScheme());
            assertEquals(uriHttp, ret);
        }
        {
            HashMap<String, List<String>> m = headers(new HashMap<>());
            URI ret = rewriteBack("key", uri, WebServerType.NGINX, ReverseProxyMethod.PORTPERREPO, m.entrySet());
            assertEquals(uri, ret);
        }

    }

    @Test void backRewriteCheckNginxPath() throws URISyntaxException {
        URI uri = new URI("https", null,"me.art.com", 8081, "/v2/part1/image", null, null);
        URI uriHttp = new URI("http", null,"me.art.com", 8081, "/v2/part1/image", null, null);
        {
            String scheme = "https";
            HashMap<String, List<String>> m = schemeHeaders(scheme);
            URI ret = rewriteBack("key", uri, WebServerType.NGINX, ReverseProxyMethod.PORTPERREPO, m.entrySet());
            assertEquals(uri, ret);
        }
        {
            String scheme = "http";
            HashMap<String, List<String>> m = schemeHeaders(scheme);
            URI ret = rewriteBack("key", uri, WebServerType.NGINX, ReverseProxyMethod.PORTPERREPO, m.entrySet());
            assertEquals(scheme, ret.getScheme());
        }
        {
            HashMap<String, List<String>> m = headers(new HashMap<>());
            URI ret = rewriteBack("key", uri, WebServerType.NGINX, ReverseProxyMethod.PORTPERREPO, m.entrySet());
            assertEquals(uri, ret);
        }
    }

    @Test void backRewriteCheckTomcatSubDomain() throws URISyntaxException {

        String repoKey = "key";
        URI uri = new URI("https", null,"me.art.com", 8081, "/v2/part1/image", null, null);
        URI uriHttp = new URI("http", null,"me.art.com", 8081, "/v2/part1/image", null, null);
        {
            String scheme = "https";
            HashMap<String, List<String>> m = schemeHeaders(scheme);
            URI ret = rewriteBack(repoKey, uri, WebServerType.DIRECT, ReverseProxyMethod.SUBDOMAIN, m.entrySet());
            assertEquals(uri, ret);
        }
        {
            String scheme = "http";
            HashMap<String, List<String>> m = schemeHeaders(scheme);
            URI ret = rewriteBack(repoKey, uri, WebServerType.DIRECT, ReverseProxyMethod.SUBDOMAIN, m.entrySet());
            assertEquals(scheme, ret.getScheme());
            assertEquals(uriHttp, ret);
        }
        {   // NO proxy - use http
            HashMap<String, List<String>> m = headers(new HashMap<>());
            URI ret = rewriteBack(repoKey, uri, WebServerType.DIRECT, ReverseProxyMethod.SUBDOMAIN, m.entrySet());
            assertEquals(uriHttp, ret);
        }
    }


    @Test void backRewriteCheckTomcatPath() throws URISyntaxException {
        String repoKey = "key";
        URI uri = new URI("https", null,"me.art.com", 8081, "/v2/part1/image", null, null);
        URI uriHttp = new URI("http", null,"me.art.com", 8081, "/v2/part1/image", null, null);
        URI uriGoal = new URI("https", null,"me.art.com", 8081, "/v2/"+repoKey+"/part1/image", null, null);
        URI uriHttpGoal = new URI("http", null,"me.art.com", 8081, "/v2/"+repoKey+"/part1/image", null, null);
        {
            String scheme = "https";
            HashMap<String, List<String>> m = schemeHeaders(scheme);
            URI ret = rewriteBack(repoKey, uri, WebServerType.DIRECT, ReverseProxyMethod.REPOPATHPREFIX, m.entrySet());
            Assert.assertEquals(uriGoal, ret);
        }
        {
            String scheme = "http";
            HashMap<String, List<String>> m = schemeHeaders(scheme);
            URI ret = rewriteBack(repoKey, uri, WebServerType.DIRECT, ReverseProxyMethod.REPOPATHPREFIX, m.entrySet());
            assertEquals(scheme, ret.getScheme());
            assertEquals(uriHttpGoal, ret);
        }
        {   // NO proxy - use http
            HashMap<String, List<String>> m = headers(new HashMap<>());
            URI ret = rewriteBack(repoKey, uri, WebServerType.DIRECT, ReverseProxyMethod.REPOPATHPREFIX, m.entrySet());
            assertEquals(uriHttpGoal, ret);
        }
    }


// "test/blobs/uploads/ea25b53d-738a-4c05-af66-b204680da4a0"

    private HashMap<String, List<String>> schemeHeaders(String scheme) {

        HashMap<String, String> m = new HashMap<String, String>();
        m.put("X-ForWarded-Proto", scheme);
        return headers(m);
    }
    private HashMap<String, List<String>> headers(Map<String, String> m) {
        HashMap<String, List<String>> res = new HashMap<>();
        m.entrySet().stream().forEach(it->{res.put(it.getKey(), Collections.singletonList(it.getValue()));});
        return res;
    }

}
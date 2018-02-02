/*
 *
 * Copyright 2016 JFrog Ltd. All rights reserved.
 * JFROG PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package org.artifactory.logging.sumologic;

import com.google.common.collect.Sets;

import java.util.Set;

/**
 * @author Shay Yaakov
 */
public class DashboardDetails {

    private String url;
    private Set<String> cookies = Sets.newHashSet();

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Set<String> getCookies() {
        return cookies;
    }

    public void addCookie(String cookie) {
        cookies.add(cookie);
    }
}

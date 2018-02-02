/*
 *
 * Copyright 2016 JFrog Ltd. All rights reserved.
 * JFROG PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package org.artifactory.request;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpHeaders;
import org.jfrog.client.util.PathUtils;

/**
 * Class that helps to identify if the request came from a browser.
 * Used when we want to return a content-type that the browser can identify and display it instead of promoting the user
 * to download it (e.g., instead of 'application/x-maven-pom+xml', for browsers, return 'application/xml')
 *
 * @author Shay Bagants
 */
public class BrowserRequestHelper {

    private BrowserRequestHelper() {
    }

    public static boolean isBrowserRequest(Request request) {
        if (request != null) {
            String agent = request.getHeader(HttpHeaders.USER_AGENT);
            //Today, almost any browser 'User-Agent' header starts with "Mozilla"
            String mozilla = "mozilla";
            return agent != null && agent.toLowerCase().startsWith(mozilla);
        }
        return false;
    }

    /**
     * Get the mimetype for browser requests only.
     * Use in browser requests only, The motivation to use this is when you need the generic subtype of the file, so
     * you can later suggest the browser to display it instead of promoting the user to save it.
     * (e.g. instead of return 'application/x-maven-pom+xml' for a pom file, return 'application/xml')
     *
     * @param filePath The artifact path
     * @return The MimeType of the artifact
     */
    public static String getMimeType(String filePath) {
        String xml = "application/xml";
        String text = "text/plain";
        String extension = PathUtils.getExtension(filePath);
        if (StringUtils.isNotBlank(extension)) {
            switch (extension) {
                case "pom":
                    return xml;
                case "md5":
                case "sha1":
                case "md":
                    return text;
            }
        }
        return null;
    }
}

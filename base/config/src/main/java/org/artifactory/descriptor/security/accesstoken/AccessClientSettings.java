/*
 *
 * Copyright 2016 JFrog Ltd. All rights reserved.
 * JFROG PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package org.artifactory.descriptor.security.accesstoken;

/**
 * @author Yinon Avraham
 */

import org.artifactory.descriptor.Descriptor;
import org.jfrog.common.GenerateDiffFunction;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "AccessClientSettingsType",
        propOrder = {
                "serverUrl",
                "adminToken",
                "userTokenMaxExpiresInMinutes",
                "tokenVerifyResultCacheSize",
                "tokenVerifyResultCacheExpirySeconds",
                "maxConnections",
                "connectionTimeout",
                "socketTimeout"
        },
        namespace = Descriptor.NS)
@GenerateDiffFunction
public class AccessClientSettings implements Descriptor {

    public static final long USER_TOKEN_MAX_EXPIRES_IN_MINUTES_DEFAULT = 60L;
    public static final long USER_TOKEN_MAX_EXPIRES_IN_MINUTES_UNLIMITED = 0L;

    @XmlElement(defaultValue = "" + USER_TOKEN_MAX_EXPIRES_IN_MINUTES_DEFAULT, required = false)
    private Long userTokenMaxExpiresInMinutes = USER_TOKEN_MAX_EXPIRES_IN_MINUTES_DEFAULT;

    @XmlElement
    private String serverUrl;

    @XmlElement
    private String adminToken;

    @XmlElement
    private Long tokenVerifyResultCacheSize;

    @XmlElement
    private Long tokenVerifyResultCacheExpirySeconds;

    @XmlElement
    private Integer maxConnections;

    @XmlElement
    private Integer connectionTimeout;

    @XmlElement
    private Integer socketTimeout;

    /**
     * Get the max expiration time in minutes for tokens created by users
     *
     * @see #USER_TOKEN_MAX_EXPIRES_IN_MINUTES_DEFAULT
     * @see #USER_TOKEN_MAX_EXPIRES_IN_MINUTES_UNLIMITED
     */
    public Long getUserTokenMaxExpiresInMinutes() {
        return userTokenMaxExpiresInMinutes;
    }

    /**
     * Set the max expiration time in minutes for tokens created by users
     *
     * @param userTokenMaxExpiresInMinutes time in minutes
     * @see #USER_TOKEN_MAX_EXPIRES_IN_MINUTES_DEFAULT
     * @see #USER_TOKEN_MAX_EXPIRES_IN_MINUTES_UNLIMITED
     */
    public void setUserTokenMaxExpiresInMinutes(Long userTokenMaxExpiresInMinutes) {
        this.userTokenMaxExpiresInMinutes = userTokenMaxExpiresInMinutes;
    }

    /**
     * Get the JFrog Access server base URL.
     * @return the server URL, or <tt>null</tt> if it is not set (in such case the bundled Access server will be used).
     */
    @Nullable
    public String getServerUrl() {
        return serverUrl;
    }

    /**
     * Set the JFrog Access server URL. If set to <tt>null</tt> the bundled Access server will be used.
     * @param serverUrl the URL to set
     */
    public void setServerUrl(@Nullable String serverUrl) {
        this.serverUrl = serverUrl;
    }

    /**
     * Get this Artifactory instance's Access admin token
     * @return the token value (optionally encrypted)
     */
    @Nullable
    public String getAdminToken() {
        return adminToken;
    }

    /**
     * Set this Artifactory instance's Access admin token
     * @param adminToken the token value (optionally encrypted)
     */
    public void setAdminToken(@Nullable String adminToken) {
        this.adminToken = adminToken;
    }

    /**
     * Get the size for the cache (number of entries to store) of token verification results managed by the client.
     * <p>
     * The size value can be either:
     * <ul>
     *     <li>&gt; 0 (positive value)</li>
     *     <li>= 0 (zero) - no cache</li>
     *     <li>&lt; 0 (negative value) - default value defined by the client</li>
     * </ul>
     * </p>
     * @return the size, or <tt>null</tt> if not set
     */
    public Long getTokenVerifyResultCacheSize() {
        return tokenVerifyResultCacheSize;
    }

    /**
     * Set the size for the cache (number of entries to store) of token verification results managed by the client.
     * @param size the size
     * @see #getTokenVerifyResultCacheSize()
     */
    public void setTokenVerifyResultCacheSize(Long size) {
        this.tokenVerifyResultCacheSize = size;
    }

    /**
     * Get the expiry (in seconds) for entries in the cache of token verification results managed by the client.
     * <p>
     * The expiry value can be either:
     * <ul>
     *     <li>&gt; 0 (positive value)</li>
     *     <li>= 0 (zero) - no cache</li>
     *     <li>&lt; 0 (negative value) - default value defined by the client</li>
     * </ul>
     * </p>
     * @return the expiry, or <tt>null</tt> if not set
     */
    public Long getTokenVerifyResultCacheExpirySeconds() {
        return tokenVerifyResultCacheExpirySeconds;
    }

    /**
     * Set the expiry (in seconds) for entries in the cache of token verification results managed by the client.
     * @param expiry the expiry in seconds
     */
    public void setTokenVerifyResultCacheExpirySeconds(Long expiry) {
        this.tokenVerifyResultCacheExpirySeconds = expiry;
    }

    /**
     * Get the maximum connections allowed for access client
     * <p>
     * The expiry value can be either:
     * <ul>
     *     <li>&gt; 0 (positive value)</li>
     *     <li>&lt;= 0 (non-positive value) - default value defined by the client</li>
     * </ul>
     * </p>
     */
    public Integer getMaxConnections() {
        return maxConnections;
    }

    /**
     * Set the max connections for access client
     */
    public void setMaxConnections(Integer maxConnections) {
        this.maxConnections = maxConnections;
    }

    /**
     * Get the timeout for the initial connection (vs. socket timeout, after connected) in millis
     * <p>
     * The expiry value can be either:
     * <ul>
     *     <li>&gt; 0 (positive value)</li>
     *     <li>&lt;= 0 (non-positive) - default value defined by the client</li>
     * </ul>
     * </p>
     */
    public Integer getConnectionTimeout() {
        return connectionTimeout;
    }

    /**
     * Set the timeout in millis for the initial connection (vs. socket timeout, after connected)
     */
    public void setConnectionTimeout(Integer connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    /**
     * Get the timeout for the socket connection (vs. connection timeout, initial connection) in millis
     * <p>
     * The expiry value can be either:
     * <ul>
     *     <li>&gt; 0 (positive value)</li>
     *     <li>&lt;= 0 (non-positive value) - default value defined by the client</li>
     * </ul>
     * </p>
     */
    public Integer getSocketTimeout() {
        return socketTimeout;
    }

    /**
     * Set the socket timeout in millis
     */
    public void setSocketTimeout(Integer socketTimeout) {
        this.socketTimeout = socketTimeout;
    }
}

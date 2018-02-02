package org.artifactory.logging.sumologic;

import org.artifactory.spring.ReloadableBean;

/**
 * Main service for interactions with Sumo Logic
 *
 * @author Shay Yaakov
 */
public interface SumoLogicService extends ReloadableBean {

    /**
     * Create a Sumo Logic access token for the given user. This call is  part of the OAuth2 protocol with Sumo Logic.
     * @param username the user for which to create the token
     * @param code     a code provided by Sumo Logic as part of the OAuth2 protocol
     * @param baseUri  the base URI to use for requesting the token from Sumo Logic (provided as part of the OAuth2 protocol)
     * @return the new access token
     * @throws SumoLogicException
     */
    String createToken(String username, String code, String baseUri);

    /**
     * Refresh the Sumo Logic access token for the given user. This call is part of the OAuth2 protocol with Sumo Logic.
     * @param username the user for which to refresh the token
     * @return the new access token
     * @throws SumoLogicException
     */
    String refreshToken(String username);

    /**
     * Get the current access token of the given user
     * @param username the user for which to return the token
     * @return the current access token
     */
    String getAccessToken(String username);

    /**
     * Create a new application (organization) in Sumo Logic. Stores the client ID and secret after they are registered.
     * @throws SumoLogicException
     */
    void registerApplication();

    /**
     * Reset the current application settings, removing all Sumo Logic related configuration, revoking all related tokens, etc.
     * @param enabled Indicate whether to set the config as enabled/disabled after reset is done.
     */
    void resetApplication(boolean enabled);

    /**
     * Setup an Artifactory application dashboards in Sumo Logic.
     * @param username the user who triggered this action
     * @param baseUri  the base URI to use for interacting with Sumo Logic
     * @param newSetup whether to issue a new setup or use an existing. For the first time a new application is
     *                 registered it needs to setup new integration. After that all subsequent requests should be called
     *                 with <code>false</code> in order to use the existing setup.
     * @throws SumoLogicException
     */
    void setupApplication(String username, String baseUri, boolean newSetup);
}
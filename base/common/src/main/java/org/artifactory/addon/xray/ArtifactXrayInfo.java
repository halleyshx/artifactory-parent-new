package org.artifactory.addon.xray;

/**
 * Xray specific information for artifacts.
 *
 * @author Yinon Avraham
 */
public interface ArtifactXrayInfo {

    String getIndexStatus();

    Long getIndexLastUpdated();

    String getAlertTopSeverity(); //Soon to be deprecated, hopefully

    Long getAlertLastUpdated();

    boolean isAlertIgnored();

    // Xray sets this on artifacts that should be blocked - depending on it's own watch for this repo\path
    boolean isBlocked();

    ArtifactXrayInfo EMPTY = new ArtifactXrayInfo() {

        @Override
        public String getIndexStatus() {
            return null;
        }

        @Override
        public Long getIndexLastUpdated() {
            return null;
        }

        @Override
        public String getAlertTopSeverity() {
            return XrayAddon.ALERT_SEVERITY_NONE;
        }

        @Override
        public Long getAlertLastUpdated() {
            return null;
        }

        @Override
        public boolean isAlertIgnored() {
            return false;
        }

        @Override
        public boolean isBlocked() {
            return false;
        }
    };
}

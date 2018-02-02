package org.artifactory.logging.sumo;

/**
 * @author Shay Yaakov
 */
public enum SumoCategory {

    CONSOLE("console"),
    ACCESS("access"),
    REQUEST("request"),
    TRAFFIC("traffic");

    private final String name;

    SumoCategory(String name) {
        this.name = name;
    }

    public static SumoCategory findByName(String name) {
        for (SumoCategory sumoCategory : SumoCategory.values()) {
            if (sumoCategory.name.equalsIgnoreCase(name)) {
                return sumoCategory;
            }
        }
        return null;
    }

    public String getName() {
        return name;
    }

    public String headerValue() {
        return "artifactory/" + name;
    }
}

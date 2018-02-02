package org.artifactory.test;

import com.google.common.collect.Maps;

import java.util.Map;

/**
 * @author Yinon Avraham.
 */
public class SystemProperties {

    private final Map<String, String> originalSystemProps = Maps.newHashMap();

    public String get(String key) {
        return System.getProperty(key);
    }

    public String clear(String key) {
        return set(key, null);
    }

    public String set(String key, String value) {
        if (!originalSystemProps.containsKey(key)) {
            originalSystemProps.put(key, System.getProperty(key));
        }
        if (value != null) {
            return System.setProperty(key, value);
        } else {
            return System.clearProperty(key);
        }
    }

    public void init(String key) {
        set(key, System.getProperty(key));
    }

    public void restoreOriginals() {
        originalSystemProps.forEach((key, value) -> {
            if (value != null) {
                System.setProperty(key, value);
            } else {
                System.clearProperty(key);
            }
        });
        originalSystemProps.clear();
    }
}

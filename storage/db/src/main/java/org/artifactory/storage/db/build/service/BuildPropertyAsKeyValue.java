package org.artifactory.storage.db.build.service;

import lombok.Data;
import org.artifactory.api.build.BuildProps;

/**
 * Used for doing programmatically distinct
 * Must be package level class (not inner) when we use lombok
 */
@Data
class BuildPropertyAsKeyValue {
    final String propKey;
    final String propValue;

    @Override
    public String toString() {
        return "BuildPropertyAsKeyValue{" +
                "propKey='" + propKey + '\'' +
                ", propValue='" + propValue + '\'' +
                '}';
    }

    BuildPropertyAsKeyValue(BuildProps bp) {
        this(bp.getKey(), bp.getValue());
    }


    BuildPropertyAsKeyValue(String propKey, String propValue) {
        this.propKey = propKey;
        this.propValue = propValue;
    }
}

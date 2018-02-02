package org.artifactory.rest.resource.system;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.artifactory.rest.common.model.RestModel;

@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY,
        getterVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY,
        setterVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY
)

@Data
public class SamlInfo implements RestModel {
    @JsonProperty("loginUrl")
    private String loginUrl;
    @JsonProperty("logoutUrl")
    private String logoutUrl;
    @JsonProperty("enabled")
    private boolean enabled;
}

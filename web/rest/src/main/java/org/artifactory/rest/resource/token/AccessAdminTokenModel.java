package org.artifactory.rest.resource.token;


import lombok.Data;
import lombok.NoArgsConstructor;
import org.codehaus.jackson.annotate.JsonProperty;

@Data
@NoArgsConstructor
public class AccessAdminTokenModel {

    @JsonProperty("service_id")
    private String serviceId;

}

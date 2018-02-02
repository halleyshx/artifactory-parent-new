package org.artifactory.rest.common.dataholder;

import lombok.Data;
import org.codehaus.jackson.annotate.JsonProperty;
import org.jfrog.access.rest.user.UserWithGroupsResponse;

@Data
public class UserWithGroupsWrapper {
    @JsonProperty("usr")
    private UserWithGroupsResponse usr;
}

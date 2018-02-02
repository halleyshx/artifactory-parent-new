package org.artifactory.api.bintray.distribution;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.List;

@Data
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class FileSpec {

    @JsonProperty(value = "source_path")
    String sourcePath;
    List<ArtifactProperty> props;
    @JsonProperty(value = "target_artifactory_url")
    String targetArtifactoryUrl;
    @JsonProperty(value = "target_path")
    String targetPath;
    @JsonProperty(value = "release_bundle_name")
    String releaseBundleName;
    @JsonProperty(value = "release_bundle_version")
    String releaseBundleVersion;
    @JsonProperty(value = "close_checksum_hint")
    String checksumHint;
    @JsonProperty(value = "component_type")
    String componentType;
    @JsonProperty(value = "component_name")
    String componentName;
    @JsonProperty(value = "component_version")
    String componentVersion;
}

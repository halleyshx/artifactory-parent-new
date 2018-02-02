package org.artifactory.addon.ha.message;

import com.google.common.collect.Maps;
import org.codehaus.jackson.annotate.JsonTypeName;

import java.util.Map;
import java.util.Set;

/**
 * @author Shay Bagants
 */
@JsonTypeName("nugetAddEvent")
public class NuPkgAddBaseMessage extends HaBaseMessage {
    private String repoKey;
    private String path;
    private Map<String, Set<String>> properties = Maps.newHashMap();


    public NuPkgAddBaseMessage() {
        super("");
    }

    public NuPkgAddBaseMessage(String repoKey, String path, Map<String, Set<String>> properties,
            String publishingMemberId) {
        super(publishingMemberId);
        this.repoKey = repoKey;
        this.path = path;
        this.properties = properties;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        NuPkgAddBaseMessage that = (NuPkgAddBaseMessage) o;

        if (repoKey != null ? !repoKey.equals(that.repoKey) : that.repoKey != null) {
            return false;
        }
        if (path != null ? !path.equals(that.path) : that.path != null) {
            return false;
        }
        return properties != null ? properties.equals(that.properties) : that.properties == null;
    }

    @Override
    public int hashCode() {
        int result = repoKey != null ? repoKey.hashCode() : 0;
        result = 31 * result + (path != null ? path.hashCode() : 0);
        result = 31 * result + (properties != null ? properties.hashCode() : 0);
        return result;
    }

    public String getRepoKey() {
        return repoKey;
    }

    public void setRepoKey(String repoKey) {
        this.repoKey = repoKey;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Map<String, Set<String>> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Set<String>> properties) {
        this.properties = properties;
    }
}

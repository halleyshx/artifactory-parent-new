package org.artifactory.addon.ha.message;

import org.codehaus.jackson.annotate.JsonTypeName;

/**
 * @author Shay Bagants
 */
@JsonTypeName("nugetRemoveEvent")
public class NuPkgRemoveBaseMessage extends HaBaseMessage {
    public String repoKey;
    public String packageId;
    public String packageVersion;

    public NuPkgRemoveBaseMessage() {
        super("");
    }

    public NuPkgRemoveBaseMessage(String repoKey, String packageId, String packageVersion, String publishingMemberId) {
        super(publishingMemberId);
        this.repoKey = repoKey;
        this.packageId = packageId;
        this.packageVersion = packageVersion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        NuPkgRemoveBaseMessage that = (NuPkgRemoveBaseMessage) o;

        if (repoKey != null ? !repoKey.equals(that.repoKey) : that.repoKey != null) {
            return false;
        }
        if (packageId != null ? !packageId.equals(that.packageId) : that.packageId != null) {
            return false;
        }
        return packageVersion != null ? packageVersion.equals(that.packageVersion) : that.packageVersion == null;
    }

    @Override
    public int hashCode() {
        int result = repoKey != null ? repoKey.hashCode() : 0;
        result = 31 * result + (packageId != null ? packageId.hashCode() : 0);
        result = 31 * result + (packageVersion != null ? packageVersion.hashCode() : 0);
        return result;
    }
}

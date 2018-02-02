/*
 *
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2016 JFrog Ltd.
 *
 * Artifactory is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 * Artifactory is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Artifactory.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.artifactory.repo;

import com.google.common.collect.Lists;
import org.artifactory.descriptor.property.PropertySet;
import org.artifactory.descriptor.repo.LocalRepoChecksumPolicyType;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.descriptor.repo.SnapshotVersionBehavior;
import org.artifactory.util.RepoLayoutUtils;

import java.util.List;
import java.util.Map;

/**
 * Local repository configuration
 *
 * @author Tomer Cohen
 * @see org.artifactory.descriptor.repo.LocalRepoDescriptor
 */
public class LocalRepositoryConfigurationImpl extends RepositoryConfigurationBase
        implements LocalRepositoryConfiguration {

    private String checksumPolicyType = "client-checksums";
    private boolean handleReleases = true;
    private boolean handleSnapshots = true;
    private int maxUniqueSnapshots;
    private int maxUniqueTags;
    private String snapshotVersionBehavior = "non-unique";
    private boolean suppressPomConsistencyChecks = false;
    private boolean blackedOut = false;
    private List<String> propertySets;
    private List<String> optionalIndexCompressionFormats;
    private boolean archiveBrowsingEnabled = false;
    private boolean calculateYumMetadata = false;
    private boolean enableFileListsIndexing = false;
    private int yumRootDepth = 0;
    private String yumGroupFileNames;

    public LocalRepositoryConfigurationImpl() {
        setRepoLayoutRef(RepoLayoutUtils.MAVEN_2_DEFAULT_NAME);
    }

    public LocalRepositoryConfigurationImpl(LocalRepoDescriptor localRepoDescriptor) {
        super(localRepoDescriptor, TYPE);
        LocalRepoChecksumPolicyType checksumPolicyType = localRepoDescriptor.getChecksumPolicyType();
        setChecksumPolicyType(checksumPolicyType.getMessage());
        setHandleReleases(localRepoDescriptor.isHandleReleases());
        setHandleSnapshots(localRepoDescriptor.isHandleSnapshots());
        setMaxUniqueSnapshots(localRepoDescriptor.getMaxUniqueSnapshots());
        setMaxUniqueTags(localRepoDescriptor.getMaxUniqueTags());
        Map<String, String> snapshotBehaviours = extractXmlValueFromEnumAnnotations(SnapshotVersionBehavior.class);
        for (Map.Entry<String, String> annotationToField : snapshotBehaviours.entrySet()) {
            if (annotationToField.getValue().equals(localRepoDescriptor.getSnapshotVersionBehavior().name())) {
                setSnapshotVersionBehavior(annotationToField.getKey());
            }
        }
        setSuppressPomConsistencyChecks(localRepoDescriptor.isSuppressPomConsistencyChecks());
        setBlackedOut(localRepoDescriptor.isBlackedOut());
        List<PropertySet> propertySets = localRepoDescriptor.getPropertySets();
        if (propertySets != null && !propertySets.isEmpty()) {
            setPropertySets(Lists.transform(propertySets, PropertySet::getName));
        } else {
            setPropertySets(Lists.newArrayList());
        }
        setOptionalIndexCompressionFormats(localRepoDescriptor.getOptionalIndexCompressionFormats());
        setArchiveBrowsingEnabled(localRepoDescriptor.isArchiveBrowsingEnabled());
        xrayRepoConfig = localRepoDescriptor.getXrayConfig();
        setCalculateYumMetadata(localRepoDescriptor.isCalculateYumMetadata());
        setEnableFileListsIndexing(localRepoDescriptor.isEnableFileListsIndexing());
        setYumRootDepth(localRepoDescriptor.getYumRootDepth());
        setDebianTrivialLayout(localRepoDescriptor.isDebianTrivialLayout());
    }

    @Override
    public boolean isBlackedOut() {
        return blackedOut;
    }

    public void setBlackedOut(boolean blackedOut) {
        this.blackedOut = blackedOut;
    }

    @Override
    public String getChecksumPolicyType() {
        return checksumPolicyType;
    }

    public void setChecksumPolicyType(String checksumPolicyType) {
        this.checksumPolicyType = checksumPolicyType;
    }

    @Override
    public boolean isHandleReleases() {
        return handleReleases;
    }

    public void setHandleReleases(boolean handleReleases) {
        this.handleReleases = handleReleases;
    }

    @Override
    public boolean isHandleSnapshots() {
        return handleSnapshots;
    }

    public void setHandleSnapshots(boolean handleSnapshots) {
        this.handleSnapshots = handleSnapshots;
    }

    @Override
    public int getMaxUniqueSnapshots() {
        return maxUniqueSnapshots;
    }

    public void setMaxUniqueSnapshots(int maxUniqueSnapshots) {
        this.maxUniqueSnapshots = maxUniqueSnapshots;
    }

    @Override
    public int getMaxUniqueTags() {
        return maxUniqueTags;
    }

    public void setMaxUniqueTags(int maxUniqueTags) {
        this.maxUniqueTags = maxUniqueTags;
    }

    @Override
    public List<String> getPropertySets() {
        return propertySets;
    }

    public void setOptionalIndexCompressionFormats(List<String> optionalIndexCompressionFormats) {
        this.optionalIndexCompressionFormats = optionalIndexCompressionFormats;
    }

    @Override
    public List<String> getOptionalIndexCompressionFormats() {
        return this.optionalIndexCompressionFormats;
    }

    public void setPropertySets(List<String> propertySetKeys) {
        this.propertySets = propertySetKeys;
    }

    @Override
    public String getSnapshotVersionBehavior() {
        return snapshotVersionBehavior;
    }

    public void setSnapshotVersionBehavior(String snapshotVersionBehavior) {
        this.snapshotVersionBehavior = snapshotVersionBehavior;
    }

    @Override
    public boolean isSuppressPomConsistencyChecks() {
        return suppressPomConsistencyChecks;
    }

    public void setSuppressPomConsistencyChecks(boolean suppressPomConsistencyChecks) {
        this.suppressPomConsistencyChecks = suppressPomConsistencyChecks;
    }

    @Override
    public boolean isArchiveBrowsingEnabled() {
        return archiveBrowsingEnabled;
    }

    public void setArchiveBrowsingEnabled(boolean archiveBrowsingEnabled) {
        this.archiveBrowsingEnabled = archiveBrowsingEnabled;
    }

    @Override
    public boolean isXrayIndex() {
        return xrayRepoConfig != null && xrayRepoConfig.isEnabled();
    }

    public void setXrayIndex(boolean xrayIndex) {
        getLazyXrayConfig().setEnabled(xrayIndex);
    }

    @Override
    public String getXrayMinimumBlockedSeverity() {
        if (xrayRepoConfig != null && xrayRepoConfig.getMinimumBlockedSeverity() != null) {
            return xrayRepoConfig.getMinimumBlockedSeverity();
        }
        return "";
    }

    public void setXrayMinimumBlockedSeverity(String minimumBlockedSeverity) {
        getLazyXrayConfig().setMinimumBlockedSeverity(minimumBlockedSeverity);
    }

    @Override
    public boolean isBlockXrayUnscannedArtifacts() {
        return xrayRepoConfig != null && xrayRepoConfig.isBlockUnscannedArtifacts();
    }

    public void setBlockXrayUnscannedArtifacts(boolean blockUnscannedArtifacts) {
        getLazyXrayConfig().setBlockUnscannedArtifacts(blockUnscannedArtifacts);
    }

    @Override
    public boolean isCalculateYumMetadata() {
        return calculateYumMetadata;
    }

    public void setCalculateYumMetadata(boolean calculateYumMetadata) {
        this.calculateYumMetadata = calculateYumMetadata;
    }

    public boolean isEnableFileListsIndexing() {
        return enableFileListsIndexing;
    }

    public void setEnableFileListsIndexing(boolean enableFileListsIndexing) {
        this.enableFileListsIndexing = enableFileListsIndexing;
    }

    @Override
    public int getYumRootDepth() {
        return yumRootDepth;
    }

    public void setYumRootDepth(int yumRootDepth) {
        this.yumRootDepth = yumRootDepth;
    }

    @Override
    public String getYumGroupFileNames() {
        return yumGroupFileNames;
    }

    public void setYumGroupFileNames(String yumGroupFileNames) {
        this.yumGroupFileNames = yumGroupFileNames;
    }
}

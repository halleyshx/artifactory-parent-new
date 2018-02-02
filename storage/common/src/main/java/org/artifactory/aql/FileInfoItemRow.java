package org.artifactory.aql;

import com.google.common.collect.Sets;
import lombok.Data;
import org.apache.commons.lang.StringUtils;
import org.artifactory.aql.model.AqlPhysicalFieldEnum;
import org.artifactory.aql.model.DomainSensitiveField;
import org.artifactory.aql.result.rows.RowResult;
import org.artifactory.checksum.ChecksumInfo;
import org.artifactory.checksum.ChecksumType;
import org.artifactory.fs.FileInfo;
import org.artifactory.model.xstream.fs.FileInfoImpl;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.RepoPathFactory;

import java.util.Date;
import java.util.Set;

import static org.artifactory.checksum.ChecksumInfo.TRUSTED_FILE_MARKER;

/**
 * Item row that's fully mappable to a {@link org.artifactory.fs.FileInfo}
 *
 * @author Dan Feldman
 */
@Data
public class FileInfoItemRow implements RowResult {

    private long itemId;
    private String repo;
    private String path;
    private String name;
    private long size;

    private long modified;
    private long created;
    private long updated;

    private String createdBy;
    private String modifiedBy;

    private String originalMd5;
    private String actualMd5;
    private String originalSha1;
    private String actualSha1;
    private String sha2;

    private String propKey;
    private String propVal;


    @Override
    public void put(DomainSensitiveField field, Object value) {
        if (field.getField() == AqlPhysicalFieldEnum.itemId) {
            itemId = (Long) value;
        } else if (field.getField() == AqlPhysicalFieldEnum.itemRepo) {
            repo = (String) value;
        } else if (field.getField() == AqlPhysicalFieldEnum.itemPath) {
            path = (String) value;
        } else if (field.getField() == AqlPhysicalFieldEnum.itemName) {
            name = (String) value;
        } else if (field.getField() == AqlPhysicalFieldEnum.itemSize) {
            size = (long) value;
        } else if (field.getField() == AqlPhysicalFieldEnum.itemModified) {
            modified = ((Date) value).getTime();
        } else if (field.getField() == AqlPhysicalFieldEnum.itemCreated) {
            created = ((Date) value).getTime();
        } else if (field.getField() == AqlPhysicalFieldEnum.itemUpdated) {
            updated = ((Date) value).getTime();
        } else if (field.getField() == AqlPhysicalFieldEnum.itemCreatedBy) {
            createdBy = (String) value;
        } else if (field.getField() == AqlPhysicalFieldEnum.itemModifiedBy) {
            modifiedBy = (String) value;
        } else if (field.getField() == AqlPhysicalFieldEnum.itemOriginalMd5) {
            originalMd5 = (String) value;
        } else if (field.getField() == AqlPhysicalFieldEnum.itemActualMd5) {
            actualMd5 = (String) value;
        } else if (field.getField() == AqlPhysicalFieldEnum.itemOriginalSha1) {
            originalSha1 = (String) value;
        } else if (field.getField() == AqlPhysicalFieldEnum.itemActualSha1) {
            actualSha1 = (String) value;
        } else if (field.getField() == AqlPhysicalFieldEnum.itemSha2) {
            sha2 = (String) value;
        } else if (field.getField() == AqlPhysicalFieldEnum.propertyKey) {
            propKey = (String) value;
        } else if (field.getField() == AqlPhysicalFieldEnum.propertyValue) {
            propVal = (String) value;
        }
    }

    @Override
    public Object get(DomainSensitiveField field) {
        return null;
    }

    public RepoPath getRepoPath() {
        if (StringUtils.equals(path, ".")) {
            return RepoPathFactory.create(repo, name);
        } else {
            return RepoPathFactory.create(repo, path + "/" + name);
        }
    }

    public FileInfo toFileInfo() {
        FileInfoImpl fileInfo = new FileInfoImpl(getRepoPath(), itemId);
        fileInfo.setSize(size);
        fileInfo.setLastModified(modified);
        fileInfo.setCreated(created);
        fileInfo.setLastUpdated(updated);
        fileInfo.setCreatedBy(createdBy);
        fileInfo.setModifiedBy(modifiedBy);
        Set<ChecksumInfo> checksums = Sets.newHashSet();
        checksums.add(new ChecksumInfo(ChecksumType.md5, originalMd5, actualMd5));
        checksums.add(new ChecksumInfo(ChecksumType.sha1, originalSha1, actualSha1));
        if (StringUtils.isNotBlank(sha2)) {
            checksums.add(new ChecksumInfo(ChecksumType.sha256, TRUSTED_FILE_MARKER, sha2));
        }
        fileInfo.setChecksums(checksums);
        return fileInfo;
    }
}

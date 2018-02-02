/*
 *
 * Copyright 2016 JFrog Ltd. All rights reserved.
 * JFROG PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package org.artifactory.repo.service.trash.prune;

import org.artifactory.aql.model.AqlPhysicalFieldEnum;
import org.artifactory.aql.model.DomainSensitiveField;
import org.artifactory.aql.result.rows.RowResult;

public class FullPathInfo implements RowResult {
    private String repo;
    private String path;
    private String name;
    private int dept;

    public FullPathInfo() {
    }

    @Override
    public void put(DomainSensitiveField field, Object value) {
        if (field.getField() == AqlPhysicalFieldEnum.itemRepo) {
            repo = (String) value;
        } else if (field.getField() == AqlPhysicalFieldEnum.itemName) {
            name = (String) value;
        } else if (field.getField() == AqlPhysicalFieldEnum.itemPath) {
            path = (String) value;
        } else if (field.getField() == AqlPhysicalFieldEnum.itemDepth) {
            dept = (int)value;
        } else {
            throw new RuntimeException("Unexpected field for FullPathInfo.class.");
        }
    }

    @Override
    public Object get(DomainSensitiveField field) {
        return null;
    }

    public String getRepo() {
        return repo;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getName() {
        return name;
    }

    public int getDept() {
        return dept;
    }
}
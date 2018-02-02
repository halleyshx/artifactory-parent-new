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

package org.artifactory.storage.db.properties.service;

import edu.emory.mathcs.backport.java.util.Collections;
import org.artifactory.storage.StorageException;
import org.artifactory.common.storage.db.properties.DBPropertiesComparator;
import org.artifactory.storage.db.properties.dao.DbPropertiesDao;
import org.artifactory.common.storage.db.properties.DbVersionInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.List;

/**
 * @author Gidi Shabat
 */
@Service
public class ArtifactoryDbPropertiesServiceImpl implements ArtifactoryDbPropertiesService {

    @Autowired
    private DbPropertiesDao dbPropertiesDao;

    @Override
    public void updateDbVersionInfo(DbVersionInfo versionInfo) {
        try {
            dbPropertiesDao.createProperties(versionInfo);
        } catch (SQLException e) {
            throw new StorageException("Failed to load db properties from database", e);
        }
    }

    @Override
    public DbVersionInfo getDbVersionInfo() {
        try {
            List<DbVersionInfo> versionInfo = dbPropertiesDao.getProperties();
            Collections.sort(versionInfo, new DBPropertiesComparator());
            if (versionInfo.size() > 0) {
                return versionInfo.get(versionInfo.size() - 1);
            }
        } catch (Exception e) {
            throw new StorageException("Failed to load db properties from database", e);
        }
        return null;
    }

    @Override
    public boolean isDbPropertiesTableExists() {
        try {
            return dbPropertiesDao.isDbPropertiesTableExists();
        } catch (SQLException e) {
            throw new StorageException("Failed to check if the  db_properties table exists in the database", e);
        }
    }
}

package org.artifactory.common.config.db;

import com.google.common.collect.Lists;
import org.artifactory.common.storage.db.properties.DBPropertiesComparator;
import org.artifactory.common.storage.db.properties.DbVersionInfo;
import org.jfrog.config.DbChannel;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

/**
 * @author Dan Feldman
 */
public class DbVersionDataAccessObject {

    private DbChannel dbChannel;

    public DbVersionDataAccessObject(DbChannel dbChannel) {
        this.dbChannel = dbChannel;
    }

    public DbVersionInfo getDbVersion() {
        List<DbVersionInfo> dbProperties = Lists.newArrayList();
        try (ResultSet resultSet = dbChannel.executeSelect("SELECT * FROM db_properties ")) {
            while (resultSet.next()) {
                dbProperties.add(getDbVersionFromResult(resultSet));
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve version information from database:" + e.getMessage(), e);
        }
        Collections.sort(dbProperties, new DBPropertiesComparator());
        return dbProperties.size() > 0 ? dbProperties.get(dbProperties.size() - 1) : null;
    }

    public boolean isDbPropertiesTableExists() {
        //noinspection EmptyTryBlock, unused
        try (ResultSet resultSet = dbChannel.executeSelect("SELECT * FROM db_properties")) {
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    private DbVersionInfo getDbVersionFromResult(ResultSet resultSet) throws SQLException {
        return new DbVersionInfo(resultSet.getLong(1), resultSet.getString(2),
                zeroIfNull(resultSet.getInt(3)), zeroIfNull(resultSet.getLong(4)));
    }

    public void destroy() {
        dbChannel.close();
    }

    private static int zeroIfNull(Integer id) {
        return (id == null) ? 0 : id;
    }

    private static long zeroIfNull(Long id) {
        return (id == null) ? 0L : id;
    }
}

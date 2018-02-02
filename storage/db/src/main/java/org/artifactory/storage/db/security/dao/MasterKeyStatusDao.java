package org.artifactory.storage.db.security.dao;

import org.jfrog.security.common.MasterKeyStatus;
import org.artifactory.storage.db.util.BaseDao;
import org.artifactory.storage.db.util.JdbcHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;

/**
 * @author Shay Bagants
 */
@Repository
public class MasterKeyStatusDao extends BaseDao {

    @Autowired
    public MasterKeyStatusDao(JdbcHelper jdbcHelper) {
        super(jdbcHelper);
    }

    public int addMasterKeyStatus(MasterKeyStatus masterKeyStatus) throws SQLException {
        return jdbcHelper.executeUpdate("INSERT INTO master_key_status VALUES(?, ?, ?, ?, ?)",
                1, masterKeyStatus.getStatus(), masterKeyStatus.getSetByNodeId(),
                masterKeyStatus.getKid(), masterKeyStatus.getExpires());
    }
}

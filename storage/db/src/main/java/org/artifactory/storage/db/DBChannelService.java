package org.artifactory.storage.db;

import org.jfrog.config.DbChannel;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;

/**
 * @author gidis
 */
public interface DBChannelService extends DbChannel {
    @Transactional
    int executeUpdateInternal(String query, Object... params) throws SQLException;
}

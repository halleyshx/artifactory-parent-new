package org.artifactory.storage.db.xray.dao;

import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.artifactory.storage.StorageException;
import org.artifactory.storage.db.util.BaseDao;
import org.artifactory.storage.db.util.JdbcHelper;
import org.artifactory.util.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.util.*;

/**
 * A data access table for xray.
 *
 * @author Shay Bagants
 */
@Repository
public class XrayDao extends BaseDao {
    private static final Logger log = LoggerFactory.getLogger(XrayDao.class);

    @Autowired
    public XrayDao(JdbcHelper jdbcHelper) {
        super(jdbcHelper);
    }

    /**
     * Removes all 'xray.blocked' properties from {@param repoKey} - used whenever there's a change in severity
     * or an Xray watch is created/removed
     */
    public void removeAllBlockedPropsFromRepo(String repoKey) {
        if (StringUtils.isBlank(repoKey)) {
            throw new IllegalArgumentException("Repository key cannot be empty");
        }
        String query = "DELETE FROM node_props " +
                "WHERE prop_key like 'xray.%.blocked' AND node_id IN (" +
                "SELECT node_id FROM nodes WHERE repo = ?)";
        try {
            jdbcHelper.executeUpdate(query, repoKey);
        } catch (SQLException e) {
            log.error("Failed to count the number of potential for xray indexing for repo '" + repoKey + "'");
            log.debug("Failed to execute query", e);
            throw new StorageException(e);
        }
    }

    /**
     * Get the list of artifacts that are allowed to be indexed (including these which already indexed or being indexed)
     *
     * @param repoKey    The repository key
     * @param extensions The list of extensions (possible candidates for xray index) to search
     * @param fileNames  The list of filenames (possible candidates for xray index) to search
     * @return           The amount of artifacts that are potential for xray indexing
     */
    public int getPotentialForIndex(String repoKey, Set<String> extensions, Set<String> fileNames) {
        assertValidArguments(repoKey, extensions, fileNames);
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT COUNT(node_name) FROM nodes WHERE node_type=1 AND repo = ? AND (");
        List<String> params = Lists.newArrayList(repoKey);
        sb.append(buildExtensionsCriteria(extensions, fileNames, params));
        sb.append(")");
        try {
            return jdbcHelper.executeSelectCount(sb.toString(), params.toArray());
        } catch (SQLException e) {
            log.error("Failed to count the number of potential for xray indexing for repo '" + repoKey + "'");
            log.debug("Failed to execute query", e);
            throw new StorageException(e);
        }
    }

    /**
     * @param repoKey   The repository to search on
     * @param propKey   The key of the index status property (depends on the xrayId)
     * @param statuses  The status names
     * @return The number of artifacts matching the given index status
     */
    public int countArtifactsByIndexStatus(String repoKey, String propKey, String... statuses) {
        String query = "SELECT COUNT(*) FROM node_props INNER JOIN nodes ON node_props.node_id=nodes.node_id WHERE" +
                " repo=? AND prop_key=? AND " + buildStatusesCriteria(statuses.length);
        //Need this kombina because passing varargs is like passing String[]
        List<String> params = Lists.newArrayList(repoKey, propKey);
        Collections.addAll(params, statuses);
        try {
            return jdbcHelper.executeSelectCount(query, (Object[]) params.toArray(new Object[params.size()]));
        } catch (SQLException e) {
            log.error("Failed to retrieve the number of artifacts that are marked as: '" + Arrays.toString(statuses) +
                    "' in repo '" + repoKey + "'");
            log.debug("Failed to execute query", e);
            throw new StorageException(e);
        }
    }

    private String buildStatusesCriteria(int noOfStatuses) {
        StringBuilder queryBuilder = new StringBuilder("(");
        for (int i = 0; i < noOfStatuses; i++) {
            if (i > 0) {
                queryBuilder.append(" OR ");
            }
            queryBuilder.append("prop_value=?");
        }
        queryBuilder.append(")");
        return queryBuilder.toString();
    }

    private String buildExtensionsCriteria(Set<String> extensions, Set<String> fileNames, List<String> params) {
        StringBuilder sb = new StringBuilder();
        Iterator<String> it = extensions.iterator();
        int i = 0;
        while (it.hasNext()) {
            if (i > 0) {
                sb.append(" OR ");
            }
            sb.append("node_name like ?");
            params.add("%." + it.next());
            i++;
        }

        it = fileNames.iterator();
        while (it.hasNext()) {
            if (i > 0) {
                sb.append(" OR ");
            }
            sb.append("node_name=?");
            params.add(it.next());
        }

        return sb.toString();
    }

    //Usually, this should not be here and there should be no handler with empty both 'extensions' and 'fileNames', however
    //if all of these will be empty, it might cause huge searches on the db.
    private void assertValidArguments(String repoKey, Set<String> extensions, Set<String> fileNames) {
        if (CollectionUtils.isNullOrEmpty(extensions) && CollectionUtils.isNullOrEmpty(fileNames)) {
            throw new IllegalArgumentException("extensions and file names potential cannot be empty");
        }
        if (StringUtils.isBlank(repoKey)) {
            throw new IllegalArgumentException("Repository key cannot be empty");
        }
    }
}

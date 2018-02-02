package org.artifactory.ui.utils;

import org.apache.commons.lang.StringUtils;
import org.artifactory.api.search.artifact.ChecksumSearchControls;
import org.artifactory.checksum.ChecksumType;

/**
 * @author Dan Feldman
 */
public class SearchUtils {

    public static void addChecksumCriteria(String query, ChecksumSearchControls searchControls) {
        if (StringUtils.length(query) == ChecksumType.md5.length()) {
            searchControls.addChecksum(ChecksumType.md5, query);
            searchControls.setLimitSearchResults(true);
        } else if (StringUtils.length(query) == ChecksumType.sha1.length()) {
            searchControls.addChecksum(ChecksumType.sha1, query);
        } else if (StringUtils.length(query) == ChecksumType.sha256.length()) {
            searchControls.addChecksum(ChecksumType.sha256, query);
        }
    }

}

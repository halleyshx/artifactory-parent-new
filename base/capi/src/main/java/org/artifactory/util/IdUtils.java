package org.artifactory.util;

import com.google.common.hash.Hashing;

import java.nio.charset.Charset;

/**
 * @author Noam Shemesh
 */
public abstract class IdUtils {
    private IdUtils() {}

    private static final int MAXIMUM_LENGTH = 30;
    private static final int HASH_LENGTH = 10;

    public static String produceReplicationId(String repoKey, String url) {
        String concat = normalize((repoKey == null ? "" : repoKey) + "_" + (url == null ? "" : url));

        if (concat.length() > MAXIMUM_LENGTH - HASH_LENGTH) {
            return concat.substring(0, MAXIMUM_LENGTH - HASH_LENGTH) + hash(concat, HASH_LENGTH);
        }

        return concat;
    }

    private static String hash(String concat, int hashLength) {
        return Hashing.md5().hashString(concat, Charset.defaultCharset()).toString().substring(0, hashLength);
    }

    private static String normalize(String s) {
        return s.replaceAll("[:/@\\\\.]", "_");
    }
}

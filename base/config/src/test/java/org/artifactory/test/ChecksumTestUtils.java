package org.artifactory.test;

import org.apache.commons.lang.RandomStringUtils;

/**
 * Utility class to help checksum related tests.
 *
 * @author Yossi Shaul
 */
public class ChecksumTestUtils {

    /**
     * @return Randomly generated 32 bytes MD5 string
     */
    public static String randomMd5() {
        return randomHex(32);
    }

    /**
     * @return Randomly generated 40 bytes SHA-1 string
     */
    public static String randomSha1() {
        return randomHex(40);
    }

    /**
     * @param count Number of characters to include in the random string
     * @return Randomly generated HEX string with the specified length
     */
    public static String randomHex(int count) {
        return RandomStringUtils.random(count, "abcdef0123456789");
    }

}

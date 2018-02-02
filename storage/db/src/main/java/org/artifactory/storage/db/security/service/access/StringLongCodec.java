package org.artifactory.storage.db.security.service.access;

import org.apache.commons.lang.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Naive codec to/from user/group name and long value.
 * Used temporarily for compatibility with current db scheme of the permissions.
 * Assumes username alphabet is a-z.
 *
 * @deprecated need to delete after deleting {@link org.artifactory.storage.db.security.service.AclStoreServiceImpl}
 * @author Yossi Shaul
 */
@Deprecated
public class StringLongCodec {

    private static final char[] alphabet = "abcdefghijklmnopqrstuvwxyz1234567890-_".toCharArray();
    private static final String[] intDigits = new String[alphabet.length];

    private static final Map<Character, String> alphabetToInteger = new HashMap<>();
    private static final Map<String, Character> intDigitToAlphabet = new HashMap<>();

    static {
        for (int i = 0; i < alphabet.length; i++) {
            intDigits[i] = i + 10 + "";
        }

        for (int i = 0; i < alphabet.length; i++) {
            alphabetToInteger.put(alphabet[i], intDigits[i]);
            intDigitToAlphabet.put(intDigits[i], alphabet[i]);
        }
    }

    public static long encode(String str) {
        StringBuilder sb = new StringBuilder();
        for (char c : StringUtils.trim(str).toCharArray()) {
            String integer = alphabetToInteger.get(c);
            if (integer == null) {
                throw new IllegalArgumentException("String '" + str + "' contains unsupported character: " + c);
            }
            sb.append(integer);
        }
        return Long.parseUnsignedLong(sb.toString());
    }

    public static String decode(long encoded) {
        StringBuilder sb = new StringBuilder();
        String str = Long.toString(encoded);
        char[] chars = str.toCharArray();
        for (int i = 0; i < chars.length; i += 2) {
            String digits = "" + chars[i] + chars[i + 1];
            Character c = intDigitToAlphabet.get(digits);
            if (c == null) {
                throw new IllegalArgumentException("String '" + str + "' contains unsupported digits: " + digits);
            }
            sb.append(c);
        }
        return sb.toString();
    }

    // for testing
    public static void main(String[] args) {
        long encoded = encode("yossis");
        System.out.println("yossis encoded to " + encoded);
        if (!Long.toString(encoded).equals("342428281828")) {
            throw new RuntimeException("Unexpected encoded value: expected: 342428281828 but got " + encoded);
        }
        String decoded = decode(encoded);
        System.out.println(encoded + " decoded to " + decoded);
        if (!decoded.equals("yossis")) {
            throw new RuntimeException("Unexpected decoded value: expected: yossis but got " + decoded);
        }
    }
}

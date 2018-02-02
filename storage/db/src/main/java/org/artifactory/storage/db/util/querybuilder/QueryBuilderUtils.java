package org.artifactory.storage.db.util.querybuilder;

/**
 * @author Yinon Avraham
 *         Created on 09/08/2016.
 */
final class QueryBuilderUtils {

    private QueryBuilderUtils() {}

    static long addButLimit(long positiveA, long positiveB, long positiveMax) {
        assert positiveA >= 0;
        assert positiveB >= 0;
        assert positiveMax >= 0;
        try {
            long result = Math.addExact(positiveA, positiveB);
            return result <= positiveMax ? result : positiveMax;
        } catch (ArithmeticException e) {
            return positiveMax;
        }
    }

}

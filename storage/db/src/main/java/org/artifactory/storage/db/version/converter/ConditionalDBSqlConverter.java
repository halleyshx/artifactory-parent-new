package org.artifactory.storage.db.version.converter;

import org.artifactory.storage.db.conversion.ConversionPredicate;
import org.artifactory.storage.db.util.JdbcHelper;
import org.jfrog.storage.DbType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.function.BiPredicate;

/**
 * DB conversion that tests for a certain {@link BiPredicate<JdbcHelper, DbType>} before running.
 * Conversion Will not run if condition returns false.
 *
 * @author Dan Feldman
 */
public class ConditionalDBSqlConverter extends DBSqlConverter {
    private static final Logger log = LoggerFactory.getLogger(ConditionalDBSqlConverter.class);

    private BiPredicate<JdbcHelper, DbType> shouldConvert;

    public ConditionalDBSqlConverter(String fromVersion, @Nullable ConversionPredicate predicate) {
        super(fromVersion);
        this.shouldConvert = predicate != null ? predicate.condition() : null;
    }

    @Override
    public void convert(JdbcHelper jdbcHelper, DbType dbType) {
        if (shouldConvert == null || shouldConvert.test(jdbcHelper, dbType)) {
            super.convert(jdbcHelper, dbType);
        } else {
            log.debug("Condition for running sql schema conversion '{}' not met, skipping.", fromVersion);
        }
    }
}

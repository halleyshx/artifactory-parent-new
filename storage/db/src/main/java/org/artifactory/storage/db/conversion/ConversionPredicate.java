package org.artifactory.storage.db.conversion;

import org.artifactory.storage.db.util.JdbcHelper;
import org.jfrog.storage.DbType;

import java.util.function.BiPredicate;


/**
 * Predicates for conditional conversion {@link org.artifactory.storage.db.version.converter.ConditionalDBSqlConverter}.
 *
 * @author Dan Feldman
 */
public interface ConversionPredicate {

    BiPredicate<JdbcHelper, DbType> condition();

}

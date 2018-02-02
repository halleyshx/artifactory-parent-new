/*
 *
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2016 JFrog Ltd.
 *
 * Artifactory is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 * Artifactory is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Artifactory.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.artifactory.storage.db.version;

import com.google.common.collect.Lists;
import org.artifactory.storage.db.conversion.version.v202.V202ConversionPredicate;
import org.artifactory.storage.db.conversion.version.v203.V203ConversionPredicates.V550ConversionPredicate;
import org.artifactory.storage.db.conversion.version.v203.V203ConversionPredicates.V550aConversionPredicate;
import org.artifactory.storage.db.conversion.version.v203.V203ConversionPredicates.V550bConversionPredicate;
import org.artifactory.storage.db.conversion.version.v203.V203ConversionPredicates.V550cConversionPredicate;
import org.artifactory.storage.db.util.JdbcHelper;
import org.artifactory.storage.db.version.converter.ConditionalDBSqlConverter;
import org.artifactory.storage.db.version.converter.DBConverter;
import org.artifactory.storage.db.version.converter.DBSqlConverter;
import org.artifactory.version.ArtifactoryVersion;
import org.artifactory.version.SubConfigElementVersion;
import org.artifactory.version.VersionComparator;
import org.jfrog.storage.DbType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

/**
 * NOTE! The conversion logic for this enum is different from the one used by
 * {@link org.artifactory.version.ArtifactoryConfigVersion}: here the converters are *in line* with the version that
 * requires them.
 * Meaning for example, that passing into 4.4.1 (denoted v441) requires that you run conversion for db version 'v441'
 * and this db version is the one used until 4.14.0 (denoted v4140)
 * Artifactory DB version
 */
public enum ArtifactoryDBVersion implements SubConfigElementVersion {
    v100(ArtifactoryVersion.v300, ArtifactoryVersion.v304),
    v101(ArtifactoryVersion.v310, ArtifactoryVersion.v310, new DBSqlConverter("v310")),
    v102(ArtifactoryVersion.v311, ArtifactoryVersion.v402, new DBSqlConverter("v311")),
    v103(ArtifactoryVersion.v410, ArtifactoryVersion.v413, new DBSqlConverter("v410")),
    v104(ArtifactoryVersion.v420, ArtifactoryVersion.v431, new DBSqlConverter("v420")),
    v106(ArtifactoryVersion.v432, ArtifactoryVersion.v433),
    v107(ArtifactoryVersion.v440, ArtifactoryVersion.v440, new DBSqlConverter("v440")),
    v108(ArtifactoryVersion.v441, ArtifactoryVersion.v4141, new DBSqlConverter("v441")),
    v109(ArtifactoryVersion.v4142, ArtifactoryVersion.v4161),
    v200(ArtifactoryVersion.v500beta1, ArtifactoryVersion.v522m001, new DBSqlConverter("v500")),
    v201(ArtifactoryVersion.v530, ArtifactoryVersion.v530, new DBSqlConverter("v530")),
    v202(ArtifactoryVersion.v531, ArtifactoryVersion.v548, new ConditionalDBSqlConverter("v441", new V202ConversionPredicate())),
    v203(ArtifactoryVersion.v550m001, ArtifactoryVersion.v566,
            new ConditionalDBSqlConverter("v550", new V550ConversionPredicate()),
            new ConditionalDBSqlConverter("v550a", new V550aConversionPredicate()),
            new ConditionalDBSqlConverter("v550b", new V550bConversionPredicate()),
            new ConditionalDBSqlConverter("v550c", new V550cConversionPredicate())),
    v204(ArtifactoryVersion.v570m001, ArtifactoryVersion.v571, new DBSqlConverter("v570")),
    v205(ArtifactoryVersion.v572, ArtifactoryVersion.v572, new DBSqlConverter("v572")),
    v206(ArtifactoryVersion.v580m008, ArtifactoryVersion.getCurrent(), new DBSqlConverter("v580"));

    private static final Logger log = LoggerFactory.getLogger(ArtifactoryDBVersion.class);

    private final VersionComparator comparator;
    private final DBConverter[] converters;

    ArtifactoryDBVersion(ArtifactoryVersion from, ArtifactoryVersion until, DBConverter... converters) {
        this.comparator = new VersionComparator(from, until);
        this.converters = converters;
    }

    public static ArtifactoryDBVersion getLast() {
        ArtifactoryDBVersion[] versions = ArtifactoryDBVersion.values();
        return versions[versions.length - 1];
    }

    public static void convert(ArtifactoryVersion from, JdbcHelper jdbcHelper, DbType dbType) {
        // All converters of versions above me needs to be executed in sequence
        List<DBConverter> converters = Lists.newArrayList();
        for (ArtifactoryDBVersion version : ArtifactoryDBVersion.values()) {
            if (version.comparator.isAfter(from) && !version.comparator.supports(from)) {
                converters.addAll(Arrays.asList(version.getConverters()));
            }
        }

        if (converters.isEmpty()) {
            log.debug("No database converters found between version {} and {}", from, ArtifactoryVersion.getCurrent());
        } else {
            log.info("Starting database conversion from {} to {}", from, ArtifactoryVersion.getCurrent());
            for (DBConverter converter : converters) {
                converter.convert(jdbcHelper, dbType);
            }
            log.info("Finished database conversion from {} to {}", from, ArtifactoryVersion.getCurrent());
        }
    }

    public DBConverter[] getConverters() {
        return converters;
    }

    @Override
    public VersionComparator getComparator() {
        return comparator;
    }
}

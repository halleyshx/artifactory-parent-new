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

package org.artifactory.converter.postinit;

import org.artifactory.common.ArtifactoryHome;
import org.artifactory.version.ArtifactoryVersion;
import org.artifactory.version.CompoundVersionDetails;
import org.artifactory.version.VersionComparator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * Converters that will run post-init (but after all conversions)
 *
 * @author Nadav Yogev
 */
public enum PostInitVersions {
    v100(ArtifactoryVersion.v4110, ArtifactoryVersion.v4121, new DockerInvalidImageLocationConverter()),
    v101(ArtifactoryVersion.v4150, ArtifactoryVersion.v560m001, new ConanRepoPathConverter());

    private final VersionComparator comparator;
    private final PostInitConverter[] converters;

    /**
     * @param from       The artifactory version this config version was first used
     * @param until      The artifactory version this config was last used in (inclusive)
     * @param converters A list of converters to use to move from <b>this</b> config version to the <b>next</b> config
     *                   version
     */
    PostInitVersions(ArtifactoryVersion from, ArtifactoryVersion until, PostInitConverter... converters) {
        this.comparator = new VersionComparator(from, until);
        this.converters = converters;
    }

    public static PostInitVersions getCurrent() {
        PostInitVersions[] versions = PostInitVersions.values();
        return versions[versions.length - 1];
    }

    public boolean isCurrent() {
        return comparator.isCurrent();
    }

    public void convert(CompoundVersionDetails from, CompoundVersionDetails until) {
        // First create the list of converters to apply
        List<PostInitConverter> converters = new ArrayList<>();
        // All converters of versions between desired range needs to be executed in sequence
        for (PostInitVersions version : PostInitVersions.values()) {
            if (shouldConvert(from, version)) {
                converters.addAll(Arrays.asList(version.getConverters()));
            }
        }
        // Apply converters
        converters.forEach(converter -> converter.convert(from, until));
    }

    /**
     * Assert that all the post init converters that should run have all their preconditions met before running them
     */
    public void assertPreConditions(ArtifactoryHome home, CompoundVersionDetails from, CompoundVersionDetails until) {
        Arrays.stream(PostInitVersions.values())
                .filter(version -> shouldConvert(from, version))
                .flatMap(version -> Stream.of(version.getConverters()))
                .forEach(converter -> converter.assertConversionPrecondition(home, from, until));
    }

    /**
     * The shouldConvert logic here is a bit special as we want post init conversion to only run if upgrading
     * from an impacted version.
     * For instance, the docker conversion fixes a bug that was introduced in 4.11.0 and solved in 4.12.1 so conversion
     * should run only when upgrading from versions that are between these 2.
     * (meaning that if an upgrade is 4.9 -> 4.13 conversion will not run in this case)
     */
    private boolean shouldConvert(CompoundVersionDetails from, PostInitVersions version) {
        return from.getVersion().after(version.comparator.getFrom())
                && from.getVersion().before(version.comparator.getUntil())
                && version.getConverters() != null && version.getConverters().length > 0;
    }

    public PostInitConverter[] getConverters() {
        return converters;
    }
}

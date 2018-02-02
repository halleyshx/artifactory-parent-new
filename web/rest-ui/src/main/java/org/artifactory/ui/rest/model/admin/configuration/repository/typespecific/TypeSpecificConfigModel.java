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

package org.artifactory.ui.rest.model.admin.configuration.repository.typespecific;

import org.artifactory.descriptor.repo.RepoType;
import org.artifactory.rest.common.model.RestModel;
import org.codehaus.jackson.annotate.JsonSubTypes;
import org.codehaus.jackson.annotate.JsonTypeInfo;

/**
 * @author Dan Feldman
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "repoType")
@JsonSubTypes({
        @JsonSubTypes.Type(value = BowerTypeSpecificConfigModel.class, name = "Bower"),
        @JsonSubTypes.Type(value = ChefTypeSpecificConfigModel.class, name = "Chef"),
        @JsonSubTypes.Type(value = CocoaPodsTypeSpecificConfigModel.class, name = "CocoaPods"),
        @JsonSubTypes.Type(value = ComposerTypeSpecificConfigModel.class, name = "Composer"),
        @JsonSubTypes.Type(value = ConanTypeSpecificConfigModel.class, name = "Conan"),
        @JsonSubTypes.Type(value = DebTypeSpecificConfigModel.class, name = "Debian"),
        @JsonSubTypes.Type(value = DistRepoTypeSpecificConfigModel.class, name = "Distribution"),
        @JsonSubTypes.Type(value = OpkgTypeSpecificConfigModel.class, name = "Opkg"),
        @JsonSubTypes.Type(value = DockerTypeSpecificConfigModel.class, name = "Docker"),
        @JsonSubTypes.Type(value = GemsTypeSpecificConfigModel.class, name = "Gems"),
        @JsonSubTypes.Type(value = GenericTypeSpecificConfigModel.class, name = "Generic"),
        @JsonSubTypes.Type(value = GitLfsTypeSpecificConfigModel.class, name = "GitLfs"),
        @JsonSubTypes.Type(value = GradleTypeSpecificConfigModel.class, name = "Gradle"),
        @JsonSubTypes.Type(value = HelmTypeSpecificConfigModel.class, name = "Helm"),
        @JsonSubTypes.Type(value = IvyTypeSpecificConfigModel.class, name = "Ivy"),
        @JsonSubTypes.Type(value = MavenTypeSpecificConfigModel.class, name = "Maven"),
        @JsonSubTypes.Type(value = NpmTypeSpecificConfigModel.class, name = "Npm"),
        @JsonSubTypes.Type(value = NugetTypeSpecificConfigModel.class, name = "NuGet"),
        @JsonSubTypes.Type(value = P2TypeSpecificConfigModel.class, name = "P2"),
        @JsonSubTypes.Type(value = PypiTypeSpecificConfigModel.class, name = "Pypi"),
        @JsonSubTypes.Type(value = PuppetTypeSpecificConfigModel.class, name = "Puppet"),
        @JsonSubTypes.Type(value = SbtTypeSpecificConfigModel.class, name = "SBT"),
        @JsonSubTypes.Type(value = VagrantTypeSpecificConfigModel.class, name = "Vagrant"),
        @JsonSubTypes.Type(value = VcsTypeSpecificConfigModel.class, name = "VCS"),
        @JsonSubTypes.Type(value = YumTypeSpecificConfigModel.class, name = "YUM"),
})
public interface TypeSpecificConfigModel extends RestModel {

    RepoType getRepoType();

    /**
     * This should retrieve the default remote url for each package type
     * For instance: Maven, Gradle, Ivy and SBT should return http://jcenter.bintray.com
     * <p>
     * Notice: method name corresponds to the JSON model field name and is used by the UI.
     */
    String getUrl();
}

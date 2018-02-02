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

package org.artifactory.ui.rest.model.artifacts.search.packagesearch.criteria;

import org.artifactory.aql.model.AqlComparatorEnum;
import org.artifactory.aql.model.AqlDomainEnum;
import org.artifactory.aql.model.AqlPhysicalFieldEnum;
import org.artifactory.descriptor.repo.RepoType;
import org.artifactory.mime.DebianNaming;
import org.artifactory.ui.rest.model.artifacts.search.packagesearch.search.AqlUISearchModel;
import org.artifactory.ui.rest.service.artifacts.search.packagesearch.result.*;
import org.artifactory.ui.rest.service.artifacts.search.packagesearch.strategy.*;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.artifactory.ui.rest.service.artifacts.search.packagesearch.result.DummyPackageSearchResultMerger.DUMMY_MERGER;

/**
 * Contains all available criteria specific to each package type the search supports
 *
 * @author Dan Feldman
 */
public enum PackageSearchCriteria {
    //Keep packages ordered alphabetically please!

    bowerName(PackageSearchType.bower, "bower.name",
            new AqlUISearchModel("bowerName", "Name", "Bower Package Name", true,
                    new AqlComparatorEnum[]{AqlComparatorEnum.equals, AqlComparatorEnum.matches}),
            new AqlUIPropertySearchStrategy("bower.name",
                    new AqlDomainEnum[]{AqlDomainEnum.items, AqlDomainEnum.properties}),
            new AqlUISearchDummyResultManipulator()),

    bowerVersion(PackageSearchType.bower, "bower.version",
            new AqlUISearchModel("bowerVersion", "Version", "Bower Version", true,
                    new AqlComparatorEnum[]{AqlComparatorEnum.equals, AqlComparatorEnum.matches}),
            new AqlUIPropertySearchStrategy("bower.version",
                    new AqlDomainEnum[]{AqlDomainEnum.items, AqlDomainEnum.properties}),
            new AqlUISearchDummyResultManipulator()),

    chefName(PackageSearchType.chef, "chef.name",
            new AqlUISearchModel("chefName", "Name", "Chef Name", true,
                    new AqlComparatorEnum[]{AqlComparatorEnum.equals, AqlComparatorEnum.matches}),
            new AqlUIPropertySearchStrategy("chef.name",
                    new AqlDomainEnum[]{AqlDomainEnum.items, AqlDomainEnum.properties}),
            new AqlUISearchDummyResultManipulator()),

    chefVersion(PackageSearchType.chef, "chef.version",
            new AqlUISearchModel("chefVersion", "Version", "Chef Version", true,
                    new AqlComparatorEnum[]{AqlComparatorEnum.equals, AqlComparatorEnum.matches}),
            new AqlUIPropertySearchStrategy("chef.version",
                    new AqlDomainEnum[]{AqlDomainEnum.items, AqlDomainEnum.properties}),
            new AqlUISearchDummyResultManipulator()),

    chefCategory(PackageSearchType.chef, "chef.category",
            new AqlUISearchModel("chefCategory", "Category", "Chef Category", true,
                    new AqlComparatorEnum[]{AqlComparatorEnum.equals, AqlComparatorEnum.matches}),
            new AqlUIPropertySearchStrategy("chef.category",
                    new AqlDomainEnum[]{AqlDomainEnum.items, AqlDomainEnum.properties}),
            new AqlUISearchDummyResultManipulator()),

    chefPlatform(PackageSearchType.chef, "chef.platform",
            new AqlUISearchModel("chefPlatform", "Platform", "Chef Platform", true,
                    new AqlComparatorEnum[]{AqlComparatorEnum.equals, AqlComparatorEnum.matches}),
            new AqlUIPropertySearchStrategy("chef.platform",
                    new AqlDomainEnum[]{AqlDomainEnum.items, AqlDomainEnum.properties}),
            new AqlUISearchDummyResultManipulator()),

    cocoapodsName(PackageSearchType.cocoapods, "pods.name",
            new AqlUISearchModel("cocoapodsName", "Name", "CocoaPods Package Name", true,
                    new AqlComparatorEnum[]{AqlComparatorEnum.equals, AqlComparatorEnum.matches}),
            new AqlUIPropertySearchStrategy("pods.name",
                    new AqlDomainEnum[]{AqlDomainEnum.items, AqlDomainEnum.properties}),
            new AqlUISearchDummyResultManipulator()),

    cocoapodsVersion(PackageSearchType.cocoapods, "pods.version",
            new AqlUISearchModel("cocoapodsVersion", "Version", "CocoaPods Version", true,
                    new AqlComparatorEnum[]{AqlComparatorEnum.equals, AqlComparatorEnum.matches}),
            new AqlUIPropertySearchStrategy("pods.version",
                    new AqlDomainEnum[]{AqlDomainEnum.items, AqlDomainEnum.properties}),
            new AqlUISearchDummyResultManipulator()),

    composerName(PackageSearchType.composer, "composer.name",
            new AqlUISearchModel("composerName", "Name", "Composer Package Name", true,
                    new AqlComparatorEnum[]{AqlComparatorEnum.equals, AqlComparatorEnum.matches}),
            new AqlUIPropertySearchStrategy("composer.name",
                    new AqlDomainEnum[]{AqlDomainEnum.items, AqlDomainEnum.properties}),
            new AqlUISearchDummyResultManipulator()),

    composerVersion(PackageSearchType.composer, "composer.version",
            new AqlUISearchModel("composerVersion", "Version", "Composer Version", true,
                    new AqlComparatorEnum[]{AqlComparatorEnum.equals, AqlComparatorEnum.matches}),
            new AqlUIPropertySearchStrategy("composer.version",
                    new AqlDomainEnum[]{AqlDomainEnum.items, AqlDomainEnum.properties}),
            new AqlUISearchDummyResultManipulator()),

    conanName(PackageSearchType.conan, "conan.package.name",
            new AqlUISearchModel("conanName", "Name", "Conan Name", true,
                    new AqlComparatorEnum[]{AqlComparatorEnum.equals, AqlComparatorEnum.matches}),
            new AqlUIPropertySearchStrategy("conan.package.name",
                    new AqlDomainEnum[]{AqlDomainEnum.items, AqlDomainEnum.properties}),
            new AqlUISearchDummyResultManipulator()),

    conanVersion(PackageSearchType.conan, "conan.package.version",
            new AqlUISearchModel("conanVersion", "Version", "Conan Version", true,
                    new AqlComparatorEnum[]{AqlComparatorEnum.equals, AqlComparatorEnum.matches}),
            new AqlUIPropertySearchStrategy("conan.package.version",
                    new AqlDomainEnum[]{AqlDomainEnum.items, AqlDomainEnum.properties}),
            new AqlUISearchDummyResultManipulator()),

    conanUser(PackageSearchType.conan, "conan.package.user",
            new AqlUISearchModel("conanUser", "User", "Conan User", true,
                    new AqlComparatorEnum[]{AqlComparatorEnum.equals, AqlComparatorEnum.matches}),
            new AqlUIPropertySearchStrategy("conan.package.user",
                    new AqlDomainEnum[]{AqlDomainEnum.items, AqlDomainEnum.properties}),
            new AqlUISearchDummyResultManipulator()),

    conanChannel(PackageSearchType.conan, "conan.package.channel",
            new AqlUISearchModel("conanChannel", "Channel", "Conan Channel", true,
                    new AqlComparatorEnum[]{AqlComparatorEnum.equals, AqlComparatorEnum.matches}),
            new AqlUIPropertySearchStrategy("conan.package.channel",
                    new AqlDomainEnum[]{AqlDomainEnum.items, AqlDomainEnum.properties}),
            new AqlUISearchDummyResultManipulator()),

    conanOs(PackageSearchType.conan, "conan.settings.os",
            new AqlUISearchModel("conanOs", "OS", "Conan OS", false,
                    new AqlComparatorEnum[]{AqlComparatorEnum.equals, AqlComparatorEnum.matches}),
            new AqlUIPropertySearchStrategy("conan.settings.os",
                    new AqlDomainEnum[]{AqlDomainEnum.items, AqlDomainEnum.properties}),
            new AqlUISearchDummyResultManipulator()),

    conanArch(PackageSearchType.conan, "conan.settings.arch",
            new AqlUISearchModel("conanArch", "Architecture", "Conan Architecture", false,
                    new AqlComparatorEnum[]{AqlComparatorEnum.equals, AqlComparatorEnum.matches}),
            new AqlUIPropertySearchStrategy("conan.settings.arch",
                    new AqlDomainEnum[]{AqlDomainEnum.items, AqlDomainEnum.properties}),
            new AqlUISearchDummyResultManipulator()),

    conanBuildType(PackageSearchType.conan, "conan.settings.build_type",
            new AqlUISearchModel("conanBuildType", "Build Type", "Conan Build Type", false,
                    new AqlComparatorEnum[]{AqlComparatorEnum.equals, AqlComparatorEnum.matches}),
            new AqlUIPropertySearchStrategy("conan.settings.build_type",
                    new AqlDomainEnum[]{AqlDomainEnum.items, AqlDomainEnum.properties}),
            new AqlUISearchDummyResultManipulator()),

    conanCompiler(PackageSearchType.conan, "conan.settings.compiler",
            new AqlUISearchModel("conanCompiler", "Compiler", "Conan Compiler", false,
                    new AqlComparatorEnum[]{AqlComparatorEnum.equals, AqlComparatorEnum.matches}),
            new AqlUIPropertySearchStrategy("conan.settings.compiler",
                    new AqlDomainEnum[]{AqlDomainEnum.items, AqlDomainEnum.properties}),
            new AqlUISearchDummyResultManipulator()),

    debianName(PackageSearchType.debian, "deb.name",
            new AqlUISearchModel("debianName", "Name", "Debian Name", true,
                    new AqlComparatorEnum[]{AqlComparatorEnum.equals, AqlComparatorEnum.matches}),
            new AqlUIPropertySearchStrategy("deb.name",
                    new AqlDomainEnum[]{AqlDomainEnum.items, AqlDomainEnum.properties}),
            new AqlUISearchDummyResultManipulator()),

    debianVersion(PackageSearchType.debian, "deb.version",
            new AqlUISearchModel("debianVersion", "Version", "Debian Version", true,
                    new AqlComparatorEnum[]{AqlComparatorEnum.equals, AqlComparatorEnum.matches}),
            new AqlUIPropertySearchStrategy("deb.version",
                    new AqlDomainEnum[]{AqlDomainEnum.items, AqlDomainEnum.properties}),
            new AqlUISearchDummyResultManipulator()),

    debianPriority(PackageSearchType.debian, "deb.priority",
            new AqlUISearchModel("debianPriority", "Priority", "Debian Priority", false,
                    new AqlComparatorEnum[]{AqlComparatorEnum.equals, AqlComparatorEnum.matches}),
            new AqlUIPropertySearchStrategy("deb.priority",
                    new AqlDomainEnum[]{AqlDomainEnum.items, AqlDomainEnum.properties}),
            new AqlUISearchDummyResultManipulator()),

    debianMaintainer(PackageSearchType.debian, "deb.maintainer",
            new AqlUISearchModel("debianMaintainer", "Maintainer", "Debian Maintainer", false,
                    new AqlComparatorEnum[]{AqlComparatorEnum.equals, AqlComparatorEnum.matches}),
            new AqlUIPropertySearchStrategy("deb.maintainer",
                    new AqlDomainEnum[]{AqlDomainEnum.items, AqlDomainEnum.properties}),
            new AqlUISearchDummyResultManipulator()),

    debianDistribution(PackageSearchType.debian, DebianNaming.DISTRIBUTION_PROP,
            new AqlUISearchModel("debianDistribution", "Distribution", "Debian Distribution", false,
                    new AqlComparatorEnum[]{AqlComparatorEnum.equals, AqlComparatorEnum.matches}),
            new AqlUIPropertySearchStrategy(DebianNaming.DISTRIBUTION_PROP,
                    new AqlDomainEnum[]{AqlDomainEnum.items, AqlDomainEnum.properties}),
            new AqlUISearchDummyResultManipulator()),

    debianComponent(PackageSearchType.debian, DebianNaming.COMPONENT_PROP,
            new AqlUISearchModel("debianComponent", "Component", "Debian Component", false,
                    new AqlComparatorEnum[]{AqlComparatorEnum.equals, AqlComparatorEnum.matches}),
            new AqlUIPropertySearchStrategy(DebianNaming.COMPONENT_PROP,
                    new AqlDomainEnum[]{AqlDomainEnum.items, AqlDomainEnum.properties}),
            new AqlUISearchDummyResultManipulator()),

    debianArchitecture(PackageSearchType.debian, DebianNaming.ARCHITECTURE_PROP,
            new AqlUISearchModel("debianArchitecture", "Architecture", "Debian Architecture", false,
                    new AqlComparatorEnum[]{AqlComparatorEnum.equals, AqlComparatorEnum.matches}),
            new AqlUIPropertySearchStrategy(DebianNaming.ARCHITECTURE_PROP,
                    new AqlDomainEnum[]{AqlDomainEnum.items, AqlDomainEnum.properties}),
            new AqlUISearchDummyResultManipulator()),

    dockerV1Image(PackageSearchType.dockerV1, "path",
            new AqlUISearchModel("dockerV1Image", "Image", "Docker V1 Image", true,
                    new AqlComparatorEnum[]{AqlComparatorEnum.matches}),
            new AqlUIDockerV1ImageSearchStrategy(AqlPhysicalFieldEnum.itemPath,
                    new AqlDomainEnum[]{AqlDomainEnum.items}),
            new AqlUISearchDockerV1ResultManipulator()),

    dockerV1Tag(PackageSearchType.dockerV1, "docker.tag.name",
            new AqlUISearchModel("dockerV1Tag", "Tag", "Docker V1 Tag", true,
                    new AqlComparatorEnum[]{AqlComparatorEnum.equals, AqlComparatorEnum.matches}),
            new AqlUIPropertySearchStrategy("docker.tag.name",
                    new AqlDomainEnum[]{AqlDomainEnum.items, AqlDomainEnum.properties}),
            new AqlUISearchDummyResultManipulator()),

    dockerV2Image(PackageSearchType.dockerV2, "docker.repoName",
            new AqlUISearchModel("dockerV2Image", "Image", "Docker V2 Image", true,
                    new AqlComparatorEnum[]{AqlComparatorEnum.equals, AqlComparatorEnum.matches}),
            new AqlUIDockerV2ImageSearchStrategy("docker.repoName",
                    new AqlDomainEnum[]{AqlDomainEnum.items, AqlDomainEnum.properties}),
            new AqlUISearchDummyResultManipulator()),

    dockerV2Tag(PackageSearchType.dockerV2, "docker.manifest",
            new AqlUISearchModel("dockerV2Tag", "Tag", "Docker V2 Tag", true,
                    new AqlComparatorEnum[]{AqlComparatorEnum.equals, AqlComparatorEnum.matches}),
            new AqlUIPropertySearchStrategy("docker.manifest",
                    new AqlDomainEnum[]{AqlDomainEnum.items, AqlDomainEnum.properties}),
            new AqlUISearchDummyResultManipulator()),

    //TODO [by dan][sha2]:remove after migration-enforcing major --> don't remove the logic that puts the prop from docker without replacing this!
    dockerV2ImageDigest(PackageSearchType.dockerV2, "sha256",
            new AqlUISearchModel("dockerV2ImageDigest", "Image Digest", "Docker V2 Image Digest", false,
                    new AqlComparatorEnum[]{AqlComparatorEnum.equals, AqlComparatorEnum.matches}),
            new AqlUIDockerV2ImageDigestSearchStrategy("sha256",
                    new AqlDomainEnum[]{AqlDomainEnum.items, AqlDomainEnum.properties}),
            new AqlUISearchDummyResultManipulator()),

    gemName(PackageSearchType.gems, "gem.name",
            new AqlUISearchModel("gemName", "Name", "Gem Name", true,
                    new AqlComparatorEnum[]{AqlComparatorEnum.equals, AqlComparatorEnum.matches}),
            new AqlUIPropertySearchStrategy("gem.name",
                    new AqlDomainEnum[]{AqlDomainEnum.items, AqlDomainEnum.properties}),
            new AqlUISearchDummyResultManipulator()),

    gemVersion(PackageSearchType.gems, "gem.version",
            new AqlUISearchModel("gemVersion", "Version", "Gem Version", true,
                    new AqlComparatorEnum[]{AqlComparatorEnum.equals, AqlComparatorEnum.matches}),
            new AqlUIPropertySearchStrategy("gem.version",
                    new AqlDomainEnum[]{AqlDomainEnum.items, AqlDomainEnum.properties}),
            new AqlUISearchDummyResultManipulator()),

    nugetPackageId(PackageSearchType.nuget, "nuget.id",
            new AqlUISearchModel("nugetPackageId", "ID", "NuGet Package ID", true,
                    new AqlComparatorEnum[]{AqlComparatorEnum.equals, AqlComparatorEnum.matches}),
            new AqlUIPropertySearchStrategy("nuget.id",
                    new AqlDomainEnum[]{AqlDomainEnum.items, AqlDomainEnum.properties}),
            new AqlUISearchDummyResultManipulator()),

    nugetVersion(PackageSearchType.nuget, "nuget.version",
            new AqlUISearchModel("nugetVersion", "Version", "NuGet Package Version", true,
                    new AqlComparatorEnum[]{AqlComparatorEnum.equals, AqlComparatorEnum.matches}),
            new AqlUIPropertySearchStrategy("nuget.version",
                    new AqlDomainEnum[]{AqlDomainEnum.items, AqlDomainEnum.properties}),
            new AqlUISearchDummyResultManipulator()),

/*    nugetTags(PackageSearchType.nuget, "nuget.tags",
            new AqlUISearchModel("nugetTags", "Tags", "NuGet Tags",
                    new AqlComparatorEnum[]{AqlComparatorEnum.equals, AqlComparatorEnum.matches}),
            new AqlUIPropertySearchStrategy("nuget.tags",
                    new AqlDomainEnum[]{AqlDomainEnum.items, AqlDomainEnum.properties}),
            new AqlUISearchDummyResultManipulator()),

    nugetDigest(PackageSearchType.nuget, "nuget.digest",
            new AqlUISearchModel("nugetDigest", "Digest", "NuGet Digest",
                    new AqlComparatorEnum[]{AqlComparatorEnum.equals, AqlComparatorEnum.matches}),
            new AqlUIPropertySearchStrategy("nuget.digest",
                    new AqlDomainEnum[]{AqlDomainEnum.items, AqlDomainEnum.properties}),
            new AqlUISearchDummyResultManipulator()),*/


    npmName(PackageSearchType.npm, "npm.name",
            new AqlUISearchModel("npmName", "Name", "Npm Name", true,
                    new AqlComparatorEnum[]{AqlComparatorEnum.equals, AqlComparatorEnum.matches}),
            new AqlUINpmNameSearchStrategy("npm.name",
                    new AqlDomainEnum[]{AqlDomainEnum.items, AqlDomainEnum.properties}),
            new AqlUISearchDummyResultManipulator()),

    npmVersion(PackageSearchType.npm, "npm.version",
            new AqlUISearchModel("npmVersion", "Version", "Npm Version", true,
                    new AqlComparatorEnum[]{AqlComparatorEnum.equals, AqlComparatorEnum.matches}),
            new AqlUIPropertySearchStrategy("npm.version",
                    new AqlDomainEnum[]{AqlDomainEnum.items, AqlDomainEnum.properties}),
            new AqlUISearchDummyResultManipulator()),

    npmScope(PackageSearchType.npm, "npm.name",
            new AqlUISearchModel("npmScope", "Scope", "Npm Scope", false,
                    new AqlComparatorEnum[]{AqlComparatorEnum.equals, AqlComparatorEnum.matches}),
            new AqlUINpmScopeSearchStrategy("npm.name",
                    new AqlDomainEnum[]{AqlDomainEnum.items, AqlDomainEnum.properties}),
            new AqlUISearchNpmResultManipulator()),

    opkgName(PackageSearchType.opkg, "opkg.name",
            new AqlUISearchModel("opkgName", "Name", "Opkg Name", true,
                    new AqlComparatorEnum[]{AqlComparatorEnum.equals, AqlComparatorEnum.matches}),
            new AqlUIPropertySearchStrategy("opkg.name",
                    new AqlDomainEnum[]{AqlDomainEnum.items, AqlDomainEnum.properties}),
            new AqlUISearchDummyResultManipulator()),

    opkgVersion(PackageSearchType.opkg, "opkg.version",
            new AqlUISearchModel("opkgVersion", "Version", "Opkg Version", true,
                    new AqlComparatorEnum[]{AqlComparatorEnum.equals, AqlComparatorEnum.matches}),
            new AqlUIPropertySearchStrategy("opkg.version",
                    new AqlDomainEnum[]{AqlDomainEnum.items, AqlDomainEnum.properties}),
            new AqlUISearchDummyResultManipulator()),

    opkgArchitecture(PackageSearchType.opkg, "opkg.architecture",
            new AqlUISearchModel("opkgArchitecture", "Architecture", "Opkg Architecture", false,
                    new AqlComparatorEnum[]{AqlComparatorEnum.equals, AqlComparatorEnum.matches}),
            new AqlUIPropertySearchStrategy("opkg.architecture",
                    new AqlDomainEnum[]{AqlDomainEnum.items, AqlDomainEnum.properties}),
            new AqlUISearchDummyResultManipulator()),

    opkgPriority(PackageSearchType.opkg, "opkg.priority",
            new AqlUISearchModel("opkgPriority", "Priority", "Opkg Priority", false,
                    new AqlComparatorEnum[]{AqlComparatorEnum.equals, AqlComparatorEnum.matches}),
            new AqlUIPropertySearchStrategy("opkg.priority",
                    new AqlDomainEnum[]{AqlDomainEnum.items, AqlDomainEnum.properties}),
            new AqlUISearchDummyResultManipulator()),

    opkgMaintainer(PackageSearchType.opkg, "opkg.maintainer",
            new AqlUISearchModel("opkgMaintainer", "Maintainer", "Opkg Maintainer", false,
                    new AqlComparatorEnum[]{AqlComparatorEnum.equals, AqlComparatorEnum.matches}),
            new AqlUIPropertySearchStrategy("opkg.maintainer",
                    new AqlDomainEnum[]{AqlDomainEnum.items, AqlDomainEnum.properties}),
            new AqlUISearchDummyResultManipulator()),

    pypiName(PackageSearchType.pypi, "pypi.name",
            new AqlUISearchModel("pypiName", "Name", "PyPi Name", true,
                    new AqlComparatorEnum[]{AqlComparatorEnum.equals, AqlComparatorEnum.matches}),
            new AqlUIPropertySearchStrategy("pypi.name",
                    new AqlDomainEnum[]{AqlDomainEnum.items, AqlDomainEnum.properties}),
            new AqlUISearchDummyResultManipulator()),

    pypiVersion(PackageSearchType.pypi, "pypi.version",
            new AqlUISearchModel("pypiVersion", "Version", "Pypi Version", true,
                    new AqlComparatorEnum[]{AqlComparatorEnum.equals, AqlComparatorEnum.matches}),
            new AqlUIPropertySearchStrategy("pypi.version",
                    new AqlDomainEnum[]{AqlDomainEnum.items, AqlDomainEnum.properties}),
            new AqlUISearchDummyResultManipulator()),

    puppetName(PackageSearchType.puppet, "puppet.name",
            new AqlUISearchModel("puppetName", "Name", "Puppet Name", true,
                    new AqlComparatorEnum[]{AqlComparatorEnum.equals, AqlComparatorEnum.matches}),
            new AqlUIPropertySearchStrategy("puppet.name",
                    new AqlDomainEnum[]{AqlDomainEnum.items, AqlDomainEnum.properties}),
            new AqlUISearchDummyResultManipulator()),

    puppetVersion(PackageSearchType.puppet, "puppet.version",
            new AqlUISearchModel("puppetVersion", "Version", "Puppet Version", true,
                    new AqlComparatorEnum[]{AqlComparatorEnum.equals, AqlComparatorEnum.matches}),
            new AqlUIPropertySearchStrategy("puppet.version",
                    new AqlDomainEnum[]{AqlDomainEnum.items, AqlDomainEnum.properties}),
            new AqlUISearchDummyResultManipulator()),

    rpmName(PackageSearchType.rpm, "rpm.metadata.name",
            new AqlUISearchModel("rpmName", "Name", "RPM Name", true,
                    new AqlComparatorEnum[]{AqlComparatorEnum.equals, AqlComparatorEnum.matches}),
            new AqlUIPropertySearchStrategy("rpm.metadata.name",
                    new AqlDomainEnum[]{AqlDomainEnum.items, AqlDomainEnum.properties}),
            new AqlUISearchDummyResultManipulator()),

    rpmVersion(PackageSearchType.rpm, "rpm.metadata.version",
            new AqlUISearchModel("rpmVersion", "Version", "RPM Version", true,
                    new AqlComparatorEnum[]{AqlComparatorEnum.equals, AqlComparatorEnum.matches}),
            new AqlUIPropertySearchStrategy("rpm.metadata.version",
                    new AqlDomainEnum[]{AqlDomainEnum.items, AqlDomainEnum.properties}),
            new AqlUISearchDummyResultManipulator()),

    rpmArchitecture(PackageSearchType.rpm, "rpm.metadata.arch",
            new AqlUISearchModel("rpmArchitecture", "Architecture", "RPM Architecture", false,
                    new AqlComparatorEnum[]{AqlComparatorEnum.equals, AqlComparatorEnum.matches}),
            new AqlUIPropertySearchStrategy("rpm.metadata.arch",
                    new AqlDomainEnum[]{AqlDomainEnum.items, AqlDomainEnum.properties}),
            new AqlUISearchDummyResultManipulator()),

   /* rpmRelease(PackageSearchType.rpm, "rpm.metadata.release",
            new AqlUISearchModel("rpmRelease", "Release", "RPM Release",
                    new AqlComparatorEnum[]{AqlComparatorEnum.equals, AqlComparatorEnum.matches}),
            new AqlUIPropertySearchStrategy("rpm.metadata.release",
                    new AqlDomainEnum[]{AqlDomainEnum.items, AqlDomainEnum.properties}),
            new AqlUISearchDummyResultManipulator()),*/

    vagrantName(PackageSearchType.vagrant, "box_name",
            new AqlUISearchModel("vagrantName", "Box Name", "Vagrant Box Name", true,
                    new AqlComparatorEnum[]{AqlComparatorEnum.equals, AqlComparatorEnum.matches}),
            new AqlUIPropertySearchStrategy("box_name",
                    new AqlDomainEnum[]{AqlDomainEnum.items, AqlDomainEnum.properties}),
            new AqlUISearchDummyResultManipulator()),

    vagrantVersion(PackageSearchType.vagrant, "box_version",
            new AqlUISearchModel("vagrantVersion", "Box Version", "Vagrant Box Version", true,
                    new AqlComparatorEnum[]{AqlComparatorEnum.equals, AqlComparatorEnum.matches}),
            new AqlUIPropertySearchStrategy("box_version",
                    new AqlDomainEnum[]{AqlDomainEnum.items, AqlDomainEnum.properties}),
            new AqlUISearchDummyResultManipulator()),

    vagrantProvider(PackageSearchType.vagrant, "box_provider",
            new AqlUISearchModel("vagrantProvider", "Box Provider", "Vagrant Box Provider", false,
                    new AqlComparatorEnum[]{AqlComparatorEnum.equals, AqlComparatorEnum.matches}),
            new AqlUIPropertySearchStrategy("box_provider",
                    new AqlDomainEnum[]{AqlDomainEnum.items, AqlDomainEnum.properties}),
            new AqlUISearchDummyResultManipulator());

    PackageSearchType type;
    String aqlName;
    AqlUISearchModel model;
    AqlUISearchStrategy strategy;
    AqlUISearchResultManipulator resultManipulator;

    PackageSearchCriteria(PackageSearchType type, String aqlName, AqlUISearchModel model,
            AqlUISearchStrategy strategy, AqlUISearchResultManipulator resultManipulator) {
        this.type = type;
        this.aqlName = aqlName;
        this.model = model;
        this.strategy = strategy;
        this.resultManipulator = resultManipulator;
    }

    public PackageSearchType getType() {
        return type;
    }

    public AqlUISearchModel getModel() {
        return model;
    }

    public AqlUISearchStrategy getStrategy() {
        return strategy;
    }

    public static AqlUISearchStrategy getStrategyByFieldId(String id) {
        return valueOf(id).strategy;
    }

    public AqlUISearchResultManipulator getResultManipulator() {
        return resultManipulator;
    }

    /**
     * Returns the criteria that matches the AQL field name or the property key that {@param aqlName} references
     */
    public static PackageSearchCriteria getCriteriaByAqlFieldOrPropName(String aqlName) {
        return Stream.of(values())
                .filter(value -> value.aqlName.equalsIgnoreCase(aqlName))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported field or property '" + aqlName + "'."));
    }

    public static List<PackageSearchCriteria> getCriteriaByPackage(String packageType) {
        return Stream.of(values())
                .filter(searchCriterion -> searchCriterion.type.equals(PackageSearchType.getById(packageType)))
                .collect(Collectors.toList());
    }

    public static List<PackageSearchCriteria> getCriteriaByPackage(PackageSearchType packageType) {
        return Stream.of(values())
                .filter(searchCriterion -> searchCriterion.type.equals(packageType))
                .collect(Collectors.toList());
    }

    /**
     * Returns the {@link AqlUISearchResultManipulator} the AQL field name or the property key that {@param aqlName}
     * references
     */
    public static AqlUISearchResultManipulator getResultManipulatorByAqlFieldOrPropName(String aqlName) {
        return Stream.of(values())
                .filter(value -> value.aqlName.equalsIgnoreCase(aqlName))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported field or property '" + aqlName + "'."))
                .getResultManipulator();
    }

    public static List<AqlUISearchResultManipulator> getResultManipulatorsByPackage(PackageSearchType packageType) {
        return getCriteriaByPackage(packageType)
                .stream()
                .map(PackageSearchCriteria::getResultManipulator)
                .collect(Collectors.toList());
    }

    public static PackageSearchType getPackageTypeByFieldId(String fieldId) {
        try {
            return valueOf(fieldId).getType();
        } catch (IllegalArgumentException iae) {
            //no such fieldId
        }
        return null;
    }

    public static List<AqlUISearchStrategy> getStartegiesByPackageSearchType(PackageSearchType type) {
        return getCriteriaByPackage(type).stream()
                .map(PackageSearchCriteria::getStrategy)
                .collect(Collectors.toList());
    }

    //Keep packages ordered alphabetically please!
    public enum PackageSearchType {
        bower(RepoType.Bower, true, "bower", true, DUMMY_MERGER),
        chef(RepoType.Chef, true, "chef", true, DUMMY_MERGER),
        cocoapods(RepoType.CocoaPods, true, "cocoapods", true, DUMMY_MERGER),
        composer(RepoType.Composer, true, "composer", true, DUMMY_MERGER),
        //downloadEnabled is false - no sense in downloading a conan file
        conan(RepoType.Conan, false, "conan", false, new ConanPackageSearchResultMerger()),
        debian(RepoType.Debian, false, "deb", true, DUMMY_MERGER),
        //downloadEnabled is false - no sense in downloading a manifest.json or tag.json for docker images
        dockerV1(RepoType.Docker, true, "docker", false, DUMMY_MERGER),
        dockerV2(RepoType.Docker, true, "docker", false, DUMMY_MERGER),
        gavc(RepoType.Maven, true, "pom", true, DUMMY_MERGER),
        gems(RepoType.Gems, false, "ruby-gems", true, DUMMY_MERGER),
        helm(RepoType.Helm, false, "helm", true, DUMMY_MERGER),
        nuget(RepoType.NuGet, true, "nuget", true, DUMMY_MERGER),
        npm(RepoType.Npm, true, "npm", true, DUMMY_MERGER),
        opkg(RepoType.Opkg, false, "opkg", true, DUMMY_MERGER),
        pypi(RepoType.Pypi, false, "pypi", true, DUMMY_MERGER),
        puppet(RepoType.Puppet, false, "puppet", true, DUMMY_MERGER),
        rpm(RepoType.YUM, true, "yum", true, DUMMY_MERGER),
        vagrant(RepoType.Vagrant, false, "vagrant", true, DUMMY_MERGER);
        /*, all(""),*/ /*gitlfs(RepoType.GitLfs, false, "git-lfs"),*/

        final boolean remoteCachesProps;
        final RepoType repoType;
        final String icon;
        final boolean downloadEnabled;
        final PackageSearchResultMerger resultMerger;

        PackageSearchType(RepoType repoType, boolean remoteCachesProps, String icon, boolean downloadEnabled,
                PackageSearchResultMerger resultMerger) {
            this.repoType = repoType;
            this.remoteCachesProps = remoteCachesProps;
            this.icon = icon;
            this.downloadEnabled = downloadEnabled;
            this.resultMerger = resultMerger;
        }

        public static PackageSearchType getById(String id) {
            for (PackageSearchType type : values()) {
                if (type.name().equalsIgnoreCase(id)) {
                    return type;
                }
            }
            throw new UnsupportedOperationException("Unsupported package " + id);
        }

        public String getDisplayName() {
            if (this.equals(dockerV1)) {
                return "Docker V1";
            } else if (this.equals(dockerV2)) {
                return "Docker V2";
            } else if (this.equals(rpm)) {
                return "RPM";
            } else if (this.equals(gavc)) {
                return "GAVC";
            } else if (this.equals(pypi)) {
                return "PyPI";
            }
            return repoType.name();
        }

        public boolean isRemoteCachesProps() {
            return remoteCachesProps;
        }

        public boolean isDownloadEnabled() {
            return downloadEnabled;
        }

        public String getId() {
            return this.name();
        }

        public RepoType getRepoType() {
            return repoType;
        }

        public String getIcon() {
            return icon;
        }

        public PackageSearchResultMerger getResultMerger() {
            return resultMerger;
        }
    }
}

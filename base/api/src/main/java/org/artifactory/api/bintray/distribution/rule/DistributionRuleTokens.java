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

package org.artifactory.api.bintray.distribution.rule;

import com.google.common.collect.Lists;
import org.artifactory.descriptor.repo.RepoLayout;
import org.artifactory.descriptor.repo.RepoType;
import org.artifactory.mime.DebianNaming;
import org.artifactory.util.RepoLayoutUtils;
import org.artifactory.util.distribution.DistributionConstants;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.artifactory.util.distribution.DistributionConstants.*;

/**
 * Tokens that are available to each rule based on its {@link RepoType}
 *
 * @author Dan Feldman
 */
public enum DistributionRuleTokens {

    bowerName(RepoType.Bower, () -> new DistributionRulePropertyToken(Keys.packageName.key, "bower.name")),
    bowerVersion(RepoType.Bower, () -> new DistributionRulePropertyToken(Keys.packageVersion.key, "bower.version")),
    cocoaPodsName(RepoType.CocoaPods, () -> new DistributionRulePropertyToken(Keys.packageName.key, "pods.name")),
    cocoaPodsVersion(RepoType.CocoaPods, () -> new DistributionRulePropertyToken(Keys.packageVersion.key, "pods.version")),
    debianName(RepoType.Debian, () -> new DistributionRulePropertyToken(Keys.packageName.key, DebianNaming.DEBIAN_NAME)),
    debianVersion(RepoType.Debian, () -> new DistributionRulePropertyToken(Keys.packageVersion.key, DebianNaming.DEBIAN_VERSION)),
    debianDistribution(RepoType.Debian, () -> new DistributionRulePropertyToken(Keys.debDist.key, DebianNaming.DISTRIBUTION_PROP)),
    debianComponent(RepoType.Debian, () -> new DistributionRulePropertyToken(Keys.debComp.key, DebianNaming.COMPONENT_PROP)),
    debianArchitecture(RepoType.Debian, () -> new DistributionRulePropertyToken(Keys.architecture.key, DebianNaming.ARCHITECTURE_PROP)),
    dockerImage(RepoType.Docker, () -> new DistributionRulePropertyToken(Keys.dockerImage.key, "docker.repoName")),
    dockerTag(RepoType.Docker, () -> new DistributionRulePropertyToken(Keys.dockerTag.key, "docker.manifest")),
    // gemName(RepoType.Gems, () -> new DistributionRulePropertyToken(Keys.packageName.key, "gem.name")),
    // gemVersion(RepoType.Gems, () -> new DistributionRulePropertyToken(Keys.packageVersion.key, "gem.version")),
    gradleOrg(RepoType.Gradle, () -> new DistributionRuleLayoutToken(Keys.layoutOrg.key)),
    gradleModule(RepoType.Gradle, () -> new DistributionRuleLayoutToken(Keys.layoutModule.key)),
    gradleBaseRev(RepoType.Gradle, () -> new DistributionRuleLayoutToken(Keys.layoutBaseRev.key)),
    // gradleFolderRev(RepoType.Gradle, () -> new DistributionRuleLayoutToken(Keys.layoutFolderRev.key)),
    // gradleFileRev(RepoType.Gradle, () -> new DistributionRuleLayoutToken(Keys.layoutFileRev.key)),
    gradleClassifier(RepoType.Gradle, () -> new DistributionRuleLayoutToken(Keys.layoutClassifier.key)),
    gradleExt(RepoType.Gradle, () -> new DistributionRuleLayoutToken(Keys.layoutExt.key)),
    ivyOrg(RepoType.Ivy, () -> new DistributionRuleLayoutToken(Keys.layoutOrg.key)),
    ivyModule(RepoType.Ivy, () -> new DistributionRuleLayoutToken(Keys.layoutModule.key)),
    ivyBaseRev(RepoType.Ivy, () -> new DistributionRuleLayoutToken(Keys.layoutBaseRev.key)),
    // ivyFolderRev(RepoType.Ivy, () -> new DistributionRuleLayoutToken(Keys.layoutFolderRev.key)),
    ivyType(RepoType.Ivy, () -> new DistributionRuleLayoutToken(Keys.layoutType.key)),
    ivyClassifier(RepoType.Ivy, () -> new DistributionRuleLayoutToken(Keys.layoutClassifier.key)),
    // ivyFileRev(RepoType.Ivy, () -> new DistributionRuleLayoutToken(Keys.layoutFileRev.key)),
    ivyExt(RepoType.Ivy, () -> new DistributionRuleLayoutToken(Keys.layoutExt.key)),
    mavenOrg(RepoType.Maven, () -> new DistributionRuleLayoutToken(Keys.layoutOrgPath.key)),
    mavenModule(RepoType.Maven, () -> new DistributionRuleLayoutToken(Keys.layoutModule.key)),
    mavenBaseRev(RepoType.Maven, () -> new DistributionRuleLayoutToken(Keys.layoutBaseRev.key)),
    // mavenFolderRev(RepoType.Maven, () -> new DistributionRuleLayoutToken(Keys.layoutFolderRev.key)),
    // mavenFileRev(RepoType.Maven, () -> new DistributionRuleLayoutToken(Keys.layoutFileRev.key)),
    mavenClassifier(RepoType.Maven, () -> new DistributionRuleLayoutToken(Keys.layoutClassifier.key)),
    mavenExt(RepoType.Maven, () -> new DistributionRuleLayoutToken(Keys.layoutExt.key)),
    npmName(RepoType.Npm, () -> new DistributionRulePropertyToken(Keys.packageName.key, "npm.name")),
    npmVersion(RepoType.Npm, () -> new DistributionRulePropertyToken(Keys.packageVersion.key, "npm.version")),
    // npmScope(RepoType.Npm, () -> new DistributionRulePropertyToken(Keys.scope.key, "npm.name")), //TODO [by dan]: special handle scope
    nuGetName(RepoType.NuGet, () -> new DistributionRulePropertyToken(Keys.packageName.key, "nuget.id")),
    nuGetVersion(RepoType.NuGet, () -> new DistributionRulePropertyToken(Keys.packageVersion.key, "nuget.version")),
    opkgName(RepoType.Opkg, () -> new DistributionRulePropertyToken(Keys.packageName.key, "opkg.name")),
    opkgVersion(RepoType.Opkg, () -> new DistributionRulePropertyToken(Keys.packageVersion.key, "opkg.version")),
    opkgArchitecture(RepoType.Opkg, () -> new DistributionRulePropertyToken(Keys.architecture.key, "opkg.architecture")),
    // pypiName(RepoType.Pypi, () -> new DistributionRulePropertyToken(Keys.packageName.key, "pypi.name")),
    // pypiVersion(RepoType.Pypi, () -> new DistributionRulePropertyToken(Keys.packageVersion.key, "pypi.version")),
    rpmName(RepoType.YUM, () -> new DistributionRulePropertyToken(Keys.packageName.key, "rpm.metadata.name")),
    rpmVersion(RepoType.YUM, () -> new DistributionRulePropertyToken(Keys.packageVersion.key, "rpm.metadata.version")),
    rpmArchitecture(RepoType.YUM, () -> new DistributionRulePropertyToken(Keys.architecture.key, "rpm.metadata.arch")),
    sbtOrg(RepoType.SBT, () -> new DistributionRuleLayoutToken(Keys.layoutOrg.key)),
    sbtModule(RepoType.SBT, () -> new DistributionRuleLayoutToken(Keys.layoutModule.key)),
    sbtScalaVersion(RepoType.SBT, () -> new DistributionRuleLayoutToken(Keys.layoutScalaVer.key)),
    sbtSbtVersion(RepoType.SBT, () -> new DistributionRuleLayoutToken(Keys.layoutSbtVer.key)),
    sbtBaseRev(RepoType.SBT, () -> new DistributionRuleLayoutToken(Keys.layoutBaseRev.key)),
    sbtType(RepoType.SBT, () -> new DistributionRuleLayoutToken(Keys.layoutType.key)),
    sbtClassifier(RepoType.SBT, () -> new DistributionRuleLayoutToken(Keys.layoutClassifier.key)),
    sbtExt(RepoType.SBT, () -> new DistributionRuleLayoutToken(Keys.layoutExt.key)),
    vagrantName(RepoType.Vagrant, () -> new DistributionRulePropertyToken(Keys.packageName.key, "box_name")),
    vagrantVersion(RepoType.Vagrant, () -> new DistributionRulePropertyToken(Keys.packageVersion.key, "box_version")),
    vagrantProvider(RepoType.Vagrant, () -> new DistributionRulePropertyToken(Keys.vagrantProvider.key, "box_provider"));
    // vcsOrg(RepoType.VCS, () -> new DistributionRulePathToken(Keys.vcsOrg.key, 2)),
    // vcsRepo(RepoType.VCS, () -> new DistributionRulePathToken(Keys.vcsRepo.key, 3)),
    // vcsTag(RepoType.VCS, () -> new DistributionRulePropertyToken(Keys.vcsTag.key, "vcs.tag")),
    // vcsBranch(RepoType.VCS, () -> new DistributionRulePropertyToken(Keys.vcsBranch.key, "vcs.branch"));

    final RepoType type;
    final DistributionRuleTokenFactory tokenFactory;

    DistributionRuleTokens(RepoType type, DistributionRuleTokenFactory tokenFactory) {
        this.type = type;
        this.tokenFactory = tokenFactory;
    }

    public DistributionRuleToken getToken() {
        return tokenFactory.create();
    }

    DistributionRuleTokenFactory getTokenFactory() {
        return tokenFactory;
    }

    public static final DistributionRuleTokenFactory PRODUCT_NAME_TOKEN_FACTORY = () -> new DistributionRulePropertyToken(PRODUCT_NAME_TOKEN, PRODUCT_NAME_DUMMY_PROP);

    public static DistributionRuleToken getProductNameToken() {
        return PRODUCT_NAME_TOKEN_FACTORY.create();
    }

    /**
     * @return all tokens available to the given {@param type}, with the default path and product name tokens.
     */
    public static Set<DistributionRuleToken> tokensByType(RepoType type, @Nullable RepoLayout layout) {
        Set<DistributionRuleToken> allTokens = new HashSet<>();
        if (type == null) {
            return allTokens;
        }
        allTokens.add(new DistributionRulePathToken(PATH_TOKEN));
        allTokens.addAll(Stream.of(Types.values())
                .filter(types -> type.equals(types.repoType))
                .findAny()
                .orElse(Types.dummy)
                .tokenFactories.stream()
                .map(DistributionRuleTokenFactory::create)
                .collect(Collectors.toSet()));
        if ((type.isMavenGroup() || type.equals(RepoType.Generic)) && layout != null) {
            //Only add layout tokens if rule type is Maven-y or for generic rules.
            allTokens.addAll(getResolverCompatibleLayoutTokens(layout));
        }
        return allTokens;
    }

    private static List<DistributionRuleTokenFactory> tokensByPackageType(RepoType type) {
        return Stream.of(values())
                .filter(tokenKey -> tokenKey.type.equals(type))
                .map(DistributionRuleTokens::getTokenFactory)
                .collect(Collectors.toList());
    }

    /**
     * Performance wise, this is not the best place for getting layout tokens as we already cache them in
     * DistributionService::addLayoutTokens but it makes it quite unreadable to have actual tokens added in the
     * same place that also resolves them.
     * If this is identified as a performance bottleneck return this logic to where it was in commit d806bf3
     */
    private static List<DistributionRuleLayoutToken> getResolverCompatibleLayoutTokens(RepoLayout layout) {
        return RepoLayoutUtils.getLayoutTokens(layout).stream()
                .map(DistributionConstants::stripTokenBrackets)
                .map(DistributionConstants::wrapToken)
                .map(DistributionRuleLayoutToken::new)
                .collect(Collectors.toList());
    }

    public enum Types {
        bower(RepoType.Bower, tokensByPackageType(RepoType.Bower)),
        cocoaPods(RepoType.CocoaPods, tokensByPackageType(RepoType.CocoaPods)),
        debian(RepoType.Debian, tokensByPackageType(RepoType.Debian)),
        docker(RepoType.Docker, tokensByPackageType(RepoType.Docker)),
        // gems(RepoType.Gems, tokensByPackageType(RepoType.Gems)),
        gradle(RepoType.Gradle, tokensByPackageType(RepoType.Gradle)),
        ivy(RepoType.Ivy, tokensByPackageType(RepoType.Ivy)),
        maven(RepoType.Maven, tokensByPackageType(RepoType.Maven)),
        npm(RepoType.Npm, tokensByPackageType(RepoType.Npm)),
        nuGet(RepoType.NuGet, tokensByPackageType(RepoType.NuGet)),
        opkg(RepoType.Opkg, tokensByPackageType(RepoType.Opkg)),
        // pypi(RepoType.Pypi, tokensByPackageType(RepoType.Pypi)),
        rpm(RepoType.YUM, tokensByPackageType(RepoType.YUM)),
        sbt(RepoType.SBT, tokensByPackageType(RepoType.SBT)),
        vagrant(RepoType.Vagrant, tokensByPackageType(RepoType.Vagrant)),
        // vcs(RepoType.VCS, tokensByPackageType(RepoType.VCS)),
        dummy(null, new ArrayList<>());

        final RepoType repoType;
        final List<DistributionRuleTokenFactory> tokenFactories;

        Types(RepoType repoType, List<DistributionRuleTokenFactory> tokenFactories) {
            this.repoType = repoType;
            this.tokenFactories = Collections.unmodifiableList(Lists.newArrayList(tokenFactories));
        }
    }

    public enum Keys {
        packageName(PACKAGE_NAME_TOKEN),
        packageVersion(PACKAGE_VERSION_TOKEN),
        debDist(wrapToken("distribution")),
        debComp(wrapToken("component")),
        architecture(ARCHITECTURE_TOKEN),
        vagrantProvider(wrapToken("boxProvider")),
        layoutOrg(wrapToken("org")),
        layoutOrgPath(wrapToken("orgPath")),
        layoutModule(MODULE_TOKEN),
        layoutBaseRev(BASE_REV_TOKEN),
        layoutFolderRev(wrapToken("folderItegRev")),
        layoutFileRev(wrapToken("fileItegRev")),
        layoutClassifier(wrapToken("classifier")),
        layoutType(wrapToken("type")),
        layoutExt(wrapToken("ext")),
        layoutScalaVer(wrapToken("scalaVersion")),
        layoutSbtVer(wrapToken("sbtVersion")),
        // vcsTag(VCS_TAG_TOKEN),
        // vcsRepo(VCS_REPO_TOKEN),
        // vcsOrg(wrapToken("vcsOrg")),
        // vcsBranch(wrapToken("vcsBranch")),
        scope(wrapToken("scope")),
        dockerImage(DOCKER_IMAGE_TOKEN),
        dockerTag(DOCKER_TAG_TOKEN);

        public final String key;

        Keys(String key) {
            this.key = key;
        }
    }
}

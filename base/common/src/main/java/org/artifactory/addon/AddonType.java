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

package org.artifactory.addon;

/**
 * @author Yoav Aharoni
 */
public enum AddonType {
    //
    //PLEASE MAKE SURE THESE DETAILS ARE CONSISTENT WITH THE ONES IN THE PROPERTY FILES
    //
    AOL("aol", "Artifactory Online", -1, new String[]{"all"}, "pro", "aol"),
    BUILD("build", "Build Integration", 100, new String[]{"all", "features"}, "pro", "Build+Integration"),
    XRAY("xray", "JFrog Xray", 500, new String[]{"all", "features", "ecosystem"}, "pro", "Welcome+to+JFrog+Xray"),
    MULTIPUSH("multipush", "Multipush Replication", 100, new String[]{"all", "enterprise"}, "ent", "Repository+Replication#RepositoryReplication-Multi-pushReplication"),
    LICENSES("license", "License Control", 200, new String[]{"all", "features"}, "pro", "License+Control"),
    REST("rest", "Advanced REST", 300, new String[]{"all", "features"}, "pro", "Artifactory+REST+API"),
    LDAP("ldap", "LDAP Groups", 400, new String[]{"all", "features"}, "pro", "Ldap+Groups"),
    REPLICATION("replication", "Repository Replication", 500, new String[]{"all", "features"}, "pro", "Repository+Replication"),
    PROPERTIES("properties", "Properties", 600, new String[]{"all", "features"}, "pro", "Properties"),
    SEARCH("search", "Smart Searches", 700, new String[]{"all", "features"}, "pro", "Smart+Searches"),
    PLUGINS("plugins", "User Plugins", 800, new String[]{"all", "features"}, "pro", "User+Plugins"),
    YUM("rpm", "RPM", 900, new String[]{"all", "packageManagement"}, "pro", "RPM+Repositories"),
    P2("p2", "P2", 1000, new String[]{"all", "packageManagement"}, "pro", "P2+Repositories"),
    NUGET("nuget", "NuGet", 1100, new String[]{"all", "packageManagement"}, "pro", "Nuget+Repositories"),
    LAYOUTS("layouts", "Repository Layouts", 1200, new String[]{"all", "features"}, "pro", "Repository+Layouts"),
    FILTERED_RESOURCES("filtered-resources", "Filtered Resources", 1300, new String[]{"all", "features"}, "pro", "Filtered+Resources"),
    SSO("sso", "Crowd & SSO", 1400, new String[]{"all"}, "pro", "Atlassian+Crowd+and+JIRA+Integration"),
    SSH("ssh", "SSH", 500, new String[]{"all", "features"}, "oss", "SSH+Integration"),
    WATCH("watch", "Watches", 1500, new String[]{"all", "features"}, "pro", "Watches"),
    WEBSTART("webstart", "Jar Signing", 1600, new String[]{"all", "features"}, "pro","WebStart+and+Jar+Signing"),
    GEMS("gems", "RubyGems", 1100, new String[]{"all", "packageManagement"}, "pro", "RubyGems+Repositories") ,
    NPM("npm", "npm", 860, new String[]{"all", "packageManagement"}, "pro", "NPM+Registry"),
    BOWER("bower", "Bower", 870, new String[]{"all", "packageManagement"}, "pro", "Bower+Repositories"),
    COCOAPODS("cocoapods", "CocoaPods", 880, new String[]{"all", "packageManagement"}, "pro", "CocoaPods+Repositories"),
    CONAN("conan", "Conan", 890, new String[]{"all", "packageManagement"}, "pro", "Conan+Repositories"),
    DEBIAN("debian", "Debian", 900, new String[]{"all", "packageManagement"}, "pro", "Debian+Repositories"),
    DISTRIBUTION("distribution", "JFrog Bintray Distribution", 930, new String[]{"all", "ecosystem"}, "oss", "Distribution+Repository"),
    OPKG("opkg", "Opkg", 700, new String[]{"all", "packageManagement"}, "pro", "Opkg+Repositories"),
    PYPI("pypi", "PyPI", 970, new String[]{"all", "packageManagement"}, "pro", "PyPI+Repositories"),
    PUPPET("puppet", "Puppet", 990, new String[]{"all", "packageManagement"}, "pro", "Puppet+Repositories"),
    DOCKER("docker", "Docker", 910, new String[]{"all", "packageManagement"}, "pro", "Docker+Repositories"),
    VAGRANT("vagrant", "Vagrant", 915, new String[]{"all", "packageManagement"}, "pro", "Vagrant+Repositories"),
    VCS("vcs", "VCS", 920, new String[]{"all", "packageManagement"}, "pro", "VCS+Repositories"),
    GITLFS("git-lfs", "Git LFS", 930, new String[]{"all", "packageManagement"}, "pro", "Git+LFS+Repositories"),
    HA("ha", "High Availability", 2000, new String[]{"all", "enterprise"}, "ent", "Artifactory+High+Availability"),
    S3("s3fileStore", "S3 Object Store", 2000, new String[]{"all", "enterprise"}, "ent", "S3+Object+Storage"),
    GCS("gcs", "Google Cloud Storage", 2000, new String[]{"all", "enterprise"}, "ent", "Google+Cloud+Storage"),
    HDFS("hdfsFileStore", "HDFS", 2020, new String[]{"all", "enterprise"}, "ent", "HDFS+Storage" ),
    SHARDING("sharding", "Sharding", 2020, new String[]{"all", "enterprise"}, "ent", "Filestore+Sharding" ),
    AQL("aql", "AQL", 2000, new String[]{"all", "features"}, "oss", "Artifactory+Query+Language"),
    MAVEN_PLUGIN("maven", "Maven Plugin", 2000, new String[]{"all", "ecosystem"}, "oss", "Maven+Artifactory+Plugin"),
    GRADLE_PLUGIN("gradle", "Gradle Plugin", 2000, new String[]{"all", "ecosystem"}, "oss", "Gradle+Artifactory+Plugin"),
    JENKINS_PLUGIN("jenkins", "Jenkins Plugin", 2000, new String[]{"all", "ecosystem"}, "oss", "Jenkins+(Hudson)+Artifactory+Plug-in"),
    BAMBOO_PLUGIN("bamboo", "Bamboo Plugin", 2000, new String[]{"all", "ecosystem"}, "oss", "Bamboo+Artifactory+Plug-in"),
    TC_PLUGIN("teamcity", "TeamCity Plugin", 2000, new String[]{"all", "ecosystem"}, "oss", "TeamCity+Artifactory+Plug-in"),
    MSBUILD_PLUGIN("msbuild", "MSBuild/TFS Plugin", 2000, new String[]{"all", "ecosystem"}, "oss", "MSBuild+Artifactory+Plugin"),
    BINTRAY_INTEGRATION("bintray-integration", "Bintray Integration", 2000, new String[]{"all", "ecosystem"}, "oss", "Bintray+Integration"),
    JFROG_CLI("jfrog-cli", "JFrog CLI", 930, new String[]{"all", "ecosystem"}, "jfrog-cli","Welcome+to+JFrog+CLI"),
    SMART_REPO("smart-repo", "Smart Remote Repo", 2000, new String[]{"all", "features"}, "pro", "Smart+Remote+Repositories"),
    SUMOLOGIC("sumo-logic", "Sumo Logic", 2000, new String[]{"all", "ecosystem"}, "oss", "Log+Analytics"),
    OAUTH("oauth", "OAuth", 2000, new String[]{"all", "features"}, "pro", "OAuth+Integration"),
    SBT("sbt", "SBT", 2000, new String[]{"all", "packageManagement"}, "oss", "SBT+Repositories"),
    IVY("ivy", "Ivy Plugin", 2000, new String[]{"all", "ecosystem"}, "oss", "Working+with+Ivy"),
    COMPOSER("composer", "PHP Composer", 2100, new String[]{"all", "packageManagement"}, "pro", "PHP+Composer+Repositories"),
    CHEF("chef", "Chef Cookbook", 2200, new String[]{"all", "packageManagement"}, "pro", "Chef+Supermarket"),
    HELM("helm", "Helm", 900, new String[]{"all", "packageManagement"}, "pro", "Helm+Charts+Repositories");

    private String addonName;
    private String addonDisplayName;
    private int displayOrdinal;
    private String[] categories;
    private String type;
    private String configureUrlSuffix;

    AddonType(String addonName, String addonDisplayName, int displayOrdinal, String[] categories, String type, String configureUrlSuffix) {
        this.addonName = addonName;
        this.addonDisplayName = addonDisplayName;
        this.displayOrdinal = displayOrdinal;
        this.categories = categories;
        this.type = type;
        this.configureUrlSuffix = configureUrlSuffix;
    }

    public String getAddonDisplayName() {
        return addonDisplayName;
    }

    public String getAddonName() {
        return addonName;
    }

    public int getDisplayOrdinal() {
        return displayOrdinal;
    }

    public String[] getCategories() {
        return categories;
    }

    public String getType() {
        return type;
    }

    public String getConfigureUrlSuffix() {
        return configureUrlSuffix;
    }
}
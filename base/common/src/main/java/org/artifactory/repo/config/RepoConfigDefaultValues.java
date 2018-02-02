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

package org.artifactory.repo.config;

import com.google.common.collect.Lists;
import org.artifactory.descriptor.delegation.ContentSynchronisation;
import org.artifactory.descriptor.repo.*;
import org.artifactory.descriptor.repo.vcs.VcsGitProvider;
import org.artifactory.descriptor.repo.vcs.VcsType;

import java.util.List;

/**
 * @author Dan Feldman
 */
public abstract class RepoConfigDefaultValues {

    //local basic
    public static final String DEFAULT_INCLUDES_PATTERN = "**/*";
    public static final String DEFAULT_REPO_LAYOUT = "simple-default";

    //local advanced
    public static final boolean DEFAULT_BLACKED_OUT = false;
    public static final boolean DEFAULT_ALLOW_CONTENT_BROWSING = false;

    //remote basic
    public static final boolean DEFAULT_OFFLINE = false;
    public static final ContentSynchronisation DEFAULT_DELEGATION_CONTEXT = null;

    //remote advanced
    public static final boolean DEFAULT_HARD_FAIL = false;
    public static final boolean DEFAULT_STORE_ARTIFACTS_LOCALLY = true;
    public static final boolean DEFAULT_SYNC_PROPERTIES = false;
    public static final boolean DEFAULT_SHARE_CONFIG = false;
    public static final boolean DEFAULT_BLOCK_MISMATCHING_MIME_TYPES = true;
    public static final boolean DEFAULT_BYPASS_HEAD_REQUESTS = false;

    public static final boolean DEFAULT_LIST_REMOTE_ITEMS_SUPPORTED_TYPE = true;
    public static final boolean DEFAULT_LIST_REMOTE_ITEMS_UNSUPPORTED_TYPE = false;

    //network
    public static final int DEFAULT_SOCKET_TIMEOUT = 15000;
    public static final boolean DEFAULT_LENIENENT_HOST_AUTH = false;
    public static final boolean DEFAULT_COOKIE_MANAGEMENT = false;

    //cache
    public static final int DEFAULT_KEEP_UNUSED_ARTIFACTS = 0;
    public static final long DEFAULT_RETRIEVAL_CACHE_PERIOD = 600;
    public static final long DEFAULT_ASSUMED_OFFLINE = 300;
    public static final long DEFAULT_MISSED_RETRIEVAL_PERIOD = 1800;

    //replication
    public static final boolean DEFAULT_LOCAL_REPLICATION_ENABLED = true;
    public static final boolean DEFAULT_REMOTE_REPLICATION_ENABLED = false;
    public static final boolean DEFAULT_EVENT_REPLICATION = false;
    public static final boolean DEFAULT_REPLICATION_SYNC_DELETES = false;
    public static final boolean DEFAULT_REPLICATION_SYNC_STATISTICS = false;

    //virtual
    public static final boolean DEFAULT_VIRTUAL_CAN_RETRIEVE_FROM_REMOTE = false;
    public static final long DEFAULT_VIRTUAL_RETRIEVAL_CACHE_PERIOD = 600;

    //distribution
    public static final boolean DEFAULT_GPG_SIGN = false;
    public static final boolean DEFAULT_NEW_BINTRAY_REPO_PRIVATE = true;
    public static final boolean DEFAULT_NEW_BINTRAY_REPO_PREMIUM = true;

    //xray
    public static boolean DEFAULT_XRAY_INDEX = false;
    public static boolean DEFAULT_XRAY_BLOCK_UNSCANNED = false;

    //bower
    public static final String DEFAULT_BOWER_REGISTRY = "https://registry.bower.io";

    //CocoaPods
    public static final String DEFAULT_PODS_SPECS_REPO = "https://github.com/CocoaPods/Specs";

    //Composer
    public static final String DEFAULT_COMPOSER_REGISTRY = "https://packagist.org";

    //debian
    public static final boolean DEFAULT_DEB_TRIVIAL_LAYOUT = false;

    //docker
    public static final DockerApiVersion DEFAULT_DOCKER_API_VER = DockerApiVersion.V2;
    public static final boolean DEFAULT_TOKEN_AUTH = true;
    public static final int DEFAULT_MAX_UNIQUE_TAGS = 0;

    //maven / gradle / ivy / sbt
    public static final int DEFAULT_MAX_UNIQUE_SNAPSHOTS = 0;
    public static final boolean DEFAULT_HANDLE_RELEASES = true;
    public static final boolean DEFAULT_HANDLE_SNAPSHOTS = true;
    public static final boolean DEFAULT_SUPPRESS_POM_CHECKS = true;
    public static final boolean DEFAULT_SUPPRESS_POM_CHECKS_MAVEN = false;
    public static final SnapshotVersionBehavior DEFAULT_SNAPSHOT_BEHAVIOR = SnapshotVersionBehavior.UNIQUE;
    public static final LocalRepoChecksumPolicyType DEFAULT_CHECKSUM_POLICY = LocalRepoChecksumPolicyType.CLIENT;
    public static final boolean DEFAULT_EAGERLY_FETCH_JARS = false;
    public static final boolean DEFAULT_EAGERLY_FETCH_SOURCES = false;
    public static final ChecksumPolicyType DEFAULT_REMOTE_CHECKSUM_POLICY = ChecksumPolicyType.GEN_IF_ABSENT;
    public static final boolean DEFAULT_REJECT_INVALID_JARS = false;
    public static final PomCleanupPolicy DEFAULT_POM_CLEANUP_POLICY = PomCleanupPolicy.discard_active_reference;

    //nuget
    public static final String DEFAULT_NUGET_FEED_PATH = "api/v2";
    public static final String DEFAULT_NUGET_DOWNLOAD_PATH = "api/v2/package";
    public static final boolean DEFAULT_FORCE_NUGET_AUTH = false;

    //vcs
    public static final VcsType DEFAULT_VCS_TYPE = VcsType.GIT;
    public static final VcsGitProvider DEFAULT_GIT_PROVIDER = VcsGitProvider.GITHUB;
    public static final VcsGitConfiguration DEFAULT_VCS_GIT_CONFIG = new VcsGitConfiguration();

    //yum
    public static final int DEFAULT_YUM_METADATA_DEPTH = 0;
    public static final String DEFAULT_YUM_GROUPFILE_NAME = "groups.xml";
    public static final boolean DEFAULT_YUM_AUTO_CALCULATE = true;
    public static final boolean DEFAULT_ENABLE_FILELIST_INDEXING = false;

    // default remote registry urls
    public static final String NUGET_URL = "https://www.nuget.org/";
    public static final String RUBYGEMS_URL = "https://rubygems.org/";
    public static final String MAVEN_GROUP_URL = "https://jcenter.bintray.com";
    public static final String NPM_URL = "https://registry.npmjs.org";
    public static final String PYPI_URL = "https://pypi.python.org";
    public static final String DOCKER_URL = "https://registry-1.docker.io/";
    public static final String CONAN_URL = "https://server.conan.io/";
    public static final String PUPPET_URL = "https://forgeapi.puppetlabs.com/";
    public static final String VCS_URL = "https://github.com/";
    public static final String OPKG_URL = "https://downloads.openwrt.org/chaos_calmer/15.05.1/";
    public static final String RPM_URL = "http://mirror.centos.org/centos/";
    public static final String DEBIAN_URL = "http://archive.ubuntu.com/ubuntu/";
    public static final String CHEF_URL = "https://supermarket.chef.io";
    public static final String HELM_URL = "https://storage.googleapis.com/kubernetes-charts";

    // example repository key
    public static final String EXAMPLE_REPO_KEY = "example-repo-local";

    public static List<String> getDefaultDebianPackagesFileFormats() {
        return Lists.newArrayList("bz2");
    }


}
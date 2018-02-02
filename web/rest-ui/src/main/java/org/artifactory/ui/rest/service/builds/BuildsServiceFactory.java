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

package org.artifactory.ui.rest.service.builds;

import org.artifactory.ui.rest.model.builds.DeleteBuildsModel;
import org.artifactory.ui.rest.service.builds.bintray.*;
import org.artifactory.ui.rest.service.builds.buildsinfo.*;
import org.artifactory.ui.rest.service.builds.buildsinfo.tabs.BuildIssuesService;
import org.artifactory.ui.rest.service.builds.buildsinfo.tabs.BuildReleaseHistoryService;
import org.artifactory.ui.rest.service.builds.buildsinfo.tabs.GetBuildGeneralInfoService;
import org.artifactory.ui.rest.service.builds.buildsinfo.tabs.GetBuildJsonService;
import org.artifactory.ui.rest.service.builds.buildsinfo.tabs.builddiff.*;
import org.artifactory.ui.rest.service.builds.buildsinfo.tabs.env.GetEnvBuildPropsService;
import org.artifactory.ui.rest.service.builds.buildsinfo.tabs.env.GetSystemBuildPropsService;
import org.artifactory.ui.rest.service.builds.buildsinfo.tabs.licenses.BuildLicensesService;
import org.artifactory.ui.rest.service.builds.buildsinfo.tabs.licenses.ChangeBuildLicenseService;
import org.artifactory.ui.rest.service.builds.buildsinfo.tabs.licenses.ExportLicenseToCsvService;
import org.artifactory.ui.rest.service.builds.buildsinfo.tabs.licenses.OverrideSelectedLicensesService;
import org.artifactory.ui.rest.service.builds.buildsinfo.tabs.publishedmodules.GetModuleArtifactsService;
import org.artifactory.ui.rest.service.builds.buildsinfo.tabs.publishedmodules.GetModuleDependencyService;
import org.artifactory.ui.rest.service.builds.buildsinfo.tabs.publishedmodules.GetPublishedModulesService;
import org.springframework.beans.factory.annotation.Lookup;

/**
 * @author Chen Keinan
 */
public abstract class BuildsServiceFactory {

    @Lookup
    public abstract GetAllBuildIDsService getAllBuilds();

    @Lookup
    public abstract GetBuildHistoryService getBuildHistory();

    @Lookup
    public abstract GetBuildGeneralInfoService getBuildGeneralInfo();

    @Lookup
    public abstract GetPublishedModulesService getPublishedModules();

    @Lookup
    public abstract GetModuleArtifactsService getModuleArtifacts();

    @Lookup
    public abstract GetModuleDependencyService getModuleDependency();

    @Lookup
    public abstract DeleteAllBuildsService deleteAllBuilds();

    @Lookup
    public abstract DeleteBuildService<DeleteBuildsModel> deleteBuild();

    @Lookup
    public abstract GetBuildJsonService getBuildJson();

    @Lookup
    public abstract DiffBuildModuleArtifactService diffBuildModuleArtifact();

    @Lookup
    public abstract DiffBuildArtifactService diffBuildArtifact();

    @Lookup
    public abstract DiffBuildDependenciesService diffBuildDependencies();

    @Lookup
    public abstract DiffBuildModuleDependencyService diffBuildModuleDependency();

    @Lookup
    public abstract GetPrevBuildListService getPrevBuildList();

    @Lookup
    public abstract DiffBuildPropsService diffBuildProps();

    @Lookup
    public abstract GetEnvBuildPropsService getEnvBuildProps();

    @Lookup
    public abstract GetSystemBuildPropsService getSystemBuildProps();

    @Lookup
    public abstract BuildIssuesService getBuildIssues();

    @Lookup
    public abstract BuildLicensesService buildLicenses();

    @Lookup
    public abstract BuildReleaseHistoryService buildReleaseHistory();

    @Lookup
    public abstract BuildDiffService buildDiff();

    @Lookup
    public abstract ExportLicenseToCsvService exportLicenseToCsv();

    @Lookup
    public abstract OverrideSelectedLicensesService overrideSelectedLicenses();

    @Lookup
    public abstract ChangeBuildLicenseService changeBuildLicense();

    @Lookup
    public abstract GetBintrayVersionsService getBintrayVersions();

    @Lookup
    public abstract GetBintrayPackagesService getBintrayPackages();

    @Lookup
    public abstract GetBintrayRepositoriesService getBintrayRepositories();

    @Lookup
    public abstract PushBuildToBintrayService pushToBintray();

    @Lookup
    public abstract PushArtifactToBintrayService pushArtifactToBintray();

    @Lookup
    public abstract PushDockerTagToBintrayService pushDockerTagToBintray();

    @Lookup
    public abstract GetBintrayArtifactService getBintrayArtifact();

    @Lookup
    public abstract DistributeBuildService distributeBuild();

}

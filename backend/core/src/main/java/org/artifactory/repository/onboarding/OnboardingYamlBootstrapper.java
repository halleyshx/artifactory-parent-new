package org.artifactory.repository.onboarding;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.ArtifactoryRunningMode;
import org.artifactory.addon.CoreAddons;
import org.artifactory.addon.OssAddonsManager;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.jackson.JacksonReader;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.descriptor.config.MutableCentralConfigDescriptor;
import org.artifactory.descriptor.repo.*;
import org.artifactory.repo.onboarding.DefaultRepoModel;
import org.artifactory.repo.onboarding.RemoteDefaultRepoModel;
import org.artifactory.repo.onboarding.VirtualDefaultRepoModel;
import org.artifactory.repo.onboarding.YamlConfigCreator;
import org.codehaus.jackson.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.artifactory.repo.config.RepoConfigDefaultValues.*;

/**
 * Service to create default repositories from a list in artifactory.config.import.yaml (if exists in etc)
 * YAML file will also include license, proxy and default url setup.
 * service will not run when artifactory is not in "vanilla" state
 *
 * @author nadavy
 */
public class OnboardingYamlBootstrapper {
    private static final Logger log = LoggerFactory.getLogger(OnboardingYamlBootstrapper.class);

    private MutableCentralConfigDescriptor centralConfigDescriptor;
    private Map<String, DefaultRepoModel> defaultRepositoriesByType;
    private OnboardingYamlBootstrapperValidator onboardingYamlBootstrapperValidator;
    private File yamlFile;
    private final String importYamlFilename;
    private final static String DEFAULT_REPO_RESOURCE = "/templates/defaultRepository.json";
    private AddonsManager addonsManager;

    public OnboardingYamlBootstrapper(File yamlImportFile, MutableCentralConfigDescriptor currentConfig) {
        this.yamlFile = yamlImportFile;
        centralConfigDescriptor = currentConfig;
        importYamlFilename = yamlImportFile.getName();
        addonsManager = ContextHelper.get().beanForType(AddonsManager.class);
        onboardingYamlBootstrapperValidator = new OnboardingYamlBootstrapperValidator(centralConfigDescriptor,
                importYamlFilename);
    }

    /**
     * Initialize default repos map
     */
    public MutableCentralConfigDescriptor loadBootstrapSettingsFromYaml() {
        if (addonsManager.addonByType(CoreAddons.class).isAol()) {
            log.error("Can't bootstrap yaml file from Artifactory online instance!");
            return null;
        }
        if (!onboardingYamlBootstrapperValidator.isHasOnlyEmptyDefaultRepo()) {
            log.error("can't import file " + importYamlFilename +
                    " - artifactory repositories have already been created");
            return null;
        }
        // load json to model

        if (!isMasterNode()) {
            log.warn("Bootstrapping from YAML can only start from primary node. canceling...");
            return null;
        }

        try (InputStream json = getClass().getResourceAsStream(DEFAULT_REPO_RESOURCE)) {
            defaultRepositoriesByType = JacksonReader
                    .streamAsValueTypeReference(json, new TypeReference<List<DefaultRepoModel>>() {
                    })
                    .stream()
                    .collect(Collectors.toMap(DefaultRepoModel::getRepoType, defaultModel -> defaultModel));
            log.debug("Default repositories configuration has been successfully loaded from " + DEFAULT_REPO_RESOURCE);
            return importBootstrapYaml(yamlFile);
        } catch (Exception e) {
            log.error("Unable to parse YAML file {} - Artifactory can not initialize default bootstrap settings",
                    importYamlFilename);
            log.debug(e.getMessage());
            return null;
        }
    }

    private boolean isMasterNode() {
        if (!addonsManager.getArtifactoryRunningMode().isHa()) {
            return true;
        }
        ArtifactoryHome artifactoryHome = ArtifactoryHome.get();
        return artifactoryHome.getHaNodeProperties().isPrimary();
    }

    /**
     * Import yaml bootstrap file as default artifactory settings.
     */
    private MutableCentralConfigDescriptor importBootstrapYaml(File yamlFile) {
        Yaml yaml = new Yaml();
        try (InputStream inputStream = new FileInputStream(yamlFile)) {
            ConfigurationYamlModel configurationYamlModel = yaml.loadAs(inputStream, ConfigurationYamlModel.class);
            if (onboardingYamlBootstrapperValidator.areConfigurationActionsValid(configurationYamlModel)) {
                log.info("Bootstrapping artifactory from " + importYamlFilename);
                setupLicense(configurationYamlModel);
                log.debug("License(s) installed from "+ importYamlFilename);
                setupDefaultRepos(configurationYamlModel.OnboardingConfiguration.repoTypes);
                log.debug("Repositories created from "+ importYamlFilename);
                if (configurationYamlModel.GeneralConfiguration != null) {
                    setupBaseUrl(configurationYamlModel.GeneralConfiguration.baseUrl);
                    log.debug("Base url has been setup from " + importYamlFilename);
                    setupProxy(configurationYamlModel.GeneralConfiguration.proxies);
                    log.debug("Proxy(ies) has been setup from " + importYamlFilename);
                }
                exportAndDeleteImportYaml(yamlFile, configurationYamlModel);
                log.info("Configuration reloaded from " + importYamlFilename);
                return centralConfigDescriptor;
            }
            return null;
        } catch (IOException e) {
            log.error("Artifactory can't import bootstrap settings from " + importYamlFilename);
            log.debug(e.getMessage());
            return null;
        }
    }

    private void exportAndDeleteImportYaml(File yamlFile, ConfigurationYamlModel configurationYamlModel) {
        if (configurationYamlModel.OnboardingConfiguration != null &&
                configurationYamlModel.OnboardingConfiguration.repoTypes != null &&
                this.centralConfigDescriptor.removeRepository(EXAMPLE_REPO_KEY) != null) {
            exportYamlConfigurationToFile(configurationYamlModel.OnboardingConfiguration.repoTypes);
        }
        try {
            if (!yamlFile.delete()) {
                log.error("Artifactory can't delete file " + importYamlFilename);
            }
        } catch (Exception e) {
            log.error("Artifactory can't delete file " + importYamlFilename);
        }
    }


    /**
     * Create default repositories for the given repo types list
     *
     * @param repoTypes list of repo types
     */
    private void setupDefaultRepos(List<String> repoTypes) {
        if (repoTypes == null) {
            return;
        }
        AddonsManager addonsManager = ContextHelper.get().beanForType(AddonsManager.class);
        if (addonsManager instanceof OssAddonsManager) {
            repoTypes = filterNotOssRepos(repoTypes);
        }
        repoTypes
                .stream()
                .map(defaultRepositoriesByType::get)
                .filter(Objects::nonNull)
                .forEach(repoDefaultModel -> createRepositories(repoDefaultModel,
                        RepoType.fromType(repoDefaultModel.getRepoType())));
    }

    /**
     * Filter repositories not available in Artifactory OSS
     */
    private List<String> filterNotOssRepos(List<String> repoTypes) {
        List<String> ossRepos = Lists
                .newArrayList(RepoType.Maven.getType(), RepoType.Gradle.getType(), RepoType.Ivy.getType(),
                        RepoType.SBT.getType());
        return repoTypes.stream()
                .filter(ossRepos::contains)
                .collect(Collectors.toList());
    }

    /**
     * Create local, remote and virtual (whatever applies) of a given repo type, as stated in the json model
     */
    private void createRepositories(DefaultRepoModel defaultModel, RepoType repoType) {
        log.info("Creating default repositories for " + repoType.getType());
        RepoLayout repoLayout = centralConfigDescriptor.getRepoLayout(defaultModel.getLayout());
        Map<String, LocalRepoDescriptor> localRepoDescriptorMap = Maps.newHashMap();
        Map<String, RemoteRepoDescriptor> remoteRepoDescriptorMap = Maps.newHashMap();
        // create local
        if (defaultModel.getLocalRepoKeys() != null) {
            defaultModel.getLocalRepoKeys()
                    .forEach(localRepo -> addLocalRepoDescriptor(repoType, repoLayout, localRepoDescriptorMap,
                            localRepo));
        }
        // create remote
        if (defaultModel.getRemoteRepoKeys() != null) {
            defaultModel.getRemoteRepoKeys()
                    .forEach(remoteRepo -> addRemoteRepoDescriptor(repoType, repoLayout, remoteRepoDescriptorMap,
                            remoteRepo));
        }
        // create virtual
        if (defaultModel.getVirtualRepoKeys() != null) {
            defaultModel.getVirtualRepoKeys()
                    .forEach(virtualRepo -> addVirtualRepoDescriptor(repoType, repoLayout, localRepoDescriptorMap,
                            remoteRepoDescriptorMap, virtualRepo));
        }
    }

    /**
     * Add a single local repo to the config descriptor
     */
    private void addLocalRepoDescriptor(RepoType repoType, RepoLayout layout,
            Map<String, LocalRepoDescriptor> localRepoDescriptorMap, String localRepo) {
        LocalRepoDescriptor localRepoDescriptor = new LocalRepoDescriptor();
        localRepoDescriptor.setType(repoType);
        if (repoType == RepoType.Maven) {
            handleLocalMavenRepo(localRepo, localRepoDescriptor);
        }
        if (repoType == RepoType.Debian) {
            localRepoDescriptor.setOptionalIndexCompressionFormats(getDefaultDebianPackagesFileFormats());
        }
        localRepoDescriptor.setKey(localRepo);
        localRepoDescriptor.setRepoLayout(layout);
        localRepoDescriptorMap.put(localRepo, localRepoDescriptor);
        centralConfigDescriptor.addLocalRepository(localRepoDescriptor);
    }

    /**
     * set snapshot and release repositories in Maven local repos
     */
    private void handleLocalMavenRepo(String localRepo, LocalRepoDescriptor localRepoDescriptor) {
        if (localRepo.contains("snapshot")) {
            localRepoDescriptor.setHandleSnapshots(true);
            localRepoDescriptor.setHandleSnapshots(false);
        } else {
            localRepoDescriptor.setHandleSnapshots(false);
            localRepoDescriptor.setHandleSnapshots(true);
        }
    }

    /**
     * Add a single remote repo to the config descriptor
     */
    private void addRemoteRepoDescriptor(RepoType repoType, RepoLayout layout,
            Map<String, RemoteRepoDescriptor> remoteRepoDescriptorMap, RemoteDefaultRepoModel remoteRepo) {
        RemoteRepoDescriptor remoteRepoDescriptor = new HttpRepoDescriptor();
        if (RepoType.Gradle == repoType || RepoType.Ivy == repoType || RepoType.SBT == repoType) {
            remoteRepoDescriptor.setType(RepoType.Maven);
            remoteRepoDescriptor.setRepoLayout(centralConfigDescriptor.getRepoLayout("maven-2-default"));
        } else {
            remoteRepoDescriptor.setType(repoType);
            remoteRepoDescriptor.setRepoLayout(layout);
        }
        remoteRepoDescriptor.setKey(remoteRepo.getRepoKey());
        if (Strings.isNullOrEmpty(remoteRepo.getUrl())) {
            remoteRepoDescriptor.setUrl(remoteUrlByType(repoType));
        } else {
            remoteRepoDescriptor.setUrl(remoteRepo.getUrl());
        }
        remoteRepoDescriptorMap.put(remoteRepo.getRepoKey(), remoteRepoDescriptor);
        centralConfigDescriptor.addRemoteRepository(remoteRepoDescriptor);
    }

    /**
     * Create a single virtual repo to the config descriptor or update existing one
     */
    private void addVirtualRepoDescriptor(RepoType repoType, RepoLayout layout,
            Map<String, LocalRepoDescriptor> localRepoDescriptorMap,
            Map<String, RemoteRepoDescriptor> remoteRepoDescriptorMap, VirtualDefaultRepoModel virtualRepo) {
        // create new virtual repo
        VirtualRepoDescriptor virtualRepoDescriptor = new VirtualRepoDescriptor();
        virtualRepoDescriptor.setType(repoType);
        virtualRepoDescriptor.setKey(virtualRepo.getRepoKey());
        virtualRepoDescriptor.setRepoLayout(layout);
        virtualRepoDescriptor.setDefaultDeploymentRepo(
                localRepoDescriptorMap.get(virtualRepo.getDefaultDeployment()));
        virtualRepoDescriptor.setRepositories(
                getSelectedVirtualRepos(virtualRepo.getIncludedLocalRepos(), virtualRepo.getIncludedRemoteRepos(),
                        localRepoDescriptorMap, remoteRepoDescriptorMap));
        centralConfigDescriptor.addVirtualRepository(virtualRepoDescriptor);
    }

    /**
     * Return the included repositories of a virtual repository, as stated in the defaultRepository.json model
     */
    private List<RepoDescriptor> getSelectedVirtualRepos(List<String> includedLocalRepos,
            List<String> includedRemoteRepos, Map<String, LocalRepoDescriptor> localRepoDescriptorMap,
            Map<String, RemoteRepoDescriptor> remoteRepoDescriptorMap) {
        List<RepoDescriptor> repoDescriptorList = Lists.newArrayList();
        if (includedLocalRepos != null) {
            includedLocalRepos.forEach(includedRepo -> {
                if (localRepoDescriptorMap.get(includedRepo) != null) {
                    repoDescriptorList.add(localRepoDescriptorMap.get(includedRepo));
                }
            });
        }
        if (includedRemoteRepos != null) {
            includedRemoteRepos.forEach(includedRepo -> {
                if (remoteRepoDescriptorMap.get(includedRepo) != null) {
                    repoDescriptorList.add(remoteRepoDescriptorMap.get(includedRepo));
                }
            });
        }
        return repoDescriptorList;
    }

    /**
     * Remote url for remote repositories
     */
    private String remoteUrlByType(RepoType repoType) {
        switch (repoType) {
            case NuGet:
                return NUGET_URL;
            case Gems:
                return RUBYGEMS_URL;
            case Maven:
            case Ivy:
            case Gradle:
            case SBT:
                return MAVEN_GROUP_URL;
            case Npm:
                return NPM_URL;
            case Pypi:
                return PYPI_URL;
            case Docker:
                return DOCKER_URL;
            case Opkg:
                return OPKG_URL;
            case Puppet:
                return PUPPET_URL;
            case Chef:
                return CHEF_URL;
            case VCS:
            case Composer:
            case Bower:
            case CocoaPods:
                return VCS_URL;
            case Debian:
                return DEBIAN_URL;
            case YUM:
                return RPM_URL;
        }
        return " ";
    }

    private void setupLicense(ConfigurationYamlModel configurationYamlModel) {
        ArtifactoryRunningMode artifactoryRunningMode = addonsManager.getArtifactoryRunningMode();
        GeneralConfigurationYamlModel generalConfiguration = configurationYamlModel.GeneralConfiguration;
        if (generalConfiguration == null) {
            return;
        }
        if (artifactoryRunningMode.isHa()) {
            setupHaLicenses(generalConfiguration.licenseKeys, addonsManager);
        } else if (!artifactoryRunningMode.isOss()){
            setupNoHaLicense(generalConfiguration.licenseKey, addonsManager);
        }
    }

    private void setupHaLicenses(Set<String> licenseKeys, AddonsManager addonsManager) {
        if (licenseKeys != null && !licenseKeys.isEmpty()) {
            addonsManager.addAndActivateLicenses(licenseKeys, false, true);
        }
    }

    private void setupNoHaLicense(String licenseKey, AddonsManager addonsManager) {
        if (licenseKey != null && !addonsManager.isLicenseInstalled()) {
            addonsManager.addAndActivateLicense(licenseKey, false, true);
        }
    }

    private void setupBaseUrl(String baseUrl) {
        if (baseUrl == null) {
            return;
        }
        log.info("Setting up base url from " + importYamlFilename);
        centralConfigDescriptor.setUrlBase(baseUrl);
    }

    private void setupProxy(List<ProxyConfigurationYamlModel> proxies) {
        if (proxies == null) {
            return;
        }
        log.info("Setting up proxies from " + importYamlFilename);
        proxies.forEach(proxy -> {
            ProxyDescriptor proxyDescriptor = new ProxyDescriptor();
            proxyDescriptor.setKey(proxy.key);
            proxyDescriptor.setHost(proxy.host);
            proxyDescriptor.setPort(proxy.port);
            proxyDescriptor.setUsername(proxy.userName);
            proxyDescriptor.setPassword(proxy.password);
            proxyDescriptor.setDefaultProxy(proxy.defaultProxy);
            centralConfigDescriptor.addProxy(proxyDescriptor, proxy.defaultProxy);
        });
    }

    private void exportYamlConfigurationToFile(List<String> repoTypeList) {
        DateFormat formatter = new SimpleDateFormat("yyyyMMdd.HHmmss");
        String timestamp = formatter.format(System.currentTimeMillis());
        File yamlOutput = new File(ContextHelper.get().getArtifactoryHome().getEtcDir()
                + "/artifactory.config." + timestamp + ".yml");
        try {
            YamlConfigCreator yamlConfigCreator = new YamlConfigCreator();
            yamlConfigCreator.saveBootstrapYaml(repoTypeList, centralConfigDescriptor, yamlOutput);
        } catch (IOException e) {
            log.error("artifactory can't export settings to " + yamlOutput.getName());
        }
    }

}

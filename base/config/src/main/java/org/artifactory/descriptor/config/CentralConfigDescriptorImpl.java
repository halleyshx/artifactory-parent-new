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

package org.artifactory.descriptor.config;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.artifactory.descriptor.Descriptor;
import org.artifactory.descriptor.addon.AddonSettings;
import org.artifactory.descriptor.backup.BackupDescriptor;
import org.artifactory.descriptor.bintray.BintrayConfigDescriptor;
import org.artifactory.descriptor.cleanup.CleanupConfigDescriptor;
import org.artifactory.descriptor.download.FolderDownloadConfigDescriptor;
import org.artifactory.descriptor.gc.GcConfigDescriptor;
import org.artifactory.descriptor.index.IndexerDescriptor;
import org.artifactory.descriptor.mail.MailServerDescriptor;
import org.artifactory.descriptor.message.SystemMessageDescriptor;
import org.artifactory.descriptor.property.PropertySet;
import org.artifactory.descriptor.quota.QuotaConfigDescriptor;
import org.artifactory.descriptor.replication.GlobalReplicationsConfigDescriptor;
import org.artifactory.descriptor.replication.LocalReplicationDescriptor;
import org.artifactory.descriptor.replication.RemoteReplicationDescriptor;
import org.artifactory.descriptor.replication.ReplicationBaseDescriptor;
import org.artifactory.descriptor.repo.*;
import org.artifactory.descriptor.repo.distribution.DistributionRepoDescriptor;
import org.artifactory.descriptor.repo.jaxb.*;
import org.artifactory.descriptor.security.SecurityDescriptor;
import org.artifactory.descriptor.sumologic.SumoLogicConfigDescriptor;
import org.artifactory.descriptor.trashcan.TrashcanConfigDescriptor;
import org.artifactory.util.AlreadyExistsException;
import org.artifactory.util.DoesNotExistException;
import org.artifactory.util.IdUtils;
import org.artifactory.util.RepoLayoutUtils;
import org.artifactory.util.stream.BiOptional;
import org.jfrog.client.util.PathUtils;
import org.jfrog.common.DiffElement;
import org.jfrog.common.DiffIgnore;
import org.jfrog.common.DiffReferenceable;
import org.jfrog.common.GenerateDiffFunction;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.*;

@XmlRootElement(name = "config")
@XmlType(name = "CentralConfigType",
        propOrder = {"serverName", "offlineMode", "helpLinksEnabled", "fileUploadMaxSizeMb", "revision", "dateFormat", "addons",
                "mailServer", "xrayConfig", "bintrayConfig", "security", "backups", "indexer", "localRepositoriesMap",
                "remoteRepositoriesMap", "virtualRepositoriesMap", "distributionRepositoriesMap", "proxies",
                "reverseProxies", "propertySets", "urlBase", "logo", "footer", "repoLayouts", "remoteReplications",
                "localReplications", "gcConfig", "cleanupConfig", "virtualCacheCleanupConfig", "quotaConfig",
                "systemMessageConfig", "folderDownloadConfig", "trashcanConfig",
                "replicationsConfig", "bintrayApplications", "sumoLogicConfig" },
        namespace = Descriptor.NS)
@XmlAccessorType(XmlAccessType.FIELD)
@GenerateDiffFunction
public class CentralConfigDescriptorImpl implements MutableCentralConfigDescriptor {

    public static final String DEFAULT_DATE_FORMAT = "dd-MM-yy HH:mm:ss z";

    @XmlElement(name = "localRepositories", required = true)
    @XmlJavaTypeAdapter(LocalRepositoriesMapAdapter.class)
    private Map<String, LocalRepoDescriptor> localRepositoriesMap = Maps.newLinkedHashMap();

    @XmlElement(name = "remoteRepositories", required = false)
    @XmlJavaTypeAdapter(RemoteRepositoriesMapAdapter.class)
    private Map<String, RemoteRepoDescriptor> remoteRepositoriesMap = Maps.newLinkedHashMap();

    @XmlElement(name = "virtualRepositories", required = false)
    @XmlJavaTypeAdapter(VirtualRepositoriesMapAdapter.class)
    private Map<String, VirtualRepoDescriptor> virtualRepositoriesMap = Maps.newLinkedHashMap();

    @XmlElement(name = "distributionRepositories", required = false)
    @XmlJavaTypeAdapter(DistributionRepositoriesMapAdapter.class)
    private Map<String, DistributionRepoDescriptor> distributionRepositoriesMap = Maps.newLinkedHashMap();

    @XmlElementWrapper(name = "proxies")
    @XmlElement(name = "proxy", required = false)
    private List<ProxyDescriptor> proxies = new ArrayList<>();

    @XmlElementWrapper(name = "reverseProxies")
    @XmlElement(name = "reverseProxy", required = false)
    private List<ReverseProxyDescriptor> reverseProxies = new ArrayList<>();

    @XmlElement(defaultValue = DEFAULT_DATE_FORMAT)
    private String dateFormat = DEFAULT_DATE_FORMAT;

    @XmlElement(defaultValue = "100", required = false)
    private int fileUploadMaxSizeMb = 100;

    @XmlElementWrapper(name = "backups")
    @XmlElement(name = "backup", required = false)
    private List<BackupDescriptor> backups = new ArrayList<>();

    private IndexerDescriptor indexer;

    @XmlElement
    private GlobalReplicationsConfigDescriptor replicationsConfig = new GlobalReplicationsConfigDescriptor();

    /**
     * A name uniquely identifying this artifactory server instance
     */
    @XmlElement
    private String serverName;

    /**
     * if this flag is set all the remote repos will work in offline mode
     */
    @XmlElement(defaultValue = "false", required = false)
    private boolean offlineMode;

    @XmlElement
    private boolean helpLinksEnabled = true;

    private AddonSettings addons = new AddonSettings();

    @XmlElement
    private MailServerDescriptor mailServer;

    @XmlElement
    private XrayDescriptor xrayConfig;

    /**
     * security might not be present in the xml but we always want to create it
     */
    @XmlElement
    private SecurityDescriptor security = new SecurityDescriptor();

    @XmlElementWrapper(name = "propertySets")
    @XmlElement(name = "propertySet", required = false)
    private List<PropertySet> propertySets = new ArrayList<>();

    @XmlElement
    private String urlBase;

    @XmlElement
    private String logo;

    @XmlElement
    private SystemMessageDescriptor systemMessageConfig;

    @XmlElement
    private FolderDownloadConfigDescriptor folderDownloadConfig;

    @XmlElement
    private String footer;

    @XmlElementWrapper(name = "repoLayouts")
    @XmlElement(name = "repoLayout", required = false)
    private List<RepoLayout> repoLayouts = Lists.newArrayList();

    @XmlElementWrapper(name = "remoteReplications")
    @XmlElement(name = "remoteReplication", required = false)
    private List<RemoteReplicationDescriptor> remoteReplications = Lists.newArrayList();

    @XmlElementWrapper(name = "localReplications")
    @XmlElement(name = "localReplication", required = false)
    private List<LocalReplicationDescriptor> localReplications = Lists.newArrayList();

    @XmlElement
    private GcConfigDescriptor gcConfig;

    @XmlElement
    private CleanupConfigDescriptor cleanupConfig;

    @XmlElement
    private CleanupConfigDescriptor virtualCacheCleanupConfig;

    @XmlElement
    private BintrayConfigDescriptor bintrayConfig;

    private QuotaConfigDescriptor quotaConfig;

    @XmlElement
    private TrashcanConfigDescriptor trashcanConfig = new TrashcanConfigDescriptor();

    @XmlJavaTypeAdapter(BintrayApplicationConfigsMapAdapter.class)
    @XmlElement(name = "bintrayApplications", required = false)
    private Map<String, BintrayApplicationConfig> bintrayApplications = new HashMap<>();

    private SumoLogicConfigDescriptor sumoLogicConfig = new SumoLogicConfigDescriptor();

    @XmlElement
    private long revision;

    @Override
    @DiffElement(name="localRepositories")
    @DiffReferenceable
    public Map<String, LocalRepoDescriptor> getLocalRepositoriesMap() {
        return localRepositoriesMap;
    }

    @Override
    public void setLocalRepositoriesMap(Map<String, LocalRepoDescriptor> localRepositoriesMap) {
        this.localRepositoriesMap = Optional.ofNullable(localRepositoriesMap).orElseGet(Maps::newLinkedHashMap);
    }

    @Override
    @DiffElement(name="remoteRepositories")
    @DiffReferenceable
    public Map<String, RemoteRepoDescriptor> getRemoteRepositoriesMap() {
        return remoteRepositoriesMap;
    }

    @Override
    public void setRemoteRepositoriesMap(Map<String, RemoteRepoDescriptor> remoteRepositoriesMap) {
        this.remoteRepositoriesMap = Optional.ofNullable(remoteRepositoriesMap).orElseGet(Maps::newLinkedHashMap);
    }

    @Override
    @DiffElement(name="virtualRepositories")
    @DiffReferenceable
    public Map<String, VirtualRepoDescriptor> getVirtualRepositoriesMap() {
        return virtualRepositoriesMap;
    }

    @Override
    public void setVirtualRepositoriesMap(Map<String, VirtualRepoDescriptor> virtualRepositoriesMap) {
        this.virtualRepositoriesMap = Optional.ofNullable(virtualRepositoriesMap).orElseGet(Maps::newLinkedHashMap);
    }

    @Override
    @DiffElement(name="distributionRepositories")
    @DiffReferenceable
    public Map<String, DistributionRepoDescriptor> getDistributionRepositoriesMap() {
        return distributionRepositoriesMap;
    }

    @Override
    public void setDistributionRepositoriesMap(Map<String, DistributionRepoDescriptor> distributionRepositoriesMap) {
        this.distributionRepositoriesMap = Optional.ofNullable(distributionRepositoriesMap).orElseGet(
                Maps::newLinkedHashMap);
    }

    @Override
    @DiffReferenceable
    public List<ProxyDescriptor> getProxies() {
        return proxies;
    }

    @Override
    public void setProxies(List<ProxyDescriptor> proxies) {
        this.proxies = Optional.ofNullable(proxies).orElseGet(ArrayList::new);
    }

    @Override
    public List<ReverseProxyDescriptor> getReverseProxies() {
        return reverseProxies;
    }

    @Override
    public void setReverseProxies(List<ReverseProxyDescriptor> reverseProxies) {
        this.reverseProxies = Optional.ofNullable(reverseProxies).orElseGet(ArrayList::new);
    }

    @DiffIgnore
    @Override
    public ProxyDescriptor getDefaultProxy() {
        for (ProxyDescriptor proxy : proxies) {
            if (proxy.isDefaultProxy()) {
                return proxy;
            }
        }
        return null;
    }

    @Override
    public String getDateFormat() {
        return dateFormat;
    }

    @Override
    public void setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
    }

    @Override
    public int getFileUploadMaxSizeMb() {
        return fileUploadMaxSizeMb;
    }

    @Override
    public void setFileUploadMaxSizeMb(int fileUploadMaxSizeMb) {
        this.fileUploadMaxSizeMb = fileUploadMaxSizeMb;
    }

    @Override
    public long getRevision() {
        return revision;
    }

    @Override
    public void setRevision(long revision) {
        this.revision = revision;
    }

    @Override
    public List<BackupDescriptor> getBackups() {
        return backups;
    }

    @Override
    public void setBackups(List<BackupDescriptor> backups) {
        this.backups = Optional.ofNullable(backups).orElseGet(ArrayList::new);
    }

    @Override
    public IndexerDescriptor getIndexer() {
        return indexer;
    }

    @Override
    public void setIndexer(IndexerDescriptor mavenIndexer) {
        this.indexer = mavenIndexer;
    }

    @Override
    public String getServerName() {
        return serverName;
    }

    @Override
    public void setServerName(String serverName) {
        this.serverName = StringUtils.stripToNull(serverName);
    }

    @Override
    public SecurityDescriptor getSecurity() {
        return security;
    }

    @Override
    public void setSecurity(SecurityDescriptor security) {
        if (security == null) {
            security = new SecurityDescriptor();
        }
        this.security = security;
    }

    @Override
    public AddonSettings getAddons() {
        return addons;
    }

    @Override
    public void setAddons(AddonSettings addons) {
        this.addons = addons;
    }

    @Override
    public MailServerDescriptor getMailServer() {
        return mailServer;
    }

    @Override
    public void setMailServer(MailServerDescriptor mailServer) {
        this.mailServer = mailServer;
    }

    @Override
    public XrayDescriptor getXrayConfig() {
        return xrayConfig;
    }

    @Override
    public void setXrayConfig(XrayDescriptor xrayConfig) {
        this.xrayConfig = xrayConfig;
    }

    @Override
    @DiffReferenceable
    public List<PropertySet> getPropertySets() {
        return propertySets;
    }

    @Override
    public void setPropertySets(List<PropertySet> propertySets) {
        this.propertySets = Optional.ofNullable(propertySets).orElseGet(ArrayList::new);
    }

    @Override
    public String getUrlBase() {
        return urlBase;
    }

    @Override
    public void setUrlBase(String urlBase) {
        this.urlBase = PathUtils.trimTrailingSlashes(urlBase);
    }

    @Override
    public RepoDescriptor removeRepository(String repoKey) {
        // first remove the repository itself
        RepoDescriptor removedRepo = removeFromMaps(repoKey);
        if (removedRepo == null) {
            return null;
        }

        removeReferences(repoKey, removedRepo);

        return removedRepo;
    }

    private void removeReferences(String repoKey, RepoDescriptor removedRepo) {
        // remove from any virtual repository
        for (VirtualRepoDescriptor virtualRepoDescriptor : virtualRepositoriesMap.values()) {
            removeVirtualRepoReferences(removedRepo, virtualRepoDescriptor);
        }

        if (removedRepo instanceof RealRepoDescriptor) {
            // remove the repository from any backup exclude list
            for (BackupDescriptor backup : getBackups()) {
                backup.removeExcludedRepository((RealRepoDescriptor) removedRepo);
            }
        }

        if (removedRepo instanceof RepoBaseDescriptor) {
            // remove from the indexer include list
            IndexerDescriptor indexer = getIndexer();
            if (indexer != null) {
                indexer.removeIncludedRepository((RepoBaseDescriptor) removedRepo);
            }
        }

        if (removedRepo instanceof HttpRepoDescriptor) {
            RemoteReplicationDescriptor existingReplication = getRemoteReplication(removedRepo.getKey());
            if (existingReplication != null) {
                removeRemoteReplication(existingReplication);
            }
        }

        if (removedRepo instanceof LocalRepoDescriptor) {
            List<LocalReplicationDescriptor> existingReplications = getMultiLocalReplications(removedRepo.getKey());
            if (existingReplications != null && !existingReplications.isEmpty()) {
                existingReplications.forEach(this::removeLocalReplication);
            }
        }

        //Remove this repo's Bintray OAuth App config from descriptor if it's not used by any other repo.
        if (removedRepo instanceof DistributionRepoDescriptor) {
            removeDistributionRepoReferences((DistributionRepoDescriptor) removedRepo);
        }

        //Remove repo's reverse proxy config
        getReverseProxies()
                .forEach(reverseProxyDescriptor -> reverseProxyDescriptor.deleteReverseProxyConfig(repoKey));
    }

    private RepoDescriptor removeFromMaps(String repoKey) {
        RepoDescriptor removedRepo = localRepositoriesMap.remove(repoKey);
        if (removedRepo == null) {
            removedRepo = remoteRepositoriesMap.remove(repoKey);
        }
        if (removedRepo == null) {
            removedRepo = virtualRepositoriesMap.remove(repoKey);
        }
        if (removedRepo == null) {
            removedRepo = distributionRepositoriesMap.remove(repoKey);
        }
        if (removedRepo == null) {
            // not found - finish
            return null;
        }
        return removedRepo;
    }

    private void removeDistributionRepoReferences(DistributionRepoDescriptor removedRepo) {
        BintrayApplicationConfig btAppConfig = removedRepo.getBintrayApplication();
        if (btAppConfig != null) {
            String btConfigKey = btAppConfig.getKey();
            BiOptional.of(distributionRepositoriesMap.values().stream()
                    .map(DistributionRepoDescriptor::getBintrayApplication)
                    .filter(Objects::nonNull)
                    .filter(btApp -> btConfigKey.equals(btApp.getKey()))
                    .findAny())
                    .ifNotPresent(() -> bintrayApplications.remove(btConfigKey));
        }
    }

    private void removeVirtualRepoReferences(RepoDescriptor removedRepo, VirtualRepoDescriptor virtualRepoDescriptor) {
        virtualRepoDescriptor.removeRepository(removedRepo);

        if (removedRepo instanceof LocalRepoDescriptor &&
                virtualRepoDescriptor.getDefaultDeploymentRepo() != null &&
                virtualRepoDescriptor.getDefaultDeploymentRepo().getKey().equals(removedRepo.getKey())) {
            virtualRepoDescriptor.setDefaultDeploymentRepo(null);
        }

        if (removedRepo instanceof RemoteRepoDescriptor &&
                virtualRepoDescriptor.getExternalDependencies() != null &&
                virtualRepoDescriptor.getExternalDependencies().getRemoteRepo() != null &&
                virtualRepoDescriptor.getExternalDependencies().getRemoteRepo().getKey().equals(removedRepo.getKey())) {
            virtualRepoDescriptor.setExternalDependencies(null);
        }
    }


    @Override
    public String getFooter() {
        return footer;
    }

    public void setFooter(String footer) {
        this.footer = footer;
    }

    @Override
    public boolean isKeyAvailable(String key) {
        return !(isRepositoryExists(key) ||
                isProxyExists(key) ||
                isBackupExists(key) ||
                isLdapExists(key) ||
                isPropertySetExists(key) ||
                isRepoLayoutExists(key));
    }

    @Override
    public boolean isRepositoryExists(String repoKey) {
        return containsKeyIgnoreCase(localRepositoriesMap, repoKey)
                || containsKeyIgnoreCase(remoteRepositoriesMap, repoKey)
                || containsKeyIgnoreCase(virtualRepositoriesMap, repoKey)
                || containsKeyIgnoreCase(distributionRepositoriesMap, repoKey);
    }

    private boolean containsKeyIgnoreCase(Map map, String repoKey) {

        Set<String> keys = map.keySet();
        for (String key : keys) {
            if (key.compareToIgnoreCase(repoKey) == 0) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void addLocalRepository(LocalRepoDescriptor localRepoDescriptor)
            throws AlreadyExistsException {
        String repoKey = localRepoDescriptor.getKey();
        localRepositoriesMap.put(repoKey, localRepoDescriptor);
    }

    @Override
    public void addRemoteRepository(RemoteRepoDescriptor remoteRepoDescriptor) {
        String repoKey = remoteRepoDescriptor.getKey();
        remoteRepositoriesMap.put(repoKey, remoteRepoDescriptor);
        conditionallyAddToBackups(remoteRepoDescriptor);
    }

    @Override
    public void addDistributionRepository(DistributionRepoDescriptor distributionRepoDescriptor)
            throws AlreadyExistsException {
        String repoKey = distributionRepoDescriptor.getKey();
        repoKeyExists(repoKey, false);
        distributionRepositoriesMap.put(repoKey, distributionRepoDescriptor);
    }

    @Override
    public void conditionallyAddToBackups(RealRepoDescriptor remoteRepoDescriptor) {
        // Conditionally add the repository to any backup exclude list
        for (BackupDescriptor backup : getBackups()) {
            backup.addExcludedRepository(remoteRepoDescriptor);
        }
    }

    @Override
    public void addVirtualRepository(VirtualRepoDescriptor virtualRepoDescriptor) {
        String repoKey = virtualRepoDescriptor.getKey();
        virtualRepositoriesMap.put(repoKey, virtualRepoDescriptor);
    }

    @Override
    public boolean isProxyExists(String proxyKey) {
        return getProxy(proxyKey) != null;
    }

    @Override
    public void addProxy(ProxyDescriptor proxyDescriptor, boolean defaultForAllRemoteRepo) {
        String proxyKey = proxyDescriptor.getKey();
        if (isProxyExists(proxyKey)) {
            throw new AlreadyExistsException("Proxy " + proxyKey + " already exists");
        }
        if (proxyDescriptor.isDefaultProxy()) {
            proxyChanged(proxyDescriptor, defaultForAllRemoteRepo);
            // remove default flag from other existing proxy if exist
            for (ProxyDescriptor proxy : proxies) {
                proxy.setDefaultProxy(false);
            }
        }
        proxies.add(proxyDescriptor);
    }

    @Override
    public ProxyDescriptor removeProxy(String proxyKey) {
        ProxyDescriptor proxyDescriptor = getProxy(proxyKey);
        if (proxyDescriptor == null) {
            return null;
        }

        // remove the proxy from the proxies list
        proxies.remove(proxyDescriptor);

        // remove references from all remote repositories
        for (RemoteRepoDescriptor remoteRepo : remoteRepositoriesMap.values()) {
            if (remoteRepo instanceof HttpRepoDescriptor) {
                HttpRepoDescriptor httpRemoteRepo = (HttpRepoDescriptor) remoteRepo;
                if (httpRemoteRepo.getProxy() != null && proxyKey.equals(httpRemoteRepo.getProxy().getKey())) {
                    httpRemoteRepo.setProxy(null);
                }
            }
        }

        // remove references from all distribution repositories
        for (DistributionRepoDescriptor distRepo : distributionRepositoriesMap.values()) {
            if (distRepo.getProxy() != null && proxyKey.equals(distRepo.getProxy().getKey())) {
                distRepo.setProxy(null);
            }
        }

        for (LocalReplicationDescriptor localReplication : localReplications) {
            localReplication.setProxy(null);
        }

        if (sumoLogicConfig.getProxy() != null && proxyKey.equals(sumoLogicConfig.getProxy().getKey())) {
            sumoLogicConfig.setProxy(null);
        }

        return proxyDescriptor;
    }

    @Override
    public boolean isReverseProxyExists(String key) {
        return getReverseProxy(key) != null;
    }

    @Override
    public void addReverseProxy(ReverseProxyDescriptor descriptor) {
        String proxyKey = descriptor.getKey();
        if (isReverseProxyExists(proxyKey)) {
            throw new AlreadyExistsException("Reverse Proxy " + proxyKey + " already exists");
        }
        reverseProxies.add(descriptor);
    }

    @Override
    public void updateReverseProxy(ReverseProxyDescriptor descriptor) {
        if (descriptor != null) {
            removeCurrentReverseProxy();
            addReverseProxy(descriptor);
        }
    }

    @Override
    public ReverseProxyDescriptor removeReverseProxy(String key) {
        ReverseProxyDescriptor reverseProxy = getReverseProxy(key);
        if (reverseProxy == null) {
            return null;
        }

        reverseProxies.remove(reverseProxy);
        return reverseProxy;
    }

    @Override
    public void removeCurrentReverseProxy() {
        if (!reverseProxies.isEmpty()) {
            reverseProxies.clear();
        }
    }

    @Override
    public void proxyChanged(ProxyDescriptor proxy, boolean updateExistingRepos) {
        if (proxy.isDefaultProxy()) {
            if (updateExistingRepos) {
                updateExistingRepos(proxy);
                updateExistingLocalReplications(proxy);
                sumoLogicConfig.setProxy(proxy);
            }
            //Unset the previous default if any
            for (ProxyDescriptor proxyDescriptor : proxies) {
                if (!proxy.equals(proxyDescriptor)) {
                    proxyDescriptor.setDefaultProxy(false);
                }
            }
        }
    }

    private void updateExistingRepos(ProxyDescriptor proxy) {
        ProxyDescriptor previousDefaultProxy = findPreviousProxyDescriptor(proxy);
        for (RemoteRepoDescriptor remoteRepoDescriptor : remoteRepositoriesMap.values()) {
            if (remoteRepoDescriptor instanceof HttpRepoDescriptor) {
                HttpRepoDescriptor httpRepoDescriptor = (HttpRepoDescriptor) remoteRepoDescriptor;
                ProxyDescriptor existingRepoProxy = httpRepoDescriptor.getProxy();
                // if the repo doesn't have a proxy, or it is the previous default proxy configured then override it.
                if (existingRepoProxy == null || existingRepoProxy.equals(previousDefaultProxy)) {
                    httpRepoDescriptor.setProxy(proxy);
                }
            }
        }
        for (DistributionRepoDescriptor distRepoDescriptor : distributionRepositoriesMap.values()) {
            ProxyDescriptor existingRepoProxy = distRepoDescriptor.getProxy();
            // if the repo doesn't have a proxy, or it is the previous default proxy configured then override it.
            if (existingRepoProxy == null || existingRepoProxy.equals(previousDefaultProxy)) {
                distRepoDescriptor.setProxy(proxy);
            }
        }
    }

    private void updateExistingLocalReplications(ProxyDescriptor proxy) {
        ProxyDescriptor previousDefaultProxy = findPreviousProxyDescriptor(proxy);
        for (LocalReplicationDescriptor localReplication : localReplications) {
            ProxyDescriptor existingProxy = localReplication.getProxy();
            if (existingProxy == null || existingProxy.equals(previousDefaultProxy)) {
                localReplication.setProxy(proxy);
            }
        }
    }

    private ProxyDescriptor findPreviousProxyDescriptor(final ProxyDescriptor proxyDescriptor) {
        return Iterables.find(proxies, new Predicate<ProxyDescriptor>() {
            @Override
            public boolean apply(@Nullable ProxyDescriptor input) {
                return (input != null) && input.isDefaultProxy() && !input.getKey().equals(proxyDescriptor.getKey());
            }
        }, null);
    }

    @Override
    public boolean isBackupExists(String backupKey) {
        return getBackup(backupKey) != null;
    }


    @Override
    public String getLogo() {
        return logo;
    }

    @Override
    public void setLogo(String logo) {
        this.logo = logo;
    }

    @Override
    public SystemMessageDescriptor getSystemMessageConfig() {
        return systemMessageConfig;
    }

    @Override
    public void setSystemMessageConfig(SystemMessageDescriptor systemMessageConfig) {
        this.systemMessageConfig = systemMessageConfig;
    }

    @Override
    public FolderDownloadConfigDescriptor getFolderDownloadConfig() {
        return folderDownloadConfig;
    }

    @Override
    public void setFolderDownloadConfig(FolderDownloadConfigDescriptor folderDownloadConfig) {
        this.folderDownloadConfig = folderDownloadConfig;
    }

    @Override
    public TrashcanConfigDescriptor getTrashcanConfig() {
        return trashcanConfig;
    }

    @Override
    public void setTrashcanConfig(TrashcanConfigDescriptor trashcanConfig) {
        this.trashcanConfig = trashcanConfig;
    }

    @Override
    public SumoLogicConfigDescriptor getSumoLogicConfig() {
        return sumoLogicConfig;
    }

    @Override
    public void setSumoLogicConfig(SumoLogicConfigDescriptor sumoLogicConfig) {
        this.sumoLogicConfig = sumoLogicConfig;
    }

    @Override
    public void addBackup(BackupDescriptor backupDescriptor) {
        String backupKey = backupDescriptor.getKey();
        if (isBackupExists(backupKey)) {
            throw new AlreadyExistsException("Backup " + backupKey + " already exists");
        }
        backups.add(backupDescriptor);
    }

    @Override
    public BackupDescriptor removeBackup(String backupKey) {
        BackupDescriptor backupDescriptor = getBackup(backupKey);
        if (backupDescriptor == null) {
            return null;
        }

        // remove the backup from the backups list
        backups.remove(backupDescriptor);

        return backupDescriptor;
    }

    @Override
    public boolean isPropertySetExists(String propertySetName) {
        return getPropertySet(propertySetName) != null;
    }

    @Override
    public void addPropertySet(PropertySet propertySet) {
        String propertySetName = propertySet.getName();
        if (isPropertySetExists(propertySetName)) {
            throw new AlreadyExistsException("Property set " + propertySetName + " already exists");
        }
        propertySets.add(propertySet);
    }

    @Override
    public PropertySet removePropertySet(String propertySetName) {
        PropertySet propertySet = getPropertySet(propertySetName);
        if (propertySet == null) {
            return null;
        }

        //Remove the property set from the property sets list
        propertySets.remove(propertySet);

        //Remove the property set from any local repo which is associated with it
        Collection<LocalRepoDescriptor> localRepoDescriptorCollection = localRepositoriesMap.values();
        for (LocalRepoDescriptor localRepoDescriptor : localRepoDescriptorCollection) {
            localRepoDescriptor.removePropertySet(propertySetName);
        }

        //Remove the property set from any remote repo which is associated with it
        Collection<RemoteRepoDescriptor> remoteRepoDescriptors = remoteRepositoriesMap.values();
        for (RemoteRepoDescriptor remoteRepoDescriptor : remoteRepoDescriptors) {
            remoteRepoDescriptor.removePropertySet(propertySetName);
        }

        //Remove the property set from any distribution repo which is associated with it
        distributionRepositoriesMap.values().stream()
                .forEach(distRepo -> distRepo.removePropertySet(propertySetName));

        return propertySet;
    }

    @Override
    public boolean isOfflineMode() {
        return offlineMode;
    }

    @Override
    public void setOfflineMode(boolean offlineMode) {
        this.offlineMode = offlineMode;
    }

    @Override
    public boolean isHelpLinksEnabled() {
        return helpLinksEnabled;
    }

    @Override
    public void setHelpLinksEnabled(boolean helpLinksEnabled) {
        this.helpLinksEnabled = helpLinksEnabled;
    }

    @Override
    public ProxyDescriptor defaultProxyDefined() {
        for (ProxyDescriptor proxyDescriptor : proxies) {
            if (proxyDescriptor.isDefaultProxy()) {
                return proxyDescriptor;
            }
        }
        return null;
    }

    @Override
    public ProxyDescriptor getProxy(String proxyKey) {
        for (ProxyDescriptor proxy : proxies) {
            if (proxy.getKey().equals(proxyKey)) {
                return proxy;
            }
        }
        return null;
    }

    @Override
    public ReverseProxyDescriptor getReverseProxy(String key) {
        for (ReverseProxyDescriptor reverseProxy : reverseProxies) {
            if (reverseProxy.getKey().equals(key)) {
                return reverseProxy;
            }
        }
        return null;
    }

    @DiffIgnore
    @Override
    public ReverseProxyDescriptor getCurrentReverseProxy() {
        if (reverseProxies != null && !reverseProxies.isEmpty()) {
            return reverseProxies.get(0);
        }
        return null;
    }

    @Override
    public BackupDescriptor getBackup(String backupKey) {
        for (BackupDescriptor backup : backups) {
            if (backup.getKey().equals(backupKey)) {
                return backup;
            }
        }
        return null;
    }

    private PropertySet getPropertySet(String propertySetName) {
        for (PropertySet propertySet : propertySets) {
            if (propertySet.getName().equals(propertySetName)) {
                return propertySet;
            }
        }

        return null;
    }

    private boolean isLdapExists(String key) {
        return security != null && security.isLdapExists(key);
    }

    private void repoKeyExists(String repoKey, boolean shouldExist) {
        boolean exists = isRepositoryExists(repoKey);
        if (exists && !shouldExist) {
            throw new AlreadyExistsException("Repository " + repoKey + " already exists");
        }

        if (!exists && shouldExist) {
            throw new DoesNotExistException("Repository " + repoKey + " does not exist");
        }
    }

    @Override
    @DiffReferenceable
    public List<RepoLayout> getRepoLayouts() {
        return repoLayouts;
    }

    @Override
    public void setRepoLayouts(List<RepoLayout> repoLayouts) {
        this.repoLayouts = Optional.ofNullable(repoLayouts).orElseGet(Lists::newArrayList);
    }

    @Override
    public boolean isRepoLayoutExists(String repoLayoutName) {
        for (RepoLayout repoLayout : repoLayouts) {
            if (repoLayout.getName().equals(repoLayoutName)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void addRepoLayout(RepoLayout repoLayout) {
        String repoLayoutName = repoLayout.getName();
        if (isRepoLayoutExists(repoLayoutName)) {
            throw new AlreadyExistsException("Repo Layout " + repoLayoutName + " already exists");
        }
        repoLayouts.add(repoLayout);
    }

    @Override
    public RepoLayout removeRepoLayout(String repoLayoutName) {
        RepoLayout repoLayout = getRepoLayout(repoLayoutName);
        if (repoLayout == null) {
            return null;
        }

        repoLayouts.remove(repoLayout);


        Collection<LocalRepoDescriptor> localRepoDescriptorCollection = localRepositoriesMap.values();
        for (LocalRepoDescriptor localRepoDescriptor : localRepoDescriptorCollection) {
            if (repoLayout.equals(localRepoDescriptor.getRepoLayout())) {
                localRepoDescriptor.setRepoLayout(RepoLayoutUtils.MAVEN_2_DEFAULT);
            }
        }

        Collection<RemoteRepoDescriptor> remoteRepoDescriptors = remoteRepositoriesMap.values();
        for (RemoteRepoDescriptor remoteRepoDescriptor : remoteRepoDescriptors) {
            if (repoLayout.equals(remoteRepoDescriptor.getRepoLayout())) {
                remoteRepoDescriptor.setRepoLayout(RepoLayoutUtils.MAVEN_2_DEFAULT);
            }
            if (repoLayout.equals(remoteRepoDescriptor.getRemoteRepoLayout())) {
                remoteRepoDescriptor.setRemoteRepoLayout(null);
            }
        }

        Collection<VirtualRepoDescriptor> virtualRepoDescriptors = virtualRepositoriesMap.values();
        for (VirtualRepoDescriptor virtualRepoDescriptor : virtualRepoDescriptors) {
            if (repoLayout.equals(virtualRepoDescriptor.getRepoLayout())) {
                virtualRepoDescriptor.setRepoLayout(null);
            }
        }

        //TODO [by dan]: ok for dist repo to be without layout?
        distributionRepositoriesMap.values().stream()
                .filter(distRepo -> repoLayout.equals(distRepo.getRepoLayout()))
                .forEach(distRepo -> distRepo.setRepoLayout(null));

        return repoLayout;
    }

    @Override
    public RepoLayout getRepoLayout(String repoLayoutName) {
        for (RepoLayout repoLayout : repoLayouts) {
            if (repoLayout.getName().equals(repoLayoutName)) {
                return repoLayout;
            }
        }

        return null;
    }

    @Override
    public boolean isRemoteReplicationExists(RemoteReplicationDescriptor descriptor) {
        return remoteReplications.contains(descriptor);
    }

    @Override
    public boolean isLocalReplicationExists(LocalReplicationDescriptor descriptor) {
        return localReplications.contains(descriptor);
    }

    @Override
    public List<RemoteReplicationDescriptor> getRemoteReplications() {
        return remoteReplications;
    }

    @Override
    public void setRemoteReplications(List<RemoteReplicationDescriptor> replicationDescriptors) {
        remoteReplications = Optional.ofNullable(replicationDescriptors).orElseGet(Lists::newArrayList);
    }

    @Override
    public List<LocalReplicationDescriptor> getLocalReplications() {
        return localReplications;
    }

    @Override
    public void setLocalReplications(List<LocalReplicationDescriptor> localReplications) {
        this.localReplications = Optional.ofNullable(localReplications).orElseGet(Lists::newArrayList);
    }

    @Override
    public RemoteReplicationDescriptor getRemoteReplication(String replicatedRepoKey) {
        return getReplication(replicatedRepoKey, remoteReplications);
    }

    @Override
    public LocalReplicationDescriptor getLocalReplication(String replicatedRepoKey) {
        return getReplication(replicatedRepoKey, localReplications);
    }

    @Override
    public LocalReplicationDescriptor getEnabledLocalReplication(String replicatedRepoKey) {
        return get1stEnableLocalReplication(replicatedRepoKey, localReplications);
    }

    @Override
    public int getTotalNumOfActiveLocalReplication(String replicatedRepoKey) {
        return getNumOfActiveLocalReplication(replicatedRepoKey, localReplications);
    }

    @Override
    public LocalReplicationDescriptor getLocalReplication(String replicatedRepoKey, String replicateRepoUrl) {
        return getSpecificLocalReplication(replicatedRepoKey, replicateRepoUrl, localReplications);
    }

    @Override
    public void addRemoteReplication(RemoteReplicationDescriptor replDesc) {
        if (StringUtils.isBlank(replDesc.getReplicationKey())) {
            replDesc.setReplicationKey(IdUtils.produceReplicationId(replDesc.getRepoKey(), null));
        }
        addReplication(replDesc, remoteReplications);
    }

    @Override
    public void addLocalReplication(LocalReplicationDescriptor replDesc) {
        if (StringUtils.isBlank(replDesc.getReplicationKey())) {
            replDesc.setReplicationKey(IdUtils.produceReplicationId(replDesc.getRepoKey(), replDesc.getUrl()));
        }
        addLocalReplication(replDesc, localReplications);
    }

    @Override
    public void removeRemoteReplication(RemoteReplicationDescriptor replicationDescriptor) {
        if (replicationDescriptor == null) {
            return;
        }
        removeReplication(replicationDescriptor, remoteReplications);
    }

    @Override
    public void removeLocalReplication(LocalReplicationDescriptor replicationDescriptor) {
        if (replicationDescriptor == null) {
            return;
        }
        removeReplication(replicationDescriptor, localReplications);
    }

    @Override
    public void setGlobalReplicationConfig(GlobalReplicationsConfigDescriptor globalReplicationConfig) {
        this.replicationsConfig = globalReplicationConfig;
    }

    @DiffIgnore
    @Override
    public String getServerUrlForEmail() {
        String serverUrl = "";
        if (mailServer != null) {
            String artifactoryUrl = mailServer.getArtifactoryUrl();
            if (StringUtils.isNotBlank(artifactoryUrl)) {
                serverUrl = artifactoryUrl;
            }
        }

        if (StringUtils.isBlank(serverUrl) && StringUtils.isNotBlank(urlBase)) {
            serverUrl = urlBase;
        }

        if (StringUtils.isNotBlank(serverUrl) && !serverUrl.endsWith("/")) {
            serverUrl += "/";
        }

        return serverUrl;
    }

    @Override
    public GcConfigDescriptor getGcConfig() {
        return gcConfig;
    }

    @Override
    public void setGcConfig(GcConfigDescriptor gcConfig) {
        this.gcConfig = gcConfig;
    }

    @Override
    public CleanupConfigDescriptor getCleanupConfig() {
        return cleanupConfig;
    }

    @Override
    public void setCleanupConfig(CleanupConfigDescriptor cleanupConfigDescriptor) {
        this.cleanupConfig = cleanupConfigDescriptor;
    }

    @Override
    public QuotaConfigDescriptor getQuotaConfig() {
        return quotaConfig;
    }

    @Override
    public void setQuotaConfig(QuotaConfigDescriptor descriptor) {
        this.quotaConfig = descriptor;
    }

    @DiffIgnore
    @Override
    public Map<String, LocalReplicationDescriptor> getLocalReplicationsMap() {
        Map<String, LocalReplicationDescriptor> localReplicationsMap = Maps.newHashMap();
        for (LocalReplicationDescriptor localReplication : localReplications) {
            localReplicationsMap.put(localReplication.getRepoKey(), localReplication);
        }

        return localReplicationsMap;
    }

    @DiffIgnore
    @Override
    public Map<String, LocalReplicationDescriptor> getSingleReplicationPerRepoMap() {
        Map<String, LocalReplicationDescriptor> localReplicationsMap = Maps.newHashMap();
        for (LocalReplicationDescriptor localReplication : localReplications) {
            localReplicationsMap.put(localReplication.getRepoKey(), localReplication);
        }
        return localReplicationsMap;
    }

    @Override
    public Map<String, LocalReplicationDescriptor> getLocalReplicationsPerRepoMap(String repoName) {
        Map<String, LocalReplicationDescriptor> localReplicationsMap = new HashMap();
        for (LocalReplicationDescriptor localReplication : localReplications) {
            if (localReplication.getRepoKey().equals(repoName)) {
                localReplicationsMap.put(localReplication.getUrl(), localReplication);
            }
        }
        return localReplicationsMap;
    }

    @Override
    public Map<String, RemoteReplicationDescriptor> getRemoteReplicationsPerRepoMap(String repoName) {
        Map<String, RemoteReplicationDescriptor> localReplicationsMap = new HashMap();
        for (RemoteReplicationDescriptor remoteReplication : remoteReplications) {
            if (remoteReplication.getRepoKey().equals(repoName)) {
                localReplicationsMap.put(remoteReplication.getRepoKey(), remoteReplication);
            }
        }
        return localReplicationsMap;
    }

    @Override
    public List<String> getLocalReplicationsUniqueKeyForProperty(String repoName) {
        List<String> localReplicationsList = new ArrayList<>();
        for (LocalReplicationDescriptor localReplication : localReplications) {
            if (localReplication.getRepoKey().equals(repoName)) {
                String uniqueKey = localReplication.getUrl().replaceAll("^(http|https)://", "_").replaceAll("/|:", "_");
                localReplicationsList.add(uniqueKey);
            }
        }
        return localReplicationsList;
    }

    @Override
    public CleanupConfigDescriptor getVirtualCacheCleanupConfig() {
        return virtualCacheCleanupConfig;
    }

    @Override
    public void setVirtualCacheCleanupConfig(CleanupConfigDescriptor virtualCacheCleanupConfig) {
        this.virtualCacheCleanupConfig = virtualCacheCleanupConfig;
    }

    private <T extends ReplicationBaseDescriptor> void addReplication(T replicationDescriptor,
            List<T> replications) {
        if (replications.contains(replicationDescriptor)) {
            throw new AlreadyExistsException("Replication for '" + replicationDescriptor.getRepoKey() +
                    "' already exists");
        }
        replications.add(replicationDescriptor);
    }

    /**
     * update if exist / add (new) local replication descriptor
     *
     * @param replicationDescriptor - new or update local replication descriptor
     * @param replications          - all replication descriptors
     * @param <T>
     */
    private <T extends ReplicationBaseDescriptor> void addLocalReplication(T replicationDescriptor,
            List<T> replications) {
        if (replications.contains(replicationDescriptor)) {
            replications.remove(replicationDescriptor);
        }
        replications.add(replicationDescriptor);
    }

    private <T extends ReplicationBaseDescriptor> T getReplication(String replicatedRepoKey, List<T> replications) {
        if (StringUtils.isNotBlank(replicatedRepoKey)) {
            for (T replication : replications) {
                if (replicatedRepoKey.equals(replication.getRepoKey())) {
                    return replication;
                }
            }
        }

        return null;
    }

    /**
     * get specific Local Replication based on replicate repo key and repo url
     *
     * @param replicatedRepoKey - repository key
     * @param replicateRepoUrl  - repository url
     * @param replications      - all replication in artifactory
     * @param <T>
     * @return
     */
    private <T extends ReplicationBaseDescriptor> T getSpecificLocalReplication(String replicatedRepoKey,
            String replicateRepoUrl, List<T> replications) {
        if (StringUtils.isNotBlank(replicatedRepoKey)) {
            for (T replication : replications) {

                if (replicatedRepoKey.equals(replication.getRepoKey()) && replicateRepoUrl.equals(
                        ((LocalReplicationDescriptor) replication).getUrl())) {
                    return replication;
                }
            }
        }

        return null;
    }

    /**
     * get specific Local Replication based on replicate repo key and repo url
     *
     * @param replicatedRepoKey - repository key
     * @param replications      - all replication in artifactory
     * @return
     */
    private <T extends ReplicationBaseDescriptor> T get1stEnableLocalReplication(String replicatedRepoKey,
            List<T> replications) {
        if (StringUtils.isNotBlank(replicatedRepoKey)) {
            for (T replication : replications) {
                if (replicatedRepoKey.equals(replication.getRepoKey()) && replication.isEnabled()) {
                    return replication;
                }
            }
        }
        return null;
    }

    /**
     * get specific Local Replication based on replicate repo key and repo url
     *
     * @param replicatedRepoKey - repository key
     * @param replications      - all replication in artifactory
     * @return
     */
    private <T extends ReplicationBaseDescriptor> int getNumOfActiveLocalReplication(String replicatedRepoKey,
            List<T> replications) {
        int replicationCounter = 0;
        if (StringUtils.isNotBlank(replicatedRepoKey)) {
            for (T replication : replications) {
                if (replicatedRepoKey.equals(replication.getRepoKey()) && replication.isEnabled()) {
                    replicationCounter++;
                }
            }
        }
        return replicationCounter;
    }

    private <T extends ReplicationBaseDescriptor> void removeReplication(T replicationDescriptor,
            List<T> replications) {
        if (replicationDescriptor.getReplicationKey() == null) {
            throw new IllegalStateException("Replication key must be initialized for" +
                    replicationDescriptor.getRepoKey() + " replication.");
        }
        replications.removeIf(repl -> replicationDescriptor.getReplicationKey().equals(repl.getReplicationKey()));
    }

    @Override
    public BintrayConfigDescriptor getBintrayConfig() {
        return bintrayConfig;
    }

    @Override
    public void setBintrayConfig(BintrayConfigDescriptor bintrayConfigDescriptor) {
        this.bintrayConfig = bintrayConfigDescriptor;
    }

    @Override
    public GlobalReplicationsConfigDescriptor getReplicationsConfig() {
        return replicationsConfig;
    }

    public void setReplicationsConfig(GlobalReplicationsConfigDescriptor replications) {
        this.replicationsConfig = Optional.ofNullable(replications).orElseGet(GlobalReplicationsConfigDescriptor::new);
    }

    @Override
    public List<LocalReplicationDescriptor> getMultiLocalReplications(String repoKey) {
        return getMultiLocalReplications(repoKey, localReplications);
    }

    private <T extends ReplicationBaseDescriptor> List<T> getMultiLocalReplications(String replicatedRepoKey,
            List<T> replications) {
        List<T> localReplicationList = new ArrayList<>();
        if (StringUtils.isNotBlank(replicatedRepoKey)) {
            for (T replication : replications) {
                if (replicatedRepoKey.equals(replication.getRepoKey()))
                    localReplicationList.add(replication);
            }
        }
        return localReplicationList;
    }

    @Override
    @DiffReferenceable
    public Map<String, BintrayApplicationConfig> getBintrayApplications() {
        return bintrayApplications;
    }

    @Override
    public void setBintrayApplications(Map<String, BintrayApplicationConfig> bintrayApplications) {
        this.bintrayApplications = Optional.ofNullable(bintrayApplications).orElseGet(HashMap::new);
    }

    @Override
    public BintrayApplicationConfig getBintrayApplication(String bintrayApplicationKey) {
        return bintrayApplications.get(bintrayApplicationKey);
    }

    @Override
    public void addBintrayApplication(BintrayApplicationConfig bintrayApplicationConfig) throws AlreadyExistsException {
        String key = bintrayApplicationConfig.getKey();
        if (bintrayApplications.containsKey(key)) {
            throw new AlreadyExistsException("Bintray Application config '" + key + "' already exists");
        }
        bintrayApplications.put(key, bintrayApplicationConfig);
    }

    @Override
    public BintrayApplicationConfig removeBintrayApplication(String appConfigKey) {
        return bintrayApplications.remove(appConfigKey);
    }
}

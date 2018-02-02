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

package org.artifactory.descriptor.repo;

import org.artifactory.descriptor.Descriptor;
import org.jfrog.common.DiffAtomic;
import org.jfrog.common.DiffIgnore;
import org.jfrog.common.DiffReference;
import org.jfrog.common.GenerateDiffFunction;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@XmlType(name = "VirtualRepoType", propOrder = {"artifactoryRequestsCanRetrieveRemoteArtifacts", "repositories",
        "keyPair", "pomRepositoryReferencesCleanupPolicy", "p2", "defaultDeploymentRepo", "externalDependencies",
        "virtualCacheConfig"},
        namespace = Descriptor.NS)
@GenerateDiffFunction
public class VirtualRepoDescriptor extends RepoBaseDescriptor {

    @XmlIDREF
    @XmlElementWrapper(name = "repositories")
    @XmlElement(name = "repositoryRef", type = RepoBaseDescriptor.class, required = false)
    private List<RepoDescriptor> repositories = new ArrayList<>();

    @XmlElement(defaultValue = "false", required = false)
    private boolean artifactoryRequestsCanRetrieveRemoteArtifacts;

    @XmlElement(required = true)
    private String keyPair;

    @XmlElement(defaultValue = "discard_active_reference", required = true)
    private PomCleanupPolicy pomRepositoryReferencesCleanupPolicy = PomCleanupPolicy.discard_active_reference;

    @XmlElement(required = false)
    private P2Configuration p2;

    @XmlIDREF
    @XmlElement(name = "defaultDeploymentRepo", type = LocalRepoDescriptor.class, required = false)
    private LocalRepoDescriptor defaultDeploymentRepo;

    @XmlElement(required = false)
    private ExternalDependenciesConfig externalDependencies;

    @XmlElement(required = false)
    private VirtualCacheConfig virtualCacheConfig = new VirtualCacheConfig();

    @DiffReference
    @DiffAtomic
    public List<RepoDescriptor> getRepositories() {
        return repositories;
    }

    public void setRepositories(List<RepoDescriptor> repositories) {
        this.repositories = Optional.ofNullable(repositories).orElseGet(ArrayList::new);
    }

    public boolean isArtifactoryRequestsCanRetrieveRemoteArtifacts() {
        return artifactoryRequestsCanRetrieveRemoteArtifacts;
    }

    public void setArtifactoryRequestsCanRetrieveRemoteArtifacts(
            boolean artifactoryRequestsCanRetrieveRemoteArtifacts) {
        this.artifactoryRequestsCanRetrieveRemoteArtifacts = artifactoryRequestsCanRetrieveRemoteArtifacts;
    }

    public String getKeyPair() {
        return keyPair;
    }

    public void setKeyPair(String keyPair) {
        this.keyPair = keyPair;
    }

    @DiffIgnore
    @Override
    public boolean isReal() {
        return false;
    }

    public boolean removeRepository(RepoDescriptor repo) {
        return repositories.remove(repo);
    }

    public void removeKeyPair() {
        keyPair = null;
    }

    public PomCleanupPolicy getPomRepositoryReferencesCleanupPolicy() {
        return pomRepositoryReferencesCleanupPolicy;
    }

    public void setPomRepositoryReferencesCleanupPolicy(PomCleanupPolicy pomRepositoryReferencesCleanupPolicy) {
        this.pomRepositoryReferencesCleanupPolicy = pomRepositoryReferencesCleanupPolicy;
    }

    public P2Configuration getP2() {
        return p2;
    }

    public void setP2(P2Configuration p2) {
        this.p2 = p2;
    }

    @DiffReference
    public LocalRepoDescriptor getDefaultDeploymentRepo() {
        return defaultDeploymentRepo;
    }

    public void setDefaultDeploymentRepo(LocalRepoDescriptor defaultDeploymentRepo) {
        this.defaultDeploymentRepo = defaultDeploymentRepo;
    }

    public ExternalDependenciesConfig getExternalDependencies() {
        return externalDependencies;
    }

    public void setExternalDependencies(ExternalDependenciesConfig externalDependencies) {
        this.externalDependencies = externalDependencies;
    }

    public VirtualCacheConfig getVirtualCacheConfig() {
        return virtualCacheConfig;
    }

    public void setVirtualCacheConfig(VirtualCacheConfig virtualCacheConfig) {
        this.virtualCacheConfig = virtualCacheConfig;
    }
}
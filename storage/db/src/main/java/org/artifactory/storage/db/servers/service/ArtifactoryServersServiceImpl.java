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

package org.artifactory.storage.db.servers.service;

import org.artifactory.state.ArtifactoryServerState;
import org.artifactory.storage.StorageException;
import org.artifactory.storage.db.servers.dao.ArtifactoryServersDao;
import org.artifactory.storage.db.servers.model.ArtifactoryServer;
import org.artifactory.storage.db.servers.model.ArtifactoryServerRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.List;

/**
 * Business service for artifactory servers management.
 *
 * @author Yossi Shaul
 */
@Service
public class ArtifactoryServersServiceImpl implements ArtifactoryServersService {

    @Autowired
    private ArtifactoryServersDao serversDao;

    @Override
    public List<ArtifactoryServer> getAllArtifactoryServers() {
        try {
            return serversDao.getAllArtifactoryServers();
        } catch (SQLException e) {
            throw new StorageException("Failed to load list of Artifactory servers", e);
        }
    }

    @Override
    public ArtifactoryServer getArtifactoryServer(String nodeId) {
        try {
            return serversDao.getArtifactoryServer(nodeId);
        } catch (SQLException e) {
            throw new StorageException("Failed to get Artifactory server [" + nodeId + "]", e);
        }
    }

    @Override
    public int createArtifactoryServer(ArtifactoryServer artifactoryServer) {
        try {
            return serversDao.createArtifactoryServer(artifactoryServer);
        } catch (SQLException e) {
            throw new StorageException("Failed to create Artifactory server" +
                    " [" + getServerIdForDisplay(artifactoryServer) + "]", e);
        }
    }

    @Override
    public int updateArtifactoryServer(ArtifactoryServer artifactoryServer) {
        try {
            return serversDao.updateArtifactoryServer(artifactoryServer);
        } catch (SQLException e) {
            throw new StorageException("Failed to update Artifactory server" +
                    " [" + getServerIdForDisplay(artifactoryServer) + "]", e);
        }
    }

    //
    @Override
    public int updateArtifactoryServerState(String serverId, ArtifactoryServerState newState) {
        try {
            return serversDao.updateArtifactoryServerState(serverId, newState, System.currentTimeMillis());
        } catch (SQLException e) {
            throw new StorageException("Failed to update Artifactory server [" + serverId.trim() + "]", e);
        }
    }

    @Override
    public int updateArtifactoryServerRole(String serverId, ArtifactoryServerRole newRole) {
        try {
            return serversDao.updateArtifactoryServerRole(serverId, newRole);
        } catch (SQLException e) {
            throw new StorageException("Failed to update Artifactory server role [" + serverId + "]", e);
        }
    }

    @Override
    public int updateArtifactoryJoinPort(String serverId, int joinPort) {
        try {
            return serversDao.updateArtifactoryMembershipPort(serverId, joinPort);
        } catch (SQLException e) {
            throw new StorageException("Failed to update Artifactory server join port [" + serverId + "]", e);
        }
    }

    @Override
    public boolean removeServer(String serverId) {
        try {
            return serversDao.removeServer(serverId);
        } catch (SQLException e) {
            throw new StorageException("Failed to delete server with id '" + serverId + "': " + e.getMessage(), e);
        }
    }

    @Override
    public int updateArtifactoryServerHeartbeat(String serverId, long lastHeartbeat, String licenseKeyHash) {
        try {
            return serversDao.updateArtifactoryServerHeartbeat(serverId, lastHeartbeat, licenseKeyHash);
        } catch (SQLException e) {
            throw new StorageException("Failed to update Artifactory server heartbeat [" + serverId + "]", e);
        }
    }

    public String getServerIdForDisplay(ArtifactoryServer artifactoryServer) {
        return artifactoryServer.getServerId().trim();
    }
}

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

package org.artifactory.api.download;

import com.google.common.collect.Lists;
import org.apache.commons.collections.CollectionUtils;
import org.artifactory.repo.RepoPath;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Holds information about the folder requested for download
 *
 * @author Dan Feldman
 */
public class FolderDownloadInfo {

    private double sizeMb;
    private long totalFiles;
    private List<String> xrayBlockedPaths;

    public FolderDownloadInfo(double sizeMb, long totalFiles, List<RepoPath> xrayBlockedPaths) {
        this.sizeMb = sizeMb;
        this.totalFiles = totalFiles;
        if (CollectionUtils.isNotEmpty(xrayBlockedPaths)) {
            this.xrayBlockedPaths = xrayBlockedPaths.stream()
                    .map(RepoPath::getPath)
                    .collect(Collectors.toList());
        } else {
            this.xrayBlockedPaths = Lists.newArrayList();
        }
    }

    public double getSizeMb() {
        return sizeMb;
    }

    public long getTotalFiles() {
        return totalFiles;
    }

    public List<String> getXrayBlockedPaths() {
        return xrayBlockedPaths;
    }
}

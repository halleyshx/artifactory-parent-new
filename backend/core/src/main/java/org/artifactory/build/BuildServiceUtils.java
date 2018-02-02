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

package org.artifactory.build;

import com.google.common.collect.*;
import org.artifactory.api.common.BasicStatusHolder;
import org.artifactory.fs.FileInfo;
import org.jfrog.build.api.Artifact;
import org.jfrog.build.api.*;
import org.jfrog.build.api.Dependency;
import org.jfrog.build.api.Module;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * @author Dan Feldman
 */
public class BuildServiceUtils {
    private static final Logger log = LoggerFactory.getLogger(BuildServiceUtils.class);

    public static Set<FileInfo> filterOutNullFileInfos(Iterable<FileInfo> rawInfos) {
        return StreamSupport.stream(rawInfos.spliterator(), false)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    public static Iterable<FileInfo> toFileInfoList(Set<ArtifactoryBuildArtifact> artifacts) {
        return artifacts.stream()
                .map(input -> input != null ? input.getFileInfo() : null)
                .collect(Collectors.toList());
    }

    /**
     * Map all build dependencies to checksum, held in a multimap for occurrences of duplicate checksum for different
     * dependencies --> although we cannot be 100% positive which dependency took part in the build with the current
     * BuildInfo implementation.
     */
    static Multimap<String, Dependency> getBuildDependencies(Build build) {
        Multimap<String, Dependency> beansMap = HashMultimap.create();
        List<Module> modules = build.getModules();
        if (modules == null) {
            return beansMap;
        }
        for (Module module : modules) {
            if (module.getDependencies() != null) {
                for (Dependency dependency : module.getDependencies()) {
                    if (dependency.getSha1() != null) {
                        beansMap.put(dependency.getSha1(), dependency);
                    } else {
                        log.warn("Dependency: " + dependency.getId() + " is missing SHA1," + " under build: "
                                + build.getName());
                    }
                }
            }
        }
        return beansMap;
    }

    /**
     * Map all build artifacts to checksum, held in a multimap for occurrences of duplicate checksum for different
     * artifacts so that the search results return all
     */
    static Multimap<String, Artifact> getBuildArtifacts(Build build) {
        //ListMultiMap to hold possible duplicate artifacts coming from BuildInfo
        Multimap<String, Artifact> beansMap = ArrayListMultimap.create();

        List<Module> modules = build.getModules();
        if (modules == null) {
            return beansMap;
        }
        modules.stream()
                .map(Module::getArtifacts)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .forEach(artifact -> {
                    if (artifact.getSha1() != null) {
                        beansMap.put(artifact.getSha1(), artifact);
                    } else {
                        log.warn("Artifact: " + artifact.getName() + " is missing SHA1," + " under build: " + build.getName());
                    }
                });
        return beansMap;
    }

    static void verifyAllArtifactInfosExistInSet(Build build, boolean cleanNullEntries, BasicStatusHolder statusHolder,
            Set<ArtifactoryBuildArtifact> buildArtifactsInfos, VerifierLogLevel logLevel) {
        for (Iterator<ArtifactoryBuildArtifact> iter = buildArtifactsInfos.iterator(); iter.hasNext(); ) {
            ArtifactoryBuildArtifact artifact = iter.next();
            if (artifact.getFileInfo() == null) {
                String errorMsg = "Unable to find artifact '" + artifact.getArtifact().getName() + "' of build '" + build.getName()
                        + "' #" + build.getNumber();
                logToStatus(statusHolder, errorMsg, logLevel);
                if (cleanNullEntries) {
                    iter.remove();
                }
            }
        }
    }

    /**
     * Verifies all dependencies from the build exist in the map and writes appropriate entries to the StatusHolder
     * based on the chosen log level.
     * NOTE: Relies on missing dependency to have a null mapping (as returned by {@link getBuildDependenciesFileInfos}
     *
     * @param build                 Build to verify
     * @param statusHolder          StatusHolder that entries will be written into
     * @param buildDependenciesInfo Mapping of Dependencies to FileInfos
     */
    static void verifyAllDependencyInfosExistInMap(Build build, boolean cleanNullEntries, BasicStatusHolder statusHolder,
            Map<Dependency, FileInfo> buildDependenciesInfo, VerifierLogLevel logLevel) {
        List<BuildFileBean> keysToRemove = Lists.newArrayList();
        for (Map.Entry<Dependency, FileInfo> entry : buildDependenciesInfo.entrySet()) {
            if (entry.getValue() == null) {
                String errorMsg = "Unable to find dependency '" + entry.getKey().getId() + "' of build '"
                        + build.getName() + "' #" + build.getNumber();
                keysToRemove.add(entry.getKey());
                logToStatus(statusHolder, errorMsg, logLevel);
            }
        }
        if (cleanNullEntries) {
            for (BuildFileBean keyToRemove : keysToRemove) {
                buildDependenciesInfo.remove(keyToRemove);
            }
        }
    }

    private static void logToStatus(BasicStatusHolder statusHolder, String errorMsg, VerifierLogLevel logLevel) {
        switch (logLevel) {
            case err:
                statusHolder.error(errorMsg, log);
                break;
            case warn:
                statusHolder.warn(errorMsg, log);
                break;
            case debug:
                statusHolder.debug(errorMsg, log);
        }
    }

    public enum VerifierLogLevel {
        err, warn, debug
    }
}
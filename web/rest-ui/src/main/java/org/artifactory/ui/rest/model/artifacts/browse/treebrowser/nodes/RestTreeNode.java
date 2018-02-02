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

package org.artifactory.ui.rest.model.artifacts.browse.treebrowser.nodes;

import com.google.common.collect.Lists;
import org.artifactory.common.ConstantValues;
import org.artifactory.repo.RepoType;
import org.artifactory.rest.common.model.RestModel;
import org.codehaus.jackson.annotate.JsonSubTypes;
import org.codehaus.jackson.annotate.JsonTypeInfo;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Chen Keinan
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = JunctionNode.class, name = "junction"),
        @JsonSubTypes.Type(value = RootNode.class, name = "root")})
public interface RestTreeNode extends RestModel {

    Collection<? extends RestModel> fetchItemTypeData(boolean isCompact);

    /**
     * Merge the user repo type order with the default order which is DISTRIBUTION,LOCAL,REMOTE,VIRTUAL;
     */
    static Collection<RepoType> getRepoOrder() {
        // Load the user order
        String[] userOrder = ConstantValues.orderTreeBrowserRepositoriesByType.getString().toLowerCase().split(",");
        // Load the default order
        ArrayList<RepoType> sortedRepoTypes = Lists.newArrayList(RepoType.values());
        // Merge user and default order based on user order first
        for (int i = userOrder.length-1; i >=0; i--) {
            String stringRepoType = userOrder[i];
            RepoType repoType = RepoType.byNativeName(stringRepoType);
            if (sortedRepoTypes.contains(repoType)) {
                prioritize(sortedRepoTypes, repoType);
            }
        }
        return sortedRepoTypes;
    }

    static void prioritize(ArrayList<RepoType> sortedRepoTypes, RepoType repoType) {
        sortedRepoTypes.remove(repoType);
        sortedRepoTypes.add(0, repoType);
    }
}
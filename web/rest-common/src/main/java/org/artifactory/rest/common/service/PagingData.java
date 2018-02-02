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

package org.artifactory.rest.common.service;

/**
 * @author Chen Keinan
 */
public class PagingData {

    private String orderBy;
    private String startOffset;
    private String limit;
    private String direction;

    public PagingData(ArtifactoryRestRequest restRequest) {
        this.orderBy = restRequest.getQueryParamByKey("orderBy");
        limit = restRequest.getQueryParamByKey("numOfRows");
        if (!limit.isEmpty()) {
            int numOfRows = Integer.parseInt(limit);
            int startRowNumber = ((Integer.parseInt(restRequest.getQueryParamByKey("pageNum")) - 1) * numOfRows);
            this.startOffset = startRowNumber + "";
        }
        this.direction = restRequest.getQueryParamByKey("direction");
    }

    public String getOrderBy() {
        return orderBy;
    }

    public void setOrderBy(String orderBy) {
        this.orderBy = orderBy;
    }

    public String getStartOffset() {
        return startOffset;
    }

    public void setStartOffset(String startOffset) {
        this.startOffset = startOffset;
    }

    public String getLimit() {
        return limit;
    }

    public void setLimit(String limit) {
        this.limit = limit;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }
}

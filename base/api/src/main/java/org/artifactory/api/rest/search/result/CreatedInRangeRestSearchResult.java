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


package org.artifactory.api.rest.search.result;

import java.util.ArrayList;
import java.util.List;

/**
 * Json object retuning the CreatedInRangeResource search results
 *
 * @author Eli Givoni
 */
public class CreatedInRangeRestSearchResult {
    public List<CreatedEntry> results = new ArrayList<>();

    public static class CreatedEntry {
        public String uri;
        public String created;

        public CreatedEntry(String uri, String created) {
            this.uri = uri;
            this.created = created;
        }

        private CreatedEntry() {
        }
    }
}

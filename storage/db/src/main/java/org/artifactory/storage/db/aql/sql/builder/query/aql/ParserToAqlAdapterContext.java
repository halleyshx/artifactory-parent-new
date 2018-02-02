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

package org.artifactory.storage.db.aql.sql.builder.query.aql;

import com.google.common.collect.Lists;
import org.artifactory.storage.db.aql.parser.elements.ParserElement;
import org.jfrog.security.util.Pair;

import java.util.List;

/**
 * @author Gidi Shabat
 */
public class ParserToAqlAdapterContext extends AdapterContext {

    private int index;
    private List<Pair<ParserElement, String>> elements = Lists.newArrayList();

    public ParserToAqlAdapterContext(List<Pair<ParserElement, String>> elements) {
        this.elements = elements;
        index = elements.size() - 1;
    }

    public Pair<ParserElement, String> getElement() {
        return elements.get(index);
    }

    public void decrementIndex(int i) {
        index = index - i;
    }

    public int getIndex() {
        return index;
    }

    public void resetIndex() {
        index = elements.size() - 1;
    }

    public boolean hasNext() {
        return index >= 0;
    }
}

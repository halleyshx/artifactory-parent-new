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

package org.artifactory.storage.db.aql.parser.elements.high.level.language.action;

import org.artifactory.storage.db.aql.parser.elements.ParserElement;
import org.artifactory.storage.db.aql.parser.elements.low.level.InternalValueElement;
import org.artifactory.storage.db.aql.parser.elements.low.level.LazyParserElement;

/**
 * @author Dan Feldman
 */
public class ActionNewValueElement extends LazyParserElement {

    @Override
    protected ParserElement init() {
        return forward(new InternalValueElement());
    }

    @Override
    public boolean isVisibleInResult() {
        return true;
    }
}

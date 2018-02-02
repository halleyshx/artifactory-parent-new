package org.artifactory.storage.db.aql.parser.elements.high.level.language.action;

import org.artifactory.storage.db.aql.parser.elements.ParserElement;
import org.artifactory.storage.db.aql.parser.elements.high.level.domain.action.AqlDomainAction;
import org.artifactory.storage.db.aql.parser.elements.low.level.InternalNameElement;
import org.artifactory.storage.db.aql.parser.elements.low.level.LazyParserElement;

/**
 * @author Dan Feldman
 */
public class FindActionElement extends LazyParserElement implements AqlDomainAction {

    @Override
    protected ParserElement init() {
        return forward(new InternalNameElement("find"));
    }

    @Override
    public boolean isVisibleInResult() {
        return true;
    }
}
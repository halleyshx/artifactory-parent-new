package org.artifactory.storage.db.aql.parser.elements.high.level.domain.action;

import org.artifactory.storage.db.aql.parser.elements.ParserElement;
import org.artifactory.storage.db.aql.parser.elements.low.level.InternalNameElement;
import org.artifactory.storage.db.aql.parser.elements.low.level.LazyParserElement;

import static org.artifactory.storage.db.aql.parser.AqlParser.*;

/**
 * @author Dan Feldman
 */
public class DryRunElement extends LazyParserElement {

    @Override
    protected ParserElement init() {
        return forward(new InternalNameElement("dryRun"), openBrackets, quotes, dryRunValue, quotes, closeBrackets);
    }

    @Override
    public boolean isVisibleInResult() {
        return true;
    }
}
/*
 *
 * Copyright 2016 JFrog Ltd. All rights reserved.
 * JFROG PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package org.artifactory.maven;

import org.artifactory.aql.result.AqlLazyResult;
import org.artifactory.aql.result.rows.RowResult;
import org.artifactory.aql.result.rows.populate.PhysicalFieldResultPopulators;
import org.artifactory.aql.result.rows.populate.ResultPopulationContext;
import org.artifactory.aql.result.rows.populate.RowPopulation;
import org.artifactory.aql.result.rows.populate.RowPopulationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;

/**
 * @author gidis
 */
public class AqlLazyObjectResultStreamer<T extends RowResult> {
    private static final Logger log = LoggerFactory.getLogger(AqlLazyObjectResultStreamer.class);

    private final ResultSet resultSet;
    private final Class<T> rowClass;
    private final ResultPopulationContext resultContext;

    public AqlLazyObjectResultStreamer(AqlLazyResult aqlLazyResult, Class<T> rowClass) {
        this.rowClass = rowClass;
        this.resultSet = aqlLazyResult.getResultSet();
        this.resultContext = new ResultPopulationContext(resultSet, aqlLazyResult.getFields(), aqlLazyResult.getRepoProvider());
    }

    public T getRow() {
        try {
            T row = rowClass.newInstance();
            if (resultSet.next()) {
                RowPopulationContext populationContext = new RowPopulationContext(resultContext, row);
                RowPopulation.populatePhysicalFields(populationContext, PhysicalFieldResultPopulators.forObjects);
                RowPopulation.populateLogicalFields(populationContext);
                return row;
            }
        } catch (Exception e) {
            log.error("Fail to create row: ", e);
        }
        return null;
    }
}


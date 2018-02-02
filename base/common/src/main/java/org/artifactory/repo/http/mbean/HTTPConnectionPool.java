package org.artifactory.repo.http.mbean;

import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

/**
 * @author Ofer Cohen
 */
public class HTTPConnectionPool implements HTTPConnectionPoolMBean {

    private PoolingHttpClientConnectionManager connectionPool;

    public HTTPConnectionPool(PoolingHttpClientConnectionManager connectionPool) {
        this.connectionPool = connectionPool;

    }

    @Override
    public int getAvailable() {
        return connectionPool.getTotalStats().getAvailable();
    }

    @Override
    public int getLeased() {
        return connectionPool.getTotalStats().getLeased();
    }

    @Override
    public int getMax() {
        return connectionPool.getTotalStats().getMax();
    }

    @Override
    public int getPending() {
        return connectionPool.getTotalStats().getPending();
    }
}

package org.artifactory.repo.http;

import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

public interface ConnectionManagersHolder {

    long size();

    void put(String key, PoolingHttpClientConnectionManager value);

    PoolingHttpClientConnectionManager get(String key);

    void remove(String key);

    Iterable<PoolingHttpClientConnectionManager> values();
}

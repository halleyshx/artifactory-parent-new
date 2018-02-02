package org.artifactory.repo.http;


import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.stereotype.Component;

/**
 * Thread safe class that holds connections managers with weak reference.
 */
@Component
public class GuaveCacheConnectionManagersHolder implements ConnectionManagersHolder {

    private final Cache<String, PoolingHttpClientConnectionManager> connections = CacheBuilder
            .newBuilder()
            .weakKeys()
            .build();


    @Override
    public long size() {
        return connections.size();
    }

    @Override
    public void put(String key, PoolingHttpClientConnectionManager value) {
        connections.put(key, value);
    }

    @Override
    public void remove(String key) {
        connections.invalidate(key);
    }

    @Override
    public Iterable<PoolingHttpClientConnectionManager> values() {
        return connections.asMap().values();
    }

    public PoolingHttpClientConnectionManager get(String key) {
        return connections.getIfPresent(key);
    }
}

package org.artifactory.event;

import org.springframework.context.ApplicationEvent;

public class InvalidateCacheEvent extends ApplicationEvent {

    private CacheType cacheType;

    public InvalidateCacheEvent(Object source, CacheType cacheType) {
        super(source);
        this.cacheType = cacheType;
    }

    public CacheType getCacheType() {
        return cacheType;
    }
}

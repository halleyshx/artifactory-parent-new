package org.artifactory.work.queue;

import java.lang.reflect.Method;

/**
 * @author Shay Bagants
 */
public class WorkItemWrapper<T> {

    private T workItem;
    private Method workQueueCallback;

    public WorkItemWrapper(T workItem, Method workQueueCallback) {
        this.workItem = workItem;
        this.workQueueCallback = workQueueCallback;
    }

    public T getWorkItem() {
        return workItem;
    }

    public Method getMethod() {
        return workQueueCallback;
    }
}

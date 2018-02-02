package org.artifactory.schedule;

/**
 * @author Inbar Tal
 */
public interface TaskServiceCallBack {
    void cancelMyselfCallback(TaskBase taskBase);
}

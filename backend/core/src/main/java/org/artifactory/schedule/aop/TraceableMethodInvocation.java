/*
 *
 * Copyright 2016 JFrog Ltd. All rights reserved.
 * JFROG PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package org.artifactory.schedule.aop;

import org.aopalliance.intercept.MethodInvocation;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;

/**
 * @author gidis
 */
class TraceableMethodInvocation implements MethodInvocation {

    private final MethodInvocation wrapped;
    private final Throwable throwable;
    private final WorkExecution workExecution;

    public TraceableMethodInvocation(MethodInvocation wrapped, String threadName) {
        this(wrapped, threadName, null);
    }

    public TraceableMethodInvocation(MethodInvocation wrapped, String threadName, WorkExecution workExecution) {
        this.wrapped = wrapped;
        String msg = "[" + threadName + "] async call to '" + wrapped.getMethod() + "' completed with error.";
        this.throwable = new Throwable(msg);
        this.workExecution = workExecution;
    }

    public WorkExecution getWorkExecution() {
        return workExecution;
    }

    public MethodInvocation getWrapped() {
        return wrapped;
    }

    public Throwable getThrowable() {
        return throwable;
    }

    @Override
    public Method getMethod() {
        return wrapped.getMethod();
    }

    @Override
    public Object[] getArguments() {
        return wrapped.getArguments();
    }

    @Override
    public Object proceed() throws Throwable {
        return wrapped.proceed();
    }

    @Override
    public Object getThis() {
        return wrapped.getThis();
    }

    @Override
    public AccessibleObject getStaticPart() {
        return wrapped.getStaticPart();
    }

    @Override
    public String toString() {
        return wrapped.toString();
    }
}


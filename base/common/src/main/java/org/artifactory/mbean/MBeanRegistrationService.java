/*
 *
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2016 JFrog Ltd.
 *
 * Artifactory is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 * Artifactory is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Artifactory.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.artifactory.mbean;

import com.codahale.metrics.MetricRegistry;
import org.apache.commons.lang.StringUtils;
import org.artifactory.api.context.ArtifactoryContext;
import org.artifactory.api.context.ContextHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.net.Socket;
import java.util.Set;

@Service
public class MBeanRegistrationService {
    private static final Logger log = LoggerFactory.getLogger(MBeanRegistrationService.class);

    private static final String MBEANS_DOMAIN_NAME = "org.jfrog.artifactory";
    private static final String MANAGED_PREFIX = "Managed";
    private static final String MBEAN_SUFFIX = "MBean";

    @Autowired
    private MetricRegistry metricRegistry;

    /**
     * Registers an object as a mbean.
     * <p><i>It is assumed that the given {@code mbean} has an interface with name ends with MBean</i></p>
     *
     * @param mbean      The mbean implementation
     */
    public <T> void register(T mbean) {
        register(mbean, createObjectName(getMBeanInterface(mbean), null));
    }

    /**
     * Registers an object as an mbean.
     * <p><i>It is assumed that the given {@code mbean} has an interface with name ends with MBean</i></p>
     *
     * @param mbean      The mbean implementation
     * @param mbeanProps Optional string to attach to the mbean name
     */
    public <T> void register(T mbean, @Nullable String mbeanProps) {
        register(mbean, createObjectName(getMBeanInterface(mbean), mbeanProps));
    }

    /**
     * Registers an object as an mbean.
     *
     * @param mbean      The mbean implementation
     * @param mbeanIfc   The mbean interface
     * @param mbeanProps Optional string to attach to the mbean name
     */
    public <T> void register(T mbean, Class<T> mbeanIfc, @Nullable String mbeanProps) {
        register(mbean, createObjectName(mbeanIfc, mbeanProps));
    }

    public <T> void register(T mbean, String group, @Nullable String prop) {
        register(mbean, createObjectName(group, prop));
    }

    /**
     * IT IS HIGHLY DISCOURAGED TO USE THIS METHOD DIRECTLY, USE THE 'register'/'unregister' INSTANCE METHODS INSTEAD!
     * @param mbean
     * @param mbeanName
     * @param <T>
     */
    public static <T> void register(T mbean, ObjectName mbeanName) {
        try {
            if (getMBeanServer().isRegistered(mbeanName)) {
                log.debug("Un-registering existing mbean '{}'.", mbeanName);
                unregister(mbeanName);
            }
            log.debug("Registering mbean '{}'.", mbeanName);
            getMBeanServer().registerMBean(mbean, mbeanName);
        } catch (Exception e) {
            String warn = "Could not register mbean '" + mbeanName.getCanonicalName();
            log.warn(warn + "'-> {}: {}", e.getClass().getCanonicalName(), e.getMessage());
            log.debug(warn, e);
        }
    }

    /**
     * IT IS HIGHLY DISCOURAGED TO USE THIS METHOD DIRECTLY, USE THE 'register'/'unregister' INSTANCE METHODS INSTEAD!
     * @param mbeanName
     */
    public static void unregister(ObjectName mbeanName) {
        try {
            getMBeanServer().unregisterMBean(mbeanName);
        } catch (Exception e) {
            String warn = "Could not unregister mbean '" + mbeanName.getCanonicalName();
            log.warn(warn + "'-> {}: {}", e.getClass().getCanonicalName(), e.getMessage());
            log.debug(warn, e);
        }
    }

    /**
     * Unregisters all MBeans where the type matches given {@code type}.
     * <p>Object name is created using {@link #createObjectName(String, String)}
     * <p><i>In case of exception, a warn log will be written </i>
     * @return  the count of unregistered MBeans
     * @param type
     */
    public int unregisterAll(String type) {
        int count = 0;
        String canonicalName = createObjectName(type, null).getCanonicalName();

        Set<ObjectInstance> objectInstances = getMBeanServer().queryMBeans(null, null);
        for (ObjectInstance objectInstance : objectInstances) {
            ObjectName objectName = objectInstance.getObjectName();
            if (objectName.getCanonicalName().startsWith(canonicalName)) {
                unregister(objectName);
                count++;
            }
        }
        return count;
    }

    /**
     * Reading state of MBean server, tries to shutdown the server as clean as possible
     */
    public void shutdownServer() {
        MBeanServer server = getMBeanServer();
        ObjectName name = null;
        try {
            name = new ObjectName("Catalina:type=Server");
            if (!server.isRegistered(name)) {
                // Try the embedded version
                name = new ObjectName("Tomcat:type=Server");
            }
        } catch (MalformedObjectNameException e) {
            log.error("Could not create tomcat mbean names", e);
        }
        if (name != null && server.isRegistered(name)) {
            try {
                log.info("Shutting down Tomcat server...");
                int port = (int) server.getAttribute(name, "port");
                if (port == -1) {
                    log.info("No Tomcat management port defined. Shutting down manually...");
                    new Thread(new ShutdownThread()).start();
                } else {
                    String address = (String) server.getAttribute(name, "address");
                    String shutdownPassword = (String) server.getAttribute(name, "shutdown");
                    log.info("Tomcat shutdown using " + address + ":" + port);
                    Socket s = new Socket(address, port);
                    PrintStream ps = new PrintStream(s.getOutputStream());
                    ps.println(shutdownPassword);
                    s.close();
                }
            } catch (Exception e) {
                log.error("Failed to call tomcat shutdown!", e);
            }
        } else {
            log.info("Shutting down Jetty server...");
            new Thread(new ShutdownThread()).start();
        }
    }

    /**
     * @return A central, reusable metric registry
     */
    public MetricRegistry getMetricRegistry() {
        return metricRegistry;
    }

    static class ShutdownThread implements Runnable {
        private final ArtifactoryContext ctx;

        ShutdownThread() {
            this.ctx = ContextHelper.get();
        }

        @Override
        public void run() {
            try {
                Thread.sleep(100L);
                ctx.destroy();
                System.exit(0);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * <p>IT IS HIGHLY DISCOURAGED TO USE THIS METHOD DIRECTLY, USE THE 'register'/'unregister' INSTANCE METHODS INSTEAD!</p>
     * Create MBean name for the given {@code mbeanIfc} according to the convention {@code prefix:instance:type:props}
     * <p>for example: {@code org.jfrog.artifactory:Artifactory:Repository:libs-releases-locel}
     * @param mbeanIfc   The mbean interface
     * @param mbeanProps Optional string to attach to the mbean name
     * @return  the MBean name for the given {@code mbeanIfc}
     */
    public static ObjectName createObjectName(Class mbeanIfc, @Nullable String mbeanProps) {
        String type = mbeanIfc.getSimpleName();
        if (type.startsWith(MANAGED_PREFIX)) {
            type = type.substring(MANAGED_PREFIX.length());
        }
        if (type.endsWith(MBEAN_SUFFIX)) {
            type = type.substring(0, type.length() - MBEAN_SUFFIX.length());
        }

        return createObjectName(type, mbeanProps);
    }

    /**
     * IT IS HIGHLY DISCOURAGED TO USE THIS METHOD DIRECTLY, USE THE 'register'/'unregister' INSTANCE METHODS INSTEAD!
     * @param type
     * @param mbeanProps
     * @return
     */
    public static ObjectName createObjectName(String type, String mbeanProps) {
        ArtifactoryContext context = ContextHelper.get();
        String instanceId = context == null ? null : context.getContextId();
        if (StringUtils.isBlank(instanceId)) {
            instanceId = "Artifactory"; //default instanceId for tomcat ROOT
        } else {
            instanceId = StringUtils.capitalize(instanceId);
        }
        String nameStr = MBEANS_DOMAIN_NAME + ":" + "instance=" + instanceId + ", type=" + type;
        if (StringUtils.isNotBlank(mbeanProps)) {
            nameStr += ",prop=" + mbeanProps;
        }

        try {
            return new ObjectName(nameStr);
        } catch (MalformedObjectNameException e) {
            throw new IllegalArgumentException("Failed to create object name for '" + nameStr + "'.", e);
        }
    }

    protected static MBeanServer getMBeanServer() {
        //Delegate to the mbean server already created by the platform
        return ManagementFactory.getPlatformMBeanServer();
    }

    private static <T> Class<T> getMBeanInterface(T mbean) {
        for (Class<?> ifc : mbean.getClass().getInterfaces()) {
            if (ifc.getName().endsWith(MBEAN_SUFFIX)) {
                return (Class<T>) ifc;
            }
        }
        return null;
    }
}


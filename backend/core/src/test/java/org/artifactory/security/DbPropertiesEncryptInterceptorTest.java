package org.artifactory.security;

import org.apache.commons.io.FileUtils;
import org.artifactory.api.context.ArtifactoryContext;
import org.artifactory.api.context.ArtifactoryContextThreadBinder;
import org.artifactory.common.ArtifactoryConfigurationAdapter;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.common.ConstantValues;
import org.artifactory.common.config.db.ArtifactoryDbProperties;
import org.artifactory.common.crypto.CryptoHelper;
import org.artifactory.security.interceptor.StoragePropertiesEncryptInterceptor;
import org.jfrog.common.ResourceUtils;
import org.jfrog.config.ConfigurationManager;
import org.jfrog.config.wrappers.ConfigurationManagerImpl;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Files;

/**
 * @author gidis
 */
@Test
public class DbPropertiesEncryptInterceptorTest {
    public static <T> T proxy(Class<T> interfaceClass, InvocationHandler handler) {
        Class[] interfaces = {interfaceClass};
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        return (T) Proxy.newProxyInstance(classLoader, interfaces, handler);
    }

    /**
     * Emulates Artifactory environment
     */
    @BeforeClass
    public void init() throws IOException {
        //TODO: [by YS] We need to add functionality to the ArtifactoryHomeBoundTest to support storage.properties
        // Create Artifactory home and bind it to the thread
        File home = new File("target/test/StoragePropertiesEncryptInterceptorTest");
        ArtifactoryHome artifactoryHome = new ArtifactoryHome(home);
        File masterKeyFile = artifactoryHome.getMasterKeyFile();
        Files.write(masterKeyFile.toPath(), "0c1a1554553d487466687b339cd85f3d".getBytes());
        ConfigurationManager configurationManager = ConfigurationManagerImpl.create(new ArtifactoryConfigurationAdapter(artifactoryHome));
        configurationManager.initDbProperties();
        configurationManager.initDefaultFiles();
        artifactoryHome.initPropertiesAndReload();
        ArtifactoryHome.bind(artifactoryHome);
        // Create mock ArtifactoryContext using Proxy and invocation handler
        TestInvocationHandler handler = new TestInvocationHandler();
        ArtifactoryContext context = proxy(ArtifactoryContext.class, handler);
        ArtifactoryContextThreadBinder.bind(context);
        FileUtils.copyFile(
                ResourceUtils.getResourceAsFile("/org/artifactory/security/db.properties"),
                ArtifactoryHome.get().getDBPropertiesFile());
        String keyFileLocation = ConstantValues.securityArtifactoryKeyLocation.getString();
        File keyFile = new File(ArtifactoryHome.get().getEtcDir(), keyFileLocation);
        //noinspection ResultOfMethodCallIgnored
        keyFile.delete();
        CryptoHelper.createArtifactoryKeyFile(ArtifactoryHome.get());
    }

    @AfterClass
    public void tearDown() {
        ArtifactoryContextThreadBinder.unbind();
    }

    @Test()
    public void encryptTest() {
        StoragePropertiesEncryptInterceptor interceptor = new StoragePropertiesEncryptInterceptor();
        interceptor.encryptOrDecryptStoragePropertiesFile(true);
        ArtifactoryHome home = ArtifactoryHome.get();
        ArtifactoryDbProperties dbProperties = new ArtifactoryDbProperties(home);
        // Check the password
        String password = dbProperties.getProperty("password", null);
        Assert.assertNotEquals(password, "test1");
        password = dbProperties.getPassword();
        Assert.assertEquals(password, "test1");
        // Check the url
        String url = dbProperties.getConnectionUrl();
        Assert.assertEquals(url, "jdbc:derby:" + home.getDataDir().getAbsolutePath() + "/derby;create=true");
    }

    @Test(dependsOnMethods = "encryptTest")
    public void decryptTest() throws IOException {
        StoragePropertiesEncryptInterceptor interceptor = new StoragePropertiesEncryptInterceptor();
        interceptor.encryptOrDecryptStoragePropertiesFile(false);
        ArtifactoryHome home = ArtifactoryHome.get();
        ArtifactoryDbProperties dbProperties = new ArtifactoryDbProperties(home);
        // Check the password
        String password = dbProperties.getProperty("password", null);
        Assert.assertEquals(password, "test1");
        // Check the url
        String url = dbProperties.getConnectionUrl();
        Assert.assertEquals(url, "jdbc:derby:" + home.getDataDir().getAbsolutePath() + "/derby;create=true");
    }

    public class TestInvocationHandler implements InvocationHandler {
        public int count = 0;

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if ("getArtifactoryHome".equals(method.getName())) {
                return ArtifactoryHome.get();
            }
            if ("beanForType".equals(method.getName()) && ((Class) args[0]).getName().equals(
                    ArtifactoryDbProperties.class.getName())) {
                return new ArtifactoryDbProperties(ArtifactoryHome.get());
            }
            throw new IllegalStateException("The state is not expected in this test: " + method.getName());
        }
    }
}

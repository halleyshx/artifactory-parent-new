package org.artifactory.security.access.emigrate;

import com.thoughtworks.xstream.XStream;
import org.apache.commons.io.IOUtils;
import org.artifactory.api.security.SecurityService;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.factory.InfoFactoryHolder;
import org.artifactory.security.MutableUserInfo;
import org.artifactory.security.SaltedPassword;
import org.artifactory.security.SecurityInfo;
import org.artifactory.security.UserInfo;
import org.artifactory.storage.db.security.service.access.emigrate.SecurityEmigratorFetchers;
import org.artifactory.update.security.SecurityVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.List;
import java.util.stream.Collectors;

import static org.artifactory.update.security.v8.Md5TemplateHashPasswordConverter.convertPasswordHash;

/**
 * Move security data to access
 * Code here is specific as we don't want that changes in SecurityService will affect us
 * @author Noam Shemesh
 */
@Component
public class AccessSecurityEmigratorImpl implements AccessSecurityEmigrator {
    private static final Logger log = LoggerFactory.getLogger(AccessSecurityEmigratorImpl.class);

    private SecurityService securityService;
    private final SecurityEmigratorFetchers securityEmigratorFetchers;

    @Autowired
    public AccessSecurityEmigratorImpl(SecurityEmigratorFetchers securityEmigratorFetchers) {
        this.securityEmigratorFetchers = securityEmigratorFetchers;
    }

    @Override
    public void emigrate() {
        log.info("Emigration process to access: Backup current security data to file.");
        SecurityInfo securityData = fetchSecurityDataFromDb();

        File backupFile = new File(ArtifactoryHome.get().getEtcDir(),
                "export.security." + System.currentTimeMillis() + ".xml");

        exportSecurityInfo(securityData, backupFile);
        log.info("Security data fetched successfully from artifactory database, and saved to {}. Starting emigration", backupFile);

        securityService.importSecurityData(securityData);
        log.info("Finished emigrate security data from artifactory to access");
    }

    private void exportSecurityInfo(SecurityInfo descriptor, File file) {
        //Export the security settings as xml using xstream
        XStream xstream = getXstream();
        OutputStream os = null;
        try {
            os = new BufferedOutputStream(new FileOutputStream(file));
            xstream.toXML(descriptor, os);
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Failed to export security configuration.", e);
        } finally {
            IOUtils.closeQuietly(os);
        }
    }

    private static XStream getXstream() {
        return InfoFactoryHolder.get().getSecurityXStream();
    }

    private SecurityInfo fetchSecurityDataFromDb() {
        SecurityInfo descriptor =
                InfoFactoryHolder.get().createSecurityInfo(
                        getMigratedUsers(),
                        securityEmigratorFetchers.getAllGroupInfos(),
                        securityEmigratorFetchers.getAllAclInfos());

        // Locking the security version on 18/09/2017
        descriptor.setVersion(SecurityVersion.v9.name());
        return descriptor;
    }

    private List<UserInfo> getMigratedUsers() {
        return securityEmigratorFetchers.getAllUserInfos()
                .stream()
                .map(user -> {
                    MutableUserInfo mutableUser = InfoFactoryHolder.get().copyUser(user);
                    String hashedTemplate = convertPasswordHash(user.getPassword(), user.getSalt());
                    mutableUser.setPassword(new SaltedPassword(hashedTemplate, null));
                    return mutableUser;
                }).collect(Collectors.toList());
    }

    @Autowired
    public void setSecurityService(SecurityService securityService) {
        this.securityService = securityService;
    }
}

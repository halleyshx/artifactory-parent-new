package org.artifactory.storage.db.security.service.access;

import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.HttpStatus;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.security.PasswordExpiryUser;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.common.crypto.CryptoHelper;
import org.artifactory.config.CentralConfigKey;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.factory.InfoFactoryHolder;
import org.artifactory.md.Properties;
import org.artifactory.model.xstream.fs.PropertiesImpl;
import org.artifactory.model.xstream.security.UserImpl;
import org.artifactory.model.xstream.security.UserProperty;
import org.artifactory.sapi.security.SecurityConstants;
import org.artifactory.security.*;
import org.artifactory.security.access.AccessService;
import org.artifactory.security.props.auth.EncryptedTokenManager;
import org.artifactory.spring.Reloadable;
import org.artifactory.storage.StorageException;
import org.artifactory.storage.security.service.UserGroupStoreService;
import org.artifactory.version.CompoundVersionDetails;
import org.jfrog.access.client.AccessClient;
import org.jfrog.access.client.AccessClientHttpException;
import org.jfrog.access.client.user.FindUsersRequest;
import org.jfrog.access.client.user.UsersClient;
import org.jfrog.access.model.UserStatus;
import org.jfrog.access.rest.group.Group;
import org.jfrog.access.rest.group.Groups;
import org.jfrog.access.rest.group.ManageGroupMembersRequest;
import org.jfrog.access.rest.user.UpdateUserRequest;
import org.jfrog.access.rest.user.User;
import org.jfrog.access.rest.user.UserBase;
import org.jfrog.access.rest.user.UserRequest;
import org.jfrog.access.util.ThrowingFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.artifactory.storage.db.security.service.access.UserMapper.ArtifactoryBuiltInUserProperty.artifactory_admin;

/**
 * Implementation of {@link UserGroupStoreService} that user Access as the storage engine.
 *
 * @author Yossi Shaul
 */
@Service
@Reloadable(beanClass = UserGroupStoreService.class,
        initAfter = AccessService.class,
        listenOn = CentralConfigKey.none)
public class AccessUserGroupStoreService implements UserGroupStoreService {
    private static final Logger log = LoggerFactory.getLogger(AccessUserGroupStoreService.class);

    @Autowired
    private AccessService accessService;

    public enum GroupFilter {
        ADMIN(GroupInfo::isAdminPrivileges),
        DEFAULTS(GroupInfo::isNewUserDefault),
        EXTERNAL(gi -> !SecurityConstants.DEFAULT_REALM.equals(gi.getRealm())),
        INTERNAL(gi -> gi.getRealm() == null || gi.getRealm().equals(SecurityConstants.DEFAULT_REALM));

        final Predicate<GroupInfo> filterFunction;

        GroupFilter(Predicate<GroupInfo> filterFunction) {
            this.filterFunction = filterFunction;
        }
    }

    @Override
    public boolean createUser(UserInfo user) {
        return createUserWithProperties(user, false);
    }

    @Override
    public boolean createUserWithProperties(UserInfo user, boolean addUserProperties) {
        if (userExists(user.getUsername())) {
            return false;
        }
        // create a copy of the user, encrypting all the properties that require encryption
        user = encryptUser(user, true);
        UserRequest accessUser = UserMapper.toAccessUser(user, addUserProperties);
        getClient().users().createUser(accessUser);
        return true;
    }

    @Override
    public void updateUser(MutableUserInfo userInfo, boolean encryptProperties) {
        getClient().users().updateUser(UserMapper.toUpdateUserRequest(
                encryptUser(userInfo, encryptProperties),
                true));
    }

    private UserImpl encryptUser(UserInfo user, boolean encryptProperties) {
        return new UserImpl(user, encryptProperties);
    }

    private UserInfo decryptUser(UserInfo user) {
        Function<String, String> decryptor = v -> CryptoHelper.decryptIfNeeded(ArtifactoryHome.get(), v);

        Set<UserPropertyInfo> userDecryptedProperties = user.getUserProperties().stream()
                .map(p -> new UserProperty(p.getPropKey(), decryptor.apply(p.getPropValue()))).collect(
                        Collectors.toSet());

        MutableUserInfo userInfo = InfoFactoryHolder.get().copyUser(user);
        userInfo.setUserProperties(userDecryptedProperties);
        return userInfo;
    }

    @Override
    public UserInfo findUser(String username) {
        return findUserInternal(username, true)
                .map(UserMapper::toArtifactoryUser)
                .map(this::decryptUser)
                .orElse(null);
    }

    private Optional<? extends UserBase> findUserInternal(String username, boolean withGroups) {
        UsersClient usersClient = getClient().users();
        return withGroups ? usersClient.findUserWithGroupsByUsername(username) : usersClient.findUserByUsername(username);
    }

    @Override
    public void deleteAllGroupsAndUsers() {
        //TODO: [by YS] add api entry point in Access server unless it's only required for the import
        getAllGroups().forEach(g -> getClient().groups().deleteGroup(g.getGroupName()));
        getAllUsers(true, false).forEach(u -> getClient().users().deleteUser(u.getUsername()));
    }

    @Override
    public boolean adminUserExists() {
        return isNotEmpty(getClient().users().findUsersByCustomData(artifactory_admin.name(), "true", true).getUsers());
    }

    @Override
    public boolean userExists(String username) {
        return findUserInternal(username, false).isPresent();
    }

    @Override
    public void deleteUser(String username) {
        try {
            getClient().users().deleteUser(username);
        } catch (AccessClientHttpException e) {
            if (e.getStatusCode() != 404) {
                throw e;
            }
        }
    }

    @Nullable
    @Override
    public UserInfo findUserByProperty(String key, String val, boolean exactKeyMatch) {
        String encryptedVal =
                shouldEncryptProperty(key) ? CryptoHelper.encryptIfNeeded(ArtifactoryHome.get(), val) : val;
        // first lookup by the encrypted value
        Optional<UserInfo> matchingUser = findUserByPropertyInternal(key, encryptedVal, exactKeyMatch);
        if (!matchingUser.isPresent() && !encryptedVal.equals(val)) {
            // search for the unencrypted property value
            matchingUser = findUserByPropertyInternal(key, val, exactKeyMatch);
        }
        return matchingUser.orElse(null);
    }

    private Optional<UserInfo> findUserByPropertyInternal(String key, String value, boolean exactKeyMatch) {
        return getClient().users().findUsersByCustomData(key, value, exactKeyMatch).getUsers().stream()
                .map(UserMapper::toArtifactoryUser)
                .map(this::decryptUser)
                .findFirst();
    }

    @Nullable
    @Override
    public String findUserProperty(String username, String key) {
        UserInfo user = findUser(username);
        if (user == null) {
            log.debug("User {} doesn't exist. Cannot find property", username);
            return null;
        }

        return user.getUserProperty(key)
                .filter(StringUtils::isNotBlank)
                .map(v -> CryptoHelper.decryptIfNeeded(ArtifactoryHome.get(), v))
                .orElse(null);
    }

    @Override
    public boolean addUserProperty(String username, String key, String val) {
        if (!userExists(username)) {
            log.debug("User {} doesn't exist. Cannot add property", username);
            return false;
        }
        UpdateUserRequest updateUserRequest = UpdateUserRequest.create();
        updateUserRequest.username(username).addCustomData(key, val);
        getClient().users().updateUser(updateUserRequest);
        return true;
    }

    @Override
    public boolean deleteUserProperty(String username, String propKey) {
        if (!userExists(username)) {
            log.debug("User {} doesn't exist. Cannot delete property", username);
            return false;
        }

        return deleteUserProperty(findUser(username), propKey);
    }

    private boolean deleteUserProperty(UserInfo user, String propKey) {
        boolean propertyExist = user.getUserProperty(propKey).isPresent();
        if (!propertyExist) {
            log.debug("User {} doesn't have the specified property: {}.", user.getUsername(), propKey);
            return false;
        }

        UpdateUserRequest updateUserRequest = UpdateUserRequest.create();
        updateUserRequest.username(user.getUsername()).addCustomData(propKey, null);
        getClient().users().updateUser(updateUserRequest);
        return true;
    }

    @Override
    public void deletePropertyFromAllUsers(String propertyKey) {
        getClient().users().findUsersByCustomData(propertyKey, true).getUsers().stream()
                .map(UserMapper::toArtifactoryUser)
                .forEach(u -> deleteUserProperty(u, propertyKey));
    }

    @Override
    public Properties findPropertiesForUser(String username) {
        UserInfo user = findUser(username);
        if (user == null) {
            log.debug("User {} not found. Returning empty properties", username);
            return new PropertiesImpl();
        }

        PropertiesImpl properties = new PropertiesImpl();
        for (UserPropertyInfo userProperty : user.getUserProperties()) {
            properties.put(userProperty.getPropKey(),
                    CryptoHelper.decryptIfNeeded(ArtifactoryHome.get(), userProperty.getPropValue()));
        }
        return properties;
    }

    @Override
    public List<UserInfo> getAllUsers(boolean includeAdmins, boolean includePasswords) {
        return getAllUsers(includeAdmins, includePasswords, true);
    }

    private List<UserInfo> getAllUsers(boolean includeAdmins, boolean includePasswords, boolean withGroupAdmins) {
        Stream<UserInfo> userInfoStream = getAllUsersInternal(includePasswords).stream()
                .map(UserMapper::toArtifactoryUser)
                .map(this::decryptUser)
                .filter(userInfo -> includeAdmins || !userInfo.isAdmin());

        if (withGroupAdmins) {
            List<String> groupAdmins = getAllAdminGroupsNames();
            userInfoStream = userInfoStream.map(u -> {
                boolean groupAdmin = u.getGroups().stream()
                        .anyMatch(group -> groupAdmins.contains(group.getGroupName()));
                MutableUserInfo copyUser = InfoFactoryHolder.get().copyUser(u);
                copyUser.setGroupAdmin(groupAdmin);
                return copyUser;
            });
        }

        return userInfoStream.collect(Collectors.toList());
    }

    @Override
    public Map<String, Boolean> getAllUsersAndAdminStatus(boolean justAdmins) {
        return getAllUsers(true, false, true).stream()
                .filter(u -> !justAdmins || u.isEffectiveAdmin())
                .collect(Collectors.toMap(UserInfo::getUsername, UserInfo::isEffectiveAdmin));
    }

    @Override
    public List<String> getAllAdminGroupsNames() {
        return getGroupsByFilter(GroupFilter.ADMIN).stream().map(GroupInfo::getGroupName).collect(Collectors.toList());
    }

    private List<User> getAllUsersInternal(boolean includePasswords) {
        if (!includePasswords) {
            return getClient().users().findUsers().getUsers();
        }

        return getClient().users().findUsers(new FindUsersRequest().expand(UserRequest.Expand.passwords)).getUsers();
    }

    @Override
    public Multimap<String, String> getAllUsersInGroups() {
        ImmutableSetMultimap.Builder<String, String> builder = ImmutableSetMultimap.builder();
        getAllUsers(true, false).forEach(userInfo -> builder.putAll(
                userInfo.getUsername(),
                userInfo.getGroups()
                        .stream()
                        .map(UserGroupInfo::getGroupName)
                        .collect(Collectors.toList()))
        );

        return builder.build();
    }

    @Override
    public void lockUser(@Nonnull String username) {
        handleLockException(ignore ->
                        patchUser(createUpdateUserRequest(username, request -> request.status(UserStatus.LOCKED))), username, "lock");
    }

    @Override
    public void unlockUser(@Nonnull String username) {
        handleLockException(ignore ->
                patchUser(createUpdateUserRequest(username, (request) -> request.status(UserStatus.ENABLED))), username, "unlock");
    }

    private void handleLockException(ThrowingFunction<Void, User, Exception> execute, String username, String operation) {
        try {
            execute.apply(null);
        } catch (AccessClientHttpException e) {
            if (e.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                log.debug("User {} to {} not found. Ignoring", username, operation);
                return;
            }
            throwStorageException(username, operation, e);
        } catch (Exception e) {
            throwStorageException(username, operation, e);
        }
    }

    private void throwStorageException(String username, String operation, Exception e) {
        log.debug("Could not {} user {}, cause: {}", operation, username, e);
        throw new StorageException("Could not lock user " + username + ", reason: " + e.getMessage());
    }

    @Override
    public boolean isUserLocked(String userName) {
        return findUserInternal(userName, false)
                .map(UserBase::getStatus)
                .map(UserStatus.LOCKED::equals)
                .orElse(false);
    }

    private AccessClient getClient() {
        return accessService.getAccessClient();
    }

    @Override
    public void unlockAllUsers() {
        getLockedUsers().forEach(this::unlockUser);
    }

    @Override
    public void unlockAdminUsers() {
        getAllUsersInternal(false)
                .stream()
                .filter(user -> UserStatus.LOCKED.equals(user.getStatus()))
                .map(UserMapper::toArtifactoryUser)
                .filter(UserInfo::isAdmin)
                .map(UserInfo::getUsername)
                .forEach(this::unlockUser);
    }

    @Override
    public Set<String> getLockedUsers() {
        return getAllUsersInternal(false)
                .stream()
                .filter(user -> UserStatus.LOCKED.equals(user.getStatus()))
                .map(User::getUsername)
                .collect(Collectors.toSet());
    }

    private User patchUser(UpdateUserRequest userRequest) {
        return getClient().users().updateUser(userRequest);
    }

    private UpdateUserRequest createUpdateUserRequest(String username,
            Function<UpdateUserRequest, UserRequest> updates) {
        return (UpdateUserRequest) updates.apply((UpdateUserRequest) UpdateUserRequest.create().username(username));
    }

    @Override
    public void changePassword(UserInfo user, SaltedPassword newSaltedPassword, String rawPassword) {
        patchUser(createUpdateUserRequest(user.getUsername(),
                (request) -> request.password(rawPassword)));
    }

    @Override
    public boolean isUserPasswordExpired(String userName, int expiresIn) {
        return Optional.ofNullable(findUser(userName)).map(UserInfo::isCredentialsExpired).orElse(false);
    }

    @Override
    public void expireUserPassword(String userName) {
        patchUser(createUpdateUserRequest(userName,
                (request) -> request.passwordExpired(true)));
    }

    @Override
    public void revalidatePassword(String userName) {
        patchUser(createUpdateUserRequest(userName,
                (request) -> request.passwordExpired(false)));
    }

    @Override
    public void expirePasswordForAllUsers() {
        getAllUsers(true, false).stream().map(UserInfo::getUsername).forEach(this::expireUserPassword);
    }

    @Override
    public void revalidatePasswordForAllUsers() {
        getAllUsers(true, false).stream().map(UserInfo::getUsername).forEach(this::revalidatePassword);
    }

    @Override
    public List<String> markUsersCredentialsExpired(int daysToKeepPassword) {
        // [NS] Irrelevant, access manages expiry of users
        return Collections.emptyList();
    }

    @Override
    public void expirePasswordForUserIds(Set<Long> userIds) throws SQLException {
        // [NS] Irrelevant, access manages different ids
    }

    @Override
    public Long getUserPasswordCreationTime(String userName) {
        return findUserInternal(userName, false).map(UserBase::getPasswordLastModified).orElse(0L);
    }

    @Override
    public void encryptDecryptUserProps(String propKey, boolean encrypt) {
        try {
             getClient().users().findUsersByCustomData(propKey, true).getUsers()
                    .forEach(user -> {
                        String value = user.getCustomData(propKey);
                        String newValue = encrypt ? CryptoHelper.encryptIfNeeded(ArtifactoryHome.get(), value)
                                : CryptoHelper.decryptIfNeeded(ArtifactoryHome.get(), value);
                        if (!newValue.equals(value)) {
                            log.debug("{} user {} property {}",
                                    encrypt ? "Encrypting" : "Decrypting", user.getUsername(), propKey);
                            UpdateUserRequest updateRequest = UpdateUserRequest.create();
                            updateRequest.username(user.getUsername()).addCustomData(propKey, newValue);
                            getClient().users().updateUser(updateRequest);
                        }
                    });
        } catch (Exception e) {
            log.error("Could not encrypt or decrypt user props, cause: {}", e);
            throw new StorageException("Could not encrypt or decrypt user props, see logs for more details");
        }
    }

    @Override
    public Set<PasswordExpiryUser> getUsersWhichPasswordIsAboutToExpire(int daysToNotifyBefore,
            int daysToKeepPassword) {
        return accessService.getAccessClient().users().findUsers(new FindUsersRequest().daysToExpire(daysToNotifyBefore))
                .getUsers().stream()
                .map(user -> Pair.of(user, UserMapper.toArtifactoryUser(user)))
                .filter(users -> SecurityConstants.DEFAULT_REALM.equals(users.getRight().getRealm()))
                .map(users -> new PasswordExpiryUser(
                        users.getLeft().getUsername(), users.getLeft().getEmail(), users.getLeft().getPasswordLastModified()))
                .collect(Collectors.toSet());
    }

    @Nullable
    @Override
    public GroupInfo findGroup(String groupName) {
        Optional<Group> group = getClient().groups().findGroupByName(groupName);
        return group.map(GroupMapper::toArtifactoryGroup).orElse(null);
    }

    @Override
    public boolean createGroup(GroupInfo groupInfo) {
        try {
            getClient().groups().createGroup(GroupMapper.toAccessGroup(groupInfo));
            return true;
        } catch (AccessClientHttpException e) {
            log.debug("Create group failed: {}", e.getStatusCode(), e);
            if (e.getStatusCode() == 404 || e.getStatusCode() == 409) {
                return false;
            }
            throw e;
        }
    }

    @Override
    public void addUsersToGroup(String groupName, List<String> usernames) {
        accessService.getAccessClient().groups().manageGroupUsers(groupName,
                new ManageGroupMembersRequest().addUsers(usernames.toArray(new String[]{})));
    }

    @Override
    public void removeUsersFromGroup(String groupName, List<String> usernames) {
        accessService.getAccessClient().groups().manageGroupUsers(groupName,
                new ManageGroupMembersRequest().removeUsers(usernames.toArray(new String[]{})));

    }

    @Override
    public List<String> findUsersInGroup(String groupName) {
        return accessService.getAccessClient()
                .groups()
                .findGroupUsers(groupName).getUsers()
                .stream()
                .map(User::getUsername)
                .collect(Collectors.toList());
    }

    @Override
    public boolean deleteGroup(String groupName) {
        try {
            getClient().groups().deleteGroup(groupName);
            return true;
        } catch (AccessClientHttpException e) {
            log.debug("Delete group failed: {}", e.getStatusCode(), e);
            if (e.getStatusCode() == 404) {
                return false;
            }
            throw e;
        }
    }

    @Override
    public List<GroupInfo> getAllGroups() {
        Groups groups = getClient().groups().findGroups();
        return groups.getGroups().stream().map(GroupMapper::toArtifactoryGroup).collect(Collectors.toList());
    }

    private List<GroupInfo> getGroupsByFilter(GroupFilter filter) {
        return getAllGroups().stream().filter(filter.filterFunction).collect(Collectors.toList());
    }

    @Override
    public List<GroupInfo> getNewUserDefaultGroups() {
        return getGroupsByFilter(GroupFilter.DEFAULTS);
    }

    @Override
    public List<GroupInfo> getAllExternalGroups() {
        return getGroupsByFilter(GroupFilter.EXTERNAL);
    }

    @Override
    public List<GroupInfo> getInternalGroups() {
        return getGroupsByFilter(GroupFilter.INTERNAL);
    }

    @Override
    public Set<String> getNewUserDefaultGroupsNames() {
        return getNewUserDefaultGroups().stream().map(GroupInfo::getGroupName).collect(Collectors.toSet());
    }

    @Override
    public void updateGroup(MutableGroupInfo groupInfo) {
        try {
            getClient().groups().updateGroup(GroupMapper.toUpdateAccessGroup(groupInfo));
        } catch (AccessClientHttpException e) {
            if (e.getStatusCode() != 404) {
                log.debug("Delete group failed: {}", e.getStatusCode(), e);
            }
        }
    }

    @Override
    public void init() {
        if (accessService.getAccessClient() == null) {
            throw new IllegalStateException("Access client cannot be null at this point");
        }
    }

    @Override
    public void reload(CentralConfigDescriptor oldDescriptor) {
    }

    @Override
    public void destroy() {
        // nop
    }

    @Override
    public void convert(CompoundVersionDetails source, CompoundVersionDetails target) {
        // nop
    }

    private boolean shouldEncryptProperty(String key) {
        Map<String, EncryptedTokenManager> beans = ContextHelper.get().beansForType(EncryptedTokenManager.class);
        return beans != null &&
                beans.values().stream()
                        .anyMatch(manager -> shouldEncryptProperty(manager, key));
    }

    private boolean shouldEncryptProperty(EncryptedTokenManager manager, String key) {
        return manager.getPropKeys().stream().anyMatch(propKey -> StringUtils.equals(propKey, key));
    }
}

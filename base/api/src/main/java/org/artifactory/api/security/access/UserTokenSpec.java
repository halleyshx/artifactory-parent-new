package org.artifactory.api.security.access;

import org.jfrog.access.common.ServiceId;
import org.jfrog.access.common.SubjectFQN;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

import static org.apache.commons.lang.StringUtils.isNotBlank;

/**
 * Created by Yinon Avraham.
 */
public class UserTokenSpec extends TokenSpec<UserTokenSpec> {

    private static final int MAX_USERNAME_LENGTH = 58;

    private final String username;

    private UserTokenSpec(@Nullable String username) {
        this.username = username;//TODO [YA] requireNonBlank(username, "username is required"); currently can't require because the spec is used also for refresh where the username is not required
    }

    /**
     * Create a new user token specification.
     * @param username the username
     * @return a new empty user token specification
     */
    @Nonnull
    public static UserTokenSpec create(@Nullable String username) {
        return new UserTokenSpec(username);
    }

    /**
     * Get the username the token is for
     */
    @Nullable
    public String getUsername() {
        return username;
    }

    @Override
    public SubjectFQN createSubject(ServiceId serviceId) {
        validateUsername();
        return new SubjectFQN(serviceId, SubjectFQN.USERS_NAME_PART, username);
    }

    private void validateUsername() {
        requireNonBlank(username, "username is required");
        if (username.length() > MAX_USERNAME_LENGTH) {
            throw new IllegalArgumentException(
                    "username length exceeds maximum length of " + MAX_USERNAME_LENGTH + " characters");
        }
    }

    public static boolean isUserToken(TokenInfo tokenInfo) {
        return isUserTokenSubject(tokenInfo.getSubject());
    }

    // TODO: [NS] Move uses of these methods to not static after figuring out where to implement them
    public static boolean isUserTokenSubject(String subject) {
        return SubjectFQN.isUserTokenSubject(subject);
    }

    @Nonnull
    public static String extractUsername(String subject) {
        return SubjectFQN.extractUsername(subject);
    }
}

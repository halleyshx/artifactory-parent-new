package org.artifactory.rest.resource.security;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.rest.common.dataholder.UserWithGroupsWrapper;
import org.artifactory.security.HttpAuthenticationDetails;
import org.artifactory.security.SimpleUser;
import org.artifactory.security.UserGroupInfo;
import org.artifactory.security.UserInfo;
import org.artifactory.security.access.AccessService;
import org.artifactory.security.props.auth.EncryptedTokenManager;
import org.jfrog.access.client.token.TokenVerifyResult;
import org.jfrog.access.rest.group.GroupResponse;
import org.jfrog.access.rest.user.UserWithGroups;
import org.jfrog.access.rest.user.UserWithGroupsResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.*;

/**
 * Hanlde request login by external service.
 * Supports basic auth and
 *
 * @author Tamir Hadad
 */
public class AuthDelegationHandler {

    private static final String SENSITIVE_DATA = "sensitive_data";
    private static final String ERROR_GET_USER_DETAILS = "{\"error\" : \"Couldn't get user details\"}";
    private HttpServletRequest request;
    private ObjectMapper mapper = createMapper();
    private static final Logger log = LoggerFactory.getLogger(AuthDelegationHandler.class);

    //TODO [by dan]: need to extract JsonUtil from AccessClient to jf-commons - everybody should use that!
    private static ObjectMapper createMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return objectMapper;
    }

    public AuthDelegationHandler(HttpServletRequest request) {
        this.request = request;
    }

    public Response handleRequest(UserAuthDetails userAuthDetails) {
        AccessService accessService = ContextHelper.get().beanForType(AccessService.class);
        String token = userAuthDetails.getToken();
        if (StringUtils.isEmpty(token)) {
            return handleBasicAuth(userAuthDetails);
        } else {
            return handleToken(token, accessService);
        }
    }

    private Response handleToken(String token, AccessService accessService) {
        TokenVerifyResult verify = accessService.getAccessClient().token().verify(token);
        if (verify.isSuccessful()) {
            UserWithGroups tokenUserWithGroupData = null;
            String userWithGroupsWrapper = accessService.parseToken(token).getExtension();
            if (StringUtils.isNotEmpty(userWithGroupsWrapper)) {
                try {
                    tokenUserWithGroupData = mapper.readValue(userWithGroupsWrapper, UserWithGroupsWrapper.class)
                            .getUsr();
                    return Response.ok(getUserWithGroupsJson(tokenUserWithGroupData)).build();
                } catch (IOException e) {
                    log.error("Failed to read user data from token. ", e.getMessage());
                    log.debug("Failed to read user data from token. ", e);
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
                }
            }
        } else {
            String erroMsg = "{\"error\" : \"Verify reason:, " + verify.getReason() + "\"}";
            log.error("Token verification faild due to:" + verify.getReason());
            return Response.status(Response.Status.FORBIDDEN).entity(erroMsg).build();
        }
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ERROR_GET_USER_DETAILS).build();
    }

    private Response handleBasicAuth(UserAuthDetails userAuthDetails) {
        Authentication authentication;
        // Basic Authentication
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(userAuthDetails.getUsername().toLowerCase(),
                        userAuthDetails.getPassword());
        // Throws exeption in case authentication fails
        authentication = authenticateCredentials(authenticationToken);
        if (authentication != null) {
            UserWithGroups userWithGroups;
            Object principal = authentication.getPrincipal();
            if (principal instanceof SimpleUser) {
                userWithGroups = getUserWithGroupsResponseFromPrincipal((SimpleUser) principal);
                try {
                    return Response.ok(getUserWithGroupsJson(userWithGroups)).build();
                } catch (IOException e) {
                    log.error("Failed to send response with user and groups: {}", e.getMessage());
                    log.debug("Failed to send response with user and groups.", e);
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ERROR_GET_USER_DETAILS)
                            .build();
                }
            } else {
                log.debug("Authentication succeeded, but got unexpected user type: '{}'", principal.getClass());
                log.error("Unexpected user details retrieved for user {}", userAuthDetails.getUsername());
            }
        }
        // Will never get to this line, in case the authencation fails a error is thrown.
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ERROR_GET_USER_DETAILS).build();
    }

    private UserWithGroups getUserWithGroupsResponseFromPrincipal(SimpleUser principal) {
        UserInfo userInfo = principal.getDescriptor();
        UserWithGroupsResponse userWithGroupsResponse = new UserWithGroupsResponse(userInfo.getUsername())
                .email(userInfo.getEmail());
        List<GroupResponse> userGroupsMap = Lists.newArrayList();
        for (UserGroupInfo groupInfo : userInfo.getGroups()) {
            userGroupsMap.add(new GroupResponse().name(groupInfo.getGroupName()).customData(new HashMap<>())
                    .realmAttributes(groupInfo.getRealm()));
        }
        userWithGroupsResponse
                .addCustomData("artifactory_admin", String.valueOf(userInfo.isAdmin()))
                .groups(userGroupsMap);
        return userWithGroupsResponse;
    }

    private String getUserWithGroupsJson(UserWithGroups userWithGroups) throws JsonProcessingException {
        String jsonInString;
        mapper.addMixIn(userWithGroups.getClass(), UserGroupMixIn.class);
        FilterProvider filters = new SimpleFilterProvider()
                .addFilter(SENSITIVE_DATA, SimpleBeanPropertyFilter.serializeAllExcept(getFiledToFilter()));
        jsonInString = mapper.writer(filters).writeValueAsString(userWithGroups);
        return jsonInString;
    }

    private String[] getFiledToFilter() {
        List<String> blackList = getBlackListedFields();
        String[] stockArr = new String[blackList.size()];
        return blackList.toArray(stockArr);
    }

    private List<String> getBlackListedFields() {
        final String PRIVATE_KEY = "private_key";
        final String PUBLIC_KEY = "public_key";

        List<String> blackList = new ArrayList<>(Arrays.asList(PRIVATE_KEY, PUBLIC_KEY));
        Map<String, EncryptedTokenManager> beans = ContextHelper.get().beansForType(EncryptedTokenManager.class);
        if (beans != null) {
            beans.values().forEach(manager -> blackList.addAll(manager.getPropKeys()));
        }
        return blackList;
    }

    /**
     * authenticate credential against Security providers (Artifactory,Ldap , crown and etc)
     *
     * @param authenticationToken - user credentials
     * @return Authentication Data
     */
    private Authentication authenticateCredentials(UsernamePasswordAuthenticationToken authenticationToken) {
        AuthenticationManager authenticationManager = ContextHelper.get().beanForType(AuthenticationManager.class);
        HttpAuthenticationDetails details = new HttpAuthenticationDetails(request);
        authenticationToken.setDetails(details);
        return authenticationManager.authenticate(authenticationToken);
    }

    @JsonFilter(SENSITIVE_DATA)
    public abstract class UserGroupMixIn {
        @JsonFilter(SENSITIVE_DATA)
        abstract Map<String, String> getCustomData();
    }
}
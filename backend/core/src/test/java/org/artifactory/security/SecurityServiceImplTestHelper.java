package org.artifactory.security;

import com.beust.jcommander.internal.Maps;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.artifactory.factory.InfoFactory;
import org.artifactory.factory.InfoFactoryHolder;
import org.artifactory.storage.security.service.AclCache;

import java.util.*;

/**
 * @author nadavy
 */
class SecurityServiceImplTestHelper {

    private Map<String, Map<String, Set<AclInfo>>> userToRepoToAclMap = Maps.newHashMap();
    private Map<String, Map<String, Set<AclInfo>>> groupToRepoToAclMap = Maps.newHashMap();
    private InfoFactory factory = InfoFactoryHolder.get();
    private List<AclInfo> aclInfos = Lists.newArrayList();

    String USER_AND_GROUP_SHARED_NAME = "usergroup";

    List<AclInfo> createTestAcls() {
        return aclInfos;
    }

    AclCache createUserAndGroupResultMap() {

        // Permission Target 1
        MutableAceInfo adminAce = factory.createAce("yossis", false, ArtifactoryPermission.MANAGE.getMask());
        adminAce.setDeploy(true);
        adminAce.setRead(true);
        MutableAceInfo readerAce = factory.createAce("user", false, ArtifactoryPermission.READ.getMask());
        MutableAceInfo deleteAce = factory.createAce("shay", false, ArtifactoryPermission.DELETE.getMask());
        deleteAce.setDeploy(true);
        deleteAce.setAnnotate(true);
        deleteAce.setRead(true);
        MutableAceInfo userGroupAce =
                factory.createAce(USER_AND_GROUP_SHARED_NAME, false, ArtifactoryPermission.READ.getMask());
        MutableAceInfo deployerGroupAce =
                factory.createAce("deployGroup", true, ArtifactoryPermission.DEPLOY.getMask());

        List<String> repoPaths = Lists.newArrayList("testRepo1", "testRemote-cache");
        List<MutableAceInfo> aces = Lists.newArrayList(adminAce, readerAce, deleteAce, userGroupAce, deployerGroupAce);
        addAcesWithPathsToAclCache(aces, repoPaths);

        // Permission Target 2
        MutableAceInfo target2GroupAce = factory.createAce(USER_AND_GROUP_SHARED_NAME, true,
                ArtifactoryPermission.READ.getMask());
        addAceWithPathToAclCache(target2GroupAce, "testRepo2");

        // acl for any repository with read permissions to group
        MutableAceInfo readerGroupAce =
                factory.createAce("anyRepoReadersGroup", true, ArtifactoryPermission.READ.getMask());
        addAceWithPathToAclCache(readerGroupAce, PermissionTargetInfo.ANY_REPO);

        // acl with multiple repo keys with read permissions to group and anonymous
        MutableAceInfo multiReaderGroupAce =
                factory.createAce("multiRepoReadersGroup", true, ArtifactoryPermission.READ.getMask());
        MutableAceInfo multiReaderAnonAce =
                factory.createAce(UserInfo.ANONYMOUS, false, ArtifactoryPermission.READ.getMask());
        List<String> repoKeys = Lists.newArrayList("multi1", "multi2");
        List<MutableAceInfo> multiAces = Lists.newArrayList(multiReaderAnonAce, multiReaderGroupAce);
        addAcesWithPathsToAclCache(multiAces, repoKeys);

        // acl for any repository with specific path delete permissions to user
        MutablePermissionTargetInfo anyRepoSpecificPathTarget = InfoFactoryHolder.get().createPermissionTarget(
                "anyRepoSpecificPathTarget",
                Collections.singletonList("specific-repo"));
        anyRepoSpecificPathTarget.setIncludes(Collections.singletonList("com/acme/**"));
        addAceWithPathToAclCache(deleteAce, "specific-repo", anyRepoSpecificPathTarget);
        addAceWithPathToAclCache(deleteAce, "specific-repo", anyRepoSpecificPathTarget);

        MutableAceInfo anyLocalAce =
                factory.createAce("anyLocalUser", false, ArtifactoryPermission.DEPLOY.getMask());
        addAceWithPathToAclCache(anyLocalAce, PermissionTargetInfo.ANY_LOCAL_REPO);

        MutableAceInfo anyRemoteAce =
                factory.createAce("anyRemoteUser", false, ArtifactoryPermission.READ.getMask());
        addAceWithPathToAclCache(anyRemoteAce, PermissionTargetInfo.ANY_REMOTE_REPO);

        // create the AclCache
        return new AclCache(groupToRepoToAclMap, userToRepoToAclMap);
    }

    private void addAceWithPathToAclCache(MutableAceInfo aceInfo, String repoPath,
            MutablePermissionTargetInfo pmi) {
        Set<AceInfo> targetAces = new HashSet<>(Collections.singletonList(aceInfo));
        AclInfo aclInfo = factory.createAcl(pmi, targetAces, "me");
        aclInfos.add(aclInfo);
        Set<AclInfo> aclInfoSet = Sets.newHashSet(aclInfo);
        Map<String, Map<String, Set<AclInfo>>> resultMap;
        if (aceInfo.isGroup()) {
            resultMap = groupToRepoToAclMap;
        } else {
            resultMap = userToRepoToAclMap;
        }

        Map<String, Set<AclInfo>> itemRepoMap = resultMap.get(aceInfo.getPrincipal());
        if (itemRepoMap == null) {
            resultMap.put(aceInfo.getPrincipal(), Maps.newHashMap());
            itemRepoMap = resultMap.get(aceInfo.getPrincipal());
        }
        if (itemRepoMap.containsKey(repoPath)) {
            itemRepoMap.get(repoPath).add(aclInfo);
        } else {
            itemRepoMap.put(repoPath, aclInfoSet);
        }
    }

    private void addAceWithPathToAclCache(MutableAceInfo aceInfo, String repoPath) {
        MutablePermissionTargetInfo pmi = InfoFactoryHolder.get()
                .createPermissionTarget("target_" + aceInfo.getPrincipal(),
                        Collections.singletonList(repoPath));
        addAceWithPathToAclCache(aceInfo, repoPath, pmi);
    }

    private void addAcesWithPathToAclCache(List<MutableAceInfo> aceInfo, String repoPath) {
        aceInfo.forEach(ace -> addAceWithPathToAclCache(ace, repoPath));
    }

    private void addAcesWithPathsToAclCache(List<MutableAceInfo> aceInfo, List<String> repoPaths) {
        repoPaths.forEach(repoPath -> addAcesWithPathToAclCache(aceInfo, repoPath));
    }
}

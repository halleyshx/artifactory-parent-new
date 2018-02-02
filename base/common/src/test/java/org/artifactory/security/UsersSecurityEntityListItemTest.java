package org.artifactory.security;

import org.testng.annotations.Test;

import static org.testng.Assert.assertNotEquals;

public class UsersSecurityEntityListItemTest {
    @Test
    public void testEqualHashCode() throws Exception {
        UsersSecurityEntityListItem usersSecurityEntityListItem1 = new UsersSecurityEntityListItem("user1",
                "user/user1",
                "internal");
        UsersSecurityEntityListItem usersSecurityEntityListItem2 = new UsersSecurityEntityListItem("user2",
                "user/user2",
                "internal");
        assertNotEquals(usersSecurityEntityListItem1, usersSecurityEntityListItem2);
    }
}
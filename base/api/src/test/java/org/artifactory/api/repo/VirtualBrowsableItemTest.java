package org.artifactory.api.repo;

import org.artifactory.repo.RepoPath;
import org.testng.annotations.Test;

import java.util.Collections;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertTrue;

@Test
public class VirtualBrowsableItemTest {

    public void testTransitivityFolders() {
        RepoPath repoPath = mock(RepoPath.class);
        when(repoPath.getPath()).thenReturn("a");
        VirtualBrowsableItem a = new VirtualBrowsableItem("..", true, 0L, 0L, 0L, repoPath, Collections.emptyList());
        VirtualBrowsableItem b = new VirtualBrowsableItem("..", true, 0L, 0L, 0L, repoPath, Collections.emptyList());
        assertTrue(a.compareTo(b) == 0);


        a = new VirtualBrowsableItem("a", true, 0L, 0L, 0L, repoPath, Collections.emptyList());
        b = new VirtualBrowsableItem("a", false, 0L, 0L, 0L, repoPath, Collections.emptyList());
        assertTrue(a.compareTo(b) < 0);
    }
}


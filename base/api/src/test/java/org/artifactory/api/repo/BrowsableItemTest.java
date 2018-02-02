package org.artifactory.api.repo;

import org.artifactory.repo.RepoPath;
import org.testng.annotations.Test;

import java.util.Collections;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.*;

@Test
public class BrowsableItemTest {

    public void testTransitivityFolders() {
        RepoPath repoPath = mock(RepoPath.class);
        when(repoPath.getPath()).thenReturn("a");

        BrowsableItem a = new BrowsableItem("..", true, 0L, 0L, 0L, repoPath);
        BrowsableItem b = new BrowsableItem("..", true, 0L, 0L, 0L, repoPath);
        assertTrue(a.compareTo(b) == 0);
        assertTrue(b.compareTo(a) == 0);

        a = new BrowsableItem("a", true, 0L, 0L, 0L, repoPath);
        b = new BrowsableItem("b", true, 0L, 0L, 0L, repoPath);
        assertTrue(a.compareTo(b) < 0);
        assertTrue(b.compareTo(a) > 0);

        a = new BrowsableItem("a", true, 0L, 0L, 0L, repoPath);
        b = new BrowsableItem("b", false, 0L, 0L, 0L, repoPath);
        assertTrue(a.compareTo(b) < 0);
        assertTrue(b.compareTo(a) > 0);


        a = new BrowsableItem("a", true, 0L, 0L, 0L, repoPath);
        b = new BrowsableItem("b", true, 0L, 0L, 0L, repoPath);
        assertTrue(a.compareTo(b) < 0);
        assertTrue(b.compareTo(a) > 0);

        a = new BrowsableItem("a", true, 0L, 0L, 0L, repoPath);
        b = new BrowsableItem("a", true, 0L, 0L, 0L, repoPath);
        assertTrue(a.compareTo(b) == 0);
        assertTrue(b.compareTo(a) == 0);


        a = new BrowsableItem("a", false, 0L, 0L, 0L, repoPath);
        b = new BrowsableItem("a", false, 0L, 0L, 0L, repoPath);
        assertTrue(a.compareTo(b) == 0);
        assertTrue(b.compareTo(a) == 0);


        a = new BrowsableItem("..", false, 0L, 0L, 0L, repoPath);
        b = new BrowsableItem("a", false, 0L, 0L, 0L, repoPath);
        assertTrue(a.compareTo(b) < 0);
        assertTrue(b.compareTo(a) > 0);

        a = new BrowsableItem("..", true, 0L, 0L, 0L, repoPath);
        b = new BrowsableItem("a", false, 0L, 0L, 0L, repoPath);
        assertTrue(a.compareTo(b) < 0);
        assertTrue(b.compareTo(a) > 0);

        a = new BrowsableItem("a", true, 0L, 0L, 0L, repoPath);
        b = new BrowsableItem("a", false, 0L, 0L, 0L, repoPath);
        assertTrue(a.compareTo(b) < 0);
        assertTrue(b.compareTo(a) > 0);

    }

}
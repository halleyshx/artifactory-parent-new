package org.artifactory.api.fs;

import org.artifactory.model.common.RepoPathImpl;
import org.artifactory.model.xstream.fs.FolderInfoImpl;
import org.artifactory.test.ArtifactoryHomeBoundTest;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * Unit tests for the {@link FolderInfoImpl}.
 *
 * @author Yossi Shaul
 */
@Test
public class FolderInfoImplTest extends ArtifactoryHomeBoundTest {

    public void folderInfoNoId() {
        FolderInfoImpl f = new FolderInfoImpl(new RepoPathImpl("repo", "path"));
        assertEquals(f.getId(), -1);
    }

    public void folderInfoWithId() {
        FolderInfoImpl f = new FolderInfoImpl(new RepoPathImpl("repo", "path"), 888);
        assertEquals(f.getId(), 888);
    }

    public void folderInfoCopyWithId() {
        FolderInfoImpl f1 = new FolderInfoImpl(new RepoPathImpl("repo", "path"), 999);
        FolderInfoImpl f2 = new FolderInfoImpl(f1);
        assertEquals(f2.getId(), f1.getId());
    }
}

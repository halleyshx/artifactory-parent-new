package org.artifactory.ui.rest.model.artifacts.browse.treebrowser.nodes.actions;

import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.descriptor.download.FolderDownloadConfigDescriptor;
import org.artifactory.repo.RepoPath;
import org.easymock.EasyMock;
import org.easymock.EasyMockRunner;
import org.easymock.Mock;
import org.easymock.TestSubject;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.easymock.EasyMock.replay;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;

@RunWith(EasyMockRunner.class)
public class TreeNodeActionsPopulatorTest {
    @TestSubject
    private TreeNodeActionsPopulator populator = new TreeNodeActionsPopulator();

    @Mock
    private AuthorizationService authService;

    @Mock
    private CentralConfigService centralConfig;

    @Test
    public void testSuccessfulDownloadFolderForAnonymous() throws Exception {
        enableFolderDownload(true);
        makeTheUserAnonymousWithReadPermissions();

        TabOrAction action = populator.downloadFolder(null);

        assertNotNull(action);
    }

    @Test
    public void testDownloadFolderNotEnabledForAnonymous() throws Exception {
        enableFolderDownload(false);
        makeTheUserAnonymousWithReadPermissions();

        TabOrAction action = populator.downloadFolder(null);

        assertNull(action);
    }

    private void enableFolderDownload(boolean forAnonymous) {
        FolderDownloadConfigDescriptor folderDescriptor = new FolderDownloadConfigDescriptor();
        folderDescriptor.setEnabled(true);
        folderDescriptor.setEnabledForAnonymous(forAnonymous);
        CentralConfigDescriptor descriptor = EasyMock.createMock(CentralConfigDescriptor.class);
        EasyMock.expect(descriptor.getFolderDownloadConfig()).andReturn(folderDescriptor).anyTimes();
        replay(descriptor);
        EasyMock.expect(centralConfig.getDescriptor()).andReturn(descriptor).anyTimes();
        replay(centralConfig);
    }

    private void makeTheUserAnonymousWithReadPermissions() {
        EasyMock.expect(authService.canRead(EasyMock.anyObject(RepoPath.class))).andReturn(true).anyTimes();
        EasyMock.expect(authService.isAnonymous()).andReturn(true).anyTimes();
        replay(authService);
    }
}
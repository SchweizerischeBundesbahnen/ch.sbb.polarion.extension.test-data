package ch.sbb.polarion.extension.test_data.service;

import ch.sbb.polarion.extension.generic.service.PolarionService;
import com.polarion.alm.projects.model.IProject;
import com.polarion.alm.shared.api.model.wiki.WikiPage;
import com.polarion.alm.shared.api.model.wiki.WikiPageReference;
import com.polarion.alm.shared.api.transaction.RunnableInWriteTransaction;
import com.polarion.alm.shared.api.transaction.TransactionalExecutor;
import com.polarion.alm.shared.api.transaction.WriteTransaction;
import com.polarion.alm.tracker.model.IWikiPage;
import com.polarion.alm.ui.client.wiki.WikiDataService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WikiPageServiceTest {

    private static final String TEST_PROJECT_ID = "testProjId";
    private static final String TEST_SPACE_ID = "testSpaceId";
    private static final String TEST_WIKI_NAME = "wikiName";

    @Mock
    private PolarionService polarionService;

    @Mock
    private WikiDataService wikiDataService;

    @InjectMocks
    private WikiPageService wikiPageService;

    @Test
    void createWikiPageTest() {
        wikiPageService.createWikiPage(TEST_PROJECT_ID, TEST_SPACE_ID, TEST_WIKI_NAME);
        verify(wikiDataService).createPageFromDefaultTemplate(null, TEST_SPACE_ID, TEST_WIKI_NAME, TEST_WIKI_NAME);

        // repeat but this time with not-null project
        IProject project = mock(IProject.class);
        when(project.getId()).thenReturn(TEST_PROJECT_ID);
        when(polarionService.getProject(TEST_PROJECT_ID)).thenReturn(project);
        wikiPageService.createWikiPage(TEST_PROJECT_ID, TEST_SPACE_ID, TEST_WIKI_NAME);
        verify(wikiDataService).createPageFromDefaultTemplate(TEST_PROJECT_ID, TEST_SPACE_ID, TEST_WIKI_NAME, TEST_WIKI_NAME);
    }

    @Test
    void shouldThrowIllegalStateIfWikiPageAlreadyExists() {
        when(wikiDataService.pageExist(TEST_PROJECT_ID, TEST_SPACE_ID, TEST_WIKI_NAME)).thenReturn(true);

        assertThatThrownBy(() -> wikiPageService.createWikiPage(TEST_PROJECT_ID, TEST_SPACE_ID, TEST_WIKI_NAME))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(String.format("Wiki page already exists: '%s/%s/%s'", TEST_PROJECT_ID, TEST_SPACE_ID, TEST_WIKI_NAME));
    }

    @Test
    @SuppressWarnings("rawtypes")
    void deleteWikiPageTest() {
        try (MockedStatic<TransactionalExecutor> transactionalExecutor = mockStatic(TransactionalExecutor.class);
             MockedStatic<WikiPageReference> wikiPageReferenceMockedStatic = mockStatic(WikiPageReference.class)) {
            transactionalExecutor.when(() -> TransactionalExecutor.executeInWriteTransaction(any())).thenAnswer(invocation -> {
                RunnableInWriteTransaction runnable = invocation.getArgument(0);
                return runnable.run(mock(WriteTransaction.class));
            });
            WikiPageReference pageReference = mock(WikiPageReference.class);
            wikiPageReferenceMockedStatic.when(() -> WikiPageReference.fromPath("/testSpaceId/wikiName")).thenReturn(pageReference);
            WikiPage wikiPage = mock(WikiPage.class);
            IWikiPage oldApi = mock(IWikiPage.class);
            when(wikiPage.getOldApi()).thenReturn(oldApi);
            when(pageReference.getOriginal(any())).thenReturn(wikiPage);
            wikiPageService.deleteWikiPage(TEST_PROJECT_ID, TEST_SPACE_ID, TEST_WIKI_NAME);
            verify(oldApi, times(1)).delete();

            // repeat but this time with not-null project
            IProject project = mock(IProject.class);
            when(project.getId()).thenReturn(TEST_PROJECT_ID);
            when(polarionService.getProject(TEST_PROJECT_ID)).thenReturn(project);
            wikiPageReferenceMockedStatic.when(() -> WikiPageReference.fromPath("testProjId/testSpaceId/wikiName")).thenReturn(pageReference);
            wikiPageService.deleteWikiPage(TEST_PROJECT_ID, TEST_SPACE_ID, TEST_WIKI_NAME);
            verify(oldApi, times(2)).delete();
        }
    }

}

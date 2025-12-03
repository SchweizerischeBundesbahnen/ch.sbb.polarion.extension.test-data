package ch.sbb.polarion.extension.test_data.service;

import com.polarion.alm.projects.model.IProject;
import com.polarion.alm.shared.api.model.rp.RichPage;
import com.polarion.alm.shared.api.model.rp.RichPageReference;
import com.polarion.alm.shared.api.transaction.WriteTransaction;
import com.polarion.alm.tracker.IRichPageManager;
import com.polarion.alm.tracker.IRichPageSelector;
import com.polarion.alm.tracker.ITrackerService;
import com.polarion.alm.tracker.model.IRichPage;
import com.polarion.alm.tracker.model.ITrackerProject;
import com.polarion.platform.persistence.WrapperException;
import com.polarion.platform.persistence.ipi.ObjectAlreadyExistsException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LiveReportServiceTest {
    @InjectMocks
    LiveReportService liveReportService;

    @Mock
    ITrackerService trackerService;

    @Mock
    IRichPageManager richPageManager;

    @Test
    void shouldCreateLiveReport() {
        // Arrange
        ITrackerProject mockProject = mock(ITrackerProject.class);
        when(mockProject.getId()).thenReturn("testProjectId");

        when(trackerService.getRichPageManager()).thenReturn(richPageManager);
        when(trackerService.getTrackerProject("testProjectId")).thenReturn(mockProject);

        IRichPageSelector<IRichPage> iRichPageSelector = mock(IRichPageSelector.class);
        when(richPageManager.createRichPage()).thenReturn(iRichPageSelector);

        IRichPage iRichPage = mock(IRichPage.class);
        when(iRichPageSelector.spaceAndName("testSpace", "testName")).thenReturn(iRichPage);

        // Act
        liveReportService.createLiveReport("testProjectId", "testSpace", "testName", "text/plain", "test content");

        // Assert
        verify(iRichPageSelector).project("testProjectId");
        verify(iRichPage).setTitle("testName");
        verify(iRichPage).save();
    }

    @Test
    void shouldCreateLiveReportWithoutProject() {
        // Arrange
        when(trackerService.getRichPageManager()).thenReturn(richPageManager);

        IRichPageSelector<IRichPage> iRichPageSelector = mock(IRichPageSelector.class);
        when(richPageManager.createRichPage()).thenReturn(iRichPageSelector);

        IRichPage iRichPage = mock(IRichPage.class);
        when(iRichPageSelector.spaceAndName("testSpace", "testName")).thenReturn(iRichPage);

        // Act
        liveReportService.createLiveReport(null, "testSpace", "testName", "text/plain", "test content");

        // Assert
        verify(iRichPage).setTitle("testName");
        verify(iRichPage).save();
    }

    @Test
    void shouldCreatePath() {
        IProject mockProject = mock(IProject.class);
        when(mockProject.getId()).thenReturn("testProjectId");
        assertThat(liveReportService.createPath(mockProject, "testSpace", "testName")).isEqualTo("testProjectId/testSpace/testName");
    }

    @Test
    void shouldThrowIllegalStateIfObjectAlreadyExists() {
        // Arrange
        ITrackerProject mockProject = mock(ITrackerProject.class);
        when(mockProject.getId()).thenReturn("testProjectId");

        when(trackerService.getRichPageManager()).thenReturn(richPageManager);
        when(trackerService.getTrackerProject("testProjectId")).thenReturn(mockProject);

        IRichPageSelector<IRichPage> iRichPageSelector = mock(IRichPageSelector.class);
        when(richPageManager.createRichPage()).thenReturn(iRichPageSelector);

        when(iRichPageSelector.spaceAndName("testSpace", "testName")).thenThrow(new WrapperException(new ObjectAlreadyExistsException("", "")));

        // Act & Assert
        assertThatThrownBy(() -> liveReportService.createLiveReport("testProjectId", "testSpace", "testName", "text/plain", "test content"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("LiveReport already exists")
                .hasMessageContaining("testProjectId/testSpace/testName");
    }

    @Test
    void shouldDeleteLiveReportWithProject() {
        // Arrange
        WriteTransaction transaction = mock(WriteTransaction.class);
        ITrackerProject mockProject = mock(ITrackerProject.class);
        when(mockProject.getId()).thenReturn("testProjectId");
        when(trackerService.getTrackerProject("testProjectId")).thenReturn(mockProject);

        RichPageReference richPageReference = mock(RichPageReference.class);
        RichPage richPage = mock(RichPage.class);
        IRichPage oldApi = mock(IRichPage.class);

        try (var mockedStatic = mockStatic(RichPageReference.class)) {
            mockedStatic.when(() -> RichPageReference.fromPath("testProjectId/testSpace/testName")).thenReturn(richPageReference);
            when(richPageReference.getOriginal(transaction)).thenReturn(richPage);
            when(richPage.getOldApi()).thenReturn(oldApi);

            // Act
            liveReportService.deleteLiveReport(transaction, "testProjectId", "testSpace", "testName");

            // Assert
            verify(trackerService).getTrackerProject("testProjectId");
            verify(oldApi).delete();
        }
    }

    @Test
    void shouldDeleteLiveReportWithoutProject() {
        // Arrange
        WriteTransaction transaction = mock(WriteTransaction.class);

        RichPageReference richPageReference = mock(RichPageReference.class);
        RichPage richPage = mock(RichPage.class);
        IRichPage oldApi = mock(IRichPage.class);

        try (var mockedStatic = mockStatic(RichPageReference.class)) {
            mockedStatic.when(() -> RichPageReference.fromPath("/testSpace/testName")).thenReturn(richPageReference);
            when(richPageReference.getOriginal(transaction)).thenReturn(richPage);
            when(richPage.getOldApi()).thenReturn(oldApi);

            // Act
            liveReportService.deleteLiveReport(transaction, null, "testSpace", "testName");

            // Assert
            verify(trackerService, never()).getTrackerProject(anyString());
            verify(oldApi).delete();
        }
    }

    @Test
    void shouldDeleteLiveReportWithEmptyProjectId() {
        // Arrange
        WriteTransaction transaction = mock(WriteTransaction.class);

        RichPageReference richPageReference = mock(RichPageReference.class);
        RichPage richPage = mock(RichPage.class);
        IRichPage oldApi = mock(IRichPage.class);

        try (var mockedStatic = mockStatic(RichPageReference.class)) {
            mockedStatic.when(() -> RichPageReference.fromPath("/testSpace/testName")).thenReturn(richPageReference);
            when(richPageReference.getOriginal(transaction)).thenReturn(richPage);
            when(richPage.getOldApi()).thenReturn(oldApi);

            // Act
            liveReportService.deleteLiveReport(transaction, "", "testSpace", "testName");

            // Assert
            verify(trackerService, never()).getTrackerProject(anyString());
            verify(oldApi).delete();
        }
    }

    @Test
    void shouldCreatePathWithoutProject() {
        assertThat(liveReportService.createPath(null, "testSpace", "testName")).isEqualTo("/testSpace/testName");
    }
}

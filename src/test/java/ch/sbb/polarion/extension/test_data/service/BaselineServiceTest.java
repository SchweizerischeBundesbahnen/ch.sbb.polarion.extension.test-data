package ch.sbb.polarion.extension.test_data.service;

import ch.sbb.polarion.extension.generic.service.PolarionService;
import ch.sbb.polarion.extension.test_data.rest.model.BaselineResponse;
import ch.sbb.polarion.extension.test_data.rest.model.CollectionElementRef;
import ch.sbb.polarion.extension.test_data.rest.model.CollectionRequest;
import com.polarion.alm.projects.IProjectService;
import com.polarion.alm.projects.model.IUser;
import com.polarion.alm.tracker.IBaselinesManager;
import com.polarion.alm.tracker.ITrackerService;
import com.polarion.alm.tracker.model.IBaseline;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.ITrackerProject;
import com.polarion.alm.tracker.model.baselinecollection.IBaselineCollection;
import com.polarion.alm.tracker.model.baselinecollection.IBaselineCollectionsManager;
import com.polarion.platform.ITransactionService;
import com.polarion.platform.core.IPlatform;
import com.polarion.platform.core.PlatformContext;
import com.polarion.platform.service.repository.IRepositoryReadOnlyConnection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BaselineServiceTest {

    private MockedStatic<PlatformContext> platformContextMockedStatic;
    private IProjectService projectService;

    @BeforeEach
    void setUp() {
        IPlatform platform = mock(IPlatform.class);
        when(platform.lookupService(ITransactionService.class)).thenReturn(mock(ITransactionService.class));
        projectService = mock(IProjectService.class);
        when(projectService.getCurrentUser()).thenReturn(mock(IUser.class));
        when(platform.lookupService(IProjectService.class)).thenReturn(projectService);
        platformContextMockedStatic = mockStatic(PlatformContext.class);
        platformContextMockedStatic.when(PlatformContext::getPlatform).thenReturn(platform);
    }

    @AfterEach
    void tearDown() {
        if (platformContextMockedStatic != null) {
            platformContextMockedStatic.close();
        }
    }

    @Test
    void createBaselineRejectsBlankName() {
        BaselineService service = new BaselineService(mock(PolarionService.class));
        assertThrows(IllegalArgumentException.class, () -> service.createBaseline("p", " ", null, null));
    }

    @Test
    void createBaselineDelegatesToBaselinesManagerAndResolvesHead() throws Exception {
        PolarionService polarionService = mock(PolarionService.class);
        ITrackerProject trackerProject = mock(ITrackerProject.class);
        when(polarionService.getTrackerProject("p")).thenReturn(trackerProject);

        IRepositoryReadOnlyConnection connection = mock(IRepositoryReadOnlyConnection.class);
        when(connection.getLastRevision(any())).thenReturn("42");
        when(polarionService.getReadOnlyConnection(any())).thenReturn(connection);

        IBaselinesManager baselinesManager = mock(IBaselinesManager.class);
        when(trackerProject.getBaselinesManager()).thenReturn(baselinesManager);

        IBaseline baseline = mock(IBaseline.class);
        when(baseline.getName()).thenReturn("v1");
        when(baseline.getBaseRevision()).thenReturn("42");
        when(baselinesManager.createBaseline(eq("v1"), eq("desc"), eq("42"), any())).thenReturn(baseline);

        BaselineResponse response = new BaselineService(polarionService).createBaseline("p", "v1", "desc", null);
        assertEquals("v1", response.name());
        assertEquals("42", response.revision());
        verify(baseline).save();
    }

    @Test
    void createCollectionRejectsEmptyElements() {
        BaselineService service = new BaselineService(mock(PolarionService.class));
        assertThrows(IllegalArgumentException.class, () -> service.createCollection("p", "c",
                new CollectionRequest("d", List.of())));
    }

    @Test
    void createCollectionAddsElementsAndSaves() {
        PolarionService polarionService = mock(PolarionService.class);
        ITrackerService trackerService = mock(ITrackerService.class);
        IBaselineCollectionsManager mgr = mock(IBaselineCollectionsManager.class);
        IBaselineCollection collection = mock(IBaselineCollection.class);
        when(mgr.createCollection("p")).thenReturn(collection);
        when(trackerService.getBaselineCollectionsManager()).thenReturn(mgr);
        when(polarionService.getTrackerService()).thenReturn(trackerService);

        IModule module1 = mock(IModule.class);
        IModule module2 = mock(IModule.class);
        when(polarionService.getModule("p", "_default", "doc_1", "10")).thenReturn(module1);
        when(polarionService.getModule("p", "_default", "doc_2", "20")).thenReturn(module2);

        CollectionRequest request = new CollectionRequest("desc", List.of(
                new CollectionElementRef("_default", "doc_1", "10"),
                new CollectionElementRef("_default", "doc_2", "20")));
        IBaselineCollection result = new BaselineService(polarionService).createCollection("p", "rel-1.0", request);
        assertEquals(collection, result);
        verify(collection).setName("rel-1.0");
        verify(collection).addElement(module1);
        verify(collection).addElement(module2);
        verify(collection).save();
    }
}

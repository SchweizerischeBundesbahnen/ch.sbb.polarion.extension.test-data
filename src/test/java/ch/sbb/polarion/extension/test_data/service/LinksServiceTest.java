package ch.sbb.polarion.extension.test_data.service;

import ch.sbb.polarion.extension.generic.service.PolarionService;
import ch.sbb.polarion.extension.test_data.rest.model.CrossDocumentLinksRequest;
import ch.sbb.polarion.extension.test_data.rest.model.DocumentRef;
import ch.sbb.polarion.extension.test_data.rest.model.LinkedRevisionsRequest;
import com.polarion.alm.tracker.model.ILinkRoleOpt;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.ITrackerProject;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.platform.ITransactionService;
import com.polarion.platform.core.IPlatform;
import com.polarion.platform.core.PlatformContext;
import com.polarion.platform.persistence.IEnumeration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LinksServiceTest {

    private MockedStatic<PlatformContext> platformContextMockedStatic;

    @BeforeEach
    void setUp() {
        IPlatform platform = mock(IPlatform.class);
        when(platform.lookupService(ITransactionService.class)).thenReturn(mock(ITransactionService.class));
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
    void createCrossDocumentLinksRequiresAtLeastTwoDocuments() {
        LinksService service = new LinksService(mock(PolarionService.class));
        CrossDocumentLinksRequest request = new CrossDocumentLinksRequest(
                List.of(new DocumentRef("_default", "doc_1")), 1, "relates_to");
        assertThrows(IllegalArgumentException.class, () -> service.createCrossDocumentLinks("p", request));
    }

    @Test
    void createCrossDocumentLinksAddsLinks() {
        PolarionService polarionService = mock(PolarionService.class);
        ITrackerProject trackerProject = mock(ITrackerProject.class);
        when(polarionService.getTrackerProject("p")).thenReturn(trackerProject);

        IEnumeration<ILinkRoleOpt> roleEnum = mock(IEnumeration.class);
        ILinkRoleOpt role = mock(ILinkRoleOpt.class);
        when(roleEnum.wrapOption("relates_to")).thenReturn(role);
        when(trackerProject.getWorkItemLinkRoleEnum()).thenReturn(roleEnum);

        IModule moduleA = mock(IModule.class);
        IModule moduleB = mock(IModule.class);
        List<IWorkItem> wisA = new ArrayList<>();
        List<IWorkItem> wisB = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            IWorkItem a = mock(IWorkItem.class);
            when(a.addLinkedItem(any(), eq(role), eq(null), anyBoolean())).thenReturn(true);
            wisA.add(a);
            IWorkItem b = mock(IWorkItem.class);
            when(b.addLinkedItem(any(), eq(role), eq(null), anyBoolean())).thenReturn(true);
            wisB.add(b);
        }
        when(moduleA.getAllWorkItems()).thenReturn(wisA);
        when(moduleB.getAllWorkItems()).thenReturn(wisB);
        when(polarionService.getModule("p", "_default", "doc_a")).thenReturn(moduleA);
        when(polarionService.getModule("p", "_default", "doc_b")).thenReturn(moduleB);

        CrossDocumentLinksRequest request = new CrossDocumentLinksRequest(
                List.of(new DocumentRef("_default", "doc_a"), new DocumentRef("_default", "doc_b")),
                2, "relates_to");
        int created = new LinksService(polarionService).createCrossDocumentLinks("p", request);
        assertEquals((wisA.size() + wisB.size()) * 2, created);
        verify(wisA.get(0), atLeast(2)).addLinkedItem(any(), eq(role), eq(null), anyBoolean());
    }

    @Test
    void createCrossDocumentLinksRejectsNonPositiveLinksPerWorkItem() {
        LinksService service = new LinksService(mock(PolarionService.class));
        CrossDocumentLinksRequest request = new CrossDocumentLinksRequest(
                List.of(new DocumentRef("_default", "a"), new DocumentRef("_default", "b")), 0, "relates_to");
        assertThrows(IllegalArgumentException.class, () -> service.createCrossDocumentLinks("p", request));
    }

    @Test
    void createCrossDocumentLinksRejectsUnknownLinkRole() {
        PolarionService polarionService = mock(PolarionService.class);
        ITrackerProject trackerProject = mock(ITrackerProject.class);
        when(polarionService.getTrackerProject("p")).thenReturn(trackerProject);
        IEnumeration<ILinkRoleOpt> roleEnum = mock(IEnumeration.class);
        when(roleEnum.wrapOption("unknown")).thenReturn(null);
        when(trackerProject.getWorkItemLinkRoleEnum()).thenReturn(roleEnum);

        CrossDocumentLinksRequest request = new CrossDocumentLinksRequest(
                List.of(new DocumentRef("_default", "a"), new DocumentRef("_default", "b")), 1, "unknown");
        assertThrows(IllegalArgumentException.class,
                () -> new LinksService(polarionService).createCrossDocumentLinks("p", request));
    }

    @Test
    void createCrossDocumentLinksSkipsDocumentsWithoutWorkItems() {
        PolarionService polarionService = mock(PolarionService.class);
        ITrackerProject trackerProject = mock(ITrackerProject.class);
        when(polarionService.getTrackerProject("p")).thenReturn(trackerProject);
        IEnumeration<ILinkRoleOpt> roleEnum = mock(IEnumeration.class);
        ILinkRoleOpt role = mock(ILinkRoleOpt.class);
        when(roleEnum.wrapOption("relates_to")).thenReturn(role);
        when(trackerProject.getWorkItemLinkRoleEnum()).thenReturn(roleEnum);

        IModule moduleA = mock(IModule.class);
        IModule moduleB = mock(IModule.class);
        when(moduleA.getAllWorkItems()).thenReturn(new ArrayList<>());
        when(moduleB.getAllWorkItems()).thenReturn(new ArrayList<>());
        when(polarionService.getModule("p", "_default", "doc_a")).thenReturn(moduleA);
        when(polarionService.getModule("p", "_default", "doc_b")).thenReturn(moduleB);

        CrossDocumentLinksRequest request = new CrossDocumentLinksRequest(
                List.of(new DocumentRef("_default", "doc_a"), new DocumentRef("_default", "doc_b")), 1, "relates_to");
        int created = new LinksService(polarionService).createCrossDocumentLinks("p", request);
        assertEquals(0, created);
    }

    @Test
    void createCrossDocumentLinksDoesNotCountDuplicateAdds() {
        PolarionService polarionService = mock(PolarionService.class);
        ITrackerProject trackerProject = mock(ITrackerProject.class);
        when(polarionService.getTrackerProject("p")).thenReturn(trackerProject);
        IEnumeration<ILinkRoleOpt> roleEnum = mock(IEnumeration.class);
        ILinkRoleOpt role = mock(ILinkRoleOpt.class);
        when(roleEnum.wrapOption("relates_to")).thenReturn(role);
        when(trackerProject.getWorkItemLinkRoleEnum()).thenReturn(roleEnum);

        IModule moduleA = mock(IModule.class);
        IModule moduleB = mock(IModule.class);
        List<IWorkItem> wisA = new ArrayList<>();
        List<IWorkItem> wisB = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            IWorkItem a = mock(IWorkItem.class);
            when(a.addLinkedItem(any(), eq(role), eq(null), anyBoolean())).thenReturn(false);
            wisA.add(a);
            wisB.add(mock(IWorkItem.class));
        }
        when(moduleA.getAllWorkItems()).thenReturn(wisA);
        when(moduleB.getAllWorkItems()).thenReturn(wisB);
        when(polarionService.getModule("p", "_default", "doc_a")).thenReturn(moduleA);
        when(polarionService.getModule("p", "_default", "doc_b")).thenReturn(moduleB);

        CrossDocumentLinksRequest request = new CrossDocumentLinksRequest(
                List.of(new DocumentRef("_default", "doc_a"), new DocumentRef("_default", "doc_b")), 1, "relates_to");
        int created = new LinksService(polarionService).createCrossDocumentLinks("p", request);
        // wisA returns false for every addLinkedItem; wisB defaults to false too -> nothing counted
        assertEquals(0, created);
    }

    @Test
    void addLinkedRevisionsRejectsNonPositiveWorkItemsPerRevision() {
        LinksService service = new LinksService(mock(PolarionService.class));
        LinkedRevisionsRequest request = new LinkedRevisionsRequest(List.of("1"), 0, null);
        assertThrows(IllegalArgumentException.class,
                () -> service.addLinkedRevisions("p", "_default", "doc", request));
    }

    @Test
    void addLinkedRevisionsReturnsZeroWhenDocumentHasNoWorkItems() {
        PolarionService polarionService = mock(PolarionService.class);
        IModule module = mock(IModule.class);
        when(module.getAllWorkItems()).thenReturn(new ArrayList<>());
        when(polarionService.getModule("p", "_default", "doc")).thenReturn(module);

        int added = new LinksService(polarionService).addLinkedRevisions("p", "_default", "doc",
                new LinkedRevisionsRequest(List.of("1"), 3, null));
        assertEquals(0, added);
    }

    @Test
    void addLinkedRevisionsSkipsDuplicateAdds() {
        PolarionService polarionService = mock(PolarionService.class);
        IModule module = mock(IModule.class);
        List<IWorkItem> wis = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            IWorkItem w = mock(IWorkItem.class);
            when(w.addLinkedRevision(any(), any())).thenReturn(false);
            wis.add(w);
        }
        when(module.getAllWorkItems()).thenReturn(wis);
        when(polarionService.getModule("p", "_default", "doc")).thenReturn(module);

        int added = new LinksService(polarionService).addLinkedRevisions("p", "_default", "doc",
                new LinkedRevisionsRequest(List.of("100"), 2, null));
        assertEquals(0, added);
    }

    @Test
    void addLinkedRevisionsValidatesInput() {
        LinksService service = new LinksService(mock(PolarionService.class));
        LinkedRevisionsRequest request = new LinkedRevisionsRequest(List.of(), 1, null);
        assertThrows(IllegalArgumentException.class, () -> service.addLinkedRevisions("p", "_default", "doc", request));
    }

    @Test
    void addLinkedRevisionsAddsLinks() {
        PolarionService polarionService = mock(PolarionService.class);
        IModule module = mock(IModule.class);
        List<IWorkItem> wis = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            IWorkItem w = mock(IWorkItem.class);
            when(w.addLinkedRevision(any(), any())).thenReturn(true);
            wis.add(w);
        }
        when(module.getAllWorkItems()).thenReturn(wis);
        when(polarionService.getModule("p", "_default", "doc")).thenReturn(module);

        int added = new LinksService(polarionService).addLinkedRevisions("p", "_default", "doc",
                new LinkedRevisionsRequest(List.of("100", "200"), 2, "ref"));
        assertEquals(4, added);
        assertTrue(added > 0);
    }
}

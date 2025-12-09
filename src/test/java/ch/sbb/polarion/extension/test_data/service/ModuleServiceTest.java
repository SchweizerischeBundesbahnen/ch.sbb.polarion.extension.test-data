package ch.sbb.polarion.extension.test_data.service;

import ch.sbb.polarion.extension.generic.service.PolarionService;
import ch.sbb.polarion.extension.test_data.util.DocumentGeneratorUtils;
import com.polarion.alm.tracker.IModuleManager;
import com.polarion.alm.tracker.ITrackerService;
import com.polarion.alm.tracker.model.ILinkRoleOpt;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.ITrackerProject;
import com.polarion.alm.tracker.model.ITypeOpt;
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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class ModuleServiceTest {

    private MockedStatic<PlatformContext> platformContextMockedStatic;


    @BeforeEach
    void setUp() {
        IPlatform platform = mock(IPlatform.class);

        ITransactionService transactionService = mock(ITransactionService.class);
        when(platform.lookupService(ITransactionService.class)).thenReturn(transactionService);

        ITrackerService trackerService = mock(ITrackerService.class);
        IModuleManager moduleManager = mock(IModuleManager.class);
        when(trackerService.getModuleManager()).thenReturn(moduleManager);
        when(platform.lookupService(ITrackerService.class)).thenReturn(trackerService);

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
    void testCreateDocumentWithGeneratedWorkItems() {
        PolarionService polarionService = mock(PolarionService.class);
        ModuleService moduleService = new ModuleService(polarionService);

        ITrackerProject trackerProject = mock(ITrackerProject.class);
        when(polarionService.getTrackerProject("projectId")).thenReturn(trackerProject);
        when(trackerProject.getId()).thenReturn("projectId");
        IEnumeration<ILinkRoleOpt> linkRoleEnumeration = mock(IEnumeration.class);
        ILinkRoleOpt linkRoleOpt = mock(ILinkRoleOpt.class);
        when(linkRoleEnumeration.wrapOption("parent")).thenReturn(linkRoleOpt);
        when(trackerProject.getWorkItemLinkRoleEnum()).thenReturn(linkRoleEnumeration);

        IModule module = mock(IModule.class);
        when(module.createWorkItem(anyString())).thenReturn(mock(IWorkItem.class));

        ITrackerService trackerService = mock(ITrackerService.class);
        IModuleManager moduleManager = mock(IModuleManager.class);
        when(moduleManager.createModule(eq(trackerProject), any(), eq("documentName"), eq(null), eq(linkRoleOpt), eq(false))).thenReturn(module);

        when(trackerService.getModuleManager()).thenReturn(moduleManager);
        when(polarionService.getTrackerService()).thenReturn(trackerService);

        when(polarionService.getModule("projectId", "spaceId", "documentName")).thenReturn(module);

        IModule createdModule = moduleService.createDocumentWithGeneratedWorkItems("projectId", "spaceId", "documentName", 5);
        assertNotNull(createdModule);

        assertThrows(IllegalArgumentException.class, () -> moduleService.createDocumentWithGeneratedWorkItems("projectId", "spaceId", "documentName", 0));
    }

    @Test
    void testExtendDocumentWithGeneratedWorkItems() {
        IModule module = mock(IModule.class);
        when(module.createWorkItem(anyString())).thenReturn(mock(IWorkItem.class));

        PolarionService polarionService = mock(PolarionService.class);
        when(polarionService.getModule("projectId", "_default", "documentName")).thenReturn(module);
        ModuleService moduleService = new ModuleService(polarionService);

        IModule extendedModule = moduleService.extendDocumentWithGeneratedWorkItems("projectId", null, "documentName", 5);
        assertNotNull(extendedModule);

        assertThrows(IllegalArgumentException.class, () -> moduleService.extendDocumentWithGeneratedWorkItems("projectId", "spaceId", "documentName", 0));
    }

    @Test
    void testChangeDocumentWorkItemDescriptions() {
        IModule module = mock(IModule.class);
        IWorkItem workItemRequirement = mock(IWorkItem.class);
        ITypeOpt workItemRequirementType = mock(ITypeOpt.class);
        when(workItemRequirementType.getId()).thenReturn(DocumentGeneratorUtils.REQUIREMENT);
        when(workItemRequirement.getType()).thenReturn(workItemRequirementType);
        when(module.createWorkItem(DocumentGeneratorUtils.REQUIREMENT)).thenReturn(workItemRequirement);
        IWorkItem workItemHeading = mock(IWorkItem.class);
        when(module.createWorkItem(DocumentGeneratorUtils.HEADING)).thenReturn(workItemHeading);

        List<IWorkItem> workItems = new ArrayList<>(10);
        for (int i = 0; i < 10; i++) {
            workItems.add(i % 2 == 0 ? workItemRequirement : workItemHeading);
        }
        when(module.getAllWorkItems()).thenReturn(workItems);

        PolarionService polarionService = mock(PolarionService.class);
        when(polarionService.getModule("projectId", "spaceId", "documentName")).thenReturn(module);
        ModuleService moduleService = new ModuleService(polarionService);

        IModule updatedModule = moduleService.changeDocumentWorkItemDescriptions("projectId", "spaceId", "documentName", 5);
        assertNotNull(updatedModule);

        assertThrows(IllegalArgumentException.class, () -> moduleService.changeDocumentWorkItemDescriptions("projectId", "spaceId", "documentName", 0));
    }

    @Test
    void testCreateLargeDocumentWithImages() {
        PolarionService polarionService = mock(PolarionService.class);
        ModuleService moduleService = new ModuleService(polarionService);

        ITrackerProject trackerProject = mock(ITrackerProject.class);
        when(polarionService.getTrackerProject("projectId")).thenReturn(trackerProject);
        when(trackerProject.getId()).thenReturn("projectId");
        IEnumeration<ILinkRoleOpt> linkRoleEnumeration = mock(IEnumeration.class);
        ILinkRoleOpt linkRoleOpt = mock(ILinkRoleOpt.class);
        when(linkRoleEnumeration.wrapOption("parent")).thenReturn(linkRoleOpt);
        when(trackerProject.getWorkItemLinkRoleEnum()).thenReturn(linkRoleEnumeration);

        IModule module = mock(IModule.class);
        when(module.createWorkItem(anyString())).thenReturn(mock(IWorkItem.class));

        ITrackerService trackerService = mock(ITrackerService.class);
        IModuleManager moduleManager = mock(IModuleManager.class);
        when(moduleManager.createModule(eq(trackerProject), any(), eq("largeDoc"), eq(null), eq(linkRoleOpt), eq(false))).thenReturn(module);

        when(trackerService.getModuleManager()).thenReturn(moduleManager);
        when(polarionService.getTrackerService()).thenReturn(trackerService);

        IModule createdModule = moduleService.createLargeDocumentWithImages(
                "projectId", "spaceId", "largeDoc",
                10, 2, 800, 600
        );
        assertNotNull(createdModule);
    }

    @Test
    void testCreateLargeDocumentWithImagesValidation() {
        PolarionService polarionService = mock(PolarionService.class);
        ModuleService moduleService = new ModuleService(polarionService);

        // Test invalid pagesCount
        assertThrows(IllegalArgumentException.class, () ->
                moduleService.createLargeDocumentWithImages("projectId", "spaceId", "doc", 0, 2, 800, 600));

        // Test invalid imagesPerPage
        assertThrows(IllegalArgumentException.class, () ->
                moduleService.createLargeDocumentWithImages("projectId", "spaceId", "doc", 10, -1, 800, 600));

        // Test invalid imageWidth
        assertThrows(IllegalArgumentException.class, () ->
                moduleService.createLargeDocumentWithImages("projectId", "spaceId", "doc", 10, 2, 0, 600));

        // Test invalid imageHeight
        assertThrows(IllegalArgumentException.class, () ->
                moduleService.createLargeDocumentWithImages("projectId", "spaceId", "doc", 10, 2, 800, 0));
    }
}

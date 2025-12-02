package ch.sbb.polarion.extension.test_data.service;

import com.polarion.alm.projects.IProjectLifecycleManager;
import com.polarion.core.util.StreamUtils;
import com.polarion.platform.core.IPlatform;
import com.polarion.platform.core.PlatformContext;
import com.polarion.platform.service.repository.IRepositoryConnection;
import com.polarion.platform.service.repository.IRepositoryService;
import com.polarion.subterra.base.location.ILocation;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProjectTemplateServiceTest {
    private MockedStatic<PlatformContext> platformContextMockedStatic;
    private MockedStatic<StreamUtils> streamUtilsMockedStatic;
    private IRepositoryService repositoryService;
    private IProjectLifecycleManager projectLifecycleManager;
    private IRepositoryConnection repositoryConnection;
    private ProjectTemplateService service;

    @BeforeEach
    void setUp() {
        IPlatform platform = mock(IPlatform.class);

        repositoryService = mock(IRepositoryService.class);
        projectLifecycleManager = mock(IProjectLifecycleManager.class);
        repositoryConnection = mock(IRepositoryConnection.class);

        when(platform.lookupService(IRepositoryService.class)).thenReturn(repositoryService);
        when(platform.lookupService(IProjectLifecycleManager.class)).thenReturn(projectLifecycleManager);
        when(repositoryService.getConnection(any(ILocation.class))).thenReturn(repositoryConnection);

        platformContextMockedStatic = mockStatic(PlatformContext.class);
        platformContextMockedStatic.when(PlatformContext::getPlatform).thenReturn(platform);

        streamUtilsMockedStatic = mockStatic(StreamUtils.class);

        service = new ProjectTemplateService();
    }

    @AfterEach
    void tearDown() {
        if (platformContextMockedStatic != null) {
            platformContextMockedStatic.close();
        }
        if (streamUtilsMockedStatic != null) {
            streamUtilsMockedStatic.close();
        }
    }

    @Test
    @SneakyThrows
    void testSaveProjectTemplateSuccess() {
        String templateId = "testTemplate";
        byte[] zipData = createTestZipData();
        InputStream inputStream = new ByteArrayInputStream(zipData);
        byte[] fileContent = "test content".getBytes(StandardCharsets.UTF_8);

        streamUtilsMockedStatic.when(() -> StreamUtils.suckStream(eq(inputStream), eq(true)))
                .thenReturn(zipData);

        streamUtilsMockedStatic.when(() -> StreamUtils.suckStream(any(InputStream.class), eq(false)))
                .thenReturn(fileContent);

        when(repositoryConnection.exists(any(ILocation.class))).thenReturn(false);

        service.saveProjectTemplate(templateId, inputStream, null);

        verify(repositoryConnection).makeFolders(any(ILocation.class));
        verify(repositoryConnection, atLeastOnce()).create(any(ILocation.class), any(ByteArrayInputStream.class));
        verify(projectLifecycleManager).saveProjectTemplate(eq(templateId), any(), isNull());
    }

    @Test
    @SneakyThrows
    void testSaveProjectTemplateWithHash() {
        String templateId = "testTemplate";
        String templateHash = "abc123def456";
        byte[] zipData = createTestZipData();
        InputStream inputStream = new ByteArrayInputStream(zipData);
        byte[] fileContent = "test content".getBytes(StandardCharsets.UTF_8);

        streamUtilsMockedStatic.when(() -> StreamUtils.suckStream(eq(inputStream), eq(true)))
                .thenReturn(zipData);

        streamUtilsMockedStatic.when(() -> StreamUtils.suckStream(any(InputStream.class), eq(false)))
                .thenReturn(fileContent);

        when(repositoryConnection.exists(any(ILocation.class))).thenReturn(false);

        service.saveProjectTemplate(templateId, inputStream, templateHash);

        verify(repositoryConnection, atLeast(2)).create(any(ILocation.class), any(ByteArrayInputStream.class));
    }

    @Test
    void testReadTemplateHashSuccess() {
        String templateId = "testTemplate";
        String expectedHash = "abc123def456";
        byte[] hashBytes = expectedHash.getBytes(StandardCharsets.UTF_8);

        when(repositoryConnection.exists(any(ILocation.class))).thenReturn(true);
        when(repositoryConnection.getContent(any(ILocation.class)))
                .thenReturn(new ByteArrayInputStream(hashBytes));

        streamUtilsMockedStatic.when(() -> StreamUtils.suckStream(any(InputStream.class), eq(true)))
                .thenReturn(hashBytes);

        String result = service.readTemplateHash(templateId);

        assertEquals(expectedHash, result);
    }

    @Test
    void testReadTemplateHashNotFound() {
        String templateId = "testTemplate";

        when(repositoryConnection.exists(any(ILocation.class))).thenReturn(false);

        String result = service.readTemplateHash(templateId);

        assertNull(result);
    }

    @Test
    void testReadTemplateHashIOException() {
        String templateId = "testTemplate";

        when(repositoryConnection.exists(any(ILocation.class))).thenReturn(true);
        when(repositoryConnection.getContent(any(ILocation.class)))
                .thenThrow(new RuntimeException("IO error"));

        assertThrows(ProjectTemplateService.TemplateProcessingException.class,
                () -> service.readTemplateHash(templateId));
    }

    @Test
    void testSaveProjectTemplateThrowsTemplateProcessingException() {
        String templateId = "testTemplate";
        InputStream inputStream = mock(InputStream.class);

        streamUtilsMockedStatic.when(() -> StreamUtils.suckStream(any(InputStream.class), eq(true)))
                .thenThrow(new RuntimeException("Stream error"));

        assertThrows(ProjectTemplateService.TemplateProcessingException.class,
                () -> service.saveProjectTemplate(templateId, inputStream, null));
    }

    @Test
    void testDownloadProjectSuccess() {
        String projectId = "testProject";
        ILocation projectLocation = mock(ILocation.class);
        ILocation fileLocation = mock(ILocation.class);

        when(repositoryConnection.getSubLocations(any(ILocation.class), eq(true)))
                .thenReturn(Collections.singletonList(fileLocation));
        when(repositoryConnection.isFile(fileLocation)).thenReturn(true);
        when(repositoryConnection.isFolder(fileLocation)).thenReturn(false);
        when(repositoryConnection.getContent(fileLocation))
                .thenReturn(new ByteArrayInputStream("test".getBytes()));
        when(fileLocation.getRelativeLocation(any(ILocation.class)))
                .thenReturn(projectLocation);
        when(projectLocation.getLocationPath()).thenReturn("test.txt");

        streamUtilsMockedStatic.when(() -> StreamUtils.copy(any(InputStream.class), any()))
                .thenAnswer(invocation -> null);

        byte[] result = service.downloadProject(projectId);

        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    void testDownloadProjectInvalidId() {
        assertThrows(IllegalArgumentException.class,
                () -> service.downloadProject(null));

        assertThrows(IllegalArgumentException.class,
                () -> service.downloadProject(""));

        assertThrows(IllegalArgumentException.class,
                () -> service.downloadProject("../invalid"));

        assertThrows(IllegalArgumentException.class,
                () -> service.downloadProject("invalid/path"));
    }

    @Test
    void testNormalizeEntryNameNullOrEmpty() {
        assertNull(service.normalizeEntryName(null));
        assertNull(service.normalizeEntryName(""));
    }

    @Test
    void testNormalizeEntryNameLeadingSlashes() {
        assertEquals("file.txt", service.normalizeEntryName("/file.txt"));
        assertEquals("dir/file.txt", service.normalizeEntryName("///dir/file.txt"));
    }

    @Test
    void testNormalizeEntryNameBackslashes() {
        assertEquals("dir/file.txt", service.normalizeEntryName("\\dir\\file.txt"));
    }

    @Test
    void testNormalizeEntryNameDirectoryTraversal() {
        assertNull(service.normalizeEntryName("../file.txt"));
        assertNull(service.normalizeEntryName("dir/../file.txt"));
    }

    @Test
    void testNormalizeEntryNameValidName() {
        assertEquals("dir/file.txt", service.normalizeEntryName("dir/file.txt"));
    }

    @Test
    @SneakyThrows
    void testCanProcessZipValidZip() {
        byte[] zipData = createTestZipData();
        assertTrue(service.canProcessZip(zipData, StandardCharsets.UTF_8));
    }

    @Test
    void testCanProcessZipInvalidZip() {
        byte[] invalidData = "not a zip".getBytes(StandardCharsets.UTF_8);
        assertFalse(service.canProcessZip(invalidData, StandardCharsets.UTF_8));
    }

    @Test
    void testCleanupTemplateFolder_ExistsAndDeleted() {
        ILocation templateFolder = mock(ILocation.class);

        when(repositoryConnection.exists(templateFolder)).thenReturn(true);
        doNothing().when(repositoryConnection).delete(templateFolder);

        service.cleanupTemplateFolder(repositoryConnection, templateFolder);

        verify(repositoryConnection).exists(templateFolder);
        verify(repositoryConnection).delete(templateFolder);
    }

    @Test
    void testCleanupTemplateFolderDoesNotExist() {
        ILocation templateFolder = mock(ILocation.class);

        when(repositoryConnection.exists(templateFolder)).thenReturn(false);

        service.cleanupTemplateFolder(repositoryConnection, templateFolder);

        verify(repositoryConnection).exists(templateFolder);
        verify(repositoryConnection, never()).delete(templateFolder);
    }

    @Test
    void testCleanupTemplateFolderDeleteThrowsException() {
        ILocation templateFolder = mock(ILocation.class);
        String locationPath = "/test/path";

        when(repositoryConnection.exists(templateFolder)).thenReturn(true);
        when(templateFolder.toString()).thenReturn(locationPath);
        doThrow(new RuntimeException("Delete failed")).when(repositoryConnection).delete(templateFolder);

        // Should not throw exception - cleanup is silent
        assertDoesNotThrow(() -> service.cleanupTemplateFolder(repositoryConnection, templateFolder));

        verify(repositoryConnection).exists(templateFolder);
        verify(repositoryConnection).delete(templateFolder);
    }

    @Test
    void testCleanupTemplateFolder_ExistsThrowsException() {
        ILocation templateFolder = mock(ILocation.class);

        when(repositoryConnection.exists(templateFolder)).thenThrow(new RuntimeException("Connection error"));

        // Should not throw exception - cleanup is silent
        assertDoesNotThrow(() -> service.cleanupTemplateFolder(repositoryConnection, templateFolder));

        verify(repositoryConnection).exists(templateFolder);
        verify(repositoryConnection, never()).delete(templateFolder);
    }

    private byte[] createTestZipData() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            ZipEntry entry = new ZipEntry("test.txt");
            zos.putNextEntry(entry);
            zos.write("test content".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
        return baos.toByteArray();
    }
}

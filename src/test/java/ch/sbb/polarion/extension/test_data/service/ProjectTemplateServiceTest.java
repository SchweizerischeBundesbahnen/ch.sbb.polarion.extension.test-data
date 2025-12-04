package ch.sbb.polarion.extension.test_data.service;

import com.polarion.alm.projects.IProjectLifecycleManager;
import com.polarion.core.util.StreamUtils;
import com.polarion.platform.core.IPlatform;
import com.polarion.platform.core.PlatformContext;
import com.polarion.platform.service.repository.IRepositoryConnection;
import com.polarion.platform.service.repository.IRepositoryReadOnlyConnection;
import com.polarion.platform.service.repository.IRepositoryService;
import com.polarion.subterra.base.location.ILocation;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static ch.sbb.polarion.extension.test_data.service.ProjectTemplateService.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProjectTemplateServiceTest {
    private MockedStatic<PlatformContext> platformContextMockedStatic;
    private MockedStatic<StreamUtils> streamUtilsMockedStatic;
    private IRepositoryService repositoryService;
    private IProjectLifecycleManager projectLifecycleManager;
    private IRepositoryConnection repositoryConnection;
    private IRepositoryReadOnlyConnection repositoryReadOnlyConnection;
    private ProjectTemplateService service;

    @BeforeEach
    void setUp() {
        IPlatform platform = mock(IPlatform.class);

        repositoryService = mock(IRepositoryService.class);
        projectLifecycleManager = mock(IProjectLifecycleManager.class);
        repositoryConnection = mock(IRepositoryConnection.class);
        repositoryReadOnlyConnection = mock(IRepositoryReadOnlyConnection.class);

        when(platform.lookupService(IRepositoryService.class)).thenReturn(repositoryService);
        when(platform.lookupService(IProjectLifecycleManager.class)).thenReturn(projectLifecycleManager);
        when(repositoryService.getConnection(any(ILocation.class))).thenReturn(repositoryConnection);
        when(repositoryService.getReadOnlyConnection(any(ILocation.class))).thenReturn(repositoryReadOnlyConnection);

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

        verify(repositoryConnection, atLeast(1)).create(any(ILocation.class), any(ByteArrayInputStream.class));
    }

    @Test
    void testReadTemplateHashSuccess() {
        String templateId = "testTemplate";
        String expectedHash = "abc123def456";
        byte[] hashBytes = expectedHash.getBytes(StandardCharsets.UTF_8);

        when(repositoryReadOnlyConnection.exists(any(ILocation.class))).thenReturn(true);
        when(repositoryReadOnlyConnection.getContent(any(ILocation.class)))
                .thenReturn(new ByteArrayInputStream(hashBytes));

        streamUtilsMockedStatic.when(() -> StreamUtils.suckStream(any(InputStream.class), eq(true)))
                .thenReturn(hashBytes);

        String result = service.readTemplateHash(templateId);

        assertEquals(expectedHash, result);
    }

    @Test
    void testSaveTemplateHashCreate() {
        String templateId = "testTemplate";
        String templateHash = "abc123";
        ILocation templateFolder = TEMPLATES_ROOT_REPO.append(templateId);
        ILocation hashLocation = templateFolder.append(TEMPLATE_HASH_FILE);

        when(repositoryConnection.exists(hashLocation)).thenReturn(false);
        doNothing().when(repositoryConnection).create(eq(hashLocation), any(InputStream.class));

        service.saveTemplateHash(repositoryConnection, templateFolder, templateHash, StandardCharsets.UTF_8);

        verify(repositoryConnection).exists(hashLocation);
        verify(repositoryConnection).create(eq(hashLocation), any(InputStream.class));
        verify(repositoryConnection, never()).setContent(any(), any());
    }

    @Test
    void testSaveTemplateHashUpdate() {
        String templateId = "testTemplate";
        String templateHash = "abc123";
        ILocation templateFolder = TEMPLATES_ROOT_REPO.append(templateId);
        ILocation hashLocation = templateFolder.append(TEMPLATE_HASH_FILE);

        when(repositoryConnection.exists(hashLocation)).thenReturn(true);
        doNothing().when(repositoryConnection).setContent(eq(hashLocation), any(InputStream.class));

        service.saveTemplateHash(repositoryConnection, templateFolder, templateHash, StandardCharsets.UTF_8);

        verify(repositoryConnection).exists(hashLocation);
        verify(repositoryConnection).setContent(eq(hashLocation), any(InputStream.class));
        verify(repositoryConnection, never()).create(any(), any());
    }

    @Test
    void testSaveTemplateHashIOException() {
        String templateId = "testTemplate";
        String templateHash = "abc123";
        ILocation templateFolder = TEMPLATES_ROOT_REPO.append(templateId);
        ILocation hashLocation = templateFolder.append(TEMPLATE_HASH_FILE);

        when(repositoryConnection.exists(hashLocation)).thenReturn(false);
        doThrow(new RuntimeException("IO error")).when(repositoryConnection)
                .create(eq(hashLocation), any(InputStream.class));

        assertThrows(RuntimeException.class,
                () -> service.saveTemplateHash(repositoryConnection, templateFolder, templateHash, StandardCharsets.UTF_8));

        verify(repositoryConnection).exists(hashLocation);
        verify(repositoryConnection).create(eq(hashLocation), any(InputStream.class));
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

        when(repositoryReadOnlyConnection.exists(any(ILocation.class))).thenReturn(true);
        when(repositoryReadOnlyConnection.getContent(any(ILocation.class)))
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

    @Test
    @SneakyThrows
    void testSaveProjectTemplateWithFallbackCharset() {
        String templateId = "testTemplate";
        byte[] zipData = createTestZipDataWithSpecificCharset(FALLBACK_CHARSET);
        InputStream inputStream = new ByteArrayInputStream(zipData);
        byte[] fileContent = "test content".getBytes(FALLBACK_CHARSET);

        streamUtilsMockedStatic.when(() -> StreamUtils.suckStream(eq(inputStream), eq(true)))
                .thenReturn(zipData);

        streamUtilsMockedStatic.when(() -> StreamUtils.suckStream(any(InputStream.class), eq(false)))
                .thenReturn(fileContent);

        when(repositoryConnection.exists(any(ILocation.class))).thenReturn(false);

        // Mock canProcessZip to return false for UTF-8 and true for fallback
        ProjectTemplateService spyService = spy(service);
        doReturn(false).when(spyService).canProcessZip(zipData, StandardCharsets.UTF_8);
        doReturn(true).when(spyService).canProcessZip(zipData, FALLBACK_CHARSET);

        spyService.saveProjectTemplate(templateId, inputStream, null);

        verify(repositoryConnection).makeFolders(any(ILocation.class));
        verify(projectLifecycleManager).saveProjectTemplate(eq(templateId), any(), isNull());
    }

    @Test
    void testSaveProjectTemplateInvalidZip() {
        String templateId = "testTemplate";
        byte[] invalidZipData = "not a zip file".getBytes(StandardCharsets.UTF_8);
        InputStream inputStream = new ByteArrayInputStream(invalidZipData);

        streamUtilsMockedStatic.when(() -> StreamUtils.suckStream(eq(inputStream), eq(true)))
                .thenReturn(invalidZipData);

        ProjectTemplateService spyService = spy(service);
        doReturn(false).when(spyService).canProcessZip(invalidZipData, StandardCharsets.UTF_8);
        doReturn(false).when(spyService).canProcessZip(invalidZipData, FALLBACK_CHARSET);

        // Should complete without throwing exception but won't save anything
        spyService.saveProjectTemplate(templateId, inputStream, null);

        verify(projectLifecycleManager, never()).saveProjectTemplate(anyString(), any(), any());
    }

    @Test
    void testSaveTemplateHashUpdateExisting() {
        String templateId = "testTemplate";
        String templateHash = "newhash123";
        ILocation templateFolder = TEMPLATES_ROOT_REPO.append(templateId);
        ILocation hashLocation = templateFolder.append(TEMPLATE_HASH_FILE);

        when(repositoryConnection.exists(hashLocation)).thenReturn(true);
        doNothing().when(repositoryConnection).setContent(eq(hashLocation), any(InputStream.class));

        service.saveTemplateHash(repositoryConnection, templateFolder, templateHash, StandardCharsets.UTF_8);

        verify(repositoryConnection).exists(hashLocation);
        verify(repositoryConnection).setContent(eq(hashLocation), any(InputStream.class));
        verify(repositoryConnection, never()).create(any(), any());
    }

    @Test
    void testSaveTemplateHashIOExceptionOnUpdate() {
        String templateId = "testTemplate";
        String templateHash = "abc123";
        ILocation templateFolder = TEMPLATES_ROOT_REPO.append(templateId);
        ILocation hashLocation = templateFolder.append(TEMPLATE_HASH_FILE);

        when(repositoryConnection.exists(hashLocation)).thenReturn(true);
        doThrow(new RuntimeException("Update failed")).when(repositoryConnection)
                .setContent(eq(hashLocation), any(InputStream.class));

        assertThrows(RuntimeException.class,
                () -> service.saveTemplateHash(repositoryConnection, templateFolder, templateHash, StandardCharsets.UTF_8));

        verify(repositoryConnection).exists(hashLocation);
        verify(repositoryConnection).setContent(eq(hashLocation), any(InputStream.class));
    }

    @Test
    @SneakyThrows
    void testProcessZipEntryDirectory() {
        String templateId = "testTemplate";
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            ZipEntry dirEntry = new ZipEntry("testdir/");
            zos.putNextEntry(dirEntry);
            zos.closeEntry();
        }
        byte[] zipData = baos.toByteArray();

        streamUtilsMockedStatic.when(() -> StreamUtils.suckStream(any(InputStream.class), eq(true)))
                .thenReturn(zipData);

        InputStream inputStream = new ByteArrayInputStream(zipData);
        when(repositoryConnection.exists(any(ILocation.class))).thenReturn(false);

        service.saveProjectTemplate(templateId, inputStream, null);

        verify(repositoryConnection, atLeastOnce()).makeFolders(any(ILocation.class));
    }

    @Test
    void testDownloadProjectIOException() {
        String projectId = "testProject";

        when(repositoryConnection.getSubLocations(any(ILocation.class), eq(true)))
                .thenThrow(new RuntimeException("Connection error"));

        assertThrows(RuntimeException.class,
                () -> service.downloadProject(projectId));
    }

    @Test
    void testCreateZipEntryFolder() {
        ILocation location = mock(ILocation.class);
        ILocation projectRoot = mock(ILocation.class);
        ILocation relativeLoc = mock(ILocation.class);

        when(location.getRelativeLocation(projectRoot)).thenReturn(relativeLoc);
        when(relativeLoc.getLocationPath()).thenReturn("testdir");
        when(repositoryConnection.isFolder(location)).thenReturn(true);
        when(repositoryConnection.isFile(location)).thenReturn(false);

        ZipEntry entry = service.createZipEntry(repositoryConnection, location, projectRoot);

        assertNotNull(entry);
        assertEquals("testdir/", entry.getName());
    }

    @Test
    void testCreateZipEntryFile() {
        ILocation location = mock(ILocation.class);
        ILocation projectRoot = mock(ILocation.class);
        ILocation relativeLoc = mock(ILocation.class);

        when(location.getRelativeLocation(projectRoot)).thenReturn(relativeLoc);
        when(relativeLoc.getLocationPath()).thenReturn("test.txt");
        when(repositoryConnection.isFolder(location)).thenReturn(false);
        when(repositoryConnection.isFile(location)).thenReturn(true);

        ZipEntry entry = service.createZipEntry(repositoryConnection, location, projectRoot);

        assertNotNull(entry);
        assertEquals("test.txt", entry.getName());
    }

    @Test
    void testCreateZipEntryNeither() {
        ILocation location = mock(ILocation.class);
        ILocation projectRoot = mock(ILocation.class);
        ILocation relativeLoc = mock(ILocation.class);

        when(location.getRelativeLocation(projectRoot)).thenReturn(relativeLoc);
        when(relativeLoc.getLocationPath()).thenReturn("unknown");
        when(repositoryConnection.isFolder(location)).thenReturn(false);
        when(repositoryConnection.isFile(location)).thenReturn(false);

        ZipEntry entry = service.createZipEntry(repositoryConnection, location, projectRoot);

        assertNull(entry);
    }

    @Test
    @SneakyThrows
    void testAddEntryToZipWithContent() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(baos);
        ZipEntry entry = new ZipEntry("test.txt");
        byte[] content = "test content".getBytes(StandardCharsets.UTF_8);
        InputStream is = new ByteArrayInputStream(content);

        streamUtilsMockedStatic.when(() -> StreamUtils.copy(eq(is), eq(zos)))
                .thenAnswer(invocation -> null);

        service.addEntryToZip(zos, is, entry);

        zos.close();
        assertTrue(baos.size() > 0);
    }

    @Test
    @SneakyThrows
    void testAddEntryToZipDirectory() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(baos);
        ZipEntry entry = new ZipEntry("testdir/");

        service.addEntryToZip(zos, null, entry);

        zos.close();
        assertTrue(baos.size() > 0);
    }

    @Test
    void testNormalizeEntryNameMultipleLeadingSlashes() {
        assertEquals("path/to/file.txt", service.normalizeEntryName("////path/to/file.txt"));
    }

    @Test
    void testNormalizeEntryNameMixedSlashes() {
        assertEquals("path/to/file.txt", service.normalizeEntryName("\\path\\to/file.txt"));
    }

    @Test
    void testNormalizeEntryNameOnlySlashes() {
        assertNull(service.normalizeEntryName("///"));
    }

    @Test
    void testNormalizeEntryNameParentDirectoryInMiddle() {
        assertNull(service.normalizeEntryName("path/../file.txt"));
    }

    @Test
    void testNormalizeEntryNameParentDirectoryAtEnd() {
        assertNull(service.normalizeEntryName("path/.."));
    }

    @Test
    void testReadTemplateHashConnectionException() {
        String templateId = "testTemplate";

        when(repositoryReadOnlyConnection.exists(any(ILocation.class)))
                .thenThrow(new RuntimeException("Connection failed"));

        assertThrows(RuntimeException.class,
                () -> service.readTemplateHash(templateId));
    }

    @Test
    @SneakyThrows
    void testSaveProjectTemplateWithEmptyHash() {
        String templateId = "testTemplate";
        String emptyHash = "   ";
        byte[] zipData = createTestZipData();
        InputStream inputStream = new ByteArrayInputStream(zipData);
        byte[] fileContent = "test content".getBytes(StandardCharsets.UTF_8);

        streamUtilsMockedStatic.when(() -> StreamUtils.suckStream(eq(inputStream), eq(true)))
                .thenReturn(zipData);

        streamUtilsMockedStatic.when(() -> StreamUtils.suckStream(any(InputStream.class), eq(false)))
                .thenReturn(fileContent);

        when(repositoryConnection.exists(any(ILocation.class))).thenReturn(false);

        service.saveProjectTemplate(templateId, inputStream, emptyHash);

        // Should not save hash file for empty/whitespace hash
        verify(repositoryConnection, never()).create(
                argThat(loc -> loc.toString().contains(TEMPLATE_HASH_FILE)),
                any(InputStream.class));
    }

    @Test
    void testDownloadProjectWithMultipleFiles() {
        String projectId = "testProject";
        ILocation projectLocation = mock(ILocation.class);
        ILocation file1 = mock(ILocation.class);
        ILocation file2 = mock(ILocation.class);
        ILocation relLoc1 = mock(ILocation.class);
        ILocation relLoc2 = mock(ILocation.class);

        when(repositoryConnection.getSubLocations(any(ILocation.class), eq(true)))
                .thenReturn(List.of(file1, file2));

        when(repositoryConnection.isFile(file1)).thenReturn(true);
        when(repositoryConnection.isFile(file2)).thenReturn(true);
        when(repositoryConnection.isFolder(file1)).thenReturn(false);
        when(repositoryConnection.isFolder(file2)).thenReturn(false);

        when(file1.getRelativeLocation(any())).thenReturn(relLoc1);
        when(file2.getRelativeLocation(any())).thenReturn(relLoc2);
        when(relLoc1.getLocationPath()).thenReturn("file1.txt");
        when(relLoc2.getLocationPath()).thenReturn("file2.txt");

        when(repositoryConnection.getContent(file1))
                .thenReturn(new ByteArrayInputStream("content1".getBytes()));
        when(repositoryConnection.getContent(file2))
                .thenReturn(new ByteArrayInputStream("content2".getBytes()));

        streamUtilsMockedStatic.when(() -> StreamUtils.copy(any(InputStream.class), any()))
                .thenAnswer(invocation -> null);

        byte[] result = service.downloadProject(projectId);

        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    void testCreateZipFromProjectNullConnection() {
        ILocation projectLocation = mock(ILocation.class);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(baos);

        when(repositoryService.getConnection(projectLocation)).thenReturn(null);

        IOException exception = assertThrows(IOException.class,
                () -> service.createZipFromProject(projectLocation, zos));

        assertTrue(exception.getMessage().contains("Failed to establish repository connection"));
    }

    @Test
    void testCreateZipFromProjectLocationProcessingFailure() {
        ILocation projectLocation = mock(ILocation.class);
        ILocation file = mock(ILocation.class);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(baos);

        when(repositoryConnection.getSubLocations(projectLocation, true))
                .thenReturn(List.of(file));
        when(repositoryConnection.isFile(file)).thenReturn(true);
        when(file.getRelativeLocation(projectLocation))
                .thenThrow(new RuntimeException("Location error"));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> service.createZipFromProject(projectLocation, zos));

        assertTrue(exception.getMessage().contains("Location error"));
    }

    @Test
    void testCanProcessZipWithCorruptedData() {
        byte[] corruptedData = new byte[]{0x50, 0x4b, 0x03, 0x04, 0x00, 0x00}; // Partial ZIP header
        assertFalse(service.canProcessZip(corruptedData, StandardCharsets.UTF_8));
    }

    @Test
    @SneakyThrows
    void testSaveZipProjectTemplatesCleanupOnException() {
        String templateId = "testTemplate";
        byte[] zipData = createTestZipData();
        ILocation templateFolder = TEMPLATES_ROOT_REPO.append(templateId);

        // First call for recreateTemplateFolder, second for cleanupTemplateFolder
        when(repositoryConnection.exists(templateFolder)).thenReturn(false, true);
        doThrow(new RuntimeException("Save failed"))
                .when(projectLifecycleManager).saveProjectTemplate(anyString(), any(), any());

        assertThrows(ProjectTemplateService.TemplateProcessingException.class,
                () -> service.saveZipProjectTemplates(templateId, zipData, null, StandardCharsets.UTF_8));

        verify(repositoryConnection).delete(templateFolder);
    }

    private byte[] createTestZipDataWithSpecificCharset(Charset charset) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos, charset)) {
            ZipEntry entry = new ZipEntry("test.txt");
            zos.putNextEntry(entry);
            zos.write("test content".getBytes(charset));
            zos.closeEntry();
        }
        return baos.toByteArray();
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

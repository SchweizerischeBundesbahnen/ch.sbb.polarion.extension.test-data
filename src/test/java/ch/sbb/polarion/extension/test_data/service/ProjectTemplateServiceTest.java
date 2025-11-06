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
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
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

        streamUtilsMockedStatic.when(() -> StreamUtils.suckStream(any(ZipInputStream.class), eq(false)))
                .thenReturn(fileContent);

        when(repositoryConnection.exists(any(ILocation.class))).thenReturn(false);

        service.saveProjectTemplate(templateId, inputStream, null);

        verify(repositoryConnection).makeFolders(any(ILocation.class));
        verify(repositoryConnection).create(any(ILocation.class), any(ByteArrayInputStream.class));
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

        streamUtilsMockedStatic.when(() -> StreamUtils.suckStream(any(ZipInputStream.class), eq(false)))
                .thenReturn(fileContent);

        when(repositoryConnection.exists(any(ILocation.class))).thenReturn(false);

        service.saveProjectTemplate(templateId, inputStream, templateHash);

        verify(repositoryConnection, atLeastOnce()).create(any(ILocation.class), any(ByteArrayInputStream.class));
    }

    @Test
    void testSaveProjectTemplateNullTemplateId() {
        InputStream inputStream = new ByteArrayInputStream(new byte[0]);

        assertThrows(IllegalArgumentException.class,
                () -> service.saveProjectTemplate(null, inputStream, null));
    }

    @Test
    void testSaveProjectTemplateEmptyTemplateId() {
        InputStream inputStream = new ByteArrayInputStream(new byte[0]);

        assertThrows(IllegalArgumentException.class,
                () -> service.saveProjectTemplate("  ", inputStream, null));
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
    void testReadTemplateHashNullTemplateId() {
        assertThrows(IllegalArgumentException.class,
                () -> service.readTemplateHash(null));
    }

    @Test
    void testReadTemplateHashEmptyTemplateId() {
        assertThrows(IllegalArgumentException.class,
                () -> service.readTemplateHash("  "));
    }

    @Test
    void testReadTemplateHashIOException() {
        String templateId = "testTemplate";

        when(repositoryConnection.exists(any(ILocation.class))).thenReturn(true);
        when(repositoryConnection.getContent(any(ILocation.class)))
                .thenThrow(new RuntimeException("Connection error"));

        assertThrows(ProjectTemplateService.TemplateProcessingException.class,
                () -> service.readTemplateHash(templateId));
    }

    @Test
    void testSaveProjectTemplateThrowsTemplateProcessingException() {
        String templateId = "testTemplate";
        InputStream inputStream = mock(InputStream.class);

        // Simulate StreamUtils.suckStream throwing an exception
        streamUtilsMockedStatic.when(() -> StreamUtils.suckStream(any(InputStream.class), eq(true)))
                .thenThrow(new RuntimeException("Stream error"));

        assertThrows(ProjectTemplateService.TemplateProcessingException.class,
                () -> service.saveProjectTemplate(templateId, inputStream, null));
    }

    @Test
    void testReadTemplateHashThrowsTemplateProcessingException() {
        String templateId = "testTemplate";
        when(repositoryConnection.exists(any(ILocation.class))).thenReturn(true);
        when(repositoryConnection.getContent(any(ILocation.class)))
                .thenThrow(new RuntimeException("IO error"));

        assertThrows(ProjectTemplateService.TemplateProcessingException.class,
                () -> service.readTemplateHash(templateId));
    }

    private byte[] createTestZipData() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            ZipEntry entry = new ZipEntry("test/file.txt");
            zos.putNextEntry(entry);
            zos.write("test content".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
        return baos.toByteArray();
    }
}

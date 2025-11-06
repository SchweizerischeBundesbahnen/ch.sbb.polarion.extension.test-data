package ch.sbb.polarion.extension.test_data.service;

import com.polarion.alm.projects.IProjectLifecycleManager;
import com.polarion.core.util.StreamUtils;
import com.polarion.platform.core.IPlatform;
import com.polarion.platform.core.PlatformContext;
import com.polarion.platform.service.repository.IRepositoryConnection;
import com.polarion.platform.service.repository.IRepositoryService;
import com.polarion.subterra.base.location.ILocation;
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
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ProjectTemplateServiceTest {
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
    void testSaveProjectTemplateSuccess() throws Exception {
        String templateId = "testTemplate";
        byte[] zipData = createTestZipData();
        InputStream inputStream = new ByteArrayInputStream(zipData);
        byte[] fileContent = "test content".getBytes(StandardCharsets.UTF_8);

        // Mock the initial stream reading
        streamUtilsMockedStatic.when(() -> StreamUtils.suckStream(eq(inputStream), eq(true)))
                .thenReturn(zipData);

        // Mock reading content from ZIP entries
        streamUtilsMockedStatic.when(() -> StreamUtils.suckStream(any(ZipInputStream.class), eq(false)))
                .thenReturn(fileContent);

        when(repositoryConnection.exists(any(ILocation.class))).thenReturn(false);

        service.saveProjectTemplate(templateId, inputStream, null);

        verify(repositoryConnection).makeFolders(any(ILocation.class));
        verify(repositoryConnection).create(any(ILocation.class), any(ByteArrayInputStream.class));
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

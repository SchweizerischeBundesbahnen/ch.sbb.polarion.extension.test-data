package ch.sbb.polarion.extension.test_data.service;

import com.polarion.alm.projects.IProjectLifecycleManager;
import com.polarion.core.util.StreamUtils;
import com.polarion.core.util.StringUtils;
import com.polarion.platform.core.PlatformContext;
import com.polarion.platform.service.repository.IRepositoryConnection;
import com.polarion.platform.service.repository.IRepositoryReadOnlyConnection;
import com.polarion.platform.service.repository.IRepositoryService;
import com.polarion.subterra.base.location.ILocation;
import com.polarion.subterra.base.location.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Service for managing Polarion project templates.
 * Handles template upload, download, and storage operations.
 */
public class ProjectTemplateService {

    protected static final ILocation TEMPLATES_ROOT_REPO =
            Location.getLocationWithRepository(IRepositoryService.DEFAULT, "/.polarion/projects/templates/");
    protected static final String TEMPLATE_HASH_FILE = ".templatehash";

    private final IRepositoryService repositoryService;
    private final IProjectLifecycleManager projectLifecycleManager;

    public ProjectTemplateService() {
        this.repositoryService = PlatformContext.getPlatform().lookupService(IRepositoryService.class);
        this.projectLifecycleManager = PlatformContext.getPlatform().lookupService(IProjectLifecycleManager.class);
    }

    /**
     * Saves a project template from an input stream.
     *
     * @param templateId   The unique identifier for the template
     * @param inputStream  The input stream containing the template zip file
     * @param templateHash The hash value of the template, used for integrity verification
     * @throws IllegalArgumentException    if templateId is null or empty
     * @throws TemplateProcessingException if template processing fails
     */
    public void saveProjectTemplate(@NotNull String templateId, @NotNull InputStream inputStream, @Nullable String templateHash) {
        try {
            byte[] zipData = StreamUtils.suckStream(inputStream, true);
            if (canProcessZip(zipData)) {
                saveZipProjectTemplates(templateId, zipData, templateHash);
            } else {
                throw new TemplateProcessingException("Provided input stream is not a valid ZIP archive for template: " + templateId, null);
            }
        } catch (Exception e) {
            throw new TemplateProcessingException("Failed to save project template: " + templateId, e);
        }
    }

    /**
     * Downloads a project template as a ZIP archive from the repository.
     *
     * @param projectId    the unique identifier of the project to download
     * @param projectGroup the group of the project
     * @return byte array containing the compressed template
     * @throws IllegalArgumentException    if projectId is null or empty
     * @throws TemplateProcessingException if the repository connection fails or ZIP creation fails
     */
    @NotNull
    public byte[] downloadProject(@NotNull String projectId, @Nullable String projectGroup) {
        validateProjectId(projectId);

        String path = StringUtils.isEmpty(projectGroup)
                ? String.format("/%s", projectId)
                : String.format("/%s/%s", projectGroup, projectId);

        ILocation projectLocation = Location.getLocationWithRepository(IRepositoryService.DEFAULT, path);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos)) {
            createZipFromProject(projectLocation, zos);
            zos.finish();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new TemplateProcessingException("Failed to download template: " + projectId, e);
        }
    }

    /**
     * Reads the stored hash for a template.
     *
     * @param templateId The unique identifier of the template
     * @return The stored hash, or null if hash file doesn't exist
     * @throws TemplateProcessingException if reading fails
     */
    @Nullable
    public String readTemplateHash(@NotNull String templateId) {
        IRepositoryReadOnlyConnection connection = repositoryService.getReadOnlyConnection(TEMPLATES_ROOT_REPO);
        ILocation templateFolder = TEMPLATES_ROOT_REPO.append(templateId);
        ILocation hashLocation = templateFolder.append(TEMPLATE_HASH_FILE);

        if (!connection.exists(hashLocation)) {
            return null;
        }

        try (InputStream is = connection.getContent(hashLocation)) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new TemplateProcessingException("Failed to read template hash: " + templateId, e);
        }
    }

    @VisibleForTesting
    @SuppressWarnings("java:S5042")
    boolean canProcessZip(byte[] zipData) {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(zipData);
             ZipInputStream zis = new ZipInputStream(bais, StandardCharsets.UTF_8)) {
            return zis.getNextEntry() != null;
        } catch (IOException e) {
            return false;
        }
    }

    @VisibleForTesting
    @SuppressWarnings("java:S5042")
    void saveZipProjectTemplates(@NotNull String templateId, @NotNull byte[] zipData,
                                 @Nullable String templateHash) {
        IRepositoryConnection connection = repositoryService.getConnection(TEMPLATES_ROOT_REPO);
        ILocation templateFolder = TEMPLATES_ROOT_REPO.append(templateId);

        try {
            recreateTemplateFolder(connection, templateFolder);

            Properties properties = new Properties();
            projectLifecycleManager.saveProjectTemplate(templateId, properties, null);

            extractZipEntries(zipData, connection, templateFolder);

            if (templateHash != null && !templateHash.trim().isEmpty()) {
                saveTemplateHash(connection, templateFolder, templateHash);
            }

        } catch (Exception e) {
            cleanupTemplateFolder(connection, templateFolder);
            throw new TemplateProcessingException("Failed to process zip file for template: " + templateId, e);
        }
    }

    @VisibleForTesting
    void saveTemplateHash(@NotNull IRepositoryConnection connection, @NotNull ILocation templateFolder,
                          @NotNull String templateHash) {
        ILocation templateHashLocation = templateFolder.append(TEMPLATE_HASH_FILE);
        try (InputStream inputStream = new ByteArrayInputStream(templateHash.getBytes(StandardCharsets.UTF_8))) {
            if (connection.exists(templateHashLocation)) {
                connection.setContent(templateHashLocation, inputStream);
            } else {
                connection.create(templateHashLocation, inputStream);
            }
        } catch (IOException e) {
            throw new TemplateProcessingException("Failed to save template hash for template: " + templateFolder.getLocationPath(), e);
        }
    }

    @SuppressWarnings("java:S5042")
    private void extractZipEntries(byte @NotNull [] zipData,
                                   @NotNull IRepositoryConnection connection, @NotNull ILocation templateFolder) throws IOException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(zipData);
             ZipInputStream zis = new ZipInputStream(bais, StandardCharsets.UTF_8)) {

            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                processZipEntry(entry, zis, connection, templateFolder);
                zis.closeEntry();
            }
        }
    }

    private void processZipEntry(@NotNull ZipEntry entry, @NotNull ZipInputStream zis,
                                 @NotNull IRepositoryConnection connection, @NotNull ILocation templateFolder) throws IOException {
        String entryName = normalizeEntryName(entry.getName());

        if (entryName == null) {
            return;
        }

        ILocation fileLocation = templateFolder.append(entryName);

        if (entry.isDirectory()) {
            connection.makeFolders(fileLocation);
        } else {
            byte[] content = StreamUtils.suckStream(zis, false);
            connection.create(fileLocation, new ByteArrayInputStream(content));
        }
    }

    @VisibleForTesting
    String normalizeEntryName(@Nullable String entryName) {
        if (entryName == null || entryName.isEmpty()) {
            return null;
        }

        entryName = entryName.replace("\\", "/");

        while (entryName.startsWith("/")) {
            entryName = entryName.substring(1);
        }

        if (entryName.isEmpty() || entryName.contains("..")) {
            return null;
        }

        return entryName;
    }

    @VisibleForTesting
    void createZipFromProject(@NotNull ILocation projectRepo, @NotNull ZipOutputStream zos) throws IOException {
        IRepositoryConnection connection = repositoryService.getConnection(projectRepo);

        if (connection == null) {
            throw new IOException("Failed to establish repository connection for: " + projectRepo);
        }

        for (Object object : connection.getSubLocations(projectRepo, true)) {
            if (!(object instanceof ILocation location)) {
                continue;
            }

            try {
                ZipEntry entry = createZipEntry(connection, location, projectRepo);
                if (entry != null) {
                    if (connection.isFile(location)) {
                        try (InputStream is = connection.getContent(location)) {
                            addEntryToZip(zos, is, entry);
                        }
                    } else {
                        addEntryToZip(zos, null, entry);
                    }
                }
            } catch (IOException e) {
                throw new IOException("Failed to process location: " + location.getLocationPath(), e);
            }
        }
    }

    @VisibleForTesting
    @Nullable
    ZipEntry createZipEntry(@NotNull IRepositoryConnection connection, @NotNull ILocation location,
                            @NotNull ILocation projectRoot) {
        String relativePath = location.getRelativeLocation(projectRoot).getLocationPath();

        if (connection.isFolder(location)) {
            return new ZipEntry(relativePath + "/");
        }

        if (connection.isFile(location)) {
            return new ZipEntry(relativePath);
        }

        return null;
    }

    @VisibleForTesting
    void addEntryToZip(@NotNull ZipOutputStream zos, @Nullable InputStream is, @NotNull ZipEntry entry) throws IOException {
        entry.setMethod(ZipEntry.DEFLATED);
        zos.putNextEntry(entry);
        if (is != null) {
            StreamUtils.copy(is, zos);
        }
        zos.closeEntry();
    }

    private void recreateTemplateFolder(@NotNull IRepositoryConnection connection, @NotNull ILocation templateFolder) {
        if (connection.exists(templateFolder)) {
            connection.delete(templateFolder);
        }
        connection.makeFolders(templateFolder);
    }

    @VisibleForTesting
    void cleanupTemplateFolder(@NotNull IRepositoryConnection connection, @NotNull ILocation templateFolder) {
        try {
            if (connection.exists(templateFolder)) {
                connection.delete(templateFolder);
            }
        } catch (Exception ignored) {
            // Swallow exceptions during cleanup
        }
    }

    private void validateProjectId(@Nullable String projectId) {
        if (projectId == null || projectId.trim().isEmpty()) {
            throw new IllegalArgumentException("Project ID cannot be null or empty");
        }

        String trimmed = projectId.trim();
        if (trimmed.contains("..") || trimmed.contains("/") || trimmed.contains("\\")) {
            throw new IllegalArgumentException("Project ID contains invalid characters");
        }
    }

    /**
     * Custom exception for template processing errors.
     */
    public static class TemplateProcessingException extends RuntimeException {
        public TemplateProcessingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

package ch.sbb.polarion.extension.test_data.service;

import com.polarion.alm.projects.IProjectLifecycleManager;
import com.polarion.core.util.StreamUtils;
import com.polarion.platform.core.PlatformContext;
import com.polarion.platform.service.repository.IRepositoryConnection;
import com.polarion.platform.service.repository.IRepositoryService;
import com.polarion.subterra.base.location.ILocation;
import com.polarion.subterra.base.location.Location;
import org.jetbrains.annotations.VisibleForTesting;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Service for managing Polarion project templates.
 * Handles template upload, download, and storage operations.
 */
public class ProjectTemplateService {

    private static final ILocation TEMPLATES_ROOT_REPO =
            Location.getLocationWithRepository(IRepositoryService.DEFAULT, "/.polarion/projects/templates/");
    private static final Charset FALLBACK_CHARSET = Charset.forName("CP437");

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
    public void saveProjectTemplate(String templateId, InputStream inputStream, String templateHash) {
        validateTemplateId(templateId);

        try {
            byte[] zipData = StreamUtils.suckStream(inputStream, true);
            saveUploadedProjectTemplates(templateId, zipData, templateHash);

        } catch (Exception e) {
            throw new TemplateProcessingException("Failed to save project template: " + templateId, e);
        }
    }

    /**
     * Saves uploaded project templates with charset fallback.
     */
    @VisibleForTesting
    void saveUploadedProjectTemplates(String templateId, byte[] zipData, String templateHash) {
        if (canProcessZip(zipData, StandardCharsets.UTF_8)) {
            saveZipProjectTemplates(templateId, zipData, templateHash, StandardCharsets.UTF_8);
            return;
        }

        if (canProcessZip(zipData, FALLBACK_CHARSET)) {
            saveZipProjectTemplates(templateId, zipData, templateHash, FALLBACK_CHARSET);
            return;
        }

        throw new TemplateProcessingException(
                "Failed to process template with any supported charset", null);
    }

    /**
     * Checks if a ZIP can be processed with the given charset.
     */
    @VisibleForTesting
    @SuppressWarnings("java:S5042")
    boolean canProcessZip(byte[] zipData, Charset charset) {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(zipData);
             ZipInputStream zis = new ZipInputStream(bais, charset)) {
            return zis.getNextEntry() != null;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Extracts and saves ZIP contents to repository.
     */
    @VisibleForTesting
    @SuppressWarnings("java:S5042")
    void saveZipProjectTemplates(String templateId, byte[] zipData, String templateHash, Charset charset) {
        IRepositoryConnection connection = repositoryService.getConnection(TEMPLATES_ROOT_REPO);
        ILocation templateFolder = TEMPLATES_ROOT_REPO.append(templateId);

        recreateTemplateFolder(connection, templateFolder);

        Properties properties = new Properties();
        projectLifecycleManager.saveProjectTemplate(templateId, properties, null);

        if (templateHash != null) {
            ILocation templateHashLocation = templateFolder.append(".templatehash");
            byte[] content = templateHash.getBytes(charset);
            connection.create(templateHashLocation, new ByteArrayInputStream(content));
        }

        try (ByteArrayInputStream bais = new ByteArrayInputStream(zipData);
             ZipInputStream zis = new ZipInputStream(bais, charset)) {

            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                processZipEntry(entry, zis, connection, templateFolder);
            }

        } catch (IOException e) {
            throw new TemplateProcessingException("Failed to process zip file", e);
        }
    }

    /**
     * Recreates the template folder by deleting existing one if present.
     */
    private void recreateTemplateFolder(IRepositoryConnection connection, ILocation templateFolder) {
        if (connection.exists(templateFolder)) {
            connection.delete(templateFolder);
        }
        connection.makeFolders(templateFolder);
    }

    /**
     * Processes a single ZIP entry and saves it to the template folder.
     */
    private void processZipEntry(ZipEntry entry, ZipInputStream zis,
                                 IRepositoryConnection connection, ILocation templateFolder) throws IOException {
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

    /**
     * Normalizes entry name by removing leading directory structure and slashes.
     *
     * @return normalized entry name or null if entry should be skipped
     */
    @VisibleForTesting
    String normalizeEntryName(String entryName) {
        if (entryName == null || entryName.isEmpty()) {
            return null;
        }

        entryName = entryName.replace("\\", "/");

        while (entryName.startsWith("/")) {
            entryName = entryName.substring(1);
        }

        if (entryName.isEmpty()) {
            return null;
        }

        if (entryName.contains("..")) {
            return null;
        }

        return entryName;
    }

    /**
     * Validates template ID is not null or empty.
     */
    private void validateTemplateId(String templateId) {
        if (templateId == null || templateId.trim().isEmpty()) {
            throw new IllegalArgumentException("Template ID cannot be null or empty");
        }
    }


    /**
     * Reads the stored hash for a template.
     *
     * @param templateId The unique identifier of the template
     * @return The stored hash, or null if hash file doesn't exist
     * @throws TemplateProcessingException if reading fails
     */
    public String readTemplateHash(String templateId) {
        validateTemplateId(templateId);

        IRepositoryConnection connection = repositoryService.getConnection(TEMPLATES_ROOT_REPO);
        ILocation templateFolder = TEMPLATES_ROOT_REPO.append(templateId);
        ILocation hashLocation = templateFolder.append(".templatehash");

        if (!connection.exists(hashLocation)) {
            return null;
        }

        try (InputStream is = connection.getContent(hashLocation)) {
            byte[] hashBytes = StreamUtils.suckStream(is, true);
            return new String(hashBytes, StandardCharsets.UTF_8).trim();
        } catch (Exception e) {
            throw new TemplateProcessingException("Failed to read template hash: " + templateId, e);
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

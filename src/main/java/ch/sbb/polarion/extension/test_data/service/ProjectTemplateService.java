package ch.sbb.polarion.extension.test_data.service;

import com.polarion.alm.projects.IProjectLifecycleManager;
import com.polarion.core.util.StreamUtils;
import com.polarion.platform.core.PlatformContext;
import com.polarion.platform.service.repository.IRepositoryConnection;
import com.polarion.platform.service.repository.IRepositoryService;
import com.polarion.subterra.base.location.ILocation;
import com.polarion.subterra.base.location.Location;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
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
     * @param templateId  The unique identifier for the template
     * @param inputStream The input stream containing the template zip file
     * @throws IllegalArgumentException     if templateId is null or empty
     * @throws TemplateProcessingException if template processing fails
     */
    public void saveProjectTemplate(String templateId, InputStream inputStream) {
        validateTemplateId(templateId);

        try {
            byte[] zipData = StreamUtils.suckStream(inputStream, true);

            Properties properties = new Properties();
            projectLifecycleManager.saveProjectTemplate(templateId, properties, null);

            saveUploadedProjectTemplates(templateId, zipData);

        } catch (Exception e) {
            throw new TemplateProcessingException("Failed to save project template: " + templateId, e);
        }
    }

    /**
     * Downloads a project template as a ZIP archive.
     *
     * @param templateId The unique identifier of the template to download
     * @return byte array containing the zipped template
     * @throws IllegalArgumentException     if templateId is null or empty
     * @throws TemplateProcessingException if template doesn't exist or download fails
     */
    public byte[] downloadTemplate(String templateId) {
        validateTemplateId(templateId);

        ILocation templateLocation = TEMPLATES_ROOT_REPO.append(templateId);
        validateTemplateExists(templateLocation);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos)) {
            createZipFromTemplate(templateLocation, zos);
            zos.finish();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new TemplateProcessingException("Failed to download template: " + templateId, e);
        }
    }

    /**
     * Writes a project template to an existing ZipOutputStream.
     *
     * @param zos            The output stream to write to
     * @param templateId     The template identifier
     * @param defaultTemplate Flag indicating if this is a default template (currently unused)
     */
    public void writeProjectTemplateTo(ZipOutputStream zos, String templateId, boolean defaultTemplate) {
        validateTemplateId(templateId);
        ILocation templateLocation = TEMPLATES_ROOT_REPO.append(templateId);

        try {
            createZipFromTemplate(templateLocation, zos);
        } catch (IOException e) {
            throw new TemplateProcessingException("Failed to write template to stream: " + templateId, e);
        }
    }

    /**
     * Creates a ZIP archive from a template stored in the repository.
     */
    private void createZipFromTemplate(ILocation templateRootRepo, ZipOutputStream zos) throws IOException {
        IRepositoryConnection connection = repositoryService.getConnection(TEMPLATES_ROOT_REPO);

        for (Object object : connection.getSubLocations(templateRootRepo, true)) {
            ILocation location = (ILocation) object;

            try (InputStream is = connection.getContent(location)) {
                ZipEntry entry = createZipEntry(connection, location);

                if (entry != null) {
                    addEntryToZip(zos, is, entry);
                }
            }
        }
    }

    /**
     * Creates a ZipEntry for a given repository location.
     */
    private ZipEntry createZipEntry(IRepositoryConnection connection, ILocation location) {
        String relativePath = location.getRelativeLocation(TEMPLATES_ROOT_REPO).getLocationPath();

        if (connection.isFolder(location)) {
            return new ZipEntry(relativePath + "/");
        }

        if (connection.isFile(location)) {
            return new ZipEntry(relativePath);
        }

        return null;
    }

    /**
     * Adds an entry to the ZIP output stream.
     */
    private void addEntryToZip(ZipOutputStream zos, InputStream is, ZipEntry entry) throws IOException {
        entry.setMethod(ZipEntry.DEFLATED);
        zos.putNextEntry(entry);
        StreamUtils.copy(is, zos);
    }

    /**
     * Saves uploaded project templates with charset fallback.
     */
    private void saveUploadedProjectTemplates(String templateId, byte[] zipData) {
        if (canProcessZip(zipData, StandardCharsets.UTF_8)) {
            saveZipProjectTemplates(templateId, zipData, StandardCharsets.UTF_8);
            return;
        }

        if (canProcessZip(zipData, FALLBACK_CHARSET)) {
            saveZipProjectTemplates(templateId, zipData, FALLBACK_CHARSET);
            return;
        }

        throw new TemplateProcessingException(
                "Failed to process template with any supported charset", null);
    }

    /**
     * Checks if a ZIP can be processed with the given charset.
     */
    private boolean canProcessZip(byte[] zipData, Charset charset) {
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
    private void saveZipProjectTemplates(String templateId, byte[] zipData, Charset charset) {
        IRepositoryConnection connection = repositoryService.getConnection(TEMPLATES_ROOT_REPO);
        ILocation templateFolder = TEMPLATES_ROOT_REPO.append(templateId);

        recreateTemplateFolder(connection, templateFolder);

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
    private String normalizeEntryName(String entryName) {
        int firstSlash = entryName.indexOf("/");
        if (firstSlash >= 0) {
            entryName = entryName.substring(firstSlash);
        }

        if (entryName.equals("/") || entryName.isEmpty()) {
            return null;
        }

        return entryName.startsWith("/") ? entryName.substring(1) : entryName;
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
     * Validates that template exists in repository.
     */
    private void validateTemplateExists(ILocation templateLocation) {
        IRepositoryConnection connection = repositoryService.getConnection(TEMPLATES_ROOT_REPO);
        if (!connection.exists(templateLocation)) {
            throw new TemplateProcessingException(
                    "Template not found: " + templateLocation.getLastComponent(), null);
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

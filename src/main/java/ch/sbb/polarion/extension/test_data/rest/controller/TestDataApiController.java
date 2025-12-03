package ch.sbb.polarion.extension.test_data.rest.controller;

import ch.sbb.polarion.extension.generic.rest.filter.Secured;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;

import javax.ws.rs.Path;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

@Secured
@Path("/api")
public class TestDataApiController extends TestDataInternalController {

    @Override
    public Response createDocumentWithGeneratedWorkItems(String projectId, String spaceId, String documentName, Integer quantity) {
        return polarionService.callPrivileged(() -> super.createDocumentWithGeneratedWorkItems(projectId, spaceId, documentName, quantity));
    }

    @Override
    public Response extendDocumentWithGeneratedWorkItems(String projectId, String spaceId, String documentName, Integer quantity) {
        return polarionService.callPrivileged(() -> super.extendDocumentWithGeneratedWorkItems(projectId, spaceId, documentName, quantity));
    }

    @Override
    public Response changeDocumentWorkItemDescriptions(String projectId, String spaceId, String documentName, Integer interval) {
        return polarionService.callPrivileged(() -> super.changeDocumentWorkItemDescriptions(projectId, spaceId, documentName, interval));
    }

    @Override
    public Response saveProjectTemplate(String templateId, String templateHash, FormDataBodyPart file) {
        return polarionService.callPrivileged(() -> super.saveProjectTemplate(templateId, templateHash, file));
    }

    @Override
    public Response getTemplateHash(String templateId) {
        return polarionService.callPrivileged(() -> super.getTemplateHash(templateId));
    }

    @Override
    public Response downloadProjectTemplate(String projectId) {
        return polarionService.callPrivileged(() -> super.downloadProjectTemplate(projectId));
    }

    @Override
    public Response createWikiPage(String projectId, String spaceId, String name) {
        return polarionService.callPrivileged(() -> super.createWikiPage(projectId, spaceId, name));
    }

    @Override
    public Response deleteWikiPage(String projectId, String spaceId, String name) {
        return polarionService.callPrivileged(() -> super.deleteWikiPage(projectId, spaceId, name));
    }

    @Override
    public Response createWikiPageInGlobalRepository(String spaceId, String name) {
        return polarionService.callPrivileged(() -> super.createWikiPageInGlobalRepository(spaceId, name));
    }

    @Override
    public Response deleteWikiPageFromGlobalRepository(String spaceId, String name) {
        return polarionService.callPrivileged(() -> super.deleteWikiPageFromGlobalRepository(spaceId, name));
    }

    @Override
    public Response createLiveReport(String projectId, String spaceId, String name, HttpHeaders headers, String content) {
        return polarionService.callPrivileged(() -> super.createLiveReport(projectId, spaceId, name, headers, content));
    }

    @Override
    public Response deleteLiveReport(String projectId, String spaceId, String name) {
        return polarionService.callPrivileged(() -> super.deleteLiveReport(projectId, spaceId, name));
    }

    @Override
    public Response createLiveReportInGlobalRepository(String spaceId, String name, HttpHeaders headers, String content) {
        return polarionService.callPrivileged(() -> super.createLiveReportInGlobalRepository(spaceId, name, headers, content));
    }

    @Override
    public Response deleteLiveReportFromGlobalRepository(String spaceId, String name) {
        return polarionService.callPrivileged(() -> super.deleteLiveReportFromGlobalRepository(spaceId, name));
    }
}

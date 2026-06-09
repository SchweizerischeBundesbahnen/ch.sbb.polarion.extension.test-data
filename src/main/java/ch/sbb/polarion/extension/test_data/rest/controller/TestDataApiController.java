package ch.sbb.polarion.extension.test_data.rest.controller;

import ch.sbb.polarion.extension.generic.rest.filter.Secured;
import ch.sbb.polarion.extension.test_data.rest.model.CollectionRequest;
import ch.sbb.polarion.extension.test_data.rest.model.CrossDocumentLinksRequest;
import ch.sbb.polarion.extension.test_data.rest.model.LinkedRevisionsRequest;

import jakarta.inject.Singleton;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import java.io.InputStream;

@Singleton
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
    public Response saveProjectTemplate(String templateId, String templateHash, InputStream file) {
        return polarionService.callPrivileged(() -> super.saveProjectTemplate(templateId, templateHash, file));
    }

    @Override
    public Response getTemplateHash(String templateId) {
        return polarionService.callPrivileged(() -> super.getTemplateHash(templateId));
    }

    @Override
    public Response downloadProjectTemplate(String projectId, String projectGroup) {
        return polarionService.callPrivileged(() -> super.downloadProjectTemplate(projectId, projectGroup));
    }

    @Override
    public Response createCrossDocumentLinks(String projectId, CrossDocumentLinksRequest request) {
        return polarionService.callPrivileged(() -> super.createCrossDocumentLinks(projectId, request));
    }

    @Override
    public Response addLinkedRevisions(String projectId, String spaceId, String documentName, LinkedRevisionsRequest request) {
        return polarionService.callPrivileged(() -> super.addLinkedRevisions(projectId, spaceId, documentName, request));
    }

    @Override
    public Response createBaseline(String projectId, String baselineName, String description, String revision) {
        return polarionService.callPrivileged(() -> super.createBaseline(projectId, baselineName, description, revision));
    }

    @Override
    public Response createCollection(String projectId, String collectionName, CollectionRequest request) {
        return polarionService.callPrivileged(() -> super.createCollection(projectId, collectionName, request));
    }
}

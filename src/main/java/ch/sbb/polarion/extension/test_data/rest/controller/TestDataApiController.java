package ch.sbb.polarion.extension.test_data.rest.controller;

import ch.sbb.polarion.extension.generic.rest.filter.Secured;

import javax.ws.rs.Path;
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
}

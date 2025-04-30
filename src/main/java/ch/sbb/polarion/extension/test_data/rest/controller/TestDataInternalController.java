package ch.sbb.polarion.extension.test_data.rest.controller;

import ch.sbb.polarion.extension.generic.service.PolarionService;
import ch.sbb.polarion.extension.test_data.service.ModuleService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.PATCH;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;

@Tag(name = "Test Data")
@Hidden
@Path("/internal")
@SuppressWarnings("squid:S1200") // Ignore dependencies on other classes count limitation
public class TestDataInternalController {

    @Context
    private HttpServletRequest httpServletRequest;

    protected final PolarionService polarionService;
    private final ModuleService moduleService;

    @SuppressWarnings("unused")
    public TestDataInternalController() {
        polarionService = new PolarionService();
        moduleService = new ModuleService();
    }

    @POST
    @Path("/projects/{projectId}/spaces/{spaceId}/documents/{documentName}")
    @Operation(summary = "Create document with generated work items",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Document successfully created with generated work items"),
                    @ApiResponse(responseCode = "409", description = "Document with this name already exists")
            }
    )
    public Response createDocumentWithGeneratedWorkItems(
            @PathParam("projectId") String projectId,
            @PathParam("spaceId") String spaceId,
            @PathParam("documentName") String documentName,
            @QueryParam("quantity") @DefaultValue("100") Integer quantity
    ) {
        moduleService.createDocumentWithGeneratedWorkItems(projectId, spaceId, documentName, quantity);

        URI location = UriBuilder.fromPath(httpServletRequest.getRequestURI()).build();
        return Response.created(location).build();
    }

    @PATCH
    @Path("/projects/{projectId}/spaces/{spaceId}/documents/{documentName}/append")
    @Operation(summary = "Append generated work items to the document",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Document successfully appended with generated work items")
            }
    )
    public Response extendDocumentWithGeneratedWorkItems(
            @PathParam("projectId") String projectId,
            @PathParam("spaceId") String spaceId,
            @PathParam("documentName") String documentName,
            @QueryParam("quantity") @DefaultValue("100") Integer quantity
    ) {
        moduleService.extendDocumentWithGeneratedWorkItems(projectId, spaceId, documentName, quantity);

        return Response.noContent().build();
    }

    @PATCH
    @Path("/projects/{projectId}/spaces/{spaceId}/documents/{documentName}/change-wi-descriptions")
    @Operation(summary = "Change WorkItem Descriptions",
            responses = {
                    @ApiResponse(responseCode = "204", description = "WorkItem descriptions successfully changed")
            }
    )
    public Response changeDocumentWorkItemDescriptions(
            @PathParam("projectId") String projectId,
            @PathParam("spaceId") String spaceId,
            @PathParam("documentName") String documentName,
            @QueryParam("interval") @DefaultValue("5") Integer interval
    ) {
        moduleService.changeDocumentWorkItemDescriptions(projectId, spaceId, documentName, interval);

        return Response.noContent().build();
    }
}

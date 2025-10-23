package ch.sbb.polarion.extension.test_data.rest.controller;

import ch.sbb.polarion.extension.generic.service.PolarionService;
import ch.sbb.polarion.extension.test_data.service.ModuleService;
import ch.sbb.polarion.extension.test_data.service.ProjectTemplateService;
import com.polarion.alm.projects.UserProjectCreationException;
import com.polarion.alm.shared.api.transaction.TransactionalExecutor;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataParam;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.PATCH;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.io.InputStream;
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
    private final ProjectTemplateService projectTemplateService;

    @SuppressWarnings("unused")
    public TestDataInternalController() {
        polarionService = new PolarionService();
        moduleService = new ModuleService();
        projectTemplateService = new ProjectTemplateService();
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

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Path("/templates/{templateId}")
    @Operation(
            summary = "Upload and save a project template",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Template successfully created"),
                    @ApiResponse(responseCode = "400", description = "Invalid template data or ID")
            }
    )
    public Response saveProjectTemplate(
            @PathParam("templateId") String templateId,
            @FormDataParam("file") FormDataBodyPart file) {

        if (templateId == null || templateId.trim().isEmpty()) {
            throw new IllegalArgumentException("Template ID cannot be null or empty");
        }

        if (file == null) {
            throw new IllegalArgumentException("Template file is required");
        }

        try {
            polarionService.callPrivileged(() -> TransactionalExecutor.executeInWriteTransaction(
                    transaction -> {
                        InputStream inputStream = file.getValueAs(InputStream.class);
                        projectTemplateService.saveProjectTemplate(templateId, inputStream);
                        return null;
                    })
            );
        } catch (UserProjectCreationException | ProjectTemplateService.TemplateProcessingException e) {
            throw new IllegalArgumentException("Failed to save template: " + e.getMessage(), e);
        }

        URI location = UriBuilder.fromPath(httpServletRequest.getRequestURI()).build();
        return Response.created(location).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Path("/templates/{templateId}/download")
    @Operation(
            summary = "Download a zipped project template",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Template successfully downloaded"),
                    @ApiResponse(responseCode = "404", description = "Template not found"),
                    @ApiResponse(responseCode = "400", description = "Invalid template ID")
            }
    )
    public Response downloadProjectTemplate(@PathParam("templateId") String templateId) {
        try {
            byte[] zipBytes = projectTemplateService.downloadTemplate(templateId);

            return Response.ok(zipBytes)
                    .header("Content-Disposition", "attachment; filename=" + templateId + ".zip")
                    .header("Content-Type", MediaType.APPLICATION_OCTET_STREAM)
                    .build();

        } catch (ProjectTemplateService.TemplateProcessingException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Template not found: " + templateId)
                    .build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .build();
        }
    }
}

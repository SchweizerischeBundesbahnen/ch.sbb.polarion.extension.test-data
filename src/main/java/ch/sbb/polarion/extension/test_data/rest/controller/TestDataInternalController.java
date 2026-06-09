package ch.sbb.polarion.extension.test_data.rest.controller;

import ch.sbb.polarion.extension.generic.service.PolarionService;
import ch.sbb.polarion.extension.test_data.rest.model.BaselineResponse;
import ch.sbb.polarion.extension.test_data.rest.model.CollectionRequest;
import ch.sbb.polarion.extension.test_data.rest.model.CrossDocumentLinksRequest;
import ch.sbb.polarion.extension.test_data.rest.model.LinkedRevisionsRequest;
import ch.sbb.polarion.extension.test_data.service.BaselineService;
import ch.sbb.polarion.extension.test_data.service.LinksService;
import ch.sbb.polarion.extension.test_data.service.ModuleService;
import ch.sbb.polarion.extension.test_data.service.ProjectTemplateService;
import com.polarion.alm.projects.UserProjectCreationException;
import com.polarion.alm.shared.api.transaction.TransactionalExecutor;
import com.polarion.alm.tracker.model.baselinecollection.IBaselineCollection;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.SchemaProperty;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.jetbrains.annotations.Nullable;

import jakarta.inject.Singleton;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import java.io.InputStream;
import java.net.URI;

@Singleton
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
    private final LinksService linksService;
    private final BaselineService baselineService;

    @SuppressWarnings("unused")
    public TestDataInternalController() {
        polarionService = new PolarionService();
        moduleService = new ModuleService();
        projectTemplateService = new ProjectTemplateService();
        linksService = new LinksService();
        baselineService = new BaselineService();
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
    @Path("/templates/{templateId}/{templateHash}")
    @Operation(
            summary = "Upload and save a project template",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Template successfully created"),
                    @ApiResponse(responseCode = "400", description = "Invalid template data or ID")
            }
    )
    @RequestBody(content = @Content(
            mediaType = MediaType.MULTIPART_FORM_DATA,
            schemaProperties = @SchemaProperty(name = "file", schema = @Schema(type = "string", format = "binary"))
    ))
    public Response saveProjectTemplate(
            @PathParam("templateId") String templateId,
            @PathParam("templateHash") String templateHash,
            @FormDataParam("file") FormDataBodyPart file
    ) {

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

                        projectTemplateService.saveProjectTemplate(templateId, inputStream, templateHash);
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
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/templates/{templateId}/hash")
    @Operation(
            summary = "Get the hash of a project template",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Template hash successfully retrieved"),
                    @ApiResponse(responseCode = "404", description = "Template or hash not found"),
                    @ApiResponse(responseCode = "400", description = "Invalid template ID")
            }
    )
    public Response getTemplateHash(@PathParam("templateId") String templateId) {
        try {
            return polarionService.callPrivileged(() ->
                    TransactionalExecutor.executeInReadOnlyTransaction(transaction -> {
                        String hash = projectTemplateService.readTemplateHash(templateId);

                        if (hash == null) {
                            return Response.status(Response.Status.NOT_FOUND)
                                    .entity("Hash not found for template: " + templateId)
                                    .build();
                        }

                        return Response.ok(hash).build();
                    })
            );
        } catch (ProjectTemplateService.TemplateProcessingException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Failed to save template due to user project creation error: " + e.getMessage())
                    .build();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Path("/templates/{projectId}/download")
    @Operation(
            summary = "Download a zipped project template",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Template successfully downloaded"),
                    @ApiResponse(responseCode = "404", description = "Template not found"),
                    @ApiResponse(responseCode = "400", description = "Invalid project ID")
            }
    )
    public Response downloadProjectTemplate(@PathParam("projectId") String projectId,
                                            @QueryParam("projectGroup") @Nullable String projectGroup) {

        try {
            return polarionService.callPrivileged(() ->
                    TransactionalExecutor.executeInReadOnlyTransaction(transaction -> {
                        byte[] zipBytes = projectTemplateService.downloadProject(projectId, projectGroup);

                        return Response.ok(zipBytes)
                                .header("Content-Disposition", "attachment; filename=" + projectId + ".zip")
                                .header("Content-Type", MediaType.APPLICATION_OCTET_STREAM)
                                .build();
                    })
            );
        } catch (ProjectTemplateService.TemplateProcessingException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Template not found: " + projectId)
                    .build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .build();
        }
    }

    @POST
    @Path("/projects/{projectId}/cross-document-links")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Generate random workitem links between documents",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Cross-document links successfully created",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                    schema = @Schema(implementation = Integer.class)))
            }
    )
    public Response createCrossDocumentLinks(
            @PathParam("projectId") String projectId,
            @Parameter(required = true) CrossDocumentLinksRequest request
    ) {
        int created = linksService.createCrossDocumentLinks(projectId, request);
        return Response.ok(created).build();
    }

    @POST
    @Path("/projects/{projectId}/spaces/{spaceId}/documents/{documentName}/linked-revisions")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Add linked revisions to random workitems of a document",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Linked revisions successfully added",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                    schema = @Schema(implementation = Integer.class)))
            }
    )
    public Response addLinkedRevisions(
            @PathParam("projectId") String projectId,
            @PathParam("spaceId") String spaceId,
            @PathParam("documentName") String documentName,
            @Parameter(required = true) LinkedRevisionsRequest request
    ) {
        int added = linksService.addLinkedRevisions(projectId, spaceId, documentName, request);
        return Response.ok(added).build();
    }

    @POST
    @Path("/projects/{projectId}/baselines/{baselineName}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Create a project baseline",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Baseline successfully created",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                    schema = @Schema(implementation = BaselineResponse.class)))
            }
    )
    public Response createBaseline(
            @PathParam("projectId") String projectId,
            @PathParam("baselineName") String baselineName,
            @QueryParam("description") String description,
            @QueryParam("revision") String revision
    ) {
        BaselineResponse response = baselineService.createBaseline(projectId, baselineName, description, revision);
        URI location = UriBuilder.fromPath(httpServletRequest.getRequestURI()).build();
        return Response.created(location).entity(response).build();
    }

    @POST
    @Path("/projects/{projectId}/collections/{collectionName}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Create a baseline collection containing the given documents",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Collection successfully created")
            }
    )
    public Response createCollection(
            @PathParam("projectId") String projectId,
            @PathParam("collectionName") String collectionName,
            @Parameter(required = true) CollectionRequest request
    ) {
        IBaselineCollection collection = baselineService.createCollection(projectId, collectionName, request);
        URI location = UriBuilder.fromPath(httpServletRequest.getRequestURI()).build();
        return Response.created(location).entity(collection.getName()).build();
    }
}

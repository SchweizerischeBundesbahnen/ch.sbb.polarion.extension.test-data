package ch.sbb.polarion.extension.test_data.rest.controller.annotations.api;

import ch.sbb.polarion.extension.generic.rest.filter.Secured;
import ch.sbb.polarion.extension.test_data.rest.controller.annotations.RequestScopedResource;
import io.swagger.v3.oas.annotations.Hidden;

import jakarta.ws.rs.Path;

@Secured
@Hidden
@Path("/api/annotations/request-scoped")
public class RequestScopedApiResource extends RequestScopedResource {
}

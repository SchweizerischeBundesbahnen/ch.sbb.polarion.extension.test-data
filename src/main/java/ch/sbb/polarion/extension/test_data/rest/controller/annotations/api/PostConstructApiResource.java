package ch.sbb.polarion.extension.test_data.rest.controller.annotations.api;

import ch.sbb.polarion.extension.generic.rest.filter.Secured;
import ch.sbb.polarion.extension.test_data.rest.controller.annotations.PostConstructResource;
import io.swagger.v3.oas.annotations.Hidden;

import javax.ws.rs.Path;

@Secured
@Hidden
@Path("/api/annotations/post-construct")
public class PostConstructApiResource extends PostConstructResource {
}

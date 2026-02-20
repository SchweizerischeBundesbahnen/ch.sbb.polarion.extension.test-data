package ch.sbb.polarion.extension.test_data.rest.controller.annotations.api;

import ch.sbb.polarion.extension.generic.rest.filter.Secured;
import ch.sbb.polarion.extension.test_data.rest.controller.annotations.InjectResource;
import io.swagger.v3.oas.annotations.Hidden;

import javax.inject.Singleton;
import javax.ws.rs.Path;

@Singleton
@Secured
@Hidden
@Path("/api/annotations/inject")
public class InjectApiResource extends InjectResource {
}

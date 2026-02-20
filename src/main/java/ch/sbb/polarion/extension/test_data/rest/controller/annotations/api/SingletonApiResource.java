package ch.sbb.polarion.extension.test_data.rest.controller.annotations.api;

import ch.sbb.polarion.extension.generic.rest.filter.Secured;
import ch.sbb.polarion.extension.test_data.rest.controller.annotations.SingletonResource;
import io.swagger.v3.oas.annotations.Hidden;

import javax.inject.Singleton;
import javax.ws.rs.Path;

@Singleton
@Secured
@Hidden
@Path("/api/annotations/singleton")
public class SingletonApiResource extends SingletonResource {
}

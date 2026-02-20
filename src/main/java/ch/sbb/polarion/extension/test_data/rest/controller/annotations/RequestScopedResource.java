package ch.sbb.polarion.extension.test_data.rest.controller.annotations;

import io.swagger.v3.oas.annotations.Hidden;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.UUID;

@Hidden
@Path("/internal/annotations/request-scoped")
public class RequestScopedResource {

    private final String instanceId = UUID.randomUUID().toString();

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String get() {
        return "{\"instanceId\":\"" + instanceId + "\"}";
    }
}

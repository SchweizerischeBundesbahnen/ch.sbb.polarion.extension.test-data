package ch.sbb.polarion.extension.test_data.rest.controller.annotations;

import io.swagger.v3.oas.annotations.Hidden;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
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

package ch.sbb.polarion.extension.test_data.rest.controller.annotations;

import io.swagger.v3.oas.annotations.Hidden;

import javax.annotation.PostConstruct;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.UUID;

@Hidden
@Path("/internal/annotations/post-construct")
public class PostConstructResource {

    private final String instanceId = UUID.randomUUID().toString();
    private boolean initialized = false;

    @PostConstruct
    void init() {
        initialized = true;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String get() {
        return "{\"instanceId\":\"" + instanceId + "\",\"initialized\":" + initialized + "}";
    }
}

package ch.sbb.polarion.extension.test_data.rest.controller.annotations;

import io.swagger.v3.oas.annotations.Hidden;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.UUID;

@Singleton
@Hidden
@Path("/internal/annotations/inject")
public class InjectResource {

    private final String instanceId = UUID.randomUUID().toString();

    @Inject
    private SingletonResource singletonResource;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String get() {
        String injectedId = singletonResource.getInstanceId();
        return "{\"ownInstanceId\":\"" + instanceId + "\",\"injectedInstanceId\":\"" + injectedId + "\"}";
    }
}

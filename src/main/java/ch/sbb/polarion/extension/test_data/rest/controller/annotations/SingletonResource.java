package ch.sbb.polarion.extension.test_data.rest.controller.annotations;

import io.swagger.v3.oas.annotations.Hidden;

import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Singleton
@Hidden
@Path("/internal/annotations/singleton")
public class SingletonResource {

    private final String instanceId = UUID.randomUUID().toString();
    private final AtomicInteger callCount = new AtomicInteger(0);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String get() {
        int count = callCount.incrementAndGet();
        return "{\"instanceId\":\"" + instanceId + "\",\"callCount\":" + count + "}";
    }

    public String getInstanceId() {
        return instanceId;
    }
}

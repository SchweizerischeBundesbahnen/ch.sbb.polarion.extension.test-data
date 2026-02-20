package ch.sbb.polarion.extension.test_data.rest.controller.annotations;

import io.swagger.v3.oas.annotations.Hidden;

import javax.annotation.PreDestroy;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Hidden
@Path("/internal/annotations/pre-destroy")
public class PreDestroyResource {

    private static final AtomicInteger destroyedCount = new AtomicInteger(0);

    private final String instanceId = UUID.randomUUID().toString();

    @PreDestroy
    void cleanup() {
        destroyedCount.incrementAndGet();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String get() {
        int destroyedCountBefore = destroyedCount.get();
        int destroyedCountAfter = destroyedCountBefore + 1;
        return "{\"instanceId\":\"" + instanceId + "\"," +
                "\"destroyedCountBefore\":" + destroyedCountBefore + "," +
                "\"destroyedCountAfter\":" + destroyedCountAfter + "}";
    }
}

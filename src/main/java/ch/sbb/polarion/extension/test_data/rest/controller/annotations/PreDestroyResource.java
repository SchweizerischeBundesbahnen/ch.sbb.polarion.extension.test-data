package ch.sbb.polarion.extension.test_data.rest.controller.annotations;

import io.swagger.v3.oas.annotations.Hidden;

import jakarta.annotation.PreDestroy;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
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

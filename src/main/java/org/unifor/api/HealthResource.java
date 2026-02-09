package org.unifor.api;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Simple health check endpoint to verify the API is up.
 * For full health (DB, etc.) use SmallRye Health at /q/health.
 */
@Path("/api/health")
@Produces(MediaType.APPLICATION_JSON)
public class HealthResource {

    @GET
    public Response check() {
        return Response.ok()
                .entity(new HealthStatus("UP"))
                .build();
    }

    public record HealthStatus(String status) {}
}

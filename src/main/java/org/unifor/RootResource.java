package org.unifor;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

/**
 * Root path: redirect to Swagger UI so GET / is useful when opening the app URL.
 * Using @Path("") so the resource is mounted at the application root.
 */
@Path("")
public class RootResource {

    @GET
    @Path("/")
    public Response root() {
        return Response.seeOther(java.net.URI.create("/q/swagger-ui")).build();
    }
}

package org.unifor.exception;

import io.quarkus.security.UnauthorizedException;
import jakarta.annotation.Priority;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.unifor.dto.response.ErrorResponse;

/**
 * Maps Quarkus UnauthorizedException (unauthenticated request) to 401 with PRD error format.
 * Phase 4: 401/403 error handling.
 */
@Provider
@Priority(1)
public class UnauthorizedExceptionMapper implements ExceptionMapper<UnauthorizedException> {

    private static final String CODE = "UNAUTHORIZED";

    @Override
    public Response toResponse(UnauthorizedException exception) {
        var response = new ErrorResponse(CODE, "Não autenticado");
        return Response.status(Response.Status.UNAUTHORIZED).entity(response).build();
    }
}

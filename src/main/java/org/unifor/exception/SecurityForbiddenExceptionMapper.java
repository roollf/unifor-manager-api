package org.unifor.exception;

import jakarta.annotation.Priority;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.unifor.dto.response.ErrorResponse;

/**
 * Maps Quarkus ForbiddenException (authenticated but lacking required role) to 403 with PRD error format.
 * Phase 4: 401/403 error handling. For application-level forbidden (e.g. not owner), see ForbiddenExceptionMapper.
 */
@Provider
@Priority(1)
public class SecurityForbiddenExceptionMapper implements ExceptionMapper<io.quarkus.security.ForbiddenException> {

    private static final String CODE = "FORBIDDEN";

    @Override
    public Response toResponse(io.quarkus.security.ForbiddenException exception) {
        var response = new ErrorResponse(CODE, "Acesso negado");
        return Response.status(Response.Status.FORBIDDEN).entity(response).build();
    }
}

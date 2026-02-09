package org.unifor.exception;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.unifor.dto.response.ErrorResponse;

@Provider
public class ConflictExceptionMapper implements ExceptionMapper<ConflictException> {

    @Override
    public Response toResponse(ConflictException exception) {
        var response = new ErrorResponse(
                exception.getErrorCode(),
                exception.getMessage(),
                exception.getDetails()
        );
        return Response.status(Response.Status.CONFLICT).entity(response).build();
    }
}

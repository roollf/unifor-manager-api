package org.unifor.exception;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.unifor.dto.response.ErrorResponse;

@Provider
public class ValidationExceptionMapper implements ExceptionMapper<ValidationException> {

    @Override
    public Response toResponse(ValidationException exception) {
        var response = new ErrorResponse(
                exception.getErrorCode(),
                exception.getMessage(),
                exception.getDetails()
        );
        return Response.status(Response.Status.BAD_REQUEST).entity(response).build();
    }
}

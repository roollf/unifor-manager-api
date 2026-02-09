package org.unifor.exception;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.unifor.dto.response.ErrorResponse;

@Provider
public class NotFoundExceptionMapper implements ExceptionMapper<NotFoundException> {

    @Override
    public Response toResponse(NotFoundException exception) {
        var response = new ErrorResponse(
                exception.getErrorCode(),
                exception.getMessage(),
                exception.getDetails()
        );
        return Response.status(Response.Status.NOT_FOUND).entity(response).build();
    }
}

package org.unifor.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.unifor.dto.response.ErrorResponse;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Maps Bean Validation constraint violations to 400 with structured error format.
 */
@Provider
public class ConstraintViolationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        List<String> violations = exception.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toList());

        var details = Map.<String, Object>of("violations", violations);
        var response = new ErrorResponse("VALIDATION_ERROR", "Erro de validação nos dados enviados", details);
        return Response.status(Response.Status.BAD_REQUEST).entity(response).build();
    }
}

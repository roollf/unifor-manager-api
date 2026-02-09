package org.unifor.api.coordinator;

import jakarta.annotation.security.RolesAllowed;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.unifor.dto.request.CreateMatrixRequest;
import org.unifor.dto.response.MatrixResponse;
import org.unifor.dto.response.MatrixSummaryResponse;
import org.unifor.entity.CurriculumMatrix;
import org.unifor.security.CurrentUserService;
import org.unifor.service.coordinator.MatrixService;

import java.util.List;

@Path("/api/coordinator/matrices")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("coordinator")
@Transactional
public class MatrixResource {

    private final MatrixService matrixService;
    private final CurrentUserService currentUserService;

    public MatrixResource(MatrixService matrixService, CurrentUserService currentUserService) {
        this.matrixService = matrixService;
        this.currentUserService = currentUserService;
    }

    @GET
    public Response list() {
        var coordinator = currentUserService.getCurrentCoordinator();
        List<CurriculumMatrix> matrices = matrixService.listByCoordinator(coordinator);
        var items = matrices.stream()
                .map(this::toSummaryResponse)
                .toList();
        return Response.ok().entity(new ListWrapper(items)).build();
    }

    @POST
    public Response create(@Valid CreateMatrixRequest request, @Context UriInfo uriInfo) {
        var coordinator = currentUserService.getCurrentCoordinator();
        CurriculumMatrix matrix = matrixService.create(request.name(), coordinator);
        var response = new MatrixResponse(
                matrix.id,
                matrix.name,
                matrix.coordinator.id,
                matrix.active,
                matrix.createdAt
        );
        return Response.created(uriInfo.getRequestUriBuilder().path(matrix.id.toString()).build())
                .entity(response)
                .build();
    }

    @PUT
    @Path("{matrixId}/activate")
    public Response activate(@PathParam("matrixId") Long matrixId) {
        var coordinator = currentUserService.getCurrentCoordinator();
        matrixService.activate(matrixId, coordinator);
        return Response.noContent().build();
    }

    private MatrixSummaryResponse toSummaryResponse(CurriculumMatrix m) {
        long classCount = m.classes != null ? m.classes.size() : 0;
        return new MatrixSummaryResponse(m.id, m.name, m.active, classCount, m.createdAt);
    }

    private record ListWrapper(List<MatrixSummaryResponse> items) {}
}

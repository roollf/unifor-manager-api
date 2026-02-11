package org.unifor.api.coordinator;

import jakarta.annotation.security.RolesAllowed;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.unifor.dto.request.CreateMatrixClassRequest;
import org.unifor.dto.request.UpdateMatrixClassRequest;
import org.unifor.dto.response.*;
import org.unifor.entity.*;
import org.unifor.repository.EnrollmentRepository;
import org.unifor.security.CurrentUserService;
import org.unifor.service.coordinator.MatrixClassFilter;
import org.unifor.service.coordinator.MatrixClassService;
import org.unifor.service.coordinator.PeriodOfDay;

import java.util.List;
import java.util.Optional;

@Path("/api/coordinator/matrices/{matrixId}/classes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("coordinator")
@Transactional
public class MatrixClassResource {

    private final MatrixClassService matrixClassService;
    private final CurrentUserService currentUserService;
    private final EnrollmentRepository enrollmentRepository;

    public MatrixClassResource(MatrixClassService matrixClassService,
                               CurrentUserService currentUserService,
                               EnrollmentRepository enrollmentRepository) {
        this.matrixClassService = matrixClassService;
        this.currentUserService = currentUserService;
        this.enrollmentRepository = enrollmentRepository;
    }

    @GET
    public Response list(
            @PathParam("matrixId") Long matrixId,
            @QueryParam("periodOfDay") PeriodOfDay periodOfDay,
            @QueryParam("authorizedCourseId") Long authorizedCourseId,
            @QueryParam("maxStudentsMin") Integer maxStudentsMin,
            @QueryParam("maxStudentsMax") Integer maxStudentsMax,
            @QueryParam("includeDeleted") @DefaultValue("false") boolean includeDeleted
    ) {
        var coordinator = currentUserService.getCurrentCoordinator();
        var filter = new MatrixClassFilter(
                Optional.ofNullable(periodOfDay),
                Optional.ofNullable(authorizedCourseId),
                Optional.ofNullable(maxStudentsMin),
                Optional.ofNullable(maxStudentsMax),
                includeDeleted
        );
        List<MatrixClass> classes = matrixClassService.list(matrixId, coordinator, filter);
        var items = classes.stream()
                .map(mc -> toResponse(mc, matrixId))
                .toList();
        return Response.ok().entity(new PageResponse<>(items, items.size())).build();
    }

    @POST
    public Response create(@PathParam("matrixId") Long matrixId, @Valid CreateMatrixClassRequest request) {
        var coordinator = currentUserService.getCurrentCoordinator();
        MatrixClass matrixClass = matrixClassService.create(request, matrixId, coordinator);
        return Response.status(Response.Status.CREATED).entity(toResponse(matrixClass, matrixId)).build();
    }

    @GET
    @Path("{classId}")
    public Response getById(@PathParam("matrixId") Long matrixId, @PathParam("classId") Long classId) {
        var coordinator = currentUserService.getCurrentCoordinator();
        MatrixClass matrixClass = matrixClassService.getByIdAndCoordinator(classId, matrixId, coordinator);
        return Response.ok().entity(toResponse(matrixClass, matrixId)).build();
    }

    @PUT
    @Path("{classId}")
    public Response update(
            @PathParam("matrixId") Long matrixId,
            @PathParam("classId") Long classId,
            @Valid UpdateMatrixClassRequest request
    ) {
        var coordinator = currentUserService.getCurrentCoordinator();
        MatrixClass matrixClass = matrixClassService.update(classId, request, matrixId, coordinator);
        return Response.ok().entity(toResponse(matrixClass, matrixId)).build();
    }

    @DELETE
    @Path("{classId}")
    public Response delete(@PathParam("matrixId") Long matrixId, @PathParam("classId") Long classId) {
        var coordinator = currentUserService.getCurrentCoordinator();
        matrixClassService.softDelete(classId, matrixId, coordinator);
        return Response.noContent().build();
    }

    private MatrixClassResponse toResponse(MatrixClass mc, Long matrixId) {
        long currentEnrollments = enrollmentRepository.countByMatrixClass(mc);
        var subject = new SubjectDto(mc.subject.id, mc.subject.name);
        var professor = new ProfessorDto(mc.professor.id, mc.professor.name);
        var timeSlot = new TimeSlotDto(mc.timeSlot.id, mc.timeSlot.dayOfWeek, mc.timeSlot.startTime, mc.timeSlot.endTime, mc.timeSlot.code);
        var authorizedCourses = mc.authorizedCourses.stream()
                .map(c -> new CourseDto(c.id, c.name))
                .toList();
        return new MatrixClassResponse(
                mc.id,
                matrixId,
                subject,
                professor,
                timeSlot,
                authorizedCourses,
                mc.maxStudents,
                currentEnrollments,
                mc.deletedAt,
                mc.createdAt
        );
    }
}

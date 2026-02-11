package org.unifor.api.student;

import jakarta.annotation.security.RolesAllowed;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.unifor.dto.response.*;
import org.unifor.entity.MatrixClass;
import org.unifor.entity.User;
import org.unifor.repository.EnrollmentRepository;
import org.unifor.security.CurrentUserService;
import org.unifor.service.student.EnrollmentService;

import java.util.List;
import java.util.Optional;

@Path("/api/student/classes/available")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("student")
public class AvailableClassResource {

    private final EnrollmentService enrollmentService;
    private final CurrentUserService currentUserService;
    private final EnrollmentRepository enrollmentRepository;

    public AvailableClassResource(EnrollmentService enrollmentService,
                                 CurrentUserService currentUserService,
                                 EnrollmentRepository enrollmentRepository) {
        this.enrollmentService = enrollmentService;
        this.currentUserService = currentUserService;
        this.enrollmentRepository = enrollmentRepository;
    }

    @GET
    @Transactional
    public Response list(
            @QueryParam("matrixId") Long matrixId,
            @QueryParam("subjectId") Long subjectId
    ) {
        User student = currentUserService.getCurrentStudent();
        List<MatrixClass> classes = enrollmentService.listAvailable(
                student,
                Optional.ofNullable(matrixId),
                Optional.ofNullable(subjectId)
        );
        var items = classes.stream()
                .map(mc -> toResponse(mc, student))
                .toList();
        return Response.ok().entity(new ListWrapper(items)).build();
    }

    private AvailableClassResponse toResponse(MatrixClass mc, User student) {
        long enrollmentCount = enrollmentRepository.countByMatrixClass(mc);
        int availableSeats = (int) (mc.maxStudents - enrollmentCount);
        boolean authorized = mc.authorizedCourses.stream()
                .anyMatch(c -> student.course != null && c.id.equals(student.course.id));
        var subject = new SubjectDto(mc.subject.id, mc.subject.name);
        var professor = new ProfessorDto(mc.professor.id, mc.professor.name);
        var timeSlot = new TimeSlotDto(mc.timeSlot.id, mc.timeSlot.dayOfWeek,
                mc.timeSlot.startTime, mc.timeSlot.endTime, mc.timeSlot.code);
        return new AvailableClassResponse(
                mc.id,
                subject,
                professor,
                timeSlot,
                mc.maxStudents,
                availableSeats,
                authorized
        );
    }

    private record ListWrapper(List<AvailableClassResponse> items) {}
}

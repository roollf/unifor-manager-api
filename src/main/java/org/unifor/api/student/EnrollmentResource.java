package org.unifor.api.student;

import jakarta.annotation.security.RolesAllowed;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.unifor.dto.request.EnrollRequest;
import org.unifor.dto.response.*;
import org.unifor.entity.Enrollment;
import org.unifor.entity.MatrixClass;
import org.unifor.security.CurrentUserService;
import org.unifor.service.student.EnrollmentService;

import java.util.List;

@Path("/api/student/enrollments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("student")
@Transactional
public class EnrollmentResource {

    private final EnrollmentService enrollmentService;
    private final CurrentUserService currentUserService;

    public EnrollmentResource(EnrollmentService enrollmentService,
                              CurrentUserService currentUserService) {
        this.enrollmentService = enrollmentService;
        this.currentUserService = currentUserService;
    }

    @GET
    public Response list() {
        var student = currentUserService.getCurrentStudent();
        List<Enrollment> enrollments = enrollmentService.listEnrolled(student);
        var items = enrollments.stream()
                .map(this::toResponse)
                .toList();
        return Response.ok().entity(new ListWrapper(items)).build();
    }

    @POST
    public Response enroll(@Valid EnrollRequest request) {
        var student = currentUserService.getCurrentStudent();
        Enrollment enrollment = enrollmentService.enroll(request.matrixClassId(), student);
        return Response.status(Response.Status.CREATED).entity(toResponse(enrollment)).build();
    }

    private EnrollmentResponse toResponse(Enrollment e) {
        MatrixClass mc = e.matrixClass;
        var subject = new SubjectDto(mc.subject.id, mc.subject.name);
        var professor = new ProfessorDto(mc.professor.id, mc.professor.name);
        var timeSlot = new TimeSlotDto(mc.timeSlot.id, mc.timeSlot.dayOfWeek,
                mc.timeSlot.startTime, mc.timeSlot.endTime);
        return new EnrollmentResponse(e.id, mc.id, subject, professor, timeSlot, e.enrolledAt);
    }

    private record ListWrapper(List<EnrollmentResponse> items) {}
}

package org.unifor.api.student;

import jakarta.annotation.security.RolesAllowed;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.unifor.dto.response.CourseDto;
import org.unifor.dto.response.StudentMeResponse;
import org.unifor.entity.User;
import org.unifor.security.CurrentUserService;

@Path("/api/student/me")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("student")
public class StudentMeResource {

    private final CurrentUserService currentUserService;

    public StudentMeResource(CurrentUserService currentUserService) {
        this.currentUserService = currentUserService;
    }

    @GET
    @Transactional
    public Response me() {
        User student = currentUserService.getCurrentStudent();
        var course = student.course == null
                ? null
                : new CourseDto(student.course.id, student.course.name);
        return Response.ok(new StudentMeResponse(course)).build();
    }
}

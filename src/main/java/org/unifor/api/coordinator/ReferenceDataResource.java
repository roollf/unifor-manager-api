package org.unifor.api.coordinator;

import jakarta.annotation.security.RolesAllowed;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.unifor.dto.response.*;
import org.unifor.repository.CourseRepository;
import org.unifor.repository.ProfessorRepository;
import org.unifor.repository.SubjectRepository;
import org.unifor.repository.TimeSlotRepository;

/**
 * Reference data endpoints for coordinator "Add class" form dropdowns.
 * Returns the same IDs and shapes used in create-class and list-classes.
 */
@Path("/api/coordinator/reference")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("coordinator")
@Transactional
public class ReferenceDataResource {

    private final SubjectRepository subjectRepository;
    private final ProfessorRepository professorRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final CourseRepository courseRepository;

    public ReferenceDataResource(SubjectRepository subjectRepository,
                                ProfessorRepository professorRepository,
                                TimeSlotRepository timeSlotRepository,
                                CourseRepository courseRepository) {
        this.subjectRepository = subjectRepository;
        this.professorRepository = professorRepository;
        this.timeSlotRepository = timeSlotRepository;
        this.courseRepository = courseRepository;
    }

    @GET
    @Path("/subjects")
    public Response listSubjects() {
        var items = subjectRepository.listAll().stream()
                .map(s -> new SubjectDto(s.id, s.name))
                .toList();
        return Response.ok().entity(items).build();
    }

    @GET
    @Path("/professors")
    public Response listProfessors() {
        var items = professorRepository.listAll().stream()
                .map(p -> new ProfessorDto(p.id, p.name))
                .toList();
        return Response.ok().entity(items).build();
    }

    @GET
    @Path("/time-slots")
    public Response listTimeSlots() {
        var items = timeSlotRepository.listAll().stream()
                .map(t -> new TimeSlotDto(t.id, t.dayOfWeek, t.startTime, t.endTime, t.code))
                .toList();
        return Response.ok().entity(items).build();
    }

    @GET
    @Path("/courses")
    public Response listCourses() {
        var items = courseRepository.listAll().stream()
                .map(c -> new CourseDto(c.id, c.name))
                .toList();
        return Response.ok().entity(items).build();
    }
}

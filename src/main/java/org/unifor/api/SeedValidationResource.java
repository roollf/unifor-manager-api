package org.unifor.api;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.unifor.entity.UserRole;
import org.unifor.repository.*;

import java.util.Map;

/**
 * Dev-only endpoint to validate Phase 1 seed data.
 * Returns entity counts to verify migrations and repositories work.
 */
@Path("/api/dev/seed-validation")
@Produces(MediaType.APPLICATION_JSON)
public class SeedValidationResource {

    private final SubjectRepository subjectRepository;
    private final ProfessorRepository professorRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    public SeedValidationResource(SubjectRepository subjectRepository,
                                  ProfessorRepository professorRepository,
                                  TimeSlotRepository timeSlotRepository,
                                  CourseRepository courseRepository,
                                  UserRepository userRepository) {
        this.subjectRepository = subjectRepository;
        this.professorRepository = professorRepository;
        this.timeSlotRepository = timeSlotRepository;
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
    }

    @GET
    public Response validate() {
        long subjects = subjectRepository.count();
        long professors = professorRepository.count();
        long timeSlots = timeSlotRepository.count();
        long courses = courseRepository.count();
        long coordinators = userRepository.count("role", UserRole.COORDINATOR);
        long students = userRepository.count("role", UserRole.STUDENT);

        var result = Map.of(
                "status", "OK",
                "message", "Phase 1 seed validation",
                "counts", Map.of(
                        "subjects", subjects,
                        "professors", professors,
                        "timeSlots", timeSlots,
                        "courses", courses,
                        "coordinators", coordinators,
                        "students", students
                ),
                "expected", Map.of(
                        "subjects", 15,
                        "professors", 5,
                        "timeSlots", 9,
                        "courses", 9,
                        "coordinators", 3,
                        "students", 5
                )
        );

        return Response.ok(result).build();
    }
}

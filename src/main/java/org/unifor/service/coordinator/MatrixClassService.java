package org.unifor.service.coordinator;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.unifor.dto.request.CreateMatrixClassRequest;
import org.unifor.dto.request.UpdateMatrixClassRequest;
import org.unifor.entity.*;
import org.unifor.exception.ConflictException;
import org.unifor.exception.ForbiddenException;
import org.unifor.exception.NotFoundException;
import org.unifor.exception.ValidationException;
import org.unifor.repository.*;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Coordinator service for matrix class CRUD.
 * Enforces CR-01 to CR-06, ED-01 to ED-06, DL-01 to DL-04, VM-01 to VM-03.
 */
@ApplicationScoped
public class MatrixClassService {

    private final MatrixService matrixService;
    private final MatrixClassRepository matrixClassRepository;
    private final SubjectRepository subjectRepository;
    private final ProfessorRepository professorRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;

    public MatrixClassService(MatrixService matrixService,
                              MatrixClassRepository matrixClassRepository,
                              SubjectRepository subjectRepository,
                              ProfessorRepository professorRepository,
                              TimeSlotRepository timeSlotRepository,
                              CourseRepository courseRepository,
                              EnrollmentRepository enrollmentRepository) {
        this.matrixService = matrixService;
        this.matrixClassRepository = matrixClassRepository;
        this.subjectRepository = subjectRepository;
        this.professorRepository = professorRepository;
        this.timeSlotRepository = timeSlotRepository;
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    @Transactional
    public MatrixClass create(CreateMatrixClassRequest request, Long matrixId, User coordinator) {
        CurriculumMatrix matrix = matrixService.getByIdAndCoordinator(matrixId, coordinator);

        Subject subject = subjectRepository.findById(request.subjectId());
        if (subject == null) {
            throw new ValidationException("Disciplina não encontrada");
        }
        Professor professor = professorRepository.findById(request.professorId());
        if (professor == null) {
            throw new ValidationException("Professor não encontrado");
        }
        TimeSlot timeSlot = timeSlotRepository.findById(request.timeSlotId());
        if (timeSlot == null) {
            throw new ValidationException("Horário não encontrado");
        }

        long duplicateCount = matrixClassRepository.count(
                "matrix = ?1 and subject = ?2 and timeSlot = ?3",
                matrix, subject, timeSlot
        );
        if (duplicateCount > 0) {
            throw new ConflictException("CONFLICT_DUPLICATE_SUBJECT_SLOT",
                    "Mesma disciplina já oferecida neste horário nesta matriz");
        }

        List<Course> authorizedCourses = new ArrayList<>();
        for (Long courseId : request.authorizedCourseIds()) {
            Course course = courseRepository.findById(courseId);
            if (course == null) {
                throw new ValidationException("Curso não encontrado: " + courseId);
            }
            authorizedCourses.add(course);
        }

        var matrixClass = new MatrixClass(matrix, subject, professor, timeSlot, request.maxStudents());
        matrixClass.authorizedCourses = authorizedCourses;
        matrixClass.persist();
        return matrixClass;
    }

    public List<MatrixClass> list(Long matrixId, User coordinator, MatrixClassFilter filter) {
        CurriculumMatrix matrix = matrixService.getByIdAndCoordinator(matrixId, coordinator);

        List<MatrixClass> classes = matrixClassRepository.findByMatrix(matrix);

        return classes.stream()
                .filter(mc -> filterByPeriodOfDay(mc, filter.periodOfDay()))
                .filter(mc -> filterByAuthorizedCourse(mc, filter.authorizedCourseId()))
                .filter(mc -> filterByMaxStudents(mc, filter.maxStudentsMin(), filter.maxStudentsMax()))
                .collect(Collectors.toList());
    }

    private boolean filterByPeriodOfDay(MatrixClass mc, java.util.Optional<PeriodOfDay> periodOfDay) {
        if (periodOfDay.isEmpty()) return true;
        return periodOfDay.get().contains(mc.timeSlot.startTime);
    }

    private boolean filterByAuthorizedCourse(MatrixClass mc, java.util.Optional<Long> authorizedCourseId) {
        if (authorizedCourseId.isEmpty()) return true;
        return mc.authorizedCourses.stream()
                .anyMatch(c -> c.id.equals(authorizedCourseId.get()));
    }

    private boolean filterByMaxStudents(MatrixClass mc,
                                        java.util.Optional<Integer> min,
                                        java.util.Optional<Integer> max) {
        if (min.isPresent() && mc.maxStudents < min.get()) return false;
        if (max.isPresent() && mc.maxStudents > max.get()) return false;
        return true;
    }

    @Transactional
    public MatrixClass update(Long classId, UpdateMatrixClassRequest request, Long matrixId, User coordinator) {
        CurriculumMatrix matrix = matrixService.getByIdAndCoordinator(matrixId, coordinator);
        MatrixClass matrixClass = getMatrixClassByIdAndMatrix(classId, matrix);

        Professor professor = professorRepository.findById(request.professorId());
        if (professor == null) {
            throw new ValidationException("Professor não encontrado");
        }
        TimeSlot newTimeSlot = timeSlotRepository.findById(request.timeSlotId());
        if (newTimeSlot == null) {
            throw new ValidationException("Horário não encontrado");
        }

        List<Course> currentAuthorized = matrixClass.authorizedCourses;
        List<Long> newCourseIds = request.authorizedCourseIds();
        List<Long> removedCourseIds = currentAuthorized.stream()
                .map(c -> c.id)
                .filter(id -> !newCourseIds.contains(id))
                .collect(Collectors.toList());

        if (!removedCourseIds.isEmpty()) {
            for (Long removedCourseId : removedCourseIds) {
                long count = enrollmentRepository.count(
                        "matrixClass = ?1 and student.course.id = ?2",
                        matrixClass, removedCourseId
                );
                if (count > 0) {
                    throw new ConflictException("CONFLICT_INVALIDATE_ENROLLMENTS",
                            "Não é possível remover cursos autorizados: existem matrículas de alunos desses cursos");
                }
            }
        }

        if (!newTimeSlot.id.equals(matrixClass.timeSlot.id)) {
            if (wouldCauseScheduleConflict(matrixClass, newTimeSlot)) {
                throw new ConflictException("CONFLICT_SCHEDULE_CONFLICT",
                        "Alterar horário causaria conflito de agenda para alunos já matriculados");
            }
        }

        List<Course> authorizedCourses = new ArrayList<>();
        for (Long courseId : newCourseIds) {
            Course course = courseRepository.findById(courseId);
            if (course == null) {
                throw new ValidationException("Curso não encontrado: " + courseId);
            }
            authorizedCourses.add(course);
        }

        matrixClass.professor = professor;
        matrixClass.timeSlot = newTimeSlot;
        matrixClass.authorizedCourses = authorizedCourses;
        matrixClass.updatedAt = java.time.Instant.now();

        return matrixClass;
    }

    private boolean wouldCauseScheduleConflict(MatrixClass matrixClass, TimeSlot newTimeSlot) {
        List<Enrollment> enrollments = enrollmentRepository.find("matrixClass", matrixClass).list();
        for (Enrollment enr : enrollments) {
            List<Enrollment> otherEnrollments = enrollmentRepository.find(
                    "student = ?1 and matrixClass != ?2",
                    enr.student, matrixClass
            ).list();
            for (Enrollment other : otherEnrollments) {
                if (timeSlotsOverlap(newTimeSlot, other.matrixClass.timeSlot)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean timeSlotsOverlap(TimeSlot a, TimeSlot b) {
        if (!a.dayOfWeek.equals(b.dayOfWeek)) return false;
        return a.startTime.isBefore(b.endTime) && b.startTime.isBefore(a.endTime);
    }

    @Transactional
    public void softDelete(Long classId, Long matrixId, User coordinator) {
        CurriculumMatrix matrix = matrixService.getByIdAndCoordinator(matrixId, coordinator);
        MatrixClass matrixClass = getMatrixClassByIdAndMatrix(classId, matrix);

        long enrollmentCount = enrollmentRepository.countByMatrixClass(matrixClass);
        if (enrollmentCount > 0) {
            throw new ConflictException("CONFLICT_HAS_ENROLLMENTS",
                    "Não é possível excluir: existem alunos matriculados nesta turma");
        }

        matrixClass.softDelete();
    }

    public MatrixClass getByIdAndCoordinator(Long classId, Long matrixId, User coordinator) {
        CurriculumMatrix matrix = matrixService.getByIdAndCoordinator(matrixId, coordinator);
        return getMatrixClassByIdAndMatrix(classId, matrix);
    }

    private MatrixClass getMatrixClassByIdAndMatrix(Long classId, CurriculumMatrix matrix) {
        MatrixClass matrixClass = matrixClassRepository.findById(classId);
        if (matrixClass == null) {
            throw new NotFoundException("Turma não encontrada");
        }
        if (!matrixClass.matrix.id.equals(matrix.id)) {
            throw new ForbiddenException("Acesso negado: turma pertence a outra matriz");
        }
        return matrixClass;
    }
}

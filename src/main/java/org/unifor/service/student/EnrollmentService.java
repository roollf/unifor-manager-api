package org.unifor.service.student;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.unifor.entity.*;
import org.unifor.exception.ConflictException;
import org.unifor.exception.NotFoundException;
import org.unifor.repository.CurriculumMatrixRepository;
import org.unifor.repository.EnrollmentRepository;
import org.unifor.repository.MatrixClassRepository;
import org.unifor.service.ScheduleConflictUtil;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Student service for enrollments. Enforces VE-01 to VE-03, EN-01 to EN-08.
 */
@ApplicationScoped
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final MatrixClassRepository matrixClassRepository;
    private final CurriculumMatrixRepository curriculumMatrixRepository;

    public EnrollmentService(EnrollmentRepository enrollmentRepository,
                             MatrixClassRepository matrixClassRepository,
                             CurriculumMatrixRepository curriculumMatrixRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.matrixClassRepository = matrixClassRepository;
        this.curriculumMatrixRepository = curriculumMatrixRepository;
    }

    /**
     * Lists student's enrollments in active (non-soft-deleted) classes only (VE-03).
     */
    public List<Enrollment> listEnrolled(User student) {
        List<Enrollment> enrollments = enrollmentRepository.findByStudent(student);
        return enrollments.stream()
                .filter(e -> e.matrixClass != null) // exclude soft-deleted classes
                .collect(Collectors.toList());
    }

    /**
     * Lists classes the student can enroll in. From active matrix only.
     * Filters: authorized for course, available seats, no schedule conflict, not already enrolled, not same subject (EN-08).
     */
    public List<MatrixClass> listAvailable(User student, Optional<Long> matrixId, Optional<Long> subjectId) {
        CurriculumMatrix activeMatrix = curriculumMatrixRepository.findActive();
        if (activeMatrix == null) {
            return List.of();
        }

        if (matrixId.isPresent() && !activeMatrix.id.equals(matrixId.get())) {
            return List.of();
        }

        List<MatrixClass> classes = matrixClassRepository.findByMatrix(activeMatrix);
        List<Enrollment> studentEnrollments = enrollmentRepository.findByStudent(student);

        return classes.stream()
                .filter(mc -> subjectId.isEmpty() || mc.subject.id.equals(subjectId.get()))
                .filter(mc -> isAuthorizedForStudentCourse(mc, student))
                .filter(mc -> hasAvailableSeats(mc))
                .filter(mc -> !isAlreadyEnrolled(mc, studentEnrollments))
                .filter(mc -> !hasSameSubjectEnrollment(mc, studentEnrollments))
                .filter(mc -> !hasScheduleConflict(mc, studentEnrollments))
                .collect(Collectors.toList());
    }

    private boolean isAuthorizedForStudentCourse(MatrixClass mc, User student) {
        if (student.course == null) return false;
        return mc.authorizedCourses.stream()
                .anyMatch(c -> c.id.equals(student.course.id));
    }

    private boolean hasAvailableSeats(MatrixClass mc) {
        long count = enrollmentRepository.countByMatrixClass(mc);
        return count < mc.maxStudents;
    }

    private boolean isAlreadyEnrolled(MatrixClass mc, List<Enrollment> studentEnrollments) {
        return studentEnrollments.stream()
                .anyMatch(e -> e.matrixClass.id.equals(mc.id));
    }

    private boolean hasSameSubjectEnrollment(MatrixClass mc, List<Enrollment> studentEnrollments) {
        return studentEnrollments.stream()
                .anyMatch(e -> e.matrixClass != null && e.matrixClass.subject.id.equals(mc.subject.id));
    }

    private boolean hasScheduleConflict(MatrixClass mc, List<Enrollment> studentEnrollments) {
        TimeSlot candidateSlot = mc.timeSlot;
        return studentEnrollments.stream()
                .filter(e -> e.matrixClass != null)
                .anyMatch(e -> ScheduleConflictUtil.overlaps(candidateSlot, e.matrixClass.timeSlot));
    }

    /**
     * Enrolls student in a class. Validates EN-01 to EN-08.
     * <p>
     * Concurrency (Phase 5, CC-01, CC-02, CC-03): Uses SELECT FOR UPDATE (PESSIMISTIC_WRITE)
     * on the MatrixClass row. Lock is acquired first, then seat check, conflict check,
     * and duplicate-subject check run in the same transaction. Isolation level:
     * PostgreSQL default READ COMMITTED. Lock is held until commit, preventing overbooking.
     */
    @Transactional
    public Enrollment enroll(Long matrixClassId, User student) {
        MatrixClass matrixClass = matrixClassRepository.findByIdForUpdate(matrixClassId);
        if (matrixClass == null) {
            throw new NotFoundException("Turma não encontrada");
        }

        CurriculumMatrix activeMatrix = curriculumMatrixRepository.findActive();
        if (activeMatrix == null || !matrixClass.matrix.id.equals(activeMatrix.id)) {
            throw new ConflictException("CONFLICT_MATRIX_INACTIVE", "Turma não está na matriz ativa");
        }

        if (!isAuthorizedForStudentCourse(matrixClass, student)) {
            throw new ConflictException("CONFLICT_UNAUTHORIZED_COURSE",
                    "Turma não autorizada para o curso do estudante");
        }

        long enrollmentCount = enrollmentRepository.countByMatrixClass(matrixClass);
        if (enrollmentCount >= matrixClass.maxStudents) {
            throw new ConflictException("CONFLICT_NO_SEATS", "Não há vagas disponíveis nesta turma");
        }

        if (enrollmentRepository.existsByMatrixClassAndStudent(matrixClass, student)) {
            throw new ConflictException("CONFLICT_ALREADY_ENROLLED", "Estudante já matriculado nesta turma");
        }

        List<Enrollment> studentEnrollments = enrollmentRepository.findByStudent(student);
        if (hasSameSubjectEnrollment(matrixClass, studentEnrollments)) {
            throw new ConflictException("CONFLICT_DUPLICATE_SUBJECT",
                    "Estudante já matriculado nesta disciplina em outra turma");
        }

        if (hasScheduleConflict(matrixClass, studentEnrollments)) {
            throw new ConflictException("CONFLICT_SCHEDULE",
                    "Conflito de horário com outra disciplina matriculada");
        }

        var enrollment = new Enrollment(matrixClass, student);
        enrollment.persist();
        return enrollment;
    }
}

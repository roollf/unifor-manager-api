package org.unifor.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "matrix_classes")
@SQLRestriction("deleted_at IS NULL")
public class MatrixClass extends PanacheEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matrix_id", nullable = false)
    public CurriculumMatrix matrix;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    public Subject subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "professor_id", nullable = false)
    public Professor professor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "time_slot_id", nullable = false)
    public TimeSlot timeSlot;

    @Column(name = "max_students", nullable = false)
    public Integer maxStudents;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "matrix_class_authorized_courses",
            joinColumns = @JoinColumn(name = "matrix_class_id"),
            inverseJoinColumns = @JoinColumn(name = "course_id")
    )
    public List<Course> authorizedCourses = new ArrayList<>();

    @Column(name = "deleted_at")
    public Instant deletedAt;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt = Instant.now();

    @OneToMany(mappedBy = "matrixClass")
    public List<Enrollment> enrollments = new ArrayList<>();

    public MatrixClass() {
    }

    public MatrixClass(CurriculumMatrix matrix, Subject subject, Professor professor, TimeSlot timeSlot, Integer maxStudents) {
        this.matrix = matrix;
        this.subject = subject;
        this.professor = professor;
        this.timeSlot = timeSlot;
        this.maxStudents = maxStudents;
    }

    public void softDelete() {
        this.deletedAt = Instant.now();
        this.updatedAt = Instant.now();
    }
}

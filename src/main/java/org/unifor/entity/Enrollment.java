package org.unifor.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "enrollments")
public class Enrollment extends PanacheEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matrix_class_id", nullable = false)
    public MatrixClass matrixClass;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    public User student;

    @Column(name = "enrolled_at", nullable = false)
    public Instant enrolledAt = Instant.now();

    @Column(name = "created_at", nullable = false)
    public Instant createdAt = Instant.now();

    public Enrollment() {
    }

    public Enrollment(MatrixClass matrixClass, User student) {
        this.matrixClass = matrixClass;
        this.student = student;
    }
}

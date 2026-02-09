package org.unifor.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "curriculum_matrices")
@SQLRestriction("deleted_at IS NULL")
public class CurriculumMatrix extends PanacheEntity {

    @Column(nullable = false, length = 255)
    public String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coordinator_id", nullable = false)
    public User coordinator;

    @Column(nullable = false)
    public boolean active = false;

    @Column(name = "deleted_at")
    public Instant deletedAt;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt = Instant.now();

    @OneToMany(mappedBy = "matrix")
    public List<MatrixClass> classes = new ArrayList<>();

    public CurriculumMatrix() {
    }

    public CurriculumMatrix(String name, User coordinator) {
        this.name = name;
        this.coordinator = coordinator;
    }

    public void softDelete() {
        this.deletedAt = Instant.now();
        this.updatedAt = Instant.now();
    }
}

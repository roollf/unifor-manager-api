package org.unifor.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "professors")
public class Professor extends PanacheEntity {

    @Column(nullable = false, length = 255)
    public String name;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt = Instant.now();

    public Professor() {
    }

    public Professor(String name) {
        this.name = name;
    }
}

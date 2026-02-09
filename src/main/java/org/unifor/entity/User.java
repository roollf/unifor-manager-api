package org.unifor.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "users")
public class User extends PanacheEntity {

    @Column(nullable = false, unique = true, length = 255)
    public String email;

    @Column(nullable = false, length = 255)
    public String name;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    public UserRole role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    public Course course;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt = Instant.now();

    public User() {
    }

    public User(String email, String name, UserRole role) {
        this.email = email;
        this.name = name;
        this.role = role;
    }

    public User(String email, String name, UserRole role, Course course) {
        this.email = email;
        this.name = name;
        this.role = role;
        this.course = course;
    }
}

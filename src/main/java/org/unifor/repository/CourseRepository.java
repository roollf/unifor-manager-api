package org.unifor.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import org.unifor.entity.Course;

@ApplicationScoped
public class CourseRepository implements PanacheRepository<Course> {
}

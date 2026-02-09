package org.unifor.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import org.unifor.entity.Subject;

@ApplicationScoped
public class SubjectRepository implements PanacheRepository<Subject> {
}

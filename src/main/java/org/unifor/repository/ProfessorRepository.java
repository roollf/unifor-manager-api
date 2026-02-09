package org.unifor.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import org.unifor.entity.Professor;

@ApplicationScoped
public class ProfessorRepository implements PanacheRepository<Professor> {
}

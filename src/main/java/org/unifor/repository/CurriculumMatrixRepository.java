package org.unifor.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import org.unifor.entity.CurriculumMatrix;
import org.unifor.entity.User;

import java.util.List;

@ApplicationScoped
public class CurriculumMatrixRepository implements PanacheRepository<CurriculumMatrix> {

    public List<CurriculumMatrix> findByCoordinator(User coordinator) {
        return list("coordinator", coordinator);
    }

    public CurriculumMatrix findActive() {
        return find("active", true).firstResult();
    }

    public List<CurriculumMatrix> findAllActive() {
        return list("active", true);
    }
}

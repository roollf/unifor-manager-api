package org.unifor.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.LockModeType;
import org.unifor.entity.CurriculumMatrix;
import org.unifor.entity.MatrixClass;

import java.util.List;

@ApplicationScoped
public class MatrixClassRepository implements PanacheRepository<MatrixClass> {

    public List<MatrixClass> findByMatrix(CurriculumMatrix matrix) {
        return list("matrix", matrix);
    }

    /**
     * Loads MatrixClass with PESSIMISTIC_WRITE (SELECT FOR UPDATE).
     * Used during enrollment to prevent overbooking (Phase 5, CC-01, CC-02).
     * Lock is held until transaction commits.
     */
    public MatrixClass findByIdForUpdate(Long id) {
        return find("id", id).withLock(LockModeType.PESSIMISTIC_WRITE).firstResult();
    }
}

package org.unifor.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import org.unifor.entity.CurriculumMatrix;
import org.unifor.entity.MatrixClass;

import java.util.List;

@ApplicationScoped
public class MatrixClassRepository implements PanacheRepository<MatrixClass> {

    public List<MatrixClass> findByMatrix(CurriculumMatrix matrix) {
        return list("matrix", matrix);
    }
}

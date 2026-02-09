package org.unifor.service.coordinator;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.unifor.entity.CurriculumMatrix;
import org.unifor.entity.User;
import org.unifor.exception.ForbiddenException;
import org.unifor.exception.NotFoundException;
import org.unifor.repository.CurriculumMatrixRepository;

import java.util.List;

/**
 * Coordinator service for curriculum matrix CRUD.
 * Enforces AC-01: coordinator can access only matrices they created.
 */
@ApplicationScoped
public class MatrixService {

    private final CurriculumMatrixRepository matrixRepository;

    public MatrixService(CurriculumMatrixRepository matrixRepository) {
        this.matrixRepository = matrixRepository;
    }

    @Transactional
    public CurriculumMatrix create(String name, User coordinator) {
        var matrix = new CurriculumMatrix(name.trim(), coordinator);
        matrix.active = false;
        matrix.persist();
        return matrix;
    }

    public List<CurriculumMatrix> listByCoordinator(User coordinator) {
        return matrixRepository.findByCoordinator(coordinator);
    }

    /**
     * Returns the matrix if found and coordinator owns it. Throws otherwise.
     */
    public CurriculumMatrix getByIdAndCoordinator(Long matrixId, User coordinator) {
        CurriculumMatrix matrix = matrixRepository.findById(matrixId);
        if (matrix == null) {
            throw new NotFoundException("Matriz não encontrada");
        }
        if (!matrix.coordinator.id.equals(coordinator.id)) {
            throw new ForbiddenException("Acesso negado: matriz pertence a outro coordenador");
        }
        return matrix;
    }

    /**
     * Activates the matrix. Deactivates all other matrices first (at most one active).
     */
    @Transactional
    public void activate(Long matrixId, User coordinator) {
        CurriculumMatrix matrix = getByIdAndCoordinator(matrixId, coordinator);

        for (CurriculumMatrix active : matrixRepository.findAllActive()) {
            active.active = false;
        }
        matrix.active = true;
    }
}

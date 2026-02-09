package org.unifor.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import org.unifor.entity.Enrollment;
import org.unifor.entity.MatrixClass;
import org.unifor.entity.User;

import java.util.List;

@ApplicationScoped
public class EnrollmentRepository implements PanacheRepository<Enrollment> {

    public List<Enrollment> findByStudent(User student) {
        return list("student", student);
    }

    public long countByMatrixClass(MatrixClass matrixClass) {
        return count("matrixClass", matrixClass);
    }

    public boolean existsByMatrixClassAndStudent(MatrixClass matrixClass, User student) {
        return count("matrixClass = ?1 and student = ?2", matrixClass, student) > 0;
    }
}

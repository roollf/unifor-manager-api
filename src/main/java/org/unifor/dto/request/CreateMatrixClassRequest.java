package org.unifor.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateMatrixClassRequest(
        @NotNull(message = "ID da disciplina é obrigatório")
        Long subjectId,

        @NotNull(message = "ID do professor é obrigatório")
        Long professorId,

        @NotNull(message = "ID do horário é obrigatório")
        Long timeSlotId,

        @NotNull(message = "Lista de cursos autorizados é obrigatória")
        @Size(min = 1, message = "Pelo menos um curso deve ser autorizado")
        List<Long> authorizedCourseIds,

        @NotNull(message = "Número máximo de alunos é obrigatório")
        @Min(value = 1, message = "Número máximo de alunos deve ser pelo menos 1")
        Integer maxStudents
) {}

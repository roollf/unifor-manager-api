package org.unifor.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateMatrixClassRequest(
        @NotNull(message = "ID do horário é obrigatório")
        Long timeSlotId,

        @NotNull(message = "ID do professor é obrigatório")
        Long professorId,

        @NotNull(message = "Lista de cursos autorizados é obrigatória")
        @Size(min = 1, message = "Pelo menos um curso deve ser autorizado")
        List<Long> authorizedCourseIds
) {}

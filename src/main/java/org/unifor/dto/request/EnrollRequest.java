package org.unifor.dto.request;

import jakarta.validation.constraints.NotNull;

public record EnrollRequest(
        @NotNull(message = "ID da turma é obrigatório")
        Long matrixClassId
) {}

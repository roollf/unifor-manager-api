package org.unifor.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateMatrixRequest(
        @NotBlank(message = "Nome da matriz é obrigatório")
        @Size(max = 255)
        String name
) {}

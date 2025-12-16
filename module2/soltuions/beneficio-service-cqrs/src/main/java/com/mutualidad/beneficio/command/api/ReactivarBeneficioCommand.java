package com.mutualidad.beneficio.command.api;

import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.NotBlank;

@Value
@Builder
public class ReactivarBeneficioCommand {

    @NotBlank(message = "El ID del beneficio es obligatorio")
    String beneficioId;

    String motivo;

    @NotBlank(message = "El usuario que reactiva es obligatorio")
    String reactivadoPor;
}

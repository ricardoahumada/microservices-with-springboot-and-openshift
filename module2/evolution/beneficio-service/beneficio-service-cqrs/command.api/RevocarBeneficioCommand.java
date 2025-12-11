package com.mutualidad.beneficio.command.api;

import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.NotBlank;
import java.time.LocalDate;

@Value
@Builder
public class RevocarBeneficioCommand {

    @NotBlank(message = "El ID del beneficio es obligatorio")
    String beneficioId;

    @NotBlank(message = "El motivo de revocacion es obligatorio")
    String motivo;

    LocalDate fechaEfectiva;

    @NotBlank(message = "El usuario que revoca es obligatorio")
    String revocadoPor;
}

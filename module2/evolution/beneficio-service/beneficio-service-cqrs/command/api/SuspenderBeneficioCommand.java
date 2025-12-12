package com.mutualidad.beneficio.command.api;

import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;

@Value
@Builder
public class SuspenderBeneficioCommand {

    @NotBlank(message = "El ID del beneficio es obligatorio")
    String beneficioId;

    @NotNull(message = "La fecha de inicio de suspension es obligatoria")
    LocalDate fechaInicioSuspension;

    LocalDate fechaFinSuspension;

    @NotBlank(message = "El motivo de suspension es obligatorio")
    String motivo;

    @NotBlank(message = "El usuario que suspende es obligatorio")
    String suspendidoPor;
}

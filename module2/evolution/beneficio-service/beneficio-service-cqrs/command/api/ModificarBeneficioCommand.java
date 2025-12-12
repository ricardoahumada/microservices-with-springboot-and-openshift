package com.mutualidad.beneficio.command.api;

import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDate;

@Value
@Builder
public class ModificarBeneficioCommand {

    @NotBlank(message = "El ID del beneficio es obligatorio")
    String beneficioId;

    BigDecimal nuevoMonto;
    LocalDate nuevaFechaFin;
    String nuevaDescripcion;

    @NotBlank(message = "El motivo de modificacion es obligatorio")
    String motivo;

    @NotBlank(message = "El usuario que modifica es obligatorio")
    String modificadoPor;
}

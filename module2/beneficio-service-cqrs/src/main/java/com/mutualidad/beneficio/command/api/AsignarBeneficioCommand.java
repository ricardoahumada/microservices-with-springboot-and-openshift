package com.mutualidad.beneficio.command.api;

import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

@Value
@Builder
public class AsignarBeneficioCommand {

    @NotBlank(message = "El ID del afiliado es obligatorio")
    String afiliadoId;

    @NotNull(message = "El tipo de beneficio es obligatorio")
    TipoBeneficio tipoBeneficio;

    @NotNull(message = "La fecha de inicio es obligatoria")
    LocalDate fechaInicio;

    LocalDate fechaFin;

    @Positive(message = "El monto debe ser positivo")
    BigDecimal monto;

    String descripcion;

    @NotBlank(message = "El usuario que solicita es obligatorio")
    String solicitadoPor;

    String motivo;
}

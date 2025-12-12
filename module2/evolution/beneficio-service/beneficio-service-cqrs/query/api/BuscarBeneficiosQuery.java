package com.mutualidad.beneficio.query.api;

import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.NotBlank;

@Value
@Builder
public class BuscarBeneficiosQuery {

    @NotBlank(message = "El ID del afiliado es obligatorio")
    String afiliadoId;

    String estado;
    String tipo;

    @Builder.Default
    int page = 0;

    @Builder.Default
    int size = 20;
}

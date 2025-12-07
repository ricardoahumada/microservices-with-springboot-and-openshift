package com.mutualidad.beneficio.api.dto;

import com.mutualidad.beneficio.domain.model.Beneficio;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class BeneficioResponse {
    private Long id;
    private String nombre;
    private String descripcion;
    private String tipo;
    private BigDecimal montoMaximo;
    private Boolean activo;
    private Integer diasCarencia;

    public static BeneficioResponse fromEntity(Beneficio beneficio) {
        return BeneficioResponse.builder()
                .id(beneficio.getId())
                .nombre(beneficio.getNombre())
                .descripcion(beneficio.getDescripcion())
                .tipo(beneficio.getTipo().name())
                .montoMaximo(beneficio.getMontoMaximo())
                .activo(beneficio.getActivo())
                .diasCarencia(beneficio.getDiasCarencia())
                .build();
    }
}

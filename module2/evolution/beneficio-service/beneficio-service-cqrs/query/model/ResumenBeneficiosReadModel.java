package com.mutualidad.beneficio.query.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumenBeneficiosReadModel {
    private String afiliadoId;
    private int totalBeneficios;
    private int beneficiosActivos;
    private int beneficiosSuspendidos;
    private int beneficiosRevocados;
    private BigDecimal montoTotalActivo;
    private String montoTotalFormateado;
}

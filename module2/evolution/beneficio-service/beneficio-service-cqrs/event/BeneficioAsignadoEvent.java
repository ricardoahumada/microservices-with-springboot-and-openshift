package com.mutualidad.beneficio.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Getter
@RequiredArgsConstructor
public class BeneficioAsignadoEvent implements BeneficioEvent {
    private final String beneficioId;
    private final String afiliadoId;
    private final String tipoBeneficio;
    private final LocalDate fechaInicio;
    private final BigDecimal monto;
    private final Instant ocurridoEn = Instant.now();

    @Override
    public String getTipoEvento() {
        return "BENEFICIO_ASIGNADO";
    }

    @Override
    public String getAggregateId() {
        return beneficioId;
    }
}

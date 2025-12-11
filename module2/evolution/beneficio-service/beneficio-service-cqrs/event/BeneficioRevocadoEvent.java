package com.mutualidad.beneficio.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@RequiredArgsConstructor
public class BeneficioRevocadoEvent implements BeneficioEvent {
    private final String beneficioId;
    private final String afiliadoId;
    private final String motivo;
    private final LocalDate fechaEfectiva;
    private final Instant ocurridoEn = Instant.now();

    @Override
    public String getTipoEvento() {
        return "BENEFICIO_REVOCADO";
    }

    @Override
    public String getAggregateId() {
        return beneficioId;
    }
}

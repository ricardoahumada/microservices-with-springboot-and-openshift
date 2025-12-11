package com.mutualidad.beneficio.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Instant;

@Getter
@RequiredArgsConstructor
public class BeneficioModificadoEvent implements BeneficioEvent {
    private final String beneficioId;
    private final String afiliadoId;
    private final String motivo;
    private final String modificadoPor;
    private final Instant ocurridoEn = Instant.now();

    @Override
    public String getTipoEvento() {
        return "BENEFICIO_MODIFICADO";
    }

    @Override
    public String getAggregateId() {
        return beneficioId;
    }
}

package com.mutualidad.beneficio.event;

import java.time.Instant;

public interface BeneficioEvent {
    String getTipoEvento();
    String getAggregateId();
    Instant getOcurridoEn();
}

package com.mutualidad.afiliado.domain.event;

import java.time.Instant;

public interface AfiliadoEvent {
    String getAfiliadoId();
    Instant getOcurridoEn();
    String getTipoEvento();
}

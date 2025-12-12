package com.mutualidad.afiliado.domain.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import java.time.Instant;

@Getter
@RequiredArgsConstructor
public class AfiliadoActivadoEvent implements AfiliadoEvent {
    private final String afiliadoId;
    private final Instant ocurridoEn = Instant.now();

    @Override
    public String getTipoEvento() {
        return "AFILIADO_ACTIVADO";
    }
}

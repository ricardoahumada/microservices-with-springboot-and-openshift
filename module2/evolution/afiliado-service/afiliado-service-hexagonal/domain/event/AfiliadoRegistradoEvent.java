package com.mutualidad.afiliado.domain.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import java.time.Instant;

@Getter
@RequiredArgsConstructor
public class AfiliadoRegistradoEvent implements AfiliadoEvent {
    private final String afiliadoId;
    private final String tipoDocumento;
    private final String numeroDocumento;
    private final String nombre;
    private final String codigoEmpresa;
    private final Instant ocurridoEn = Instant.now();

    @Override
    public String getTipoEvento() {
        return "AFILIADO_REGISTRADO";
    }
}

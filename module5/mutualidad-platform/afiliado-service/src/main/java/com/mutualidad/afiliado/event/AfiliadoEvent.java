package com.mutualidad.afiliado.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AfiliadoEvent {
    private String eventId;
    private String eventType;
    private LocalDateTime timestamp;
    private AfiliadoPayload payload;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AfiliadoPayload {
        private String afiliadoId;
        private String dni;
        private String nombre;
        private String apellidos;
        private String email;
        private String empresaId;
    }
}

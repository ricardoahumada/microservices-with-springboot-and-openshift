package com.mutualidad.validacion.api.dto;

import com.mutualidad.validacion.domain.model.ValidacionExterna;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ValidacionResponse {
    private Long id;
    private Long afiliadoId;
    private String tipo;
    private String resultado;
    private String proveedorExterno;
    private Integer puntuacion;
    private String mensajeResultado;
    private LocalDateTime fechaSolicitud;
    private LocalDateTime fechaRespuesta;
    private LocalDateTime fechaExpiracion;
    private Boolean vigente;

    public static ValidacionResponse fromEntity(ValidacionExterna validacion) {
        return ValidacionResponse.builder()
                .id(validacion.getId())
                .afiliadoId(validacion.getAfiliadoId())
                .tipo(validacion.getTipo().name())
                .resultado(validacion.getResultado().name())
                .proveedorExterno(validacion.getProveedorExterno())
                .puntuacion(validacion.getPuntuacion())
                .mensajeResultado(validacion.getMensajeResultado())
                .fechaSolicitud(validacion.getFechaSolicitud())
                .fechaRespuesta(validacion.getFechaRespuesta())
                .fechaExpiracion(validacion.getFechaExpiracion())
                .vigente(validacion.estaVigente())
                .build();
    }
}

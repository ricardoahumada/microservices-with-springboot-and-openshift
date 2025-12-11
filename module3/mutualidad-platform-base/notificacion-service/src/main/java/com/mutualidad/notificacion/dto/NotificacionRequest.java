package com.mutualidad.notificacion.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificacionRequest {
    
    @NotNull(message = "ID de afiliado es requerido")
    private Long afiliadoId;
    
    @NotBlank(message = "Tipo de notificacion es requerido")
    private String tipo; // EMAIL, SMS, PUSH
    
    @NotBlank(message = "Asunto es requerido")
    private String asunto;
    
    @NotBlank(message = "Mensaje es requerido")
    private String mensaje;
    
    private String destinatario; // email o telefono
}

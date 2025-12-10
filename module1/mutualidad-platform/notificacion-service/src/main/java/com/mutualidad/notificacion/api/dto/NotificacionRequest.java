package com.mutualidad.notificacion.api.dto;

import com.mutualidad.notificacion.domain.model.Canal;
import lombok.Data;

import javax.validation.constraints.*;

@Data
public class NotificacionRequest {
    
    @NotBlank(message = "El ID del afiliado es obligatorio")
    private String afiliadoId;
    
    private String nombreDestinatario;
    
    @Email
    private String email;
    
    private String telefono;
    
    @NotBlank(message = "El asunto es obligatorio")
    @Size(max = 200)
    private String asunto;
    
    @NotBlank(message = "El contenido es obligatorio")
    private String contenido;
    
    @NotNull(message = "El canal es obligatorio")
    private Canal canal;
}

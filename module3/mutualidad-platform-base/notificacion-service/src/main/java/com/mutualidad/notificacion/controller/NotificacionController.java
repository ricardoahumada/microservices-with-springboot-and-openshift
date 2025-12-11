package com.mutualidad.notificacion.controller;

import com.mutualidad.notificacion.dto.NotificacionRequest;
import com.mutualidad.notificacion.dto.NotificacionResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/notificaciones")
public class NotificacionController {

    @PostMapping
    public ResponseEntity<NotificacionResponse> enviarNotificacion(
            @Valid @RequestBody NotificacionRequest request) {
        
        log.info("POST /api/notificaciones - Enviando notificacion tipo {} al afiliado {}", 
                request.getTipo(), request.getAfiliadoId());
        
        // Simulacion de envio de notificacion
        NotificacionResponse response = NotificacionResponse.builder()
                .id(UUID.randomUUID().toString())
                .afiliadoId(request.getAfiliadoId())
                .tipo(request.getTipo())
                .asunto(request.getAsunto())
                .estado("ENVIADO")
                .fechaEnvio(LocalDateTime.now())
                .mensaje("Notificacion enviada correctamente")
                .build();
        
        log.info("Notificacion enviada con ID: {}", response.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/bienvenida/{afiliadoId}")
    public ResponseEntity<NotificacionResponse> enviarBienvenida(
            @PathVariable Long afiliadoId,
            @RequestParam(defaultValue = "nuevo.afiliado@email.com") String email) {
        
        log.info("POST /api/notificaciones/bienvenida/{} - Enviando email de bienvenida", afiliadoId);
        
        NotificacionRequest request = NotificacionRequest.builder()
                .afiliadoId(afiliadoId)
                .tipo("EMAIL")
                .asunto("Bienvenido a la Mutualidad")
                .mensaje("Estimado afiliado, le damos la bienvenida a nuestra mutualidad.")
                .destinatario(email)
                .build();
        
        return enviarNotificacion(request);
    }

    @GetMapping("/status")
    public ResponseEntity<String> getStatus() {
        return ResponseEntity.ok("Notificacion Service OK");
    }
}

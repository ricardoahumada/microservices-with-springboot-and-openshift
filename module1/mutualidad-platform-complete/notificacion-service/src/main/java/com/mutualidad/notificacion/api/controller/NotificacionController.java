package com.mutualidad.notificacion.api.controller;

import com.mutualidad.notificacion.api.dto.NotificacionRequest;
import com.mutualidad.notificacion.api.dto.NotificacionResponse;
import com.mutualidad.notificacion.application.service.NotificacionService;
import com.mutualidad.notificacion.domain.model.Canal;
import com.mutualidad.notificacion.domain.model.Destinatario;
import com.mutualidad.notificacion.domain.model.EstadoNotificacion;
import com.mutualidad.notificacion.domain.model.Notificacion;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/notificaciones")
@RequiredArgsConstructor
@Validated
public class NotificacionController {

    private final NotificacionService notificacionService;

    @PostMapping
    public ResponseEntity<NotificacionResponse> crear(@Valid @RequestBody NotificacionRequest request) {
        Destinatario destinatario = Destinatario.builder()
                .afiliadoId(request.getAfiliadoId())
                .nombre(request.getNombreDestinatario())
                .email(request.getEmail())
                .telefono(request.getTelefono())
                .build();

        Notificacion notificacion = Notificacion.builder()
                .destinatario(destinatario)
                .asunto(request.getAsunto())
                .contenido(request.getContenido())
                .canal(request.getCanal())
                .build();

        Notificacion creada = notificacionService.crear(notificacion);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(NotificacionResponse.fromEntity(creada));
    }

    @GetMapping("/{id}")
    public ResponseEntity<NotificacionResponse> buscarPorId(@PathVariable Long id) {
        return notificacionService.buscarPorId(id)
                .map(n -> ResponseEntity.ok(NotificacionResponse.fromEntity(n)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<NotificacionResponse>> listar(
            @RequestParam(required = false) String afiliadoId,
            @RequestParam(required = false) EstadoNotificacion estado,
            @RequestParam(required = false) Canal canal) {
        
        List<Notificacion> notificaciones;
        if (afiliadoId != null) {
            notificaciones = notificacionService.buscarPorAfiliado(afiliadoId);
        } else if (estado != null) {
            notificaciones = notificacionService.buscarPorEstado(estado);
        } else if (canal != null) {
            notificaciones = notificacionService.buscarPorCanal(canal);
        } else {
            notificaciones = notificacionService.buscarPorEstado(EstadoNotificacion.PENDIENTE);
        }
        
        List<NotificacionResponse> response = notificaciones.stream()
                .map(NotificacionResponse::fromEntity)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/enviar")
    public ResponseEntity<NotificacionResponse> enviar(@PathVariable Long id) {
        Notificacion enviada = notificacionService.enviar(id);
        return ResponseEntity.ok(NotificacionResponse.fromEntity(enviada));
    }

    @PostMapping("/{id}/confirmar-entrega")
    public ResponseEntity<NotificacionResponse> confirmarEntrega(@PathVariable Long id) {
        Notificacion entregada = notificacionService.confirmarEntrega(id);
        return ResponseEntity.ok(NotificacionResponse.fromEntity(entregada));
    }

    @PostMapping("/{id}/confirmar-lectura")
    public ResponseEntity<NotificacionResponse> confirmarLectura(@PathVariable Long id) {
        Notificacion leida = notificacionService.confirmarLectura(id);
        return ResponseEntity.ok(NotificacionResponse.fromEntity(leida));
    }
}

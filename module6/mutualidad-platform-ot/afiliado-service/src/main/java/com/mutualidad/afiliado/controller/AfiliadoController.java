package com.mutualidad.afiliado.controller;

import com.mutualidad.afiliado.service.AfiliadoEventPublisher;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/afiliados")
@RequiredArgsConstructor
public class AfiliadoController {

    private final AfiliadoEventPublisher eventPublisher;
    private final Tracer tracer;

    @PostMapping
    public ResponseEntity<Map<String, Object>> crearAfiliado(
            @RequestParam String dni,
            @RequestParam String nombre,
            @RequestParam String apellidos,
            @RequestParam String email,
            @RequestParam String empresaId) {
        
        // Crear span para la operacion
        Span span = tracer.spanBuilder("afiliado.crear")
            .setAttribute("afiliado.dni", dni)
            .setAttribute("afiliado.empresaId", empresaId)
            .startSpan();
        
        try (Scope scope = span.makeCurrent()) {
            log.info("Creando afiliado: dni={}, nombre={} {}", dni, nombre, apellidos);
            
            // Span hijo para publicacion de evento
            Span publishSpan = tracer.spanBuilder("kafka.publish.afiliado-created")
                .startSpan();
            try (Scope publishScope = publishSpan.makeCurrent()) {
                eventPublisher.publishAfiliadoCreated(dni, nombre, apellidos, email, empresaId);
                publishSpan.setAttribute("kafka.topic", "afiliado-eventos");
            } finally {
                publishSpan.end();
            }
            
            span.setAttribute("afiliado.status", "ACCEPTED");
            
            return ResponseEntity.accepted().body(Map.of(
                "status", "ACCEPTED",
                "message", "Evento de creacion de afiliado publicado",
                "dni", dni,
                "traceId", span.getSpanContext().getTraceId()
            ));
        } catch (Exception e) {
            span.recordException(e);
            throw e;
        } finally {
            span.end();
        }
    }

    @PutMapping("/{afiliadoId}")
    public ResponseEntity<Map<String, Object>> actualizarAfiliado(
            @PathVariable String afiliadoId,
            @RequestParam String dni,
            @RequestParam String nombre,
            @RequestParam String apellidos,
            @RequestParam String email,
            @RequestParam String empresaId) {
        
        Span span = tracer.spanBuilder("afiliado.actualizar")
            .setAttribute("afiliado.id", afiliadoId)
            .setAttribute("afiliado.dni", dni)
            .startSpan();
        
        try (Scope scope = span.makeCurrent()) {
            log.info("Actualizando afiliado: id={}", afiliadoId);
            
            eventPublisher.publishAfiliadoUpdated(afiliadoId, dni, nombre, apellidos, email, empresaId);
            
            return ResponseEntity.accepted().body(Map.of(
                "status", "ACCEPTED",
                "message", "Evento de actualizacion de afiliado publicado",
                "afiliadoId", afiliadoId,
                "traceId", span.getSpanContext().getTraceId()
            ));
        } catch (Exception e) {
            span.recordException(e);
            throw e;
        } finally {
            span.end();
        }
    }
}

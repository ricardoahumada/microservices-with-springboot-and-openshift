package com.mutualidad.afiliado.controller;

import com.mutualidad.afiliado.service.AfiliadoEventPublisher;
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

    @PostMapping
    public ResponseEntity<Map<String, Object>> crearAfiliado(
            @RequestParam String dni,
            @RequestParam String nombre,
            @RequestParam String apellidos,
            @RequestParam String email,
            @RequestParam String empresaId) {
        
        log.info("Creando afiliado: dni={}, nombre={} {}", dni, nombre, apellidos);
        
        eventPublisher.publishAfiliadoCreated(dni, nombre, apellidos, email, empresaId);
        
        return ResponseEntity.accepted().body(Map.of(
            "status", "ACCEPTED",
            "message", "Evento de creacion de afiliado publicado",
            "dni", dni
        ));
    }

    @PutMapping("/{afiliadoId}")
    public ResponseEntity<Map<String, Object>> actualizarAfiliado(
            @PathVariable String afiliadoId,
            @RequestParam String dni,
            @RequestParam String nombre,
            @RequestParam String apellidos,
            @RequestParam String email,
            @RequestParam String empresaId) {
        
        log.info("Actualizando afiliado: id={}", afiliadoId);
        
        eventPublisher.publishAfiliadoUpdated(afiliadoId, dni, nombre, apellidos, email, empresaId);
        
        return ResponseEntity.accepted().body(Map.of(
            "status", "ACCEPTED",
            "message", "Evento de actualizacion de afiliado publicado",
            "afiliadoId", afiliadoId
        ));
    }
}

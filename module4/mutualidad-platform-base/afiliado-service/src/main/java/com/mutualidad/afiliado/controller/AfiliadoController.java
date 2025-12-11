package com.mutualidad.afiliado.controller;

import com.mutualidad.afiliado.service.AfiliadoEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller REST para gestionar afiliados.
 * 
 * Cuando se crea un afiliado, publica un evento a Kafka.
 */
@RestController
@RequestMapping("/api/afiliados")
public class AfiliadoController {

    private final AfiliadoEventPublisher eventPublisher;

    public AfiliadoController(AfiliadoEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    /**
     * Crea un afiliado y publica evento a Kafka.
     * 
     * Ejemplo:
     * POST /api/afiliados?dni=12345678A&nombre=Juan&apellidos=Garcia&email=juan@test.com
     */
    @PostMapping
    public ResponseEntity<Map<String, String>> crearAfiliado(
            @RequestParam String dni,
            @RequestParam String nombre,
            @RequestParam String apellidos,
            @RequestParam String email) {
        
        // Publicar evento a Kafka
        eventPublisher.publishAfiliadoCreated(dni, nombre, apellidos, email);
        
        return ResponseEntity.accepted().body(Map.of(
            "status", "ACCEPTED",
            "message", "Evento de creacion publicado",
            "dni", dni
        ));
    }
}

package com.mutualidad.afiliado.controller;

import com.mutualidad.afiliado.dto.AfiliadoResponse;
import com.mutualidad.afiliado.dto.AltaAfiliadoRequest;
import com.mutualidad.afiliado.service.AfiliadoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/api/afiliados")
@RequiredArgsConstructor
public class AfiliadoController {

    private final AfiliadoService afiliadoService;

    /**
     * Alta de afiliado basica.
     * En la version con resiliencia se aniadira:
     * - Soporte de idempotencia (X-Idempotency-Key)
     */
    @PostMapping
    public ResponseEntity<AfiliadoResponse> altaAfiliado(
            @Valid @RequestBody AltaAfiliadoRequest request) {
        
        log.info("POST /api/afiliados - DNI: {}", request.getDni());
        AfiliadoResponse response = afiliadoService.altaAfiliado(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AfiliadoResponse> obtenerPorId(@PathVariable Long id) {
        log.info("GET /api/afiliados/{}", id);
        return ResponseEntity.ok(afiliadoService.obtenerPorId(id));
    }

    @GetMapping("/dni/{dni}")
    public ResponseEntity<AfiliadoResponse> obtenerPorDni(@PathVariable String dni) {
        log.info("GET /api/afiliados/dni/{}", dni);
        return ResponseEntity.ok(afiliadoService.obtenerPorDni(dni));
    }

    @PutMapping("/{id}/activar")
    public ResponseEntity<AfiliadoResponse> activarAfiliado(@PathVariable Long id) {
        log.info("PUT /api/afiliados/{}/activar", id);
        return ResponseEntity.ok(afiliadoService.activarAfiliado(id));
    }

    @GetMapping("/status")
    public ResponseEntity<String> getStatus() {
        return ResponseEntity.ok("Afiliado Service Base OK");
    }
}

package com.mutualidad.beneficio.api.controller;

import com.mutualidad.beneficio.api.dto.AsignarBeneficioRequest;
import com.mutualidad.beneficio.api.dto.BeneficioResponse;
import com.mutualidad.beneficio.application.service.BeneficioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/beneficios")
@RequiredArgsConstructor
public class BeneficioController {

    private final BeneficioService beneficioService;

    @PostMapping
    public ResponseEntity<BeneficioResponse> asignarBeneficio(
            @Valid @RequestBody AsignarBeneficioRequest request) {
        log.info("POST /api/beneficios - Asignando beneficio");
        BeneficioResponse response = beneficioService.asignarBeneficio(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/basicos/{afiliadoId}")
    public ResponseEntity<List<BeneficioResponse>> asignarBeneficiosBasicos(
            @PathVariable Long afiliadoId) {
        log.info("POST /api/beneficios/basicos/{} - Asignando beneficios basicos", afiliadoId);
        List<BeneficioResponse> response = beneficioService.asignarBeneficiosBasicos(afiliadoId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/afiliado/{afiliadoId}")
    public ResponseEntity<List<BeneficioResponse>> obtenerBeneficiosPorAfiliado(
            @PathVariable Long afiliadoId) {
        log.info("GET /api/beneficios/afiliado/{}", afiliadoId);
        return ResponseEntity.ok(beneficioService.obtenerBeneficiosPorAfiliado(afiliadoId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BeneficioResponse> obtenerBeneficioPorId(@PathVariable Long id) {
        log.info("GET /api/beneficios/{}", id);
        return ResponseEntity.ok(beneficioService.obtenerBeneficioPorId(id));
    }

    @GetMapping("/status")
    public ResponseEntity<String> getStatus() {
        return ResponseEntity.ok("Beneficio Service OK");
    }
}

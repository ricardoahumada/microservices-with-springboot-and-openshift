package com.mutualidad.afiliado.api.controller;

import com.mutualidad.afiliado.api.dto.AfiliadoRequest;
import com.mutualidad.afiliado.api.dto.AfiliadoResponse;
import com.mutualidad.afiliado.application.service.AfiliadoService;
import com.mutualidad.afiliado.domain.model.Afiliado;
import com.mutualidad.afiliado.domain.model.DNI;
import com.mutualidad.afiliado.domain.model.EstadoAfiliado;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/afiliados")
@RequiredArgsConstructor
@Validated
public class AfiliadoController {

    private final AfiliadoService afiliadoService;

    @PostMapping
    public ResponseEntity<AfiliadoResponse> crear(
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody AfiliadoRequest request) {
        
        if (idempotencyKey == null || idempotencyKey.isEmpty()) {
            idempotencyKey = UUID.randomUUID().toString();
        }
        
        Afiliado afiliado = Afiliado.builder()
                .dni(DNI.of(request.getDni()))
                .nombre(request.getNombre())
                .apellidos(request.getApellidos())
                .fechaNacimiento(request.getFechaNacimiento())
                .email(request.getEmail())
                .telefono(request.getTelefono())
                .build();

        Afiliado creado = afiliadoService.crear(idempotencyKey, afiliado);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(AfiliadoResponse.fromEntity(creado));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AfiliadoResponse> buscarPorId(@PathVariable Long id) {
        return afiliadoService.buscarPorId(id)
                .map(a -> ResponseEntity.ok(AfiliadoResponse.fromEntity(a)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/dni/{dni}")
    public ResponseEntity<AfiliadoResponse> buscarPorDni(@PathVariable String dni) {
        return afiliadoService.buscarPorDni(dni)
                .map(a -> ResponseEntity.ok(AfiliadoResponse.fromEntity(a)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<AfiliadoResponse>> listarTodos(
            @RequestParam(required = false) EstadoAfiliado estado) {
        List<Afiliado> afiliados = (estado != null) 
                ? afiliadoService.buscarPorEstado(estado)
                : afiliadoService.listarTodos();
        
        List<AfiliadoResponse> response = afiliados.stream()
                .map(AfiliadoResponse::fromEntity)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AfiliadoResponse> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody AfiliadoRequest request) {
        Afiliado datos = Afiliado.builder()
                .nombre(request.getNombre())
                .apellidos(request.getApellidos())
                .email(request.getEmail())
                .telefono(request.getTelefono())
                .build();

        Afiliado actualizado = afiliadoService.actualizar(id, datos);
        return ResponseEntity.ok(AfiliadoResponse.fromEntity(actualizado));
    }

    @PostMapping("/{id}/activar")
    public ResponseEntity<AfiliadoResponse> activar(@PathVariable Long id) {
        Afiliado activado = afiliadoService.activar(id);
        return ResponseEntity.ok(AfiliadoResponse.fromEntity(activado));
    }

    @PostMapping("/{id}/baja")
    public ResponseEntity<AfiliadoResponse> darDeBaja(
            @PathVariable Long id,
            @RequestParam String motivo) {
        Afiliado dado = afiliadoService.darDeBaja(id, motivo);
        return ResponseEntity.ok(AfiliadoResponse.fromEntity(dado));
    }
}

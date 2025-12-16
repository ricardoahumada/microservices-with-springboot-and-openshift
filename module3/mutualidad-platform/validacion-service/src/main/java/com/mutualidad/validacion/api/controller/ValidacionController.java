package com.mutualidad.validacion.api.controller;

import com.mutualidad.validacion.api.dto.ValidacionRequest;
import com.mutualidad.validacion.api.dto.ValidacionResponse;
import com.mutualidad.validacion.api.dto.ValidarAfiliadoRequest;
import com.mutualidad.validacion.api.dto.ValidarAfiliadoResponse;
import com.mutualidad.validacion.application.service.ValidacionService;
import com.mutualidad.validacion.domain.model.ResultadoValidacion;
import com.mutualidad.validacion.domain.model.TipoValidacion;
import com.mutualidad.validacion.domain.model.ValidacionExterna;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/validaciones")
@RequiredArgsConstructor
@Validated
public class ValidacionController {

    private final ValidacionService validacionService;

    @PostMapping
    public ResponseEntity<ValidacionResponse> crear(@Valid @RequestBody ValidacionRequest request) {
        ValidacionExterna creada = validacionService.crear(
                request.getAfiliadoId(),
                request.getTipo(),
                request.getDatosEnviados());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ValidacionResponse.fromEntity(creada));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ValidacionResponse> buscarPorId(@PathVariable Long id) {
        return validacionService.buscarPorId(id)
                .map(v -> ResponseEntity.ok(ValidacionResponse.fromEntity(v)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<ValidacionResponse>> listar(
            @RequestParam(required = false) Long afiliadoId,
            @RequestParam(required = false) ResultadoValidacion resultado,
            @RequestParam(required = false) TipoValidacion tipo) {
        
        List<ValidacionExterna> validaciones;
        if (afiliadoId != null) {
            validaciones = validacionService.buscarPorAfiliado(afiliadoId);
        } else if (resultado != null) {
            validaciones = validacionService.buscarPorResultado(resultado);
        } else if (tipo != null) {
            validaciones = validacionService.buscarPorTipo(tipo);
        } else {
            validaciones = validacionService.buscarPorResultado(ResultadoValidacion.PENDIENTE);
        }
        
        List<ValidacionResponse> response = validaciones.stream()
                .map(ValidacionResponse::fromEntity)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/vigente")
    public ResponseEntity<ValidacionResponse> buscarVigente(
            @RequestParam Long afiliadoId,
            @RequestParam TipoValidacion tipo) {
        return validacionService.buscarValidacionVigente(afiliadoId, tipo)
                .map(v -> ResponseEntity.ok(ValidacionResponse.fromEntity(v)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/iniciar")
    public ResponseEntity<ValidacionResponse> iniciarProceso(
            @PathVariable Long id,
            @RequestParam String proveedor,
            @RequestParam String referencia) {
        ValidacionExterna iniciada = validacionService.iniciarProceso(id, proveedor, referencia);
        return ResponseEntity.ok(ValidacionResponse.fromEntity(iniciada));
    }

    @PostMapping("/{id}/aprobar")
    public ResponseEntity<ValidacionResponse> aprobar(
            @PathVariable Long id,
            @RequestParam Integer puntuacion,
            @RequestParam String mensaje) {
        ValidacionExterna aprobada = validacionService.aprobar(id, puntuacion, mensaje, null);
        return ResponseEntity.ok(ValidacionResponse.fromEntity(aprobada));
    }

    @PostMapping("/{id}/rechazar")
    public ResponseEntity<ValidacionResponse> rechazar(
            @PathVariable Long id,
            @RequestParam String mensaje) {
        ValidacionExterna rechazada = validacionService.rechazar(id, mensaje, null);
        return ResponseEntity.ok(ValidacionResponse.fromEntity(rechazada));
    }

    /**
     * Endpoint para validar datos de afiliado (usado por afiliado-service)
     */
    @PostMapping("/afiliado")
    public ResponseEntity<ValidarAfiliadoResponse> validarAfiliado(
            @RequestBody ValidarAfiliadoRequest request) {
        
        // Validacion simple de datos
        if (request.getDni() == null || request.getDni().isBlank()) {
            return ResponseEntity.ok(ValidarAfiliadoResponse.builder()
                .valido(false)
                .mensaje("DNI es obligatorio")
                .errores(List.of("DNI_REQUIRED"))
                .build());
        }
        
        if (request.getNombre() == null || request.getNombre().isBlank()) {
            return ResponseEntity.ok(ValidarAfiliadoResponse.builder()
                .valido(false)
                .mensaje("Nombre es obligatorio")
                .errores(List.of("NOMBRE_REQUIRED"))
                .build());
        }
        
        // Simulacion de validacion exitosa
        return ResponseEntity.ok(ValidarAfiliadoResponse.builder()
            .valido(true)
            .mensaje("Validacion exitosa")
            .errores(List.of())
            .build());
    }
}

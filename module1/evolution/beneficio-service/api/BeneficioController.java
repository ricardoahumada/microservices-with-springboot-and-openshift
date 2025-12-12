package com.mutualidad.beneficio.api.controller;

import com.mutualidad.beneficio.api.dto.BeneficioRequest;
import com.mutualidad.beneficio.api.dto.BeneficioResponse;
import com.mutualidad.beneficio.application.service.BeneficioService;
import com.mutualidad.beneficio.domain.model.Beneficio;
import com.mutualidad.beneficio.domain.model.TipoBeneficio;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/beneficios")
@RequiredArgsConstructor
@Validated
public class BeneficioController {

    private final BeneficioService beneficioService;

    @PostMapping
    public ResponseEntity<BeneficioResponse> crear(@Valid @RequestBody BeneficioRequest request) {
        Beneficio beneficio = Beneficio.builder()
                .nombre(request.getNombre())
                .descripcion(request.getDescripcion())
                .tipo(request.getTipo())
                .montoMaximo(request.getMontoMaximo())
                .diasCarencia(request.getDiasCarencia())
                .build();

        Beneficio creado = beneficioService.crear(beneficio);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BeneficioResponse.fromEntity(creado));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BeneficioResponse> buscarPorId(@PathVariable Long id) {
        return beneficioService.buscarPorId(id)
                .map(b -> ResponseEntity.ok(BeneficioResponse.fromEntity(b)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<BeneficioResponse>> listar(
            @RequestParam(required = false) TipoBeneficio tipo,
            @RequestParam(required = false, defaultValue = "false") Boolean soloActivos) {
        
        List<Beneficio> beneficios;
        if (tipo != null) {
            beneficios = beneficioService.buscarPorTipo(tipo);
        } else if (Boolean.TRUE.equals(soloActivos)) {
            beneficios = beneficioService.listarActivos();
        } else {
            beneficios = beneficioService.listarTodos();
        }
        
        List<BeneficioResponse> response = beneficios.stream()
                .map(BeneficioResponse::fromEntity)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<BeneficioResponse> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody BeneficioRequest request) {
        Beneficio datos = Beneficio.builder()
                .nombre(request.getNombre())
                .descripcion(request.getDescripcion())
                .montoMaximo(request.getMontoMaximo())
                .diasCarencia(request.getDiasCarencia())
                .build();

        Beneficio actualizado = beneficioService.actualizar(id, datos);
        return ResponseEntity.ok(BeneficioResponse.fromEntity(actualizado));
    }

    @PostMapping("/{id}/activar")
    public ResponseEntity<BeneficioResponse> activar(@PathVariable Long id) {
        Beneficio activado = beneficioService.activar(id);
        return ResponseEntity.ok(BeneficioResponse.fromEntity(activado));
    }

    @PostMapping("/{id}/desactivar")
    public ResponseEntity<BeneficioResponse> desactivar(@PathVariable Long id) {
        Beneficio desactivado = beneficioService.desactivar(id);
        return ResponseEntity.ok(BeneficioResponse.fromEntity(desactivado));
    }
}

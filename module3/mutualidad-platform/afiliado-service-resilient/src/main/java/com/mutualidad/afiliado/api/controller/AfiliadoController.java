package com.mutualidad.afiliado.api.controller;

import com.mutualidad.afiliado.api.dto.AfiliadoResponse;
import com.mutualidad.afiliado.api.dto.AltaAfiliadoRequest;
import com.mutualidad.afiliado.infrastructure.idempotency.IdempotencyRecord;
import com.mutualidad.afiliado.infrastructure.idempotency.IdempotencyService;
import com.mutualidad.afiliado.application.service.AfiliadoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/afiliados")
@RequiredArgsConstructor
public class AfiliadoController {

    private final AfiliadoService afiliadoService;
    private final IdempotencyService idempotencyService;

    private static final String IDEMPOTENCY_KEY_HEADER = "X-Idempotency-Key";

    /**
     * Alta de afiliado con soporte de idempotencia.
     * Si se proporciona X-Idempotency-Key y ya existe un registro,
     * se devuelve la respuesta anterior.
     */
    @PostMapping
    public ResponseEntity<AfiliadoResponse> altaAfiliado(
            @Valid @RequestBody AltaAfiliadoRequest request,
            @RequestHeader(value = IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey) {
        
        log.info("POST /api/afiliados - DNI: {}, Idempotency-Key: {}", 
                request.getDni(), idempotencyKey);

        // Si hay idempotency key, verificar si ya existe
        if (idempotencyKey != null && !idempotencyKey.isEmpty()) {
            Optional<IdempotencyRecord> existingRecord = idempotencyService.findByKey(idempotencyKey);
            
            if (existingRecord.isPresent()) {
                IdempotencyRecord record = existingRecord.get();
                
                // Verificar que el request es el mismo
                if (!idempotencyService.isRequestHashMatch(record, request)) {
                    log.warn("Idempotency key {} usado con request diferente", idempotencyKey);
                    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                            .body(AfiliadoResponse.builder()
                                    .mensaje("Idempotency key ya utilizado con un request diferente")
                                    .build());
                }
                
                log.info("Devolviendo respuesta cacheada para idempotency key: {}", idempotencyKey);
                AfiliadoResponse cachedResponse = idempotencyService.deserializeResponse(
                        record, AfiliadoResponse.class);
                return ResponseEntity.status(record.getStatusCode()).body(cachedResponse);
            }
        }

        // Procesar el alta
        AfiliadoResponse response = afiliadoService.altaAfiliado(request);

        // Guardar registro de idempotencia si hay key
        if (idempotencyKey != null && !idempotencyKey.isEmpty()) {
            idempotencyService.saveRecord(idempotencyKey, request, response, HttpStatus.CREATED.value());
        }

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

    @GetMapping("/status")
    public ResponseEntity<String> getStatus() {
        return ResponseEntity.ok("Afiliado Service Resilient OK");
    }
}

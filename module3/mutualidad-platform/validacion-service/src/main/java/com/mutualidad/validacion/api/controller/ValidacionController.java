package com.mutualidad.validacion.api.controller;

import com.mutualidad.validacion.api.dto.ValidacionEstadoLaboralRequest;
import com.mutualidad.validacion.api.dto.ValidacionEstadoLaboralResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@RestController
@RequestMapping("/api/validaciones")
public class ValidacionController {

    private final AtomicInteger requestCounter = new AtomicInteger(0);

    /**
     * Endpoint de validacion de estado laboral con simulacion de fallos.
     * 
     * @param request Datos de validacion
     * @param simulateError Tipo de error a simular: TIMEOUT, ERROR_500, ERROR_503, SLOW, NONE
     * @param delayMs Delay en milisegundos para simular latencia
     */
    @PostMapping("/estado-laboral")
    public ResponseEntity<ValidacionEstadoLaboralResponse> validarEstadoLaboral(
            @Valid @RequestBody ValidacionEstadoLaboralRequest request,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId,  // Agregar
            @RequestParam(value = "simulateError", defaultValue = "NONE") String simulateError,
            @RequestParam(value = "delayMs", defaultValue = "0") long delayMs) {
        
        int requestNum = requestCounter.incrementAndGet();
        if (correlationId == null) {
            correlationId = "corr-" + System.currentTimeMillis();
        }
        
        log.info("[{}] Request #{}: Validando estado laboral para DNI: {}, Empresa: {}, SimulateError: {}",
                correlationId, requestNum, request.getDni(), request.getEmpresaId(), simulateError);

        // Simular delay si se especifica
        if (delayMs > 0) {
            simulateDelay(delayMs);
        }

        // Simular diferentes tipos de errores
        switch (simulateError.toUpperCase()) {
            case "TIMEOUT":
                log.warn("[{}] Simulando TIMEOUT (10 segundos)", correlationId);
                simulateDelay(10000);
                break;
            case "ERROR_500":
                log.error("[{}] Simulando ERROR 500", correlationId);
                throw new RuntimeException("Error interno simulado");
            case "ERROR_503":
                log.error("[{}] Simulando ERROR 503 - Service Unavailable", correlationId);
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(ValidacionEstadoLaboralResponse.builder()
                                .valido(false)
                                .estado("ERROR")
                                .mensaje("Servicio temporalmente no disponible")
                                .build());
            case "ERROR_400":
                log.warn("[{}] Simulando ERROR 400 - Bad Request", correlationId);
                return ResponseEntity.badRequest()
                        .body(ValidacionEstadoLaboralResponse.builder()
                                .valido(false)
                                .estado("ERROR")
                                .mensaje("Solicitud invalida")
                                .build());
            case "SLOW":
                log.info("[{}] Simulando respuesta LENTA (3 segundos)", correlationId);
                simulateDelay(3000);
                break;
            case "INTERMITTENT":
                // Falla cada 3 requests
                if (requestNum % 3 == 0) {
                    log.error("[{}] Simulando fallo INTERMITENTE (request #{})", correlationId, requestNum);
                    throw new RuntimeException("Error intermitente simulado");
                }
                break;
            case "NONE":
            default:
                // Respuesta normal
                break;
        }

        // Respuesta exitosa
        ValidacionEstadoLaboralResponse response = ValidacionEstadoLaboralResponse.builder()
                .valido(true)
                .estado("ACTIVO")
                .mensaje("Estado laboral verificado correctamente")
                .dni(request.getDni())
                .empresaId(request.getEmpresaId())
                .build();

        log.info("[{}] Validacion completada exitosamente para DNI: {}", correlationId, request.getDni());
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint para resetear el contador de requests (util para pruebas)
     */
    @PostMapping("/reset-counter")
    public ResponseEntity<String> resetCounter() {
        requestCounter.set(0);
        log.info("Contador de requests reseteado");
        return ResponseEntity.ok("Contador reseteado");
    }

    /**
     * Endpoint para obtener el estado del servicio
     */
    @GetMapping("/status")
    public ResponseEntity<String> getStatus() {
        return ResponseEntity.ok("Validacion Service OK - Requests procesados: " + requestCounter.get());
    }

    private void simulateDelay(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String getCorrelationId() {
        // En produccion, se obtendria del header X-Correlation-ID
        return "corr-" + System.currentTimeMillis();
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ValidacionEstadoLaboralResponse> handleRuntimeException(RuntimeException ex) {
        log.error("Error en validacion: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ValidacionEstadoLaboralResponse.builder()
                        .valido(false)
                        .estado("ERROR")
                        .mensaje(ex.getMessage())
                        .build());
    }
}

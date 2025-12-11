package com.mutualidad.afiliado.controller;

import com.mutualidad.afiliado.client.dto.ValidacionResponse;
import com.mutualidad.afiliado.service.AfiliadoService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controlador para probar patrones de resiliencia.
 * Permite simular diferentes escenarios de fallo.
 */
@Slf4j
@RestController
@RequestMapping("/api/test/resiliencia")
@RequiredArgsConstructor
public class ResilienciaTestController {

    private final AfiliadoService afiliadoService;
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    /**
     * Probar validacion con simulacion de errores.
     * 
     * @param dni DNI a validar
     * @param empresaId ID de empresa
     * @param simulateError Tipo de error: NONE, TIMEOUT, ERROR_500, ERROR_503, SLOW, INTERMITTENT
     */
    @PostMapping("/validacion")
    public ResponseEntity<ValidacionResponse> testValidacion(
            @RequestParam String dni,
            @RequestParam String empresaId,
            @RequestParam(defaultValue = "NONE") String simulateError) {
        
        log.info("Test validacion - DNI: {}, Empresa: {}, SimulateError: {}", dni, empresaId, simulateError);
        
        ValidacionResponse response = afiliadoService.validarConSimulacion(dni, empresaId, simulateError);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Obtener estado de los Circuit Breakers.
     */
    @GetMapping("/circuit-breakers")
    public ResponseEntity<Map<String, Object>> getCircuitBreakersStatus() {
        Map<String, Object> status = new HashMap<>();
        
        circuitBreakerRegistry.getAllCircuitBreakers().forEach(cb -> {
            Map<String, Object> cbStatus = new HashMap<>();
            cbStatus.put("state", cb.getState().name());
            cbStatus.put("failureRate", cb.getMetrics().getFailureRate());
            cbStatus.put("slowCallRate", cb.getMetrics().getSlowCallRate());
            cbStatus.put("numberOfBufferedCalls", cb.getMetrics().getNumberOfBufferedCalls());
            cbStatus.put("numberOfFailedCalls", cb.getMetrics().getNumberOfFailedCalls());
            cbStatus.put("numberOfSuccessfulCalls", cb.getMetrics().getNumberOfSuccessfulCalls());
            status.put(cb.getName(), cbStatus);
        });
        
        return ResponseEntity.ok(status);
    }

    /**
     * Resetear un Circuit Breaker especifico.
     */
    @PostMapping("/circuit-breakers/{name}/reset")
    public ResponseEntity<String> resetCircuitBreaker(@PathVariable String name) {
        try {
            CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker(name);
            cb.reset();
            log.info("Circuit Breaker {} reseteado", name);
            return ResponseEntity.ok("Circuit Breaker " + name + " reseteado");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Forzar apertura de un Circuit Breaker.
     */
    @PostMapping("/circuit-breakers/{name}/force-open")
    public ResponseEntity<String> forceOpenCircuitBreaker(@PathVariable String name) {
        try {
            CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker(name);
            cb.transitionToOpenState();
            log.info("Circuit Breaker {} forzado a estado OPEN", name);
            return ResponseEntity.ok("Circuit Breaker " + name + " forzado a OPEN");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Forzar cierre de un Circuit Breaker.
     */
    @PostMapping("/circuit-breakers/{name}/force-close")
    public ResponseEntity<String> forceCloseCircuitBreaker(@PathVariable String name) {
        try {
            CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker(name);
            cb.transitionToClosedState();
            log.info("Circuit Breaker {} forzado a estado CLOSED", name);
            return ResponseEntity.ok("Circuit Breaker " + name + " forzado a CLOSED");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
}

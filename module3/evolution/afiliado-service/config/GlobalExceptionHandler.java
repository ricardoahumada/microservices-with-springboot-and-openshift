package com.mutualidad.afiliado.infrastructure.config;

import com.mutualidad.afiliado.domain.exception.BusinessException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessException(BusinessException e) {
        log.warn("Business exception: {} - {}", e.getCode(), e.getMessage());
        return ResponseEntity.badRequest().body(Map.of(
            "error", e.getCode(),
            "message", e.getMessage()
        ));
    }

    @ExceptionHandler(CallNotPermittedException.class)
    public ResponseEntity<Map<String, Object>> handleCircuitBreakerOpen(CallNotPermittedException e) {
        log.warn("Circuit breaker abierto: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
            "error", "CIRCUIT_OPEN",
            "message", "Servicio temporalmente no disponible"
        ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("Argumento invalido: {}", e.getMessage());
        return ResponseEntity.badRequest().body(Map.of(
            "error", "INVALID_ARGUMENT",
            "message", e.getMessage()
        ));
    }
}

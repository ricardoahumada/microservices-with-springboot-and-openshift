package com.mutualidad.afiliado.infrastructure.config;

import com.mutualidad.afiliado.api.dto.AfiliadoResponse;
import com.mutualidad.afiliado.domain.exception.BusinessException;
import com.mutualidad.afiliado.infrastructure.filter.CorrelationIdFilter;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<AfiliadoResponse> handleBusinessException(BusinessException ex) {
        String correlationId = CorrelationIdFilter.getCurrentCorrelationId();
        log.error("[{}] BusinessException: {}", correlationId, ex.getMessage());
        
        return ResponseEntity.badRequest()
                .body(AfiliadoResponse.builder()
                        .mensaje(ex.getMessage())
                        .build());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<AfiliadoResponse> handleValidationException(MethodArgumentNotValidException ex) {
        String correlationId = CorrelationIdFilter.getCurrentCorrelationId();
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        
        log.error("[{}] ValidationException: {}", correlationId, errors);
        
        return ResponseEntity.badRequest()
                .body(AfiliadoResponse.builder()
                        .mensaje("Errores de validacion: " + errors)
                        .build());
    }

    @ExceptionHandler(CallNotPermittedException.class)
    public ResponseEntity<AfiliadoResponse> handleCircuitBreakerOpen(CallNotPermittedException ex) {
        String correlationId = CorrelationIdFilter.getCurrentCorrelationId();
        log.error("[{}] Circuit Breaker OPEN: {}", correlationId, ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(AfiliadoResponse.builder()
                        .mensaje("Servicio temporalmente no disponible. Por favor, intente mas tarde.")
                        .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<AfiliadoResponse> handleGenericException(Exception ex) {
        String correlationId = CorrelationIdFilter.getCurrentCorrelationId();
        log.error("[{}] Exception: {}", correlationId, ex.getMessage(), ex);
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(AfiliadoResponse.builder()
                        .mensaje("Error interno del servidor")
                        .build());
    }
}

package com.mutualidad.afiliado.config;

import com.mutualidad.afiliado.dto.AfiliadoResponse;
import com.mutualidad.afiliado.exception.BusinessException;
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
        log.error("BusinessException: {}", ex.getMessage());
        
        return ResponseEntity.badRequest()
                .body(AfiliadoResponse.builder()
                        .mensaje(ex.getMessage())
                        .build());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<AfiliadoResponse> handleValidationException(MethodArgumentNotValidException ex) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        
        log.error("ValidationException: {}", errors);
        
        return ResponseEntity.badRequest()
                .body(AfiliadoResponse.builder()
                        .mensaje("Errores de validacion: " + errors)
                        .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<AfiliadoResponse> handleGenericException(Exception ex) {
        log.error("Exception: {}", ex.getMessage(), ex);
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(AfiliadoResponse.builder()
                        .mensaje("Error interno del servidor")
                        .build());
    }
}

package com.mutualidad.afiliado.infrastructure.config;

import com.mutualidad.afiliado.domain.exception.AfiliadoNoEncontradoException;
import com.mutualidad.afiliado.domain.exception.AfiliadoYaExisteException;
import com.mutualidad.afiliado.domain.exception.DocumentoInvalidoException;
import com.mutualidad.afiliado.domain.exception.EstadoInvalidoException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(AfiliadoNoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> handleAfiliadoNoEncontrado(AfiliadoNoEncontradoException ex) {
        log.warn("Afiliado no encontrado: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(AfiliadoYaExisteException.class)
    public ResponseEntity<Map<String, Object>> handleAfiliadoYaExiste(AfiliadoYaExisteException ex) {
        log.warn("Afiliado ya existe: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(DocumentoInvalidoException.class)
    public ResponseEntity<Map<String, Object>> handleDocumentoInvalido(DocumentoInvalidoException ex) {
        log.warn("Documento invalido: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(EstadoInvalidoException.class)
    public ResponseEntity<Map<String, Object>> handleEstadoInvalido(EstadoInvalidoException ex) {
        log.warn("Estado invalido: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Argumento invalido: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        log.error("Error inesperado: ", ex);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor");
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}

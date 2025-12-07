package com.mutualidad.afiliado.idempotency;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final IdempotencyRepository idempotencyRepository;
    private final ObjectMapper objectMapper;

    /**
     * Verifica si existe un registro de idempotencia para la clave dada.
     */
    public Optional<IdempotencyRecord> findByKey(String idempotencyKey) {
        return idempotencyRepository.findById(idempotencyKey);
    }

    /**
     * Guarda un nuevo registro de idempotencia.
     */
    @Transactional
    public IdempotencyRecord saveRecord(String idempotencyKey, Object request, Object response, int statusCode) {
        String requestHash = computeHash(request);
        String responseBody = serializeToJson(response);

        IdempotencyRecord record = IdempotencyRecord.builder()
                .idempotencyKey(idempotencyKey)
                .requestHash(requestHash)
                .responseBody(responseBody)
                .statusCode(statusCode)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();

        log.info("Guardando registro de idempotencia: {}", idempotencyKey);
        return idempotencyRepository.save(record);
    }

    /**
     * Verifica si el request actual coincide con el guardado.
     */
    public boolean isRequestHashMatch(IdempotencyRecord record, Object request) {
        String currentHash = computeHash(request);
        return currentHash.equals(record.getRequestHash());
    }

    /**
     * Deserializa la respuesta guardada.
     */
    public <T> T deserializeResponse(IdempotencyRecord record, Class<T> responseType) {
        try {
            return objectMapper.readValue(record.getResponseBody(), responseType);
        } catch (JsonProcessingException e) {
            log.error("Error deserializando respuesta: {}", e.getMessage());
            throw new RuntimeException("Error deserializando respuesta", e);
        }
    }

    /**
     * Limpia registros expirados (ejecutar periodicamente).
     */
    @Scheduled(fixedRate = 3600000) // Cada hora
    @Transactional
    public void cleanupExpiredRecords() {
        log.info("Limpiando registros de idempotencia expirados");
        idempotencyRepository.deleteExpiredRecords(LocalDateTime.now());
    }

    private String computeHash(Object request) {
        try {
            String json = objectMapper.writeValueAsString(request);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(json.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (JsonProcessingException | NoSuchAlgorithmException e) {
            log.error("Error computando hash: {}", e.getMessage());
            throw new RuntimeException("Error computando hash", e);
        }
    }

    private String serializeToJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("Error serializando a JSON: {}", e.getMessage());
            throw new RuntimeException("Error serializando a JSON", e);
        }
    }
}

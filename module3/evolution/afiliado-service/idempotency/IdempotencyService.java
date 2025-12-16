package com.mutualidad.afiliado.infrastructure.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class IdempotencyService {

    private final IdempotencyRepository repository;
    private final ObjectMapper objectMapper;

    private static final Duration DEFAULT_TTL = Duration.ofHours(24);

    public <T> Optional<T> getExistingResponse(String key, Class<T> responseType) {
        return repository.findById(key)
            .filter(record -> !record.isExpired())
            .map(record -> {
                try {
                    return objectMapper.readValue(record.getResponse(), responseType);
                } catch (Exception e) {
                    log.error("Error deserializando respuesta idempotente", e);
                    return null;
                }
            });
    }

    public <T> void saveResponse(String key, T response) {
        try {
            String json = objectMapper.writeValueAsString(response);
            repository.save(new IdempotencyRecord(key, json, DEFAULT_TTL));
        } catch (Exception e) {
            log.error("Error guardando respuesta idempotente", e);
        }
    }

    @Scheduled(fixedRate = 3600000)
    @Transactional
    public void cleanupExpired() {
        log.debug("Limpiando registros de idempotencia expirados");
        repository.deleteExpired(LocalDateTime.now());
    }
}

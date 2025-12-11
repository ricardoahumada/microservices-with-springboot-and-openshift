package com.mutualidad.validacion.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

// TODO: Importar anotaciones de Kafka
// import org.springframework.kafka.annotation.KafkaListener;

/**
 * Consumer de eventos para validar afiliados.
 * 
 * Funcionalidades:
 * 1. Validar datos del afiliado
 * 2. Rechazar DNIs inválidos (simular error para DLQ)
 * 3. Escuchar Dead Letter Topic para mensajes fallidos
 */
@Service
public class AfiliadoValidationConsumer {

    private static final Logger log = LoggerFactory.getLogger(AfiliadoValidationConsumer.class);

    /**
     * TODO: Implementar listener principal
     * 
     * @KafkaListener(topics = "afiliado-eventos", groupId = "validacion-group")
     */
    // @KafkaListener(topics = "afiliado-eventos", groupId = "validacion-group")
    public void handleAfiliadoEvent(String eventJson) {
        log.info("=== VALIDACION-SERVICE: Evento recibido ===");
        log.info("Payload: {}", eventJson);
        
        // TODO: Extraer DNI del evento
        // Simular validación: DNIs que empiezan con "FAIL" lanzan excepción
        
        if (eventJson.contains("\"dni\":\"FAIL")) {
            log.error("Validacion fallida para DNI invalido");
            // TODO: Lanzar excepción para activar reintentos y DLQ
            // throw new RuntimeException("DNI invalido - mensaje irá a DLQ después de reintentos");
        }
        
        log.info("[VALIDACION OK] Afiliado validado correctamente");
    }

    /**
     * TODO AVANZADO: Listener para Dead Letter Topic
     * 
     * Procesa mensajes que fallaron después de todos los reintentos.
     * El topic DLT se crea automáticamente como: {topic-original}.dlt
     * 
     * @KafkaListener(topics = "afiliado-eventos.dlt", groupId = "validacion-dlt-group")
     */
    // @KafkaListener(topics = "afiliado-eventos.dlt", groupId = "validacion-dlt-group")
    public void handleDeadLetterEvent(String eventJson) {
        log.warn("=== DLT: Mensaje en Dead Letter Topic ===");
        log.warn("Este mensaje falló después de todos los reintentos");
        log.warn("Payload: {}", eventJson);
        log.warn("TODO: Implementar lógica de recuperación manual o alertas");
    }
}

package com.mutualidad.notificacion.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

// TODO: Importar anotaciones de Kafka
// import org.springframework.kafka.annotation.KafkaListener;

/**
 * Consumer de eventos de afiliado para enviar notificaciones.
 * 
 * Responsabilidades:
 * 1. Escuchar el topic "afiliado-eventos"
 * 2. Procesar eventos según su tipo
 * 3. Simular envío de email/SMS
 */
@Service
public class AfiliadoEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(AfiliadoEventConsumer.class);

    /**
     * TODO: Implementar listener de Kafka
     * 
     * Pasos:
     * 1. Añadir @KafkaListener con topic y groupId
     * 2. Deserializar el evento (puede ser String JSON)
     * 3. Procesar según eventType
     * 
     * Ejemplo de anotación:
     * @KafkaListener(topics = "afiliado-eventos", groupId = "notificacion-group")
     * 
     * @param eventJson El mensaje recibido como JSON String
     */
    // @KafkaListener(topics = "afiliado-eventos", groupId = "notificacion-group")
    public void handleAfiliadoEvent(String eventJson) {
        log.info("=== NOTIFICACION-SERVICE: Evento recibido ===");
        log.info("Payload: {}", eventJson);
        
        // TODO: Parsear el JSON y extraer datos
        // ObjectMapper mapper = new ObjectMapper();
        // AfiliadoEvent event = mapper.readValue(eventJson, AfiliadoEvent.class);
        
        // TODO: Procesar según tipo de evento
        if (eventJson.contains("AFILIADO_CREATED")) {
            log.info("[EMAIL] Enviando bienvenida al nuevo afiliado");
            log.info("[SMS] Notificando alta en el sistema");
        } else if (eventJson.contains("AFILIADO_UPDATED")) {
            log.info("[EMAIL] Confirmando actualización de datos");
        } else if (eventJson.contains("AFILIADO_DELETED")) {
            log.info("[EMAIL] Confirmando baja del sistema");
        }
    }
}

package com.mutualidad.notificacion.service;

import com.mutualidad.notificacion.event.AfiliadoEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AfiliadoEventConsumer {

    @KafkaListener(
        topics = "${app.kafka.topic.afiliado-eventos}",
        groupId = "${spring.kafka.consumer.group-id}"
    )
    public void handleAfiliadoEvent(
            ConsumerRecord<String, AfiliadoEvent> record,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {
        
        AfiliadoEvent event = record.value();
        
        log.info("=== NOTIFICACION-SERVICE: Evento recibido ===");
        log.info("Topic: {}, Partition: {}, Offset: {}", record.topic(), partition, offset);
        log.info("EventId: {}, EventType: {}", event.getEventId(), event.getEventType());
        log.info("Payload: {}", event.getPayload());
        
        // Simular envio de notificacion
        sendNotification(event);
    }

    private void sendNotification(AfiliadoEvent event) {
        String eventType = event.getEventType();
        AfiliadoEvent.AfiliadoPayload payload = event.getPayload();
        
        switch (eventType) {
            case "AFILIADO_CREATED":
                log.info("[EMAIL] Enviando bienvenida a {} - {}", 
                    payload.getNombre() + " " + payload.getApellidos(), 
                    payload.getEmail());
                log.info("[SMS] Notificando alta de afiliado DNI: {}", payload.getDni());
                break;
            case "AFILIADO_UPDATED":
                log.info("[EMAIL] Notificando actualizacion de datos a {}", payload.getEmail());
                break;
            default:
                log.warn("Tipo de evento no reconocido: {}", eventType);
        }
    }
}

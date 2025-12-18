package com.mutualidad.notificacion.service;

import com.mutualidad.notificacion.event.AfiliadoEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AfiliadoEventConsumer {

    /**
     * TODO Ejercicio 3: Implementar consumidor de eventos de afiliado
     * 
     * Pasos:
     * 1. Anotar el metodo con @KafkaListener configurando:
     *    - topics: "${app.kafka.topic.afiliado-eventos}"
     *    - groupId: "${spring.kafka.consumer.group-id}"
     * 
     * 2. El metodo recibe ConsumerRecord<String, AfiliadoEvent>
     * 
     * 3. Extraer el evento del record con record.value()
     * 
     * 4. Loguear informacion del mensaje:
     *    - Topic, Partition, Offset
     *    - EventId, EventType
     *    - Payload
     * 
     * 5. Procesar el evento segun su tipo (AFILIADO_CREATED, AFILIADO_UPDATED)
     */
    @KafkaListener(
        topics = "${app.kafka.topic.afiliado-eventos}",
        groupId = "${spring.kafka.consumer.group-id}"
    )
    public void handleAfiliadoEvent(ConsumerRecord<String, AfiliadoEvent> record) {
        // TODO: Implementar
        log.info("TODO: Procesar evento de afiliado");
        
        // Ejemplo de implementacion:
        AfiliadoEvent event = record.value();
        
        log.info("=== NOTIFICACION-SERVICE: Evento recibido ===");
        log.info("Topic: {}, Partition: {}, Offset: {}", 
            record.topic(), record.partition(), record.offset());
        log.info("EventId: {}, EventType: {}", event.getEventId(), event.getEventType());
        
        sendNotification(event);
    }

    /**
     * TODO Ejercicio 3: Implementar logica de envio de notificaciones
     * 
     * Segun el tipo de evento:
     * - AFILIADO_CREATED: Enviar email de bienvenida + SMS
     * - AFILIADO_UPDATED: Enviar email de confirmacion de cambios
     */
    private void sendNotification(AfiliadoEvent event) {
        // TODO: Implementar
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

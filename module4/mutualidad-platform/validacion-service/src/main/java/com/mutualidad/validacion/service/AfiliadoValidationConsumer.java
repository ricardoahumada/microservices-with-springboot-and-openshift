package com.mutualidad.validacion.service;

import com.mutualidad.validacion.event.AfiliadoEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AfiliadoValidationConsumer {

    @KafkaListener(
        topics = "${app.kafka.topic.afiliado-eventos}",
        groupId = "${spring.kafka.consumer.group-id}"
    )
    public void handleAfiliadoEvent(ConsumerRecord<String, AfiliadoEvent> record) {
        
        AfiliadoEvent event = record.value();
        
        log.info("=== VALIDACION-SERVICE: Evento recibido ===");
        log.info("Topic: {}, Partition: {}, Offset: {}", record.topic(), record.partition(), record.offset());
        log.info("EventId: {}, EventType: {}", event.getEventId(), event.getEventType());
        
        // Simular validacion - falla si DNI empieza con "FAIL"
        validateAfiliado(event);
    }

    private void validateAfiliado(AfiliadoEvent event) {
        String dni = event.getPayload().getDni();
        
        // Simular fallo para DNIs que empiezan con "FAIL" (para probar DLQ)
        if (dni != null && dni.startsWith("FAIL")) {
            log.error("Validacion fallida para DNI: {}", dni);
            throw new RuntimeException("DNI invalido: " + dni);
        }
        
        log.info("[VALIDACION OK] Afiliado validado: DNI={}, Nombre={} {}", 
            dni, 
            event.getPayload().getNombre(), 
            event.getPayload().getApellidos());
    }

    @KafkaListener(
        topics = "${app.kafka.topic.afiliado-eventos-dlt}",
        groupId = "validacion-dlt-group"
    )
    public void handleDltEvent(ConsumerRecord<String, AfiliadoEvent> record) {
        AfiliadoEvent event = record.value();
        
        log.warn("=== DLT: Mensaje recibido en Dead Letter Topic ===");
        log.warn("EventId: {}, DNI: {}", event.getEventId(), event.getPayload().getDni());
        log.warn("Este mensaje requiere intervencion manual o reprocesamiento");
    }
}

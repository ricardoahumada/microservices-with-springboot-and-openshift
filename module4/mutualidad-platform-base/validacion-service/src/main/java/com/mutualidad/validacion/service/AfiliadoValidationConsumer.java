package com.mutualidad.validacion.service;

import com.mutualidad.validacion.event.AfiliadoEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AfiliadoValidationConsumer {

    /**
     * TODO Ejercicio 4: Implementar consumidor de eventos para validacion
     * 
     * Similar al Ejercicio 3, pero con logica de validacion que puede fallar.
     * Esto prepara el escenario para el Ejercicio 5 (DLQ).
     * 
     * Pasos:
     * 1. Anotar con @KafkaListener
     * 2. Extraer evento del record
     * 3. Llamar a validateAfiliado(event)
     */
    // @KafkaListener(
    //     topics = "${app.kafka.topic.afiliado-eventos}",
    //     groupId = "${spring.kafka.consumer.group-id}"
    // )
    public void handleAfiliadoEvent(ConsumerRecord<String, AfiliadoEvent> record) {
        // TODO: Implementar
        log.info("TODO: Validar evento de afiliado");
        
        // Ejemplo:
        // AfiliadoEvent event = record.value();
        // 
        // log.info("=== VALIDACION-SERVICE: Evento recibido ===");
        // log.info("Topic: {}, Partition: {}, Offset: {}", 
        //     record.topic(), record.partition(), record.offset());
        // 
        // validateAfiliado(event);
    }

    /**
     * TODO Ejercicio 4: Implementar logica de validacion
     * 
     * La validacion debe fallar (lanzar excepcion) para DNIs que empiezan con "FAIL".
     * Esto permite probar el DLQ en el Ejercicio 5.
     */
    private void validateAfiliado(AfiliadoEvent event) {
        // TODO: Implementar
        // String dni = event.getPayload().getDni();
        // 
        // // Simular fallo para DNIs que empiezan con "FAIL"
        // if (dni != null && dni.startsWith("FAIL")) {
        //     log.error("Validacion fallida para DNI: {}", dni);
        //     throw new RuntimeException("DNI invalido: " + dni);
        // }
        // 
        // log.info("[VALIDACION OK] Afiliado validado: DNI={}", dni);
    }

    /**
     * TODO Ejercicio 5: Implementar consumidor del Dead Letter Topic (DLT)
     * 
     * Este metodo procesa los mensajes que fallaron despues de los reintentos.
     * 
     * Pasos:
     * 1. Anotar con @KafkaListener apuntando al topic DLT
     * 2. Loguear el mensaje fallido para revision manual
     */
    // @KafkaListener(
    //     topics = "${app.kafka.topic.afiliado-eventos-dlt}",
    //     groupId = "validacion-dlt-group"
    // )
    public void handleDltEvent(ConsumerRecord<String, AfiliadoEvent> record) {
        // TODO: Implementar
        log.warn("TODO: Procesar mensaje de DLT");
        
        // Ejemplo:
        // AfiliadoEvent event = record.value();
        // 
        // log.warn("=== DLT: Mensaje recibido en Dead Letter Topic ===");
        // log.warn("EventId: {}, DNI: {}", event.getEventId(), event.getPayload().getDni());
        // log.warn("Este mensaje requiere intervencion manual");
    }
}

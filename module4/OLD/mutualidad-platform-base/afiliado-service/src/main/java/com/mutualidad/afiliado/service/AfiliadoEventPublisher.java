package com.mutualidad.afiliado.service;

import com.mutualidad.afiliado.event.AfiliadoEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

// TODO: Importar KafkaTemplate
// import org.springframework.kafka.core.KafkaTemplate;

/**
 * Servicio para publicar eventos de afiliado a Kafka.
 * 
 * Responsabilidades:
 * 1. Crear eventos AfiliadoEvent
 * 2. Publicar al topic "afiliado-eventos"
 * 3. Manejar callbacks de éxito/error
 */
@Service
public class AfiliadoEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(AfiliadoEventPublisher.class);
    private static final String TOPIC = "afiliado-eventos";

    // TODO: Inyectar KafkaTemplate
    // private final KafkaTemplate<String, AfiliadoEvent> kafkaTemplate;
    //
    // public AfiliadoEventPublisher(KafkaTemplate<String, AfiliadoEvent> kafkaTemplate) {
    //     this.kafkaTemplate = kafkaTemplate;
    // }

    /**
     * Publica un evento de creación de afiliado.
     * 
     * TODO: Implementar publicación a Kafka
     * 
     * Pasos:
     * 1. Crear AfiliadoEvent con tipo "AFILIADO_CREATED"
     * 2. Usar kafkaTemplate.send(TOPIC, key, event)
     * 3. Manejar el resultado con whenComplete()
     * 
     * @param dni Clave del mensaje (para particionado)
     * @param nombre Nombre del afiliado
     * @param apellidos Apellidos del afiliado
     * @param email Email del afiliado
     */
    public void publishAfiliadoCreated(String dni, String nombre, String apellidos, String email) {
        log.info("TODO: Publicar evento AFILIADO_CREATED para DNI: {}", dni);
        
        // TODO: Crear el evento
        // AfiliadoEvent event = new AfiliadoEvent("AFILIADO_CREATED", dni, nombre, apellidos, email);
        
        // TODO: Enviar a Kafka
        // kafkaTemplate.send(TOPIC, dni, event)
        //     .whenComplete((result, ex) -> {
        //         if (ex == null) {
        //             log.info("Evento publicado: topic={}, partition={}, offset={}",
        //                 TOPIC,
        //                 result.getRecordMetadata().partition(),
        //                 result.getRecordMetadata().offset());
        //         } else {
        //             log.error("Error publicando evento: {}", ex.getMessage());
        //         }
        //     });
    }
}

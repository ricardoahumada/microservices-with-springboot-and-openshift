package com.mutualidad.afiliado.service;

import com.mutualidad.afiliado.event.AfiliadoEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AfiliadoEventPublisher {

    // TODO Ejercicio 2: Inyectar KafkaTemplate para publicar eventos
    private final KafkaTemplate<String, AfiliadoEvent> kafkaTemplate;

    // TODO Ejercicio 2: Inyectar nombre del topic desde configuracion
    @Value("${app.kafka.topic.afiliado-eventos}")
    private String topic;

    /**
     * TODO Ejercicio 2: Implementar publicacion de evento AFILIADO_CREATED
     * 
     * Pasos:
     * 1. Generar un UUID para el afiliadoId
     * 2. Construir el objeto AfiliadoEvent con:
     *    - eventId: UUID aleatorio
     *    - eventType: "AFILIADO_CREATED"
     *    - timestamp: LocalDateTime.now()
     *    - payload: datos del afiliado
     * 3. Enviar al topic usando kafkaTemplate.send(topic, key, event)
     *    - Usar DNI como key para garantizar orden por afiliado
     * 4. Agregar callback para logging de exito/error
     */
    public void publishAfiliadoCreated(String dni, String nombre, String apellidos, 
                                        String email, String empresaId) {
        // TODO: Implementar
        log.info("TODO: Publicar evento AFILIADO_CREATED para DNI: {}", dni);
        
        // Ejemplo de implementacion:
        String afiliadoId = UUID.randomUUID().toString();
        
        AfiliadoEvent event = AfiliadoEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("AFILIADO_CREATED")
                .timestamp(LocalDateTime.now())
                .payload(AfiliadoEvent.AfiliadoPayload.builder()
                        .afiliadoId(afiliadoId)
                        .dni(dni)
                        .nombre(nombre)
                        .apellidos(apellidos)
                        .email(email)
                        .empresaId(empresaId)
                        .build())
                .build();
        
        ListenableFuture<SendResult<String, AfiliadoEvent>> future = 
            kafkaTemplate.send(topic, dni, event);
        
        future.addCallback(new ListenableFutureCallback<>() {
            @Override
            public void onSuccess(SendResult<String, AfiliadoEvent> result) {
                log.info("Evento publicado: partition={}, offset={}", 
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());
            }
            @Override
            public void onFailure(Throwable ex) {
                log.error("Error publicando evento: {}", ex.getMessage());
            }
        });
    }

    /**
     * TODO Ejercicio 2 (opcional): Implementar publicacion de evento AFILIADO_UPDATED
     */
    public void publishAfiliadoUpdated(String afiliadoId, String dni, String nombre, 
                                        String apellidos, String email, String empresaId) {
        // TODO: Implementar similar a publishAfiliadoCreated
        log.info("TODO: Publicar evento AFILIADO_UPDATED para DNI: {}", dni);
    }
}

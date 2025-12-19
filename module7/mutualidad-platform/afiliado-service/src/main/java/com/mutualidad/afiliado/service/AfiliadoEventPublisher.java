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

    private final KafkaTemplate<String, AfiliadoEvent> kafkaTemplate;

    @Value("${app.kafka.topic.afiliado-eventos}")
    private String topic;

    public void publishAfiliadoCreated(String dni, String nombre, String apellidos, 
                                        String email, String empresaId) {
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

        // Usar DNI como key para garantizar orden por afiliado
        ListenableFuture<SendResult<String, AfiliadoEvent>> future = 
            kafkaTemplate.send(topic, dni, event);

        future.addCallback(new ListenableFutureCallback<>() {
            @Override
            public void onSuccess(SendResult<String, AfiliadoEvent> result) {
                log.info("Evento publicado exitosamente: topic={}, partition={}, offset={}, eventId={}",
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset(),
                        event.getEventId());
            }

            @Override
            public void onFailure(Throwable ex) {
                log.error("Error publicando evento: {}", ex.getMessage(), ex);
            }
        });
    }

    public void publishAfiliadoUpdated(String afiliadoId, String dni, String nombre, 
                                        String apellidos, String email, String empresaId) {
        AfiliadoEvent event = AfiliadoEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("AFILIADO_UPDATED")
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

        kafkaTemplate.send(topic, dni, event);
        log.info("Evento AFILIADO_UPDATED publicado: {}", event.getEventId());
    }
}

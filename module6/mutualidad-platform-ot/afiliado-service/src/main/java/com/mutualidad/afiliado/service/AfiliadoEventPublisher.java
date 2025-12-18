package com.mutualidad.afiliado.service;

import com.mutualidad.afiliado.event.AfiliadoEvent;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapSetter;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Headers;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
public class AfiliadoEventPublisher {

    private final KafkaTemplate<String, AfiliadoEvent> kafkaTemplate;
    private final OpenTelemetry openTelemetry;
    private final Tracer tracer;

    @Value("${app.kafka.topic.afiliado-eventos}")
    private String topic;

    public AfiliadoEventPublisher(KafkaTemplate<String, AfiliadoEvent> kafkaTemplate, OpenTelemetry openTelemetry) {
        this.kafkaTemplate = kafkaTemplate;
        this.openTelemetry = openTelemetry;
        this.tracer = openTelemetry.getTracer("afiliado-service");
    }

    private static final TextMapSetter<Headers> KAFKA_SETTER = (headers, key, value) -> {
        if (headers != null && key != null && value != null) {
            headers.remove(key);
            headers.add(key, value.getBytes(StandardCharsets.UTF_8));
        }
    };

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

        sendWithTracing(dni, event);
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

        sendWithTracing(dni, event);
    }

    private void sendWithTracing(String key, AfiliadoEvent event) {
        Span span = tracer.spanBuilder("kafka.send")
                .setSpanKind(SpanKind.PRODUCER)
                .setAttribute("messaging.system", "kafka")
                .setAttribute("messaging.destination", topic)
                .setAttribute("messaging.operation", "send")
                .setAttribute("event.id", event.getEventId())
                .setAttribute("event.type", event.getEventType())
                .startSpan();

        try {
            ProducerRecord<String, AfiliadoEvent> record = new ProducerRecord<>(topic, key, event);

            // Inyectar contexto de trazas en los headers de Kafka
            openTelemetry.getPropagators()
                    .getTextMapPropagator()
                    .inject(Context.current().with(span), record.headers(), KAFKA_SETTER);

            log.info("Publicando evento con TraceId: {}", span.getSpanContext().getTraceId());

            ListenableFuture<SendResult<String, AfiliadoEvent>> future = kafkaTemplate.send(record);

            future.addCallback(new ListenableFutureCallback<>() {
                @Override
                public void onSuccess(SendResult<String, AfiliadoEvent> result) {
                    log.info("Evento publicado exitosamente: topic={}, partition={}, offset={}, eventId={}",
                            result.getRecordMetadata().topic(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset(),
                            event.getEventId());
                    span.end();
                }

                @Override
                public void onFailure(Throwable ex) {
                    log.error("Error publicando evento: {}", ex.getMessage(), ex);
                    span.recordException(ex);
                    span.end();
                }
            });
        } catch (Exception e) {
            span.recordException(e);
            span.end();
            throw e;
        }
    }
}

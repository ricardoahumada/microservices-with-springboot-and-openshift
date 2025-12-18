package com.mutualidad.notificacion.service;

import com.mutualidad.notificacion.event.AfiliadoEvent;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Slf4j
@Service
public class AfiliadoEventConsumer {

    private final OpenTelemetry openTelemetry;
    private final Tracer tracer;

    public AfiliadoEventConsumer(OpenTelemetry openTelemetry) {
        this.openTelemetry = openTelemetry;
        this.tracer = openTelemetry.getTracer("notificacion-service");
    }

    private static final TextMapGetter<Headers> KAFKA_GETTER = new TextMapGetter<Headers>() {
        @Override
        public Iterable<String> keys(Headers headers) {
            return () -> java.util.stream.StreamSupport.stream(headers.spliterator(), false)
                    .map(Header::key)
                    .iterator();
        }

        @Override
        public String get(Headers headers, String key) {
            Header header = headers.lastHeader(key);
            return header != null ? new String(header.value(), StandardCharsets.UTF_8) : null;
        }
    };

    @KafkaListener(
        topics = "${app.kafka.topic.afiliado-eventos}",
        groupId = "${spring.kafka.consumer.group-id}"
    )
    public void handleAfiliadoEvent(ConsumerRecord<String, AfiliadoEvent> record) {
        // Extraer contexto de trazas desde los headers de Kafka
        Context extractedContext = openTelemetry.getPropagators()
                .getTextMapPropagator()
                .extract(Context.current(), record.headers(), KAFKA_GETTER);

        // Crear span hijo con el contexto extraído
        Span span = tracer.spanBuilder("notificacion-service.process")
                .setParent(extractedContext)
                .setSpanKind(SpanKind.CONSUMER)
                .setAttribute("messaging.system", "kafka")
                .setAttribute("messaging.destination", record.topic())
                .setAttribute("messaging.operation", "process")
                .startSpan();

        try {
            AfiliadoEvent event = record.value();

            span.setAttribute("event.id", event.getEventId());
            span.setAttribute("event.type", event.getEventType());

            log.info("=== NOTIFICACION-SERVICE: Evento recibido ===");
            log.info("Topic: {}, Partition: {}, Offset: {}", record.topic(), record.partition(), record.offset());
            log.info("EventId: {}, EventType: {}", event.getEventId(), event.getEventType());
            log.info("Payload: {}", event.getPayload());
            log.info("TraceId: {}", span.getSpanContext().getTraceId());

            // Simular envio de notificacion
            sendNotification(event, span);
        } catch (Exception e) {
            span.recordException(e);
            throw e;
        } finally {
            span.end();
        }
    }

    private void sendNotification(AfiliadoEvent event, Span parentSpan) {
        String eventType = event.getEventType();
        AfiliadoEvent.AfiliadoPayload payload = event.getPayload();

        Span sendSpan = tracer.spanBuilder("notificacion-service.sendNotification")
                .setParent(Context.current().with(parentSpan))
                .startSpan();

        try {
            switch (eventType) {
                case "AFILIADO_CREATED":
                    log.info("[EMAIL] Enviando bienvenida a {} - {}",
                        payload.getNombre() + " " + payload.getApellidos(),
                        payload.getEmail());
                    log.info("[SMS] Notificando alta de afiliado DNI: {}", payload.getDni());
                    sendSpan.setAttribute("notification.type", "welcome");
                    break;
                case "AFILIADO_UPDATED":
                    log.info("[EMAIL] Notificando actualizacion de datos a {}", payload.getEmail());
                    sendSpan.setAttribute("notification.type", "update");
                    break;
                default:
                    log.warn("Tipo de evento no reconocido: {}", eventType);
                    sendSpan.setAttribute("notification.type", "unknown");
            }
        } finally {
            sendSpan.end();
        }
    }
}

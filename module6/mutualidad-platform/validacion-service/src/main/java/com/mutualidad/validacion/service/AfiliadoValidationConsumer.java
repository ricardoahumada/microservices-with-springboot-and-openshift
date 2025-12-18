package com.mutualidad.validacion.service;

import com.mutualidad.validacion.event.AfiliadoEvent;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
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
public class AfiliadoValidationConsumer {

    private final OpenTelemetry openTelemetry;
    private final Tracer tracer;

    public AfiliadoValidationConsumer(OpenTelemetry openTelemetry) {
        this.openTelemetry = openTelemetry;
        this.tracer = openTelemetry.getTracer("validacion-service");
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
        Span span = tracer.spanBuilder("validacion-service.process")
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

            log.info("=== VALIDACION-SERVICE: Evento recibido ===");
            log.info("Topic: {}, Partition: {}, Offset: {}", record.topic(), record.partition(), record.offset());
            log.info("EventId: {}, EventType: {}", event.getEventId(), event.getEventType());
            log.info("TraceId: {}", span.getSpanContext().getTraceId());

            // Simular validacion
            validateAfiliado(event, span);
        } catch (Exception e) {
            span.setStatus(StatusCode.ERROR);
            span.recordException(e);
            throw e;
        } finally {
            span.end();
        }
    }

    private void validateAfiliado(AfiliadoEvent event, Span parentSpan) {
        String dni = event.getPayload().getDni();

        Span validateSpan = tracer.spanBuilder("validacion-service.validateAfiliado")
                .setParent(Context.current().with(parentSpan))
                .startSpan();

        try {
            validateSpan.setAttribute("afiliado.dni", dni);

            // Simular fallo para DNIs que empiezan con "FAIL" (para probar DLQ)
            if (dni != null && dni.startsWith("FAIL")) {
                log.error("Validacion fallida para DNI: {}", dni);
                validateSpan.setStatus(StatusCode.ERROR, "DNI invalido");
                throw new RuntimeException("DNI invalido: " + dni);
            }

            log.info("[VALIDACION OK] Afiliado validado: DNI={}, Nombre={} {}",
                dni,
                event.getPayload().getNombre(),
                event.getPayload().getApellidos());
            validateSpan.setStatus(StatusCode.OK);
        } finally {
            validateSpan.end();
        }
    }

    @KafkaListener(
        topics = "${app.kafka.topic.afiliado-eventos-dlt}",
        groupId = "validacion-dlt-group"
    )
    public void handleDltEvent(ConsumerRecord<String, AfiliadoEvent> record) {
        Context extractedContext = openTelemetry.getPropagators()
                .getTextMapPropagator()
                .extract(Context.current(), record.headers(), KAFKA_GETTER);

        Span span = tracer.spanBuilder("validacion-service.dlt.process")
                .setParent(extractedContext)
                .setSpanKind(SpanKind.CONSUMER)
                .setAttribute("messaging.system", "kafka")
                .setAttribute("messaging.destination", record.topic())
                .startSpan();

        try {
            AfiliadoEvent event = record.value();

            log.warn("=== DLT: Mensaje recibido en Dead Letter Topic ===");
            log.warn("EventId: {}, DNI: {}", event.getEventId(), event.getPayload().getDni());
            log.warn("Este mensaje requiere intervencion manual o reprocesamiento");

            span.setAttribute("event.id", event.getEventId());
            span.setAttribute("dlt.reason", "validation_failed");
        } finally {
            span.end();
        }
    }
}

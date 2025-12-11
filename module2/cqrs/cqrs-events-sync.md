# CQRS Sincronización con Eventos - Beneficio Service

## Eventos de Dominio

### BeneficioAsignadoEvent.java

```java
package com.mutualidad.beneficio.event;

import lombok.Value;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Evento emitido cuando se asigna un nuevo beneficio.
 */
@Value
public class BeneficioAsignadoEvent implements BeneficioEvent {
    
    String beneficioId;
    String afiliadoId;
    String tipoBeneficio;
    LocalDate fechaInicio;
    BigDecimal monto;
    Instant ocurridoEn = Instant.now();

    @Override
    public String getTipoEvento() {
        return "BENEFICIO_ASIGNADO";
    }

    @Override
    public String getAggregateId() {
        return beneficioId;
    }
}
```

### BeneficioModificadoEvent.java

```java
package com.mutualidad.beneficio.event;

import lombok.Value;

import java.time.Instant;

/**
 * Evento emitido cuando se modifica un beneficio.
 */
@Value
public class BeneficioModificadoEvent implements BeneficioEvent {
    
    String beneficioId;
    String afiliadoId;
    String motivo;
    String modificadoPor;
    Instant ocurridoEn = Instant.now();

    @Override
    public String getTipoEvento() {
        return "BENEFICIO_MODIFICADO";
    }

    @Override
    public String getAggregateId() {
        return beneficioId;
    }
}
```

### BeneficioRevocadoEvent.java

```java
package com.mutualidad.beneficio.event;

import lombok.Value;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Evento emitido cuando se revoca un beneficio.
 */
@Value
public class BeneficioRevocadoEvent implements BeneficioEvent {
    
    String beneficioId;
    String afiliadoId;
    String motivo;
    LocalDate fechaEfectiva;
    Instant ocurridoEn = Instant.now();

    @Override
    public String getTipoEvento() {
        return "BENEFICIO_REVOCADO";
    }

    @Override
    public String getAggregateId() {
        return beneficioId;
    }
}
```

### BeneficioSuspendidoEvent.java

```java
package com.mutualidad.beneficio.event;

import lombok.Value;

import java.time.Instant;
import java.time.LocalDate;

@Value
public class BeneficioSuspendidoEvent implements BeneficioEvent {
    
    String beneficioId;
    String afiliadoId;
    String motivo;
    LocalDate fechaInicioSuspension;
    Instant ocurridoEn = Instant.now();

    @Override
    public String getTipoEvento() {
        return "BENEFICIO_SUSPENDIDO";
    }

    @Override
    public String getAggregateId() {
        return beneficioId;
    }
}
```

### BeneficioReactivadoEvent.java

```java
package com.mutualidad.beneficio.event;

import lombok.Value;

import java.time.Instant;

@Value
public class BeneficioReactivadoEvent implements BeneficioEvent {
    
    String beneficioId;
    String afiliadoId;
    Instant ocurridoEn = Instant.now();

    @Override
    public String getTipoEvento() {
        return "BENEFICIO_REACTIVADO";
    }

    @Override
    public String getAggregateId() {
        return beneficioId;
    }
}
```

## Event Publisher

### EventPublisher.java

```java
package com.mutualidad.beneficio.event;

/**
 * Interfaz para publicar eventos de dominio.
 */
public interface EventPublisher {
    
    void publish(BeneficioEvent event);
    
    void publishAll(Iterable<BeneficioEvent> events);
}
```

### KafkaEventPublisher.java

```java
package com.mutualidad.beneficio.infrastructure.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mutualidad.beneficio.event.BeneficioEvent;
import com.mutualidad.beneficio.event.EventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaEventPublisher implements EventPublisher {

    private static final String TOPIC = "beneficio-events";
    
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void publish(BeneficioEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            
            kafkaTemplate.send(TOPIC, event.getAggregateId(), payload)
                .addCallback(
                    result -> log.debug("Evento publicado: {}", event.getTipoEvento()),
                    ex -> log.error("Error publicando evento: {}", event.getTipoEvento(), ex)
                );
                
        } catch (Exception e) {
            log.error("Error serializando evento: {}", event.getTipoEvento(), e);
            throw new RuntimeException("Error publicando evento", e);
        }
    }

    @Override
    public void publishAll(Iterable<BeneficioEvent> events) {
        events.forEach(this::publish);
    }
}
```

## Projection (Sincronización del Read Model)

### BeneficioProjection.java

```java
package com.mutualidad.beneficio.event;

import com.mutualidad.beneficio.command.domain.Beneficio;
import com.mutualidad.beneficio.command.repository.BeneficioWriteRepository;
import com.mutualidad.beneficio.query.model.BeneficioReadModel;
import com.mutualidad.beneficio.query.model.CambioHistoricoReadModel;
import com.mutualidad.beneficio.query.repository.BeneficioReadRepository;
import com.mutualidad.beneficio.query.repository.CambioHistoricoReadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.UUID;

/**
 * Projection que actualiza el Read Model basándose en eventos.
 * Garantiza la sincronización entre Write y Read models.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BeneficioProjection {

    private final BeneficioReadRepository readRepository;
    private final CambioHistoricoReadRepository historicoRepository;
    private final BeneficioWriteRepository writeRepository;
    private final AfiliadoClient afiliadoClient; // Cliente para obtener datos del afiliado

    @KafkaListener(topics = "beneficio-events", groupId = "beneficio-projection")
    @Transactional
    public void handleEvent(String eventJson) {
        try {
            // Deserializar evento (simplificado, en producción usar un deserializador apropiado)
            BeneficioEventEnvelope envelope = parseEvent(eventJson);
            
            switch (envelope.getTipoEvento()) {
                case "BENEFICIO_ASIGNADO" -> onBeneficioAsignado(envelope);
                case "BENEFICIO_MODIFICADO" -> onBeneficioModificado(envelope);
                case "BENEFICIO_REVOCADO" -> onBeneficioRevocado(envelope);
                case "BENEFICIO_SUSPENDIDO" -> onBeneficioSuspendido(envelope);
                case "BENEFICIO_REACTIVADO" -> onBeneficioReactivado(envelope);
                default -> log.warn("Evento desconocido: {}", envelope.getTipoEvento());
            }
        } catch (Exception e) {
            log.error("Error procesando evento: {}", eventJson, e);
            // En producción: enviar a DLQ (Dead Letter Queue)
        }
    }

    private void onBeneficioAsignado(BeneficioEventEnvelope envelope) {
        log.info("Proyectando BENEFICIO_ASIGNADO: {}", envelope.getBeneficioId());

        // Obtener datos completos del write model
        Beneficio beneficio = writeRepository.findById(envelope.getBeneficioId())
            .orElseThrow(() -> new IllegalStateException("Beneficio no encontrado"));

        // Obtener datos del afiliado (desnormalización)
        AfiliadoInfo afiliado = afiliadoClient.obtenerInfo(beneficio.getAfiliadoId());

        // Crear read model desnormalizado
        BeneficioReadModel readModel = BeneficioReadModel.builder()
            .id(beneficio.getId())
            .afiliadoId(beneficio.getAfiliadoId())
            .afiliadoNombre(afiliado.getNombreCompleto())
            .afiliadoDocumento(afiliado.getDocumento())
            .tipoBeneficio(beneficio.getTipoBeneficio().name())
            .tipoBeneficioDescripcion(beneficio.getTipoBeneficio().getDescripcion())
            .estado(beneficio.getEstado().name())
            .fechaInicio(beneficio.getFechaInicio())
            .fechaFin(beneficio.getFechaFin())
            .monto(beneficio.getMonto())
            .montoFormateado(formatearMonto(beneficio.getMonto()))
            .descripcion(beneficio.getDescripcion())
            .estaVigente(beneficio.estaVigente(LocalDate.now()))
            .diasRestantes(calcularDiasRestantes(beneficio.getFechaFin()))
            .fechaCreacion(beneficio.getFechaCreacion())
            .ultimaActualizacion(LocalDateTime.now())
            .build();

        readRepository.save(readModel);

        // Registrar en historial
        registrarHistorial(
            beneficio.getId(),
            "CREACION",
            "Beneficio asignado: " + beneficio.getTipoBeneficio().getDescripcion(),
            null, null, null,
            beneficio.getSolicitadoPor()
        );

        log.info("Read model creado para beneficio: {}", beneficio.getId());
    }

    private void onBeneficioModificado(BeneficioEventEnvelope envelope) {
        log.info("Proyectando BENEFICIO_MODIFICADO: {}", envelope.getBeneficioId());

        Beneficio beneficio = writeRepository.findById(envelope.getBeneficioId())
            .orElseThrow(() -> new IllegalStateException("Beneficio no encontrado"));

        // Actualizar read model existente
        readRepository.findById(envelope.getBeneficioId())
            .ifPresent(readModel -> {
                readModel.setMonto(beneficio.getMonto());
                readModel.setMontoFormateado(formatearMonto(beneficio.getMonto()));
                readModel.setFechaFin(beneficio.getFechaFin());
                readModel.setDescripcion(beneficio.getDescripcion());
                readModel.setEstaVigente(beneficio.estaVigente(LocalDate.now()));
                readModel.setDiasRestantes(calcularDiasRestantes(beneficio.getFechaFin()));
                readModel.setUltimaActualizacion(LocalDateTime.now());
                
                readRepository.save(readModel);
            });

        // Registrar en historial
        registrarHistorial(
            beneficio.getId(),
            "MODIFICACION",
            "Beneficio modificado",
            null, null,
            envelope.getMotivo(),
            envelope.getModificadoPor()
        );
    }

    private void onBeneficioRevocado(BeneficioEventEnvelope envelope) {
        log.info("Proyectando BENEFICIO_REVOCADO: {}", envelope.getBeneficioId());

        readRepository.findById(envelope.getBeneficioId())
            .ifPresent(readModel -> {
                readModel.setEstado("REVOCADO");
                readModel.setEstaVigente(false);
                readModel.setDiasRestantes(null);
                readModel.setUltimaActualizacion(LocalDateTime.now());
                
                readRepository.save(readModel);
            });

        registrarHistorial(
            envelope.getBeneficioId(),
            "REVOCACION",
            "Beneficio revocado",
            "estado", "ACTIVO", "REVOCADO",
            envelope.getMotivo(),
            envelope.getRevocadoPor()
        );
    }

    private void onBeneficioSuspendido(BeneficioEventEnvelope envelope) {
        log.info("Proyectando BENEFICIO_SUSPENDIDO: {}", envelope.getBeneficioId());

        readRepository.findById(envelope.getBeneficioId())
            .ifPresent(readModel -> {
                readModel.setEstado("SUSPENDIDO");
                readModel.setEstaVigente(false);
                readModel.setUltimaActualizacion(LocalDateTime.now());
                
                readRepository.save(readModel);
            });

        registrarHistorial(
            envelope.getBeneficioId(),
            "SUSPENSION",
            "Beneficio suspendido",
            "estado", "ACTIVO", "SUSPENDIDO",
            envelope.getMotivo(),
            envelope.getSuspendidoPor()
        );
    }

    private void onBeneficioReactivado(BeneficioEventEnvelope envelope) {
        log.info("Proyectando BENEFICIO_REACTIVADO: {}", envelope.getBeneficioId());

        Beneficio beneficio = writeRepository.findById(envelope.getBeneficioId())
            .orElseThrow();

        readRepository.findById(envelope.getBeneficioId())
            .ifPresent(readModel -> {
                readModel.setEstado("ACTIVO");
                readModel.setEstaVigente(beneficio.estaVigente(LocalDate.now()));
                readModel.setDiasRestantes(calcularDiasRestantes(beneficio.getFechaFin()));
                readModel.setUltimaActualizacion(LocalDateTime.now());
                
                readRepository.save(readModel);
            });

        registrarHistorial(
            envelope.getBeneficioId(),
            "REACTIVACION",
            "Beneficio reactivado",
            "estado", "SUSPENDIDO", "ACTIVO",
            null,
            envelope.getReactivadoPor()
        );
    }

    // Métodos auxiliares

    private void registrarHistorial(
            String beneficioId,
            String tipoCambio,
            String descripcion,
            String campo,
            String valorAnterior,
            String valorNuevo,
            String motivo,
            String realizadoPor) {

        CambioHistoricoReadModel historial = CambioHistoricoReadModel.builder()
            .id(UUID.randomUUID().toString())
            .beneficioId(beneficioId)
            .tipoCambio(tipoCambio)
            .descripcionCambio(descripcion)
            .campoModificado(campo)
            .valorAnterior(valorAnterior)
            .valorNuevo(valorNuevo)
            .motivo(motivo)
            .realizadoPor(realizadoPor)
            .fechaCambio(LocalDateTime.now())
            .build();

        historicoRepository.save(historial);
    }

    private String formatearMonto(java.math.BigDecimal monto) {
        if (monto == null) return null;
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("es", "ES"));
        return formatter.format(monto);
    }

    private Integer calcularDiasRestantes(LocalDate fechaFin) {
        if (fechaFin == null) return null;
        long dias = ChronoUnit.DAYS.between(LocalDate.now(), fechaFin);
        return dias >= 0 ? (int) dias : null;
    }

    private BeneficioEventEnvelope parseEvent(String json) {
        // Implementación de parsing (usar ObjectMapper)
        // ...
    }
}
```

## Diagrama de Sincronización

```
┌─────────────────────────────────────────────────────────────────┐
│                  FLUJO DE SINCRONIZACIÓN                        │
└─────────────────────────────────────────────────────────────────┘

┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│   Command    │────▶│    Write     │────▶│    Event     │
│   Handler    │     │   Database   │     │   Publisher  │
└──────────────┘     └──────────────┘     └──────┬───────┘
                                                  │
                                                  │ Kafka/RabbitMQ
                                                  ▼
┌─────────────────────────────────────────────────────────────────┐
│                        MESSAGE BROKER                            │
│                     (beneficio-events topic)                     │
└──────────────────────────────────┬──────────────────────────────┘
                                   │
                                   ▼
┌─────────────────────────────────────────────────────────────────┐
│                        PROJECTION                                │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  @KafkaListener                                            │  │
│  │  - Recibe evento                                           │  │
│  │  - Identifica tipo                                         │  │
│  │  - Actualiza Read Model                                    │  │
│  │  - Registra historial                                      │  │
│  └───────────────────────────────────────────────────────────┘  │
└──────────────────────────────────┬──────────────────────────────┘
                                   │
                                   ▼
┌─────────────────────────────────────────────────────────────────┐
│                      READ DATABASE                               │
│  ┌──────────────────────┐  ┌────────────────────────┐          │
│  │ beneficios_read      │  │ cambios_historico_read │          │
│  │ (desnormalizado)     │  │ (auditoría)            │          │
│  └──────────────────────┘  └────────────────────────┘          │
└─────────────────────────────────────────────────────────────────┘
                                   │
                                   ▼
┌─────────────────────────────────────────────────────────────────┐
│                     QUERY HANDLERS                               │
│  Consultas rápidas sobre datos desnormalizados                  │
└─────────────────────────────────────────────────────────────────┘
```

## Garantías de Consistencia

| Aspecto | Estrategia |
|---------|------------|
| **Ordenamiento** | Kafka garantiza orden por partition key (aggregateId) |
| **Idempotencia** | Verificar si ya se procesó el evento (event ID) |
| **Reintentos** | Retry automático con backoff exponencial |
| **DLQ** | Eventos fallidos van a Dead Letter Queue |
| **Monitoreo** | Métricas de lag entre write y read |

## Configuración Kafka

```yaml
spring:
  kafka:
    consumer:
      group-id: beneficio-projection
      auto-offset-reset: earliest
      enable-auto-commit: false
      properties:
        isolation.level: read_committed
    producer:
      acks: all
      retries: 3
      properties:
        enable.idempotence: true
```

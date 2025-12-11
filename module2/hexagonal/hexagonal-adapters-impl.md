# Implementación Completa de Adaptadores

## Adaptador JPA Completo

### AfiliadoJpaRepository.java

```java
package com.mutualidad.afiliado.infrastructure.adapter.output.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AfiliadoJpaRepository extends JpaRepository<AfiliadoEntity, String> {

    Optional<AfiliadoEntity> findByTipoDocumentoAndNumeroDocumento(
        String tipoDocumento, 
        String numeroDocumento
    );

    boolean existsByTipoDocumentoAndNumeroDocumento(
        String tipoDocumento, 
        String numeroDocumento
    );

    List<AfiliadoEntity> findByEstado(String estado);

    List<AfiliadoEntity> findByCodigoEmpresa(String codigoEmpresa);

    @Query("SELECT a FROM AfiliadoEntity a WHERE a.estado = 'ACTIVO' " +
           "AND a.codigoEmpresa = :codigoEmpresa")
    List<AfiliadoEntity> findActivosByEmpresa(String codigoEmpresa);

    @Query("SELECT COUNT(a) FROM AfiliadoEntity a WHERE a.estado = :estado")
    long countByEstado(String estado);
}
```

### AfiliadoJpaAdapter.java (Completo)

```java
package com.mutualidad.afiliado.infrastructure.adapter.output.persistence;

import com.mutualidad.afiliado.application.port.output.AfiliadoRepository;
import com.mutualidad.afiliado.domain.model.Afiliado;
import com.mutualidad.afiliado.domain.model.Documento;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class AfiliadoJpaAdapter implements AfiliadoRepository {

    private final AfiliadoJpaRepository jpaRepository;
    private final AfiliadoMapper mapper;

    @Override
    public Afiliado save(Afiliado afiliado) {
        log.debug("Guardando afiliado: {}", afiliado.getId());
        AfiliadoEntity entity = mapper.toEntity(afiliado);
        AfiliadoEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Afiliado> findById(String id) {
        log.debug("Buscando afiliado por ID: {}", id);
        return jpaRepository.findById(id)
            .map(mapper::toDomain);
    }

    @Override
    public Optional<Afiliado> findByDocumento(Documento documento) {
        log.debug("Buscando afiliado por documento: {}", documento);
        return jpaRepository.findByTipoDocumentoAndNumeroDocumento(
                documento.getTipo().name(),
                documento.getNumero()
            )
            .map(mapper::toDomain);
    }

    @Override
    public boolean existsByDocumento(Documento documento) {
        return jpaRepository.existsByTipoDocumentoAndNumeroDocumento(
            documento.getTipo().name(),
            documento.getNumero()
        );
    }

    @Override
    public void deleteById(String id) {
        log.debug("Eliminando afiliado: {}", id);
        jpaRepository.deleteById(id);
    }

    // Métodos adicionales para consultas específicas
    public List<Afiliado> findByEstado(String estado) {
        return jpaRepository.findByEstado(estado)
            .stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }

    public List<Afiliado> findActivosByEmpresa(String codigoEmpresa) {
        return jpaRepository.findActivosByEmpresa(codigoEmpresa)
            .stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }
}
```

## Adaptador de Eventos Kafka

### KafkaEventPublisher.java

```java
package com.mutualidad.afiliado.infrastructure.adapter.output.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mutualidad.afiliado.application.port.output.EventPublisherPort;
import com.mutualidad.afiliado.domain.event.AfiliadoEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaEventPublisher implements EventPublisherPort {

    private static final String TOPIC = "afiliado-events";

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void publish(AfiliadoEvent event) {
        log.info("Publicando evento: {} para afiliado: {}", 
            event.getTipoEvento(), event.getAfiliadoId());

        try {
            EventEnvelope envelope = new EventEnvelope(
                event.getTipoEvento(),
                event.getAfiliadoId(),
                event.getOcurridoEn(),
                objectMapper.writeValueAsString(event)
            );

            ListenableFuture<SendResult<String, Object>> future = 
                kafkaTemplate.send(TOPIC, event.getAfiliadoId(), envelope);

            future.addCallback(new ListenableFutureCallback<>() {
                @Override
                public void onSuccess(SendResult<String, Object> result) {
                    log.debug("Evento publicado exitosamente: {}", event.getTipoEvento());
                }

                @Override
                public void onFailure(Throwable ex) {
                    log.error("Error publicando evento: {}", event.getTipoEvento(), ex);
                }
            });
        } catch (Exception e) {
            log.error("Error serializando evento: {}", event.getTipoEvento(), e);
            throw new RuntimeException("Error publicando evento", e);
        }
    }

    @Override
    public void publishAll(Iterable<AfiliadoEvent> events) {
        events.forEach(this::publish);
    }
}

@Value
class EventEnvelope {
    String tipoEvento;
    String aggregateId;
    Instant ocurridoEn;
    String payload;
}
```

## Adaptador de Notificaciones

### EmailNotificationAdapter.java

```java
package com.mutualidad.afiliado.infrastructure.adapter.output.notification;

import com.mutualidad.afiliado.application.port.output.NotificacionPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationAdapter implements NotificacionPort {

    private final JavaMailSender mailSender;

    @Value("${notification.email.from}")
    private String fromEmail;

    @Value("${notification.email.enabled:true}")
    private boolean emailEnabled;

    @Override
    @Async
    public void enviarBienvenida(String email, String nombre) {
        if (!emailEnabled || email == null) {
            log.debug("Notificación de bienvenida omitida para: {}", nombre);
            return;
        }

        log.info("Enviando email de bienvenida a: {}", email);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(email);
        message.setSubject("Bienvenido/a a la Mutualidad");
        message.setText(String.format(
            "Estimado/a %s,\n\n" +
            "Le damos la bienvenida a nuestra mutualidad.\n" +
            "Su registro ha sido completado exitosamente.\n\n" +
            "Atentamente,\n" +
            "Mutualidad XYZ",
            nombre
        ));

        try {
            mailSender.send(message);
            log.info("Email de bienvenida enviado a: {}", email);
        } catch (Exception e) {
            log.error("Error enviando email de bienvenida a: {}", email, e);
        }
    }

    @Override
    @Async
    public void notificarBaja(String email, String nombre, String motivo) {
        if (!emailEnabled || email == null) {
            return;
        }

        log.info("Enviando notificación de baja a: {}", email);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(email);
        message.setSubject("Notificación de Baja");
        message.setText(String.format(
            "Estimado/a %s,\n\n" +
            "Le informamos que su afiliación ha sido dada de baja.\n" +
            "Motivo: %s\n\n" +
            "Si tiene alguna consulta, no dude en contactarnos.\n\n" +
            "Atentamente,\n" +
            "Mutualidad XYZ",
            nombre, motivo
        ));

        try {
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Error enviando notificación de baja a: {}", email, e);
        }
    }

    @Override
    @Async
    public void notificarReactivacion(String email, String nombre) {
        if (!emailEnabled || email == null) {
            return;
        }

        log.info("Enviando notificación de reactivación a: {}", email);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(email);
        message.setSubject("Reactivación de Afiliación");
        message.setText(String.format(
            "Estimado/a %s,\n\n" +
            "Nos complace informarle que su afiliación ha sido reactivada.\n" +
            "Ya puede disfrutar nuevamente de todos los beneficios.\n\n" +
            "Atentamente,\n" +
            "Mutualidad XYZ",
            nombre
        ));

        try {
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Error enviando notificación de reactivación a: {}", email, e);
        }
    }
}
```

## Adaptador REST Client con Resiliencia

### ValidacionRestAdapter.java (con Circuit Breaker)

```java
package com.mutualidad.afiliado.infrastructure.adapter.output.external;

import com.mutualidad.afiliado.application.port.output.ValidacionExternaPort;
import com.mutualidad.afiliado.domain.model.Documento;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
@Slf4j
public class ValidacionRestAdapter implements ValidacionExternaPort {

    private final RestTemplate restTemplate;

    @Value("${validacion.service.url}")
    private String validacionServiceUrl;

    @Override
    @CircuitBreaker(name = "validacionService", fallbackMethod = "validarDocumentoFallback")
    @Retry(name = "validacionService")
    public boolean validarDocumento(Documento documento) {
        log.info("Validando documento: {}/{}", documento.getTipo(), documento.getNumero());

        String url = String.format("%s/api/v1/validar/documento/%s/%s",
            validacionServiceUrl,
            documento.getTipo(),
            documento.getNumero());

        ResponseEntity<ValidacionResponse> response = restTemplate.getForEntity(
            url, 
            ValidacionResponse.class
        );

        return response.getBody() != null && response.getBody().isValido();
    }

    @Override
    @CircuitBreaker(name = "validacionService", fallbackMethod = "verificarEstadoLaboralFallback")
    @Retry(name = "validacionService")
    public boolean verificarEstadoLaboral(String codigoEmpresa, String numeroDocumento) {
        log.info("Verificando estado laboral: empresa={}, documento={}", 
            codigoEmpresa, numeroDocumento);

        String url = String.format("%s/api/v1/validar/laboral/%s/%s",
            validacionServiceUrl,
            codigoEmpresa,
            numeroDocumento);

        ResponseEntity<EstadoLaboralResponse> response = restTemplate.getForEntity(
            url, 
            EstadoLaboralResponse.class
        );

        return response.getBody() != null && response.getBody().isActivo();
    }

    // Fallback methods
    private boolean validarDocumentoFallback(Documento documento, Exception e) {
        log.warn("Fallback validación documento: {}. Error: {}", 
            documento.getNumero(), e.getMessage());
        // En caso de fallo, asumimos válido (fail-open) pero registramos para revisión
        return true;
    }

    private boolean verificarEstadoLaboralFallback(
            String codigoEmpresa, String numeroDocumento, Exception e) {
        log.warn("Fallback estado laboral: empresa={}, doc={}. Error: {}", 
            codigoEmpresa, numeroDocumento, e.getMessage());
        return true;
    }
}
```

## Configuración Resilience4j

### application.yml

```yaml
resilience4j:
  circuitbreaker:
    instances:
      validacionService:
        slidingWindowSize: 10
        failureRateThreshold: 50
        waitDurationInOpenState: 30s
        permittedNumberOfCallsInHalfOpenState: 3
        slowCallDurationThreshold: 2s
        slowCallRateThreshold: 80
        
  retry:
    instances:
      validacionService:
        maxAttempts: 3
        waitDuration: 500ms
        retryExceptions:
          - org.springframework.web.client.HttpServerErrorException
          - java.net.ConnectException
```

## Adaptadores para Testing

### InMemoryAfiliadoRepository.java

```java
package com.mutualidad.afiliado.infrastructure.adapter.output.persistence;

import com.mutualidad.afiliado.application.port.output.AfiliadoRepository;
import com.mutualidad.afiliado.domain.model.Afiliado;
import com.mutualidad.afiliado.domain.model.Documento;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementación en memoria para tests.
 */
public class InMemoryAfiliadoRepository implements AfiliadoRepository {

    private final Map<String, Afiliado> store = new ConcurrentHashMap<>();

    @Override
    public Afiliado save(Afiliado afiliado) {
        store.put(afiliado.getId(), afiliado);
        return afiliado;
    }

    @Override
    public Optional<Afiliado> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public Optional<Afiliado> findByDocumento(Documento documento) {
        return store.values().stream()
            .filter(a -> a.getDocumento().equals(documento))
            .findFirst();
    }

    @Override
    public boolean existsByDocumento(Documento documento) {
        return findByDocumento(documento).isPresent();
    }

    @Override
    public void deleteById(String id) {
        store.remove(id);
    }

    public void clear() {
        store.clear();
    }
}
```

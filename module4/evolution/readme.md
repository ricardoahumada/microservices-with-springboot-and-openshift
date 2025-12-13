# Guía de Evolución: Integración con Apache Kafka

## Objetivo
Esta guía muestra cómo evolucionar el proyecto `mutualidad-platform-base` hacia `mutualidad-platform` añadiendo integración con Apache Kafka para comunicación asíncrona basada en eventos.

---

## Estructura del Proyecto

```
mutualidad-platform/
├── afiliado-service/      # Productor de eventos
├── notificacion-service/  # Consumidor para notificaciones
└── validacion-service/    # Consumidor con DLQ
```

## Flujo de Eventos

```
┌─────────────────────┐         ┌──────────────────────────┐
│  afiliado-service   │────────▶│   Topic: afiliado-eventos │
│  (Productor)        │         │   (3 particiones)         │
└─────────────────────┘         └─────────────┬─────────────┘
                                              │
                        ┌─────────────────────┴─────────────────────┐
                        ▼                                           ▼
          ┌─────────────────────────┐             ┌─────────────────────────┐
          │  notificacion-service   │             │   validacion-service    │
          │  (Consumer Group A)     │             │   (Consumer Group B)    │
          └─────────────────────────┘             └────────────┬────────────┘
                                                               │ (si falla)
                                                               ▼
                                                  ┌─────────────────────────┐
                                                  │  Topic: afiliado-eventos.dlt │
                                                  │  (Dead Letter Topic)    │
                                                  └─────────────────────────┘
```

---

## Ejercicio 1: Configuración del Entorno Kafka

### Objetivo
Configurar el topic principal para eventos de afiliados.

### Archivos a Modificar

#### 1. `afiliado-service/src/main/resources/application.yml`

Descomentar la configuración de Kafka:

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      properties:
        spring.json.add.type.headers: false
      acks: all
      retries: 3

app:
  kafka:
    topic:
      afiliado-eventos: afiliado-eventos
```

#### 2. `afiliado-service/.../config/KafkaConfig.java`

Implementar la creación del topic:

```java
@Value("${app.kafka.topic.afiliado-eventos}")
private String afiliadoEventosTopic;

@Bean
public NewTopic afiliadoEventosTopic() {
    return TopicBuilder.name(afiliadoEventosTopic)
            .partitions(3)    // Permite 3 consumidores en paralelo
            .replicas(1)      // 1 réplica para desarrollo local
            .build();
}
```

### Conceptos Clave
- **Particiones**: Permiten paralelismo. Con 3 particiones, hasta 3 consumidores del mismo grupo pueden procesar en paralelo.
- **Key del mensaje**: Usar DNI como key garantiza que todos los eventos del mismo afiliado van a la misma partición (orden garantizado).

---

## Ejercicio 2: Publicación de Eventos "AfiliadoCreado"

### Objetivo
Implementar el productor que publica eventos cuando se crea un afiliado.

### Archivos a Modificar

#### `afiliado-service/.../service/AfiliadoEventPublisher.java`

Descomentar e implementar:

```java
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

    // DNI como key = orden garantizado por afiliado
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
```

### Conceptos Clave
- **KafkaTemplate**: Cliente de alto nivel para enviar mensajes.
- **Callback asíncrono**: Permite manejar éxito/error sin bloquear.
- **acks=all**: Garantiza que el mensaje se replicó antes de confirmar.

---

## Ejercicio 3: Consumo en Notificación Service

### Objetivo
Implementar un consumidor que envía notificaciones cuando recibe eventos de afiliados.

### Archivos a Modificar

#### 1. `notificacion-service/src/main/resources/application.yml`

Descomentar configuración del consumidor:

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: notificacion-group
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "*"
        spring.json.value.default.type: com.mutualidad.notificacion.event.AfiliadoEvent

app:
  kafka:
    topic:
      afiliado-eventos: afiliado-eventos
```

#### 2. `notificacion-service/.../service/AfiliadoEventConsumer.java`

Implementar el listener:

```java
@KafkaListener(
    topics = "${app.kafka.topic.afiliado-eventos}",
    groupId = "${spring.kafka.consumer.group-id}"
)
public void handleAfiliadoEvent(ConsumerRecord<String, AfiliadoEvent> record) {
    AfiliadoEvent event = record.value();
    
    log.info("=== NOTIFICACION-SERVICE: Evento recibido ===");
    log.info("Topic: {}, Partition: {}, Offset: {}", 
        record.topic(), record.partition(), record.offset());
    
    sendNotification(event);
}

private void sendNotification(AfiliadoEvent event) {
    switch (event.getEventType()) {
        case "AFILIADO_CREATED":
            log.info("[EMAIL] Enviando bienvenida a {}", event.getPayload().getEmail());
            log.info("[SMS] Notificando alta DNI: {}", event.getPayload().getDni());
            break;
        case "AFILIADO_UPDATED":
            log.info("[EMAIL] Notificando actualización a {}", event.getPayload().getEmail());
            break;
    }
}
```

### Conceptos Clave
- **Consumer Group**: `notificacion-group` - solo una instancia del grupo procesa cada mensaje.
- **auto-offset-reset=earliest**: Lee desde el inicio si no hay offset guardado.
- **@KafkaListener**: Anotación que convierte el método en consumidor.

---

## Ejercicio 4: Consumo en Validación Service

### Objetivo
Implementar un consumidor que valida los datos del afiliado. Este consumidor puede fallar, preparando el escenario para el DLQ.

### Archivos a Modificar

#### 1. `validacion-service/src/main/resources/application.yml`

Descomentar configuración (similar a notificacion-service pero con `group-id: validacion-group`).

#### 2. `validacion-service/.../service/AfiliadoValidationConsumer.java`

Implementar el listener con validación que puede fallar:

```java
@KafkaListener(
    topics = "${app.kafka.topic.afiliado-eventos}",
    groupId = "${spring.kafka.consumer.group-id}"
)
public void handleAfiliadoEvent(ConsumerRecord<String, AfiliadoEvent> record) {
    AfiliadoEvent event = record.value();
    
    log.info("=== VALIDACION-SERVICE: Evento recibido ===");
    validateAfiliado(event);
}

private void validateAfiliado(AfiliadoEvent event) {
    String dni = event.getPayload().getDni();
    
    // Simular fallo para DNIs que empiezan con "FAIL"
    if (dni != null && dni.startsWith("FAIL")) {
        log.error("Validacion fallida para DNI: {}", dni);
        throw new RuntimeException("DNI invalido: " + dni);
    }
    
    log.info("[VALIDACION OK] Afiliado validado: DNI={}", dni);
}
```

### Conceptos Clave
- **Consumer Groups independientes**: `validacion-group` recibe los mismos mensajes que `notificacion-group`.
- **Excepciones**: Lanzar excepción marca el mensaje como fallido.

---

## Ejercicio 5: Manejo de Errores con DLQ

### Objetivo
Configurar Dead Letter Queue para mensajes que fallan después de reintentos.

### Archivos a Modificar

#### 1. `validacion-service/src/main/resources/application.yml`

Añadir configuración del productor (necesario para enviar a DLT):

```yaml
spring:
  kafka:
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer

app:
  kafka:
    topic:
      afiliado-eventos: afiliado-eventos
      afiliado-eventos-dlt: afiliado-eventos.dlt
```

#### 2. `validacion-service/.../config/KafkaConfig.java`

Implementar el error handler:

```java
@Value("${app.kafka.topic.afiliado-eventos-dlt}")
private String dltTopic;

@Bean
public NewTopic dltTopic() {
    return TopicBuilder.name(dltTopic)
            .partitions(1)
            .replicas(1)
            .build();
}

@Bean
public DefaultErrorHandler errorHandler(KafkaTemplate<String, AfiliadoEvent> kafkaTemplate) {
    // Recuperador que envía mensajes fallidos al DLT
    DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
        (record, ex) -> {
            log.error("Enviando a DLT: key={}, error={}", record.key(), ex.getMessage());
            return new org.apache.kafka.common.TopicPartition(dltTopic, 0);
        });

    // 3 reintentos con 1 segundo entre cada uno
    DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, 
        new FixedBackOff(1000L, 3));
    
    // Log de cada reintento
    errorHandler.setRetryListeners((record, ex, deliveryAttempt) -> {
        log.warn("Reintento {} de 3: key={}", deliveryAttempt, 
            ((ConsumerRecord<?, ?>)record).key());
    });

    return errorHandler;
}
```

#### 3. `validacion-service/.../service/AfiliadoValidationConsumer.java`

Añadir consumidor del DLT:

```java
@KafkaListener(
    topics = "${app.kafka.topic.afiliado-eventos-dlt}",
    groupId = "validacion-dlt-group"
)
public void handleDltEvent(ConsumerRecord<String, AfiliadoEvent> record) {
    AfiliadoEvent event = record.value();
    
    log.warn("=== DLT: Mensaje fallido recibido ===");
    log.warn("EventId: {}, DNI: {}", event.getEventId(), event.getPayload().getDni());
    log.warn("Requiere intervención manual o reprocesamiento");
}
```

### Conceptos Clave
- **Dead Letter Topic (DLT)**: Almacena mensajes que no pudieron procesarse.
- **FixedBackOff**: Estrategia de reintentos con intervalo fijo.
- **DeadLetterPublishingRecoverer**: Envía automáticamente al DLT después de agotar reintentos.

---

## Resumen de Cambios por Servicio

| Servicio | Ejercicio | Archivos Modificados |
|----------|-----------|---------------------|
| afiliado-service | 1, 2 | `application.yml`, `KafkaConfig.java`, `AfiliadoEventPublisher.java` |
| notificacion-service | 3 | `application.yml`, `AfiliadoEventConsumer.java` |
| validacion-service | 4, 5 | `application.yml`, `KafkaConfig.java`, `AfiliadoValidationConsumer.java` |

---

## Pruebas

### Iniciar Kafka (Docker)
```bash
docker-compose up -d
```

### Probar publicación de evento
```bash
curl -X POST http://localhost:8081/api/afiliados \
  -H "Content-Type: application/json" \
  -d '{"dni":"12345678A","nombre":"Juan","apellidos":"García","email":"juan@test.com"}'
```

### Probar DLQ (DNI que falla validación)
```bash
curl -X POST http://localhost:8081/api/afiliados \
  -H "Content-Type: application/json" \
  -d '{"dni":"FAIL123","nombre":"Test","apellidos":"Error","email":"fail@test.com"}'
```

### Verificar logs
- notificacion-service: Debería mostrar envío de email/SMS
- validacion-service: Debería mostrar 3 reintentos y envío a DLT

---

## Dependencias Maven

Asegurarse de tener en cada `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
```

---
AIGC:
    ContentProducer: Minimax Agent AI
    ContentPropagator: Minimax Agent AI
    Label: AIGC
    ProduceID: "00000000000000000000000000000000"
    PropagateID: "00000000000000000000000000000000"
    ReservedCode1: 304502205389a20422fb54e998ed73e6921137868b5d9868c4cd882edffb973f2af8b65c022100ce969075ca6ffcd38ac72849db027b80f414449fd9b36fc47ec34760229e0b24
    ReservedCode2: 304402204189682263264dc0905ac18309f2573977a41423a5a8980f1bcc0daaafc88a530220568c6827d94f8dbc761efdbb3042054cfc96460bcf7b68afe965dfc02133b316
---

# Evolución: mutualidad-platform-base → mutualidad-platform

## Resumen

Esta guía explica cómo evolucionar el proyecto base (con TODOs) hacia la solución completa con Kafka.

```
mutualidad-platform-base/     →     mutualidad-platform/
(esqueleto con TODOs)               (solución completa)
```

## Archivos de Referencia

Esta carpeta contiene las clases completas que debes usar para reemplazar los TODOs:

```
evolution/
├── afiliado-service/
│   ├── AfiliadoEvent.java          # DTO del evento
│   ├── KafkaConfig.java            # Configuración del producer
│   └── AfiliadoEventPublisher.java # Publicador de eventos
├── notificacion-service/
│   ├── AfiliadoEvent.java          # DTO (copia del producer)
│   └── AfiliadoEventConsumer.java  # Consumer de notificaciones
├── validacion-service/
│   ├── AfiliadoEvent.java          # DTO (copia del producer)
│   ├── KafkaConfig.java            # Configuración con DLQ
│   └── AfiliadoValidationConsumer.java # Consumer con validación
└── readme.md
```

---

## Paso 1: afiliado-service (Producer)

### 1.1 Actualizar pom.xml

Descomentar la dependencia de Kafka:

```xml
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
```

**Añadir Lombok** (usado en la solución):
```xml
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>
```

### 1.2 Actualizar application.yml

```yaml
server:
  port: 8081

spring:
  application:
    name: afiliado-service
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer

app:
  kafka:
    topic:
      afiliado-eventos: afiliado-eventos
```

### 1.3 Reemplazar AfiliadoEvent.java

Copiar: `evolution/afiliado-service/AfiliadoEvent.java`

**Ubicación destino:** `src/main/java/com/mutualidad/afiliado/event/`

**Cambios clave:**
- Usa Lombok (@Data, @Builder)
- Incluye clase interna `AfiliadoPayload`
- Campos: eventId, eventType, timestamp, payload

### 1.4 Reemplazar KafkaConfig.java

Copiar: `evolution/afiliado-service/KafkaConfig.java`

**Cambios clave:**
- Usa `TopicBuilder` para crear topic
- Inyecta nombre del topic desde application.yml
- Configura 3 particiones

### 1.5 Reemplazar AfiliadoEventPublisher.java

Copiar: `evolution/afiliado-service/AfiliadoEventPublisher.java`

**Cambios clave:**
- Inyecta `KafkaTemplate<String, AfiliadoEvent>`
- Usa `ListenableFuture` con callback para logging
- DNI como key para garantizar orden por afiliado

---

## Paso 2: notificacion-service (Consumer)

### 2.1 Actualizar pom.xml

Descomentar Kafka y añadir Lombok.

### 2.2 Actualizar application.yml

```yaml
server:
  port: 8082

spring:
  application:
    name: notificacion-service
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: notificacion-group
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "*"

app:
  kafka:
    topic:
      afiliado-eventos: afiliado-eventos
```

### 2.3 Añadir AfiliadoEvent.java

Copiar: `evolution/notificacion-service/AfiliadoEvent.java`

**Ubicación destino:** `src/main/java/com/mutualidad/notificacion/event/`

> Nota: Es una copia del DTO del producer para deserializar mensajes.

### 2.4 Reemplazar AfiliadoEventConsumer.java

Copiar: `evolution/notificacion-service/AfiliadoEventConsumer.java`

**Cambios clave:**
- `@KafkaListener` con topic y groupId desde config
- Recibe `ConsumerRecord<String, AfiliadoEvent>`
- Método `sendNotification()` con switch por eventType

---

## Paso 3: validacion-service (Consumer + DLQ)

### 3.1 Actualizar pom.xml

Descomentar Kafka y añadir Lombok.

### 3.2 Actualizar application.yml

```yaml
server:
  port: 8083

spring:
  application:
    name: validacion-service
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: validacion-group
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "*"

app:
  kafka:
    topic:
      afiliado-eventos: afiliado-eventos
      afiliado-eventos-dlt: afiliado-eventos.dlt
```

### 3.3 Añadir AfiliadoEvent.java

Copiar: `evolution/validacion-service/AfiliadoEvent.java`

**Ubicación destino:** `src/main/java/com/mutualidad/validacion/event/`

### 3.4 Reemplazar KafkaConfig.java

Copiar: `evolution/validacion-service/KafkaConfig.java`

**Cambios clave:**
- Crea topic DLT (Dead Letter Topic)
- Configura `DefaultErrorHandler` con `DeadLetterPublishingRecoverer`
- 3 reintentos con 1s de intervalo
- Log de cada reintento

### 3.5 Reemplazar AfiliadoValidationConsumer.java

Copiar: `evolution/validacion-service/AfiliadoValidationConsumer.java`

**Cambios clave:**
- Valida DNI (simula error si empieza con "FAIL")
- Lanza `RuntimeException` para activar reintentos
- Segundo `@KafkaListener` para DLT

---

## Resumen de Cambios

| Servicio | Archivo | Cambio Principal |
|----------|---------|------------------|
| afiliado | pom.xml | +kafka, +lombok |
| afiliado | application.yml | +kafka producer config |
| afiliado | AfiliadoEvent.java | Clase completa con Lombok |
| afiliado | KafkaConfig.java | TopicBuilder para crear topic |
| afiliado | AfiliadoEventPublisher.java | KafkaTemplate + callbacks |
| notificacion | pom.xml | +kafka, +lombok |
| notificacion | application.yml | +kafka consumer config |
| notificacion | AfiliadoEvent.java | Copia del DTO |
| notificacion | AfiliadoEventConsumer.java | @KafkaListener completo |
| validacion | pom.xml | +kafka, +lombok |
| validacion | application.yml | +kafka + DLT config |
| validacion | AfiliadoEvent.java | Copia del DTO |
| validacion | KafkaConfig.java | DLQ con error handler |
| validacion | AfiliadoValidationConsumer.java | Validación + DLT listener |

---

## Verificación

### 1. Levantar Kafka
```bash
cd mutualidad-platform-base
docker-compose up -d
```

### 2. Compilar y ejecutar servicios
```bash
# Terminal 1
cd afiliado-service && mvn spring-boot:run

# Terminal 2
cd notificacion-service && mvn spring-boot:run

# Terminal 3
cd validacion-service && mvn spring-boot:run
```

### 3. Probar flujo normal
```bash
curl -X POST "http://localhost:8081/api/afiliados?dni=12345678A&nombre=Juan&apellidos=Garcia&email=juan@test.com&empresaId=EMP001"
```

### 4. Probar DLQ (DNI inválido)
```bash
curl -X POST "http://localhost:8081/api/afiliados?dni=FAIL123&nombre=Test&apellidos=Error&email=fail@test.com&empresaId=EMP001"
```

Verificar en Kafdrop (http://localhost:9000):
- Topic `afiliado-eventos` tiene el mensaje
- Topic `afiliado-eventos.dlt` tiene el mensaje fallido

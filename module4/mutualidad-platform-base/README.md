# Mutualidad Platform Base - Ejercicio Práctico

## Objetivo

Completar la implementación de una arquitectura Event-Driven con Apache Kafka.

El código contiene **TODOs** que debes completar para que el sistema funcione.

## Estructura del Proyecto

```
mutualidad-platform-base/
├── docker-compose.yml           # Kafka + Zookeeper + Kafdrop (listo)
├── afiliado-service/            # Producer - COMPLETAR TODOs
├── notificacion-service/        # Consumer - COMPLETAR TODOs
└── validacion-service/          # Consumer con DLQ - COMPLETAR TODOs
```

## Requisitos

- Java 17+
- Maven 3.8+
- Docker Desktop

---

## Paso 0: Levantar Kafka

```bash
cd module4/solutions/mutualidad-platform-base
docker-compose up -d
```

Verificar en http://localhost:9000 (Kafdrop)

---

## Ejercicio 1: Configurar Producer (afiliado-service)

### Archivos a modificar:

#### 1.1 pom.xml
Descomentar la dependencia de Spring Kafka.

#### 1.2 application.yml
Descomentar la configuración de Kafka producer.

#### 1.3 AfiliadoEvent.java
Completar:
- [ ] Definir campos (eventId, eventType, dni, nombre, etc.)
- [ ] Crear constructor
- [ ] Generar getters/setters
- [ ] Implementar toString()

#### 1.4 KafkaConfig.java
Completar:
- [ ] Inyectar bootstrap-servers
- [ ] Configurar ProducerFactory
- [ ] Crear KafkaTemplate bean
- [ ] Crear topic "afiliado-eventos"

#### 1.5 AfiliadoEventPublisher.java
Completar:
- [ ] Inyectar KafkaTemplate
- [ ] Implementar publishAfiliadoCreated()
- [ ] Usar kafkaTemplate.send() con callback

### Verificar:

```bash
cd afiliado-service && mvn spring-boot:run
```

```bash
curl -X POST "http://localhost:8081/api/afiliados?dni=12345678A&nombre=Juan&apellidos=Garcia&email=juan@test.com"
```

Ver mensaje en Kafdrop → topic "afiliado-eventos"

---

## Ejercicio 2: Configurar Consumer (notificacion-service)

### Archivos a modificar:

#### 2.1 pom.xml
Descomentar la dependencia de Spring Kafka.

#### 2.2 application.yml
Descomentar la configuración de Kafka consumer.

#### 2.3 AfiliadoEventConsumer.java
Completar:
- [ ] Añadir @KafkaListener con topic y groupId
- [ ] Descomentar la anotación del método

### Verificar:

```bash
cd notificacion-service && mvn spring-boot:run
```

Con ambos servicios corriendo, enviar mensaje:
```bash
curl -X POST "http://localhost:8081/api/afiliados?dni=87654321B&nombre=Maria&apellidos=Lopez&email=maria@test.com"
```

Verificar logs de notificacion-service.

---

## Ejercicio 3: Consumer con DLQ (validacion-service)

### Archivos a modificar:

#### 3.1 pom.xml
Descomentar la dependencia de Spring Kafka.

#### 3.2 application.yml
Descomentar la configuración de Kafka consumer.

#### 3.3 AfiliadoValidationConsumer.java
Completar:
- [ ] Añadir @KafkaListener
- [ ] Descomentar throw de excepción para DNIs "FAIL*"
- [ ] Añadir @KafkaListener para DLT

#### 3.4 KafkaConfig.java (AVANZADO)
Completar:
- [ ] Configurar DeadLetterPublishingRecoverer
- [ ] Configurar DefaultErrorHandler con reintentos
- [ ] Asociar al container factory

### Verificar DLQ:

```bash
cd validacion-service && mvn spring-boot:run
```

Enviar mensaje que fallará:
```bash
curl -X POST "http://localhost:8081/api/afiliados?dni=FAIL123&nombre=Test&apellidos=Error&email=fail@test.com"
```

Verificar:
1. Logs muestran reintentos
2. Mensaje aparece en topic "afiliado-eventos.dlt" en Kafdrop

---

## Checklist Final

### Básico
- [ ] Producer publica mensajes a Kafka
- [ ] notificacion-service consume y loguea
- [ ] validacion-service consume y valida

### Avanzado
- [ ] DNIs "FAIL*" causan reintentos
- [ ] Después de 3 reintentos, mensaje va a DLT
- [ ] Consumer de DLT procesa mensajes fallidos

---

## Solución

La solución completa está en: `module4/solutions/mutualidad-platform/`

---

## Resumen de Puertos

| Servicio | Puerto |
|----------|--------|
| afiliado-service | 8081 |
| notificacion-service | 8082 |
| validacion-service | 8083 |
| Kafdrop | 9000 |
| Kafka | 9092 |

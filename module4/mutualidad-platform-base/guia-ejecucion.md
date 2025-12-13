# Módulo 4: Arquitectura Event-Driven con Apache Kafka

## Estructura del Proyecto

```
mutualidad-platform/
├── docker-compose.yml          # Kafka + Zookeeper + Kafdrop
├── afiliado-service/           # Producer (puerto 8081)
├── notificacion-service/       # Consumer (puerto 8082)
├── validacion-service/         # Consumer con DLQ (puerto 8083)
└── readme.md
```

## Requisitos Previos

- Java 11+
- Maven 3.6+
- Docker Desktop

## Inicio Rápido

### 1. Levantar Kafka

```bash
cd module4/solutions/mutualidad-platform
docker-compose up -d
```

Verificar que los contenedores están corriendo:
```bash
docker-compose ps
```

Resultado esperado:
```
NAME        IMAGE                             STATUS
kafka       confluentinc/cp-kafka:7.4.0       Up
kafdrop     obsidiandynamics/kafdrop:3.31.0   Up
zookeeper   confluentinc/cp-zookeeper:7.4.0   Up
```

### 2. Acceder a Kafdrop (UI de Kafka)

Abrir http://localhost:9000 en el navegador para ver topics y mensajes.

### 3. Compilar los Servicios

```bash
# Desde module4/solutions/mutualidad-platform
cd afiliado-service && mvn clean package -DskipTests && cd ..
cd notificacion-service && mvn clean package -DskipTests && cd ..
cd validacion-service && mvn clean package -DskipTests && cd ..
```

### 4. Iniciar los Servicios (en terminales separadas)

Terminal 1 - Producer:
```bash
cd afiliado-service && mvn spring-boot:run
```

Terminal 2 - Consumer Notificaciones:
```bash
cd notificacion-service && mvn spring-boot:run
```

Terminal 3 - Consumer Validaciones:
```bash
cd validacion-service && mvn spring-boot:run
```

---

## Ejercicio 1: Configurar Kafka con Docker (20 min)

### Objetivo
Levantar infraestructura Kafka usando Docker Compose.

### Pasos
1. Revisar `docker-compose.yml`
2. Ejecutar `docker-compose up -d`
3. Verificar Kafdrop en http://localhost:9000

### Verificación
```bash
docker-compose logs kafka | grep "started"
```

Resultado esperado:
```
kafka  | [KafkaServer id=1] started
```

---

## Ejercicio 2: Implementar Producer de Eventos (25 min)

### Objetivo
Publicar eventos `AFILIADO_CREATED` desde afiliado-service.

### Archivos Clave
- `AfiliadoEventPublisher.java`: Usa KafkaTemplate para publicar
- `KafkaConfig.java`: Crea topic con 3 particiones
- `application.yml`: Configuración del producer

### Prueba

```bash
curl -X POST "http://localhost:8081/api/afiliados?dni=12345678A&nombre=Juan&apellidos=Garcia&email=juan@test.com&empresaId=EMP001"
```

Resultado esperado:
```json
{
  "status": "ACCEPTED",
  "message": "Evento de creacion de afiliado publicado",
  "dni": "12345678A"
}
```

Log del afiliado-service:
```
Evento publicado exitosamente: topic=afiliado-eventos, partition=X, offset=Y, eventId=...
```

---

## Ejercicio 3: Implementar Consumer de Notificaciones (20 min)

### Objetivo
Consumir eventos y simular envío de notificaciones.

### Archivos Clave
- `AfiliadoEventConsumer.java`: @KafkaListener que procesa eventos

### Prueba

Con ambos servicios corriendo, ejecutar:

```bash
curl -X POST "http://localhost:8081/api/afiliados?dni=87654321B&nombre=Maria&apellidos=Lopez&email=maria@test.com&empresaId=EMP002"
```

Resultado esperado en log de notificacion-service:
```
=== NOTIFICACION-SERVICE: Evento recibido ===
Topic: afiliado-eventos, Partition: X, Offset: Y
EventId: ..., EventType: AFILIADO_CREATED
Payload: AfiliadoPayload(afiliadoId=..., dni=87654321B, nombre=Maria, ...)
[EMAIL] Enviando bienvenida a Maria Lopez - maria@test.com
[SMS] Notificando alta de afiliado DNI: 87654321B
```

---

## Ejercicio 4: Implementar Consumer de Validaciones (25 min)

### Objetivo
Consumir eventos y validar afiliados. Múltiples consumer groups.

### Archivos Clave
- `AfiliadoValidationConsumer.java`: Valida DNI y rechaza inválidos

### Prueba - Validación Exitosa

```bash
curl -X POST "http://localhost:8081/api/afiliados?dni=11111111C&nombre=Pedro&apellidos=Martinez&email=pedro@test.com&empresaId=EMP003"
```

Resultado esperado en log de validacion-service:
```
=== VALIDACION-SERVICE: Evento recibido ===
Topic: afiliado-eventos, Partition: X, Offset: Y
EventId: ..., EventType: AFILIADO_CREATED
[VALIDACION OK] Afiliado validado: DNI=11111111C, Nombre=Pedro Martinez
```

### Verificar Consumer Groups en Kafdrop

1. Ir a http://localhost:9000
2. Click en "afiliado-eventos"
3. Ver "Consumers" - deberían aparecer:
   - notificacion-group
   - validacion-group

---

## Ejercicio 5: Manejo de Errores con Dead Letter Queue (30 min)

### Objetivo
Implementar DLQ para mensajes que fallan después de reintentos.

### Archivos Clave
- `KafkaConfig.java`: Configura DefaultErrorHandler con DeadLetterPublishingRecoverer
- `AfiliadoValidationConsumer.java`: Listener para DLT

### Configuración DLQ
- 3 reintentos con intervalo de 1 segundo
- Después de 3 fallos → mensaje va a `afiliado-eventos.dlt`

### Prueba - Forzar Error (DNI que empieza con "FAIL")

```bash
curl -X POST "http://localhost:8081/api/afiliados?dni=FAIL123&nombre=Test&apellidos=Error&email=fail@test.com&empresaId=EMP001"
```

Resultado esperado en log de validacion-service:
```
=== VALIDACION-SERVICE: Evento recibido ===
Validacion fallida para DNI: FAIL123
Reintento 1 de 3 para mensaje: key=FAIL123, error=DNI invalido: FAIL123
Reintento 2 de 3 para mensaje: key=FAIL123, error=DNI invalido: FAIL123
Reintento 3 de 3 para mensaje: key=FAIL123, error=DNI invalido: FAIL123
Enviando mensaje fallido a DLT: key=FAIL123, error=DNI invalido: FAIL123
=== DLT: Mensaje recibido en Dead Letter Topic ===
EventId: ..., DNI: FAIL123
Este mensaje requiere intervencion manual o reprocesamiento
```

### Verificar DLT en Kafdrop

1. Ir a http://localhost:9000
2. Buscar topic `afiliado-eventos.dlt`
3. Ver mensaje fallido en la cola

---

## Comandos Útiles

### Ver Topics
```bash
docker exec kafka kafka-topics --list --bootstrap-server localhost:9092
```

### Ver Mensajes de un Topic
```bash
docker exec kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic afiliado-eventos \
  --from-beginning
```

### Ver Consumer Groups
```bash
docker exec kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --list
```

### Detener Kafka
```bash
docker-compose down
```

### Detener y Eliminar Volúmenes
```bash
docker-compose down -v
```

---

## Resumen de Puertos

| Servicio             | Puerto |
|---------------------|--------|
| afiliado-service    | 8081   |
| notificacion-service| 8082   |
| validacion-service  | 8083   |
| Kafka               | 9092   |
| Kafdrop (UI)        | 9000   |
| Zookeeper           | 2181   |

---

## Troubleshooting

### Kafka no arranca
```bash
docker-compose down -v
docker-compose up -d
```

### Consumer no recibe mensajes
1. Verificar que Kafka está corriendo: `docker-compose ps`
2. Verificar topic existe en Kafdrop
3. Revisar logs: `docker-compose logs kafka`

### Error de conexión al producer
Verificar que `bootstrap-servers: localhost:9092` está configurado en application.yml

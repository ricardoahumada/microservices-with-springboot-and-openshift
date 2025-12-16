# Solución - Práctica 1: Modelado del Dominio

## Ejercicio 1.1: Identificación de Subdominios

### Solución Propuesta

| Subdominio | Clasificación | Justificación |
|------------|---------------|---------------|
| **Afiliación** | **CORE** | Es el núcleo del negocio de la mutualidad. Sin afiliados no hay mutualidad. Diferencia competitivamente a la organización (procesos de alta eficientes, experiencia del afiliado). |
| **Beneficios** | **CORE** | Los beneficios son la propuesta de valor de la mutualidad. Define los productos y servicios que atraen y retienen afiliados. |
| **Notificaciones** | **GENÉRICO** | Es un servicio de soporte que puede ser externalizado (Twilio, SendGrid) o usar soluciones estándar. No aporta diferenciación de negocio. |
| **Validación Externa** | **SOPORTE** | Necesario para el cumplimiento regulatorio pero no diferencia el negocio. Integra con sistemas externos gubernamentales. |

### Diagrama de Subdominios

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    DOMINIO: MUTUALIDAD DE SALUD                         │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │                    SUBDOMINIOS CORE                              │   │
│  │     (Invertir más recursos, equipo experto, desarrollo propio)   │   │
│  │                                                                  │   │
│  │   ┌─────────────────────┐   ┌─────────────────────┐              │   │
│  │   │     AFILIACIÓN      │   │     BENEFICIOS      │              │   │
│  │   │                     │   │                     │              │   │
│  │   │  • Alta de afiliado │   │  • Catálogo         │              │   │
│  │   │  • Gestión de datos │   │  • Asignación       │              │   │
│  │   │  • Histórico        │   │  • Elegibilidad     │              │   │
│  │   │  • Beneficiarios    │   │  • Carencias        │              │   │
│  │   └─────────────────────┘   └─────────────────────┘              │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│                                                                         │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │                    SUBDOMINIO SOPORTE                            │   │
│  │        (Necesario pero no diferenciador, equipo reducido)        │   │
│  │                                                                  │   │
│  │   ┌──────────────────────────────────────────────────────────┐   │   │
│  │   │              VALIDACIÓN EXTERNA                          │   │   │
│  │   │                                                          │   │   │
│  │   │  • Verificación estado laboral (Seguridad Social)        │   │   │
│  │   │  • Verificación cobertura vigente                        │   │   │
│  │   │  • Integración con sistemas gubernamentales              │   │   │
│  │   └──────────────────────────────────────────────────────────┘   │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│                                                                         │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │                    SUBDOMINIO GENÉRICO                           │   │
│  │       (Puede externalizarse o usar soluciones estándar)          │   │
│  │                                                                  │   │
│  │   ┌──────────────────────────────────────────────────────────┐   │   │
│  │   │              NOTIFICACIONES                              │   │   │
│  │   │                                                          │   │   │
│  │   │  • Envío SMS (via Twilio/AWS SNS)                        │   │   │
│  │   │  • Envío Email (via SendGrid/AWS SES)                    │   │   │
│  │   │  • Plantillas de mensajes                                │   │   │
│  │   └──────────────────────────────────────────────────────────┘   │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## Ejercicio 1.2: Definición de Bounded Contexts

### Contexto: AFILIADO

```
BOUNDED CONTEXT: AFILIADO
═══════════════════════════════════════════════════════════════════════════

Responsabilidad:
  Gestionar el ciclo de vida completo de los afiliados de la mutualidad.

Entidades:
  • Afiliado (Aggregate Root)
    - Identificador único, datos personales, estado de afiliación
    - Responsable de mantener la consistencia del agregado
  
  • Documento
    - Documentación asociada (DNI escaneado, contratos, justificantes)
    - Solo accesible a través del Afiliado
  
  • HistorialLaboral
    - Registro de situaciones laborales del afiliado
    - Necesario para validar elegibilidad a beneficios

Value Objects:
  • DNI
    - Documento Nacional de Identidad con validación de formato y dígito
    - Inmutable, identifica unívocamente al afiliado
  
  • Direccion
    - Dirección postal completa (calle, CP, ciudad, provincia)
    - Inmutable, se reemplaza completa ante cambios
  
  • DatosContacto
    - Email, teléfono, preferencias de comunicación
    - Agrupa datos de contacto relacionados

Aggregate Root: Afiliado
  - Todas las operaciones sobre Documento e HistorialLaboral
    pasan a través del Afiliado
  - El Afiliado garantiza las invariantes del agregado

Invariantes de negocio:
  1. Un afiliado solo puede estar ACTIVO si tiene al menos un documento validado
  2. El DNI es único en el sistema y no puede modificarse tras la creación
  3. Un afiliado en estado BAJA no puede modificarse (solo consulta)
  4. La fecha de alta solo se establece al activar el afiliado
  5. La fecha de baja solo se establece al dar de baja el afiliado

Eventos de dominio emitidos:
  • AfiliadoCreado
  • AfiliadoActivado
  • AfiliadoSuspendido
  • AfiliadoDadoDeBaja
  • DatosAfiliadoActualizados
```

### Contexto: BENEFICIO

```
BOUNDED CONTEXT: BENEFICIO
═══════════════════════════════════════════════════════════════════════════

Responsabilidad:
  Administrar el catálogo de beneficios y su asignación a afiliados.

Entidades:
  • Beneficio (Aggregate Root)
    - Definición del beneficio (nombre, descripción, condiciones)
    - Tipo: SALUD, SUBSIDIO, DESCUENTO
  
  • AsignacionBeneficio
    - Relación entre afiliado y beneficio con período de vigencia
    - Contiene estado de la asignación
  
  • TipoBeneficio
    - Clasificación de beneficios (categorías, subcategorías)

Value Objects:
  • PeriodoVigencia
    - Fecha inicio y fecha fin de la asignación
    - Incluye lógica para verificar si está vigente
  
  • MontoCobertura
    - Valor monetario con divisa
    - Límites de cobertura (anual, por uso)
  
  • Condiciones
    - Requisitos para acceder al beneficio
    - Exclusiones y carencias

Aggregate Root: Beneficio
  - Controla la creación de AsignacionBeneficio
  - Verifica elegibilidad antes de asignar

Invariantes de negocio:
  1. Un beneficio solo puede asignarse a afiliados ACTIVOS
  2. No pueden existir asignaciones superpuestas del mismo beneficio
  3. La fecha fin debe ser posterior a la fecha inicio
  4. Los períodos de carencia deben respetarse
  5. El monto de cobertura no puede exceder el límite del beneficio

Eventos de dominio emitidos:
  • BeneficioAsignado
  • BeneficioRevocado
  • BeneficioPorVencer
  • CoberturaSuperada
```

### Contexto: NOTIFICACIÓN

```
BOUNDED CONTEXT: NOTIFICACIÓN
═══════════════════════════════════════════════════════════════════════════

Responsabilidad:
  Gestionar el envío de comunicaciones a afiliados por diferentes canales.

Entidades:
  • Notificacion (Aggregate Root)
    - Registro de cada notificación enviada
    - Estado: PENDIENTE, ENVIADA, FALLIDA, ENTREGADA
  
  • Plantilla
    - Plantillas de mensajes reutilizables
    - Variables de sustitución
  
  • HistorialEnvio
    - Log de intentos de envío
    - Respuestas del proveedor externo

Value Objects:
  • Canal
    - Tipo de canal: SMS, EMAIL, PUSH
    - Configuración específica del canal
  
  • Destinatario
    - Datos mínimos del receptor (email/teléfono)
    - NO contiene datos completos del afiliado (aislamiento)
  
  • Contenido
    - Mensaje procesado listo para envío
    - Resultado de aplicar plantilla con datos

Aggregate Root: Notificacion
  - Controla los reintentos de envío
  - Registra el historial completo

Invariantes de negocio:
  1. Una notificación debe tener al menos un canal de envío
  2. Máximo 3 reintentos por notificación fallida
  3. Las notificaciones ENTREGADAS no pueden reintentarse
  4. El contenido debe cumplir límites del canal (160 chars SMS)

Eventos de dominio consumidos:
  • AfiliadoActivado → Enviar bienvenida
  • BeneficioPorVencer → Enviar recordatorio
  • AfiliadoDadoDeBaja → Enviar confirmación
```

### Contexto: VALIDACIÓN

```
BOUNDED CONTEXT: VALIDACIÓN
═══════════════════════════════════════════════════════════════════════════

Responsabilidad:
  Verificar datos externos necesarios para operaciones de negocio.

Entidades:
  • SolicitudValidacion (Aggregate Root)
    - Petición de validación a sistemas externos
    - Referencia al proceso que la originó
  
  • ResultadoValidacion
    - Resultado de la validación externa
    - Datos obtenidos del sistema externo

Value Objects:
  • TipoValidacion
    - ESTADO_LABORAL, COBERTURA_VIGENTE, DOCUMENTO_IDENTIDAD
    - Define qué sistema externo consultar
  
  • EstadoValidacion
    - PENDIENTE, EN_PROCESO, COMPLETADA, ERROR
  
  • DatosExternos
    - Datos estructurados del sistema externo
    - Interpretación normalizada de respuestas

Aggregate Root: SolicitudValidacion
  - Gestiona el ciclo de vida de la validación
  - Maneja reintentos ante fallos

Invariantes de negocio:
  1. Una solicitud debe completarse en máximo 30 segundos (timeout)
  2. Máximo 3 reintentos ante fallos transitorios
  3. Los resultados se cachean durante 24 horas
  4. Las validaciones caducadas deben renovarse

Eventos de dominio emitidos:
  • ValidacionCompletada
  • ValidacionFallida
  • ValidacionExpirada
```

---

## Ejercicio 1.3: Context Mapping

### Diagrama Completo

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          CONTEXT MAP                                    │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│                       ┌─────────────────────┐                           │
│                       │      AFILIADO       │                           │
│                       │     (Upstream)      │                           │
│                       │                     │                           │
│                       │  • Datos maestros   │                           │
│                       │  • Estados          │                           │
│                       │  • Eventos          │                           │
│                       └──────────┬──────────┘                           │
│                                  │                                      │
│          ┌───────────────────────┼───────────────────────┐              │
│          │                       │                       │              │
│          ▼                       ▼                       ▼              │
│  ┌───────────────┐      ┌───────────────┐      ┌───────────────┐        │
│  │   BENEFICIO   │      │  VALIDACIÓN   │      │ NOTIFICACIÓN  │        │
│  │  (Downstream) │      │  (Downstream) │      │  (Downstream) │        │
│  │               │      │               │      │               │        │
│  │ Customer/     │      │  Conformist   │      │    ACL        │        │
│  │ Supplier      │      │               │      │               │        │
│  └───────┬───────┘      └───────────────┘      └───────────────┘        │
│          │                                              ▲               │
│          │                                              │               │
│          │         Published Language                   │               │
│          └──────────────────────────────────────────────┘               │
│                        (Eventos de dominio)                             │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### Tipos de Relaciones

#### 1. Afiliado → Beneficio: **Customer/Supplier**

```
┌─────────────────────────────────────────────────────────────────────────┐
│  RELACIÓN: Customer/Supplier                                            │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  UPSTREAM (Afiliado)                 DOWNSTREAM (Beneficio)             │
│  ┌─────────────────────┐             ┌──────────────────────┐           │
│  │ Publica:            │             │ Consume:             │           │
│  │ • AfiliadoResponse  │ ────────►  │ • ID Afiliado         │           │
│  │ • EstadoAfiliado    │             │ • Estado actual      │           │
│  │ • DatosBasicos      │             │ • Elegibilidad       │           │
│  └─────────────────────┘             └──────────────────────┘           │
│                                                                         │
│  Beneficio es "cliente" de Afiliado:                                    │
│  • Consume API de Afiliado para verificar elegibilidad                  │
│  • Afiliado mantiene contrato estable (no cambios breaking)             │
│  • Beneficio puede solicitar nuevos datos al proveedor                  │
│                                                                         │
│  Comunicación: REST síncrona (OpenFeign)                                │
│  Contrato: API versionada (/api/v1/afiliados)                           │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

#### 2. Afiliado → Validación: **Conformist**

```
┌─────────────────────────────────────────────────────────────────────────┐
│  RELACIÓN: Conformist                                                   │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  UPSTREAM (Afiliado)                 DOWNSTREAM (Validación)            │
│  ┌─────────────────────┐             ┌─────────────────────┐            │
│  │ Define:             │             │ Se adapta:          │            │
│  │ • Estructura DNI    │ ────────►  │ • Usa DNI tal cual   │            │
│  │ • Formato datos     │             │ • No traduce        │            │
│  │ • Modelo afiliado   │             │ • Adopta modelo     │            │
│  └─────────────────────┘             └─────────────────────┘            │
│                                                                         │
│  Validación se "conforma" al modelo de Afiliado:                        │
│  • No tiene poder de negociación sobre el modelo                        │
│  • Adopta la estructura de datos de Afiliado                            │
│  • Simplifica integración al no tener capa de traducción                │
│                                                                         │
│  Justificación:                                                         │
│  • Validación es subdominio de soporte (menos importante)               │
│  • Afiliado es core (define el modelo canónico)                         │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

#### 3. Afiliado → Notificación: **Anti-Corruption Layer (ACL)**

```
┌─────────────────────────────────────────────────────────────────────────┐
│  RELACIÓN: Anti-Corruption Layer                                        │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  UPSTREAM (Afiliado)                     DOWNSTREAM (Notificación)      │
│  ┌─────────────────────┐                 ┌─────────────────────┐        │
│  │ Modelo Rico:        │      ACL        │ Modelo Propio:      │        │
│  │ • Afiliado          │ ───────────►   │ • Destinatario       │        │
│  │ • Direccion         │   Traduce       │ • Canal             │        │
│  │ • DatosContacto     │                 │ • Contenido         │        │
│  │ • HistorialLaboral  │                 │                     │        │
│  └─────────────────────┘                 └─────────────────────┘        │
│                                                                         │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │                    AfiliadoTranslator (ACL)                      │   │
│  │                                                                  │   │
│  │  public Destinatario traducir(AfiliadoResponse afiliado) {       │   │
│  │      return Destinatario.builder()                               │   │
│  │          .email(afiliado.getEmail())                             │   │
│  │          .telefono(afiliado.getTelefono())                       │   │
│  │          .nombreCompleto(afiliado.getNombreCompleto())           │   │
│  │          .build();                                               │   │
│  │  }                                                               │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│                                                                         │
│  Beneficios del ACL:                                                    │
│  • Notificación mantiene su modelo limpio (principio RGPD)              │
│  • Cambios en Afiliado se absorben en el traductor                      │
│  • Notificación solo conoce lo mínimo necesario                         │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

#### 4. Beneficio → Notificación: **Published Language**

```
┌─────────────────────────────────────────────────────────────────────────┐
│  RELACIÓN: Published Language (Eventos de Dominio)                      │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  PUBLISHER (Beneficio)                   SUBSCRIBER (Notificación)      │
│  ┌─────────────────────┐                 ┌─────────────────────┐        │
│  │ Emite eventos:      │      Kafka      │ Consume eventos:    │        │
│  │                     │ ───────────►   │                      │        │
│  │ • BeneficioAsignado │                 │ • Envía bienvenida  │        │
│  │ • BeneficioPorVencer│                 │ • Envía recordatorio│        │
│  │ • BeneficioRevocado │                 │ • Envía confirmación│        │
│  └─────────────────────┘                 └─────────────────────┘        │
│                                                                         │
│  Esquema del evento (Published Language):                               │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │  {                                                               │   │
│  │    "eventType": "BeneficioPorVencer",                            │   │
│  │    "eventId": "uuid",                                            │   │
│  │    "timestamp": "2024-01-15T10:30:00Z",                          │   │
│  │    "payload": {                                                  │   │
│  │      "afiliadoId": 12345,                                        │   │
│  │      "beneficioId": 67890,                                       │   │
│  │      "nombreBeneficio": "Seguro Dental Premium",                 │   │
│  │      "fechaVencimiento": "2024-02-15",                           │   │
│  │      "diasRestantes": 30                                         │   │
│  │    }                                                             │   │
│  │  }                                                               │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│                                                                         │
│  Ventajas:                                                              │
│  • Desacoplamiento temporal (asíncrono)                                 │
│  • Contrato bien definido y versionado                                  │
│  • Notificación no depende de la disponibilidad de Beneficio            │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### Resumen de Comunicación entre Contextos

| Origen | Destino | Patrón | Mecanismo | Datos Intercambiados |
|--------|---------|--------|-----------|---------------------|
| Afiliado | Beneficio | Customer/Supplier | REST (Feign) | ID, Estado, Elegibilidad |
| Afiliado | Validación | Conformist | REST (Feign) | DNI, DatosPersonales |
| Afiliado | Notificación | ACL | REST (Feign) + Eventos | Destinatario (mínimo) |
| Beneficio | Notificación | Published Language | Kafka Events | Eventos de dominio |
| Validación | Afiliado | Callback | REST / Eventos | ResultadoValidacion |

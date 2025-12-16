**Normalización y Desnormalización en DDD**

En **Domain-Driven Design (DDD)**, la **normalización** y la **desnormalización** no son principalmente preocupaciones de base de datos — son **decisiones de modelado estratégico** impulsadas por **límites de dominio, requisitos de consistencia, rendimiento y autonomía**.

DDD te anima a pensar más allá de "tablas y joins" — en su lugar, modelas alrededor de **Aggregates**, **Bounded Contexts** y **Domain Events**. Dentro de ese marco, la normalización y desnormalización adquieren significados matizados.

## **Normalización y Desnormalización en DDD**

| Concepto | En Contexto DDD |
| --- | --- |
| **Normalización** | Evitar duplicar datos de dominio entre Aggregates o Bounded Contexts. Referenciar por ID. Preservar fuente única de verdad. |
| **Desnormalización** | Duplicar datos **estratégicamente** dentro de un Aggregate o modelo de lectura para hacer cumplir invariantes, mejorar rendimiento o asegurar autonomía — a menudo vía eventos. |

> DDD no prohíbe la duplicación — la **contextualiza**. La duplicación es aceptable si sirve a un **propósito de dominio**.


## **Normalización: Preservar Límites y Consistencia**

> ** Regla: "Referenciar otros Aggregates por ID — no embedar su estado."**

En DDD, cada **Aggregate** es un límite de consistencia. No debes **embedar** el estado completo de otro Aggregate dentro del tuyo — eso rompería la encapsulación y crearía pesadillas de consistencia.

### **Ejemplo: Normalizado (Alineado con DDD)**

```java
public class Order : AggregateRoot
{
    public OrderId Id { get; }
    public CustomerId CustomerId { get; } // ← Solo referencia por ID
    public Address ShippingAddress { get; } // ← Value Object — OK para embedar
    public DateTime OrderDate { get; }
    public List<OrderItem> Items { get; }
    
    // Lógica de negocio encapsulada aquí
    public void Cancel() { ... }
}
```

- **CustomerId** es una referencia — los datos reales de **Customer** (nombre, límite de crédito, etc.) viven en el **Customer** Aggregate (posiblemente en otro Bounded Context).
- ¿Para obtener el nombre del cliente para mostrar? Usa un **read model** o llama a un **servicio facade/query** — no lo embedes en el **Order** Aggregate.

### **Por qué esto es "Normalizado" en DDD:**

- Cada Aggregate posee sus datos.
- No hay duplicación de estado entre Aggregates.
- Consistencia fuerte dentro del Aggregate, eventual a través.
- Cambios a **Customer** no requieren actualizar todas las **Orders**.

###  **Anti-Patrón: Embeber Estado de Aggregate Externo**

*// Evitar esto en el modelo de escritura DDD*

```java
public class Order 
{
    public CustomerId CustomerId { get; }
    public string CustomerName { get; } // ← ¡Viola el límite!
    public decimal CustomerCreditLimit { get; } // ← ¡Ahora tienes que sincronizar esto!
}
```

→ Esto crea una dependencia oculta y fuerza la sincronización — rompe la autonomía e invita a la inconsistencia.

## **Desnormalización: Duplicación Estratégica por Razones de Dominio**

DDD **permite** e incluso **alienta** la desnormalización — **pero solo cuando sirve al dominio**, tal como:

- Preservar estado histórico (ej. precio al momento de la orden)
- Habilitar autonomía (servicio debe funcionar incluso si otros están caídos)
- Optimizar rendimiento (evitar joins o llamadas de servicio en tiempo de lectura)
- Soportar invariantes (reglas de negocio)

### **Ejemplo 1: Desnormalizando Value Objects (Seguro y Común)**

```java
public class Order: AggregateRoot
{
    public OrderId Id { get; }
    public Address ShippingAddress { get; } // ← Value Object Inmutable — seguro de copiar
    public Money Total { get; } // ← Snapshot del total al checkout
}
```
- **Address** y **Money** son **Value Objects** — inmutables, definidos por sus atributos. Seguros de embedar.
- Incluso si el cliente luego cambia su dirección, las órdenes pasadas deben retener la dirección *al momento de la orden* — esta es una **regla de negocio**, no una optimización técnica.

### **Ejemplo 2: Desnormalizando para Read Models (CQRS)**

*// Read Model (desnormalizado, optimizado para UI)*

```java
public class OrderSummary
{
    public string OrderNumber { get; set; }
    public string CustomerName { get; set; } // ← Copiado del Customer BC
    public string ShippingAddress { get; set; }
    public decimal Total { get; set; }
    public string Status { get; set; }
}
```

- Esto vive en un **store de lectura separado**, actualizado vía **domain events** (OrderPlaced, CustomerNameChanged).
- El modelo de escritura permanece normalizado — el modelo de lectura está desnormalizado por rendimiento.

### **Ejemplo 3: Desnormalizando a través de Bounded Contexts vía Eventos**

*// En "Order Fulfillment" Bounded Context*

```java
public class Shipment: AggregateRoot
{
    public OrderId OrderId { get; }
    public string CustomerName { get; private set; } // ← Copiado vía evento
    public Address ShippingAddress { get; } // ← Copiado al momento del envío
    
    public void Apply(OrderPlacedEvent e)
    {
        CustomerName = e.CustomerName; // ← Copia desnormalizada
        ShippingAddress = e.ShippingAddress;
    }
}
```

- **CustomerName** es copiado del contexto de **Sales** vía un evento.
    - **¿Por qué?**: Fulfillment debe imprimir etiquetas de envío incluso si el servicio de Sales está caído.
    - La **consistencia eventual** es aceptable — y esperada — a través de Bounded Contexts.

## **Cuándo Normalizar vs Desnormalizar**

| Consideración | Preferir Normalización | Preferir Desnormalización |
| --- | --- | --- |
| Consistencia | Consistencia fuerte requerida | Consistencia eventual aceptable |
| Propiedad | Datos propiedad de otro Aggregate/BC | Datos necesarios localmente para autonomía o rendimiento |
| Inmutabilidad | — | Datos son snapshot (ej. auditoría, historial) |
| Rendimiento | Bajo volumen de lectura, latencia aceptable | Alto volumen de lectura, baja latencia requerida |
| Resiliencia | Servicios dependientes son altamente disponibles | Debe funcionar cuando servicios upstream fallan |
| Tipo | Referencia a otro Aggregate | Value Object o Read Model |

## **Cómo Sincronizar Datos Desnormalizados**

Ya que DDD desaconseja compartir base de datos directa, se sincroniza vía:

### **1. Domain Events**
- CustomerAddressUpdated → escuchado por servicios Order, Shipment, Invoice.
- Actualiza copias locales o read models.

### **2. Application Events / Integration Events**
- Publicados después de que una transacción completa.
- Usados para sincronización cross-Bounded Context.

### **3. Change Data Capture (CDC)**
- Capturar cambios de DB → transmitir a otros servicios.
- Útil para sistemas legacy o no-DDD.
- [How to ...](https://www.confluent.io/blog/how-change-data-capture-works-patterns-solutions-implementation/)

### **4. Anti-Corruption Layer (ACL)**

Traducir y adaptar modelos extranjeros → almacenar copia desnormalizada local.

## **Lo que DDD Desalienta**

- ❌ **Tablas de base de datos compartidas entre servicios** → rompe la autonomía.
- ❌ **Foreign keys a través de límites de Aggregate** → rompe el modelo de consistencia transaccional.
- ❌ **Embedar directo de estado de Aggregate externo en modelos de escritura** → crea acoplamiento oculto.


## **Mejores Prácticas para Normalización/Desnormalización en DDD**
- **Modelar Aggregates como límites de consistencia** — normalizar dentro, referenciar por ID a través.
- **Desnormalizar Value Objects libremente** — son inmutables y seguros.
- **Usar Domain Events para propagar cambios** — no polling o acceso directo a DB.
- **Aplicar CQRS** — mantener modelo de escritura normalizado, modelo de lectura desnormalizado.
- **Versionar tus eventos** — para manejar evolución de esquema en consumidores desnormalizados.
- **Reconciliar inconsistencias** — construir acciones compensatorias o trabajos de auditoría.
- **Documentar por qué se desnormalizó** — ¿fue por autonomía? auditoría? rendimiento? Hacerlo explícito.

## **Lectura Adicional**

- **"Implementing Domain-Driven Design"** por Vaughn Vernon — Capítulo sobre Aggregates & Bounded Contexts.

- **"Domain Modeling Made Functional"** por Scott Wlaschin — excelentes ejemplos de Value Objects e inmutabilidad.

- **Patrones CQRS & Event Sourcing** — extensiones naturales del modelado de datos DDD.
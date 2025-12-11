# Patrón Saga

## ¿Qué es el Patrón Saga?

El **Patrón Saga** es un patrón de diseño para manejar transacciones distribuidas en arquitecturas de microservicios, donde no es posible usar transacciones ACID tradicionales. Un Saga es una secuencia de transacciones locales donde cada transacción actualiza datos dentro del alcance de un único servicio. Si una transacción falla, las transacciones compensatorias deshacen los cambios realizados por las transacciones anteriores.

### Conceptos Fundamentales

**Saga**: Secuencia de transacciones locales que se ejecutan en diferentes servicios para completar una transacción de negocio.

**Transacción Compensatoria**: Acción que deshace los efectos de una transacción anterior en el Saga.

**Orquestador**: Componente central que coordina la ejecución del Saga y determina el orden de las transacciones.

**Coreógrafo**: Enfoque donde los servicios reaccionan a eventos y deciden las próximas acciones sin un coordinador central.

<br>
<img src="./imgs/saga.png" style="max-width:700px" />

---

## **Tipos de Saga**

### **Orquestación (Orchestration)**
- Un orquestador central controla el flujo
- El orquestador llama a los servicios participantes
- Servicios responden con éxito/fallo

### **Coreografía (Choreography)**
- Los servicios reaccionan a eventos
- No hay control central
- Cada servicio decide las próximas acciones

### **Gestión de Estado**
- El Saga mantiene estado de la transacción
- Tracking del progreso y compensaciones
- Manejo de fallos y recuperación

---

## **Casos de Uso**

### **E-commerce - Procesamiento de Pedidos**
- Reserva de inventario
- Procesamiento de pago
- Envío de confirmación
- Actualización de estado

### **Servicios Financieros**
- Transferencias entre cuentas
- Actualización de balances
- Generación de transacciones
- Notificaciones

### **Reservas de Viajes**
- Reserva de vuelo
- Reserva de hotel
- Alquiler de auto
- Confirmación de paquete

### **Onboarding de Usuarios**
- Creación de perfil
- Configuración de preferencias
- Envío de bienvenida
- Integración con servicios externos

### **Gestión de Inventario**
- Reducción de stock
- Actualización de catálogo
- Sincronización entre almacenes
- Alertas de stock bajo

---

## **Ventajas y Desventajas**

### **Ventajas**

**Consistencia Eventual**
- Permite consistencia en sistemas distribuidos
- Manejo de fallos temporales
- Continuidad del servicio

**Escalabilidad**
- No requiere transacciones distribuidas
- Paralelización de transacciones independientes
- Scaling horizontal por servicio

**Desacoplamiento**
- Servicios mantienen autonomía
- Comunicación asíncrona
- Flexibilidad en implementación

**Resiliencia**
- Compensaciones automáticas
- Recuperación de fallos
- Tolerancia a errores temporales

### **Desventajas**

**Complejidad**
- Lógica de compensación compleja
- Gestión de estados distribuida
- Debugging más difícil

**Consistencia Eventual**
- Datos inconsistentes temporalmente
- Lecturas potencialmente obsoletas
- Coordinación de esperas

**Performance**
- Overhead de coordinación
- Latencia por múltiples servicios
- Sincronización de estados

**Debugging y Testing**
- Dificultad para reproducir escenarios
- Testing de casos edge complejos
- Monitoreo distribuido

---

## **Buenas Prácticas**

### **Diseño de Transacciones**

**Idempotencia**
```java
@Component
public class SagaTransaction {
    
    @Transactional
    public void executeTransaction(String transactionId) {
        // Verificar si ya se ejecutó
        if (transactionLog.isExecuted(transactionId)) {
            return; // Ya ejecutado, no hacer nada
        }
        
        try {
            // Ejecutar lógica de negocio
            processTransaction();
            
            // Marcar como exitoso
            transactionLog.markCompleted(transactionId);
            
        } catch (Exception e) {
            // Marcar como fallido
            transactionLog.markFailed(transactionId, e);
            throw e;
        }
    }
}
```

**Compensación Granular**
```java
@Service
public class OrderProcessingSaga {
    
    public void processOrder(Order order) {
        try {
            // Paso 1: Crear pedido
            String orderId = orderService.createOrder(order);
            
            // Paso 2: Procesar pago
            paymentService.processPayment(order.getPaymentInfo());
            
            // Paso 3: Reservar inventario
            inventoryService.reserveItems(order.getItems());
            
            // Paso 4: Enviar confirmación
            notificationService.sendConfirmation(order.getCustomerId(), orderId);
            
        } catch (PaymentException e) {
            // Compensar: cancelar pedido
            orderService.cancelOrder(order.getId());
            throw e;
            
        } catch (InventoryException e) {
            // Compensar: cancelar pedido y reembolsar
            orderService.cancelOrder(order.getId());
            paymentService.refundPayment(order.getPaymentInfo());
            throw e;
            
        } catch (Exception e) {
            // Compensar todas las operaciones anteriores
            compensateAllOperations(order);
            throw e;
        }
    }
    
    private void compensateAllOperations(Order order) {
        try {
            // Compensación en orden inverso
            notificationService.cancelConfirmation(order.getCustomerId());
        } catch (Exception ignored) {}
        
        try {
            inventoryService.releaseItems(order.getItems());
        } catch (Exception ignored) {}
        
        try {
            paymentService.refundPayment(order.getPaymentInfo());
        } catch (Exception ignored) {}
        
        try {
            orderService.cancelOrder(order.getId());
        } catch (Exception ignored) {}
    }
}
```

### **Gestión de Estado**

```java
@Entity
@Table(name = "saga_instance")
public class SagaInstance {
    
    @Id
    private String sagaId;
    
    @Enumerated(EnumType.STRING)
    private SagaStatus status;
    
    @Convert(converter = JsonConverter.class)
    private Map<String, Object> sagaData;
    
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private String compensatingOperation;
    
    // Métodos para gestión de estado
    public void addStepResult(String stepName, Object result) {
        if (sagaData == null) {
            sagaData = new HashMap<>();
        }
        sagaData.put(stepName, result);
    }
    
    public void markStepCompleted(String stepName) {
        addStepResult(stepName + "_status", "COMPLETED");
    }
    
    public void markStepFailed(String stepName, String error) {
        addStepResult(stepName + "_status", "FAILED");
        addStepResult(stepName + "_error", error);
    }
}
```

---

## **Ejemplos en Java**

### **Implementación con Orquestador**

```java
@Component
public class OrderSagaOrchestrator {
    
    @Autowired
    private OrderService orderService;
    
    @Autowired
    private PaymentService paymentService;
    
    @Autowired
    private InventoryService inventoryService;
    
    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private SagaRepository sagaRepository;
    
    public void executeOrderSaga(OrderRequest orderRequest) {
        String sagaId = UUID.randomUUID().toString();
        
        // Crear instancia del Saga
        SagaInstance saga = createSagaInstance(sagaId, orderRequest);
        sagaRepository.save(saga);
        
        try {
            // Paso 1: Crear pedido
            saga.markStepStarted("createOrder");
            String orderId = orderService.createOrder(orderRequest);
            saga.addStepResult("orderId", orderId);
            saga.markStepCompleted("createOrder");
            sagaRepository.save(saga);
            
            // Paso 2: Procesar pago
            saga.markStepStarted("processPayment");
            PaymentResult paymentResult = paymentService.processPayment(
                orderRequest.getPaymentInfo(), orderId);
            saga.addStepResult("paymentResult", paymentResult);
            saga.markStepCompleted("processPayment");
            sagaRepository.save(saga);
            
            // Paso 3: Reservar inventario
            saga.markStepStarted("reserveInventory");
            InventoryResult inventoryResult = inventoryService.reserveItems(
                orderRequest.getItems(), orderId);
            saga.addStepResult("inventoryResult", inventoryResult);
            saga.markStepCompleted("reserveInventory");
            sagaRepository.save(saga);
            
            // Paso 4: Enviar confirmación
            saga.markStepStarted("sendConfirmation");
            notificationService.sendConfirmation(
                orderRequest.getCustomerId(), orderId);
            saga.markStepCompleted("sendConfirmation");
            sagaRepository.save(saga);
            
            // Saga completado exitosamente
            saga.setStatus(SagaStatus.COMPLETED);
            saga.setCompletedAt(LocalDateTime.now());
            sagaRepository.save(saga);
            
        } catch (Exception e) {
            // Iniciar compensación
            startCompensation(saga, e);
        }
    }
    
    private void startCompensation(SagaInstance saga, Exception originalError) {
        saga.setStatus(SagaStatus.COMPENSATING);
        saga.setError(originalError.getMessage());
        sagaRepository.save(saga);
        
        compensateInReverseOrder(saga, originalError);
    }
    
    private void compensateInReverseOrder(SagaInstance saga, Exception originalError) {
        try {
            // Compensación en orden inverso
            
            // Cancelar confirmación si se envió
            if (saga.isStepCompleted("sendConfirmation")) {
                saga.markCompensationStarted("cancelConfirmation");
                String orderId = saga.getStepResult("orderId");
                notificationService.cancelConfirmation(
                    saga.getOrderRequest().getCustomerId(), orderId);
                saga.markCompensationCompleted("cancelConfirmation");
            }
            
            // Liberar inventario si se reservó
            if (saga.isStepCompleted("reserveInventory")) {
                saga.markCompensationStarted("releaseInventory");
                InventoryResult inventoryResult = saga.getStepResult("inventoryResult");
                inventoryService.releaseItems(inventoryResult.getReservedItems());
                saga.markCompensationCompleted("releaseInventory");
            }
            
            // Reembolsar pago si se procesó
            if (saga.isStepCompleted("processPayment")) {
                saga.markCompensationStarted("refundPayment");
                PaymentResult paymentResult = saga.getStepResult("paymentResult");
                paymentService.refundPayment(paymentResult.getTransactionId());
                saga.markCompensationCompleted("refundPayment");
            }
            
            // Cancelar pedido
            if (saga.isStepCompleted("createOrder")) {
                saga.markCompensationStarted("cancelOrder");
                String orderId = saga.getStepResult("orderId");
                orderService.cancelOrder(orderId);
                saga.markCompensationCompleted("cancelOrder");
            }
            
            saga.setStatus(SagaStatus.COMPENSATED);
            saga.setCompletedAt(LocalDateTime.now());
            sagaRepository.save(saga);
            
        } catch (Exception compensationError) {
            // Manejar errores de compensación
            saga.setStatus(SagaStatus.COMPENSATION_FAILED);
            saga.setCompensationError(compensationError.getMessage());
            sagaRepository.save(saga);
            
            // Enviar alerta para intervención manual
            alertService.sendCompensationFailureAlert(saga, originalError, compensationError);
        }
    }
}
```

### **Implementación con Coreografía**

```java
// Evento base
public abstract class SagaEvent {
    private String sagaId;
    private LocalDateTime timestamp;
    
    // Constructor y getters
}

// Eventos específicos
public class OrderCreatedEvent extends SagaEvent {
    private String orderId;
    private OrderRequest orderRequest;
}

public class PaymentProcessedEvent extends SagaEvent {
    private String orderId;
    private PaymentResult paymentResult;
}

public class InventoryReservedEvent extends SagaEvent {
    private String orderId;
    private InventoryResult inventoryResult;
}

// Servicio de pedidos
@Service
public class OrderEventHandler {
    
    @Autowired
    private EventGateway eventGateway;
    
    @EventListener
    public void handleOrderCreatedEvent(OrderCreatedEvent event) {
        try {
            PaymentResult paymentResult = paymentService.processPayment(
                event.getOrderRequest().getPaymentInfo(), event.getOrderId());
            
            PaymentProcessedEvent processedEvent = new PaymentProcessedEvent();
            processedEvent.setSagaId(event.getSagaId());
            processedEvent.setOrderId(event.getOrderId());
            processedEvent.setPaymentResult(paymentResult);
            
            eventGateway.publishEvent(processedEvent);
            
        } catch (Exception e) {
            // Enviar evento de fallo
            sendSagaFailedEvent(event.getSagaId(), event.getOrderId(), e);
        }
    }
    
    @EventListener
    public void handlePaymentFailedEvent(PaymentFailedEvent event) {
        // Compensar: cancelar pedido
        orderService.cancelOrder(event.getOrderId());
        
        // Notificar fallo del saga
        sendSagaFailedEvent(event.getSagaId(), event.getOrderId(), 
            new PaymentException("Payment failed"));
    }
}

// Servicio de inventario
@Service
public class InventoryEventHandler {
    
    @Autowired
    private EventGateway eventGateway;
    
    @EventListener
    public void handlePaymentProcessedEvent(PaymentProcessedEvent event) {
        try {
            InventoryResult inventoryResult = inventoryService.reserveItems(
                event.getOrderId());
            
            InventoryReservedEvent reservedEvent = new InventoryReservedEvent();
            reservedEvent.setSagaId(event.getSagaId());
            reservedEvent.setOrderId(event.getOrderId());
            reservedEvent.setInventoryResult(inventoryResult);
            
            eventGateway.publishEvent(reservedEvent);
            
        } catch (Exception e) {
            // Compensar: reembolsar pago
            paymentService.refundPayment(event.getPaymentResult().getTransactionId());
            
            // Enviar evento de fallo
            sendSagaFailedEvent(event.getSagaId(), event.getOrderId(), e);
        }
    }
    
    @EventListener
    public void handleInventoryFailedEvent(InventoryFailedEvent event) {
        // Compensar: reembolsar pago
        paymentService.refundPayment(event.getPaymentResult().getTransactionId());
        
        // Enviar evento de fallo
        sendSagaFailedEvent(event.getSagaId(), event.getOrderId(),
            new InventoryException("Inventory reservation failed"));
    }
}
```

### **Implementación con EventStore**

```java
@Entity
@Table(name = "event_store")
public class StoredEvent {
    
    @Id
    private String eventId;
    
    private String aggregateId;
    private String eventType;
    private String eventData;
    private String metaData;
    private LocalDateTime occurredAt;
    private long version;
    
    // Constructors, getters, setters
}

@Service
public class EventStore {
    
    @Autowired
    private EventStoreRepository repository;
    
    public void appendEvents(String aggregateId, List<Object> events) {
        long currentVersion = getCurrentVersion(aggregateId);
        
        for (int i = 0; i < events.size(); i++) {
            Object event = events.get(i);
            StoredEvent storedEvent = new StoredEvent();
            storedEvent.setEventId(UUID.randomUUID().toString());
            storedEvent.setAggregateId(aggregateId);
            storedEvent.setEventType(event.getClass().getSimpleName());
            storedEvent.setEventData(serializeEvent(event));
            storedEvent.setOccurredAt(LocalDateTime.now());
            storedEvent.setVersion(currentVersion + i + 1);
            
            repository.save(storedEvent);
        }
    }
    
    public List<StoredEvent> getEvents(String aggregateId) {
        return repository.findByAggregateIdOrderByVersionAsc(aggregateId);
    }
    
    private String serializeEvent(Object event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new EventSerializationException("Failed to serialize event", e);
        }
    }
}

// Reconstrucción de estado desde eventos
@Service
public class OrderReconstructionService {
    
    public Order reconstructOrder(String orderId) {
        List<StoredEvent> events = eventStore.getEvents(orderId);
        
        Order order = new Order();
        
        for (StoredEvent event : events) {
            applyEvent(order, event);
        }
        
        return order;
    }
    
    private void applyEvent(Order order, StoredEvent event) {
        switch (event.getEventType()) {
            case "OrderCreatedEvent":
                OrderCreatedEvent createdEvent = deserializeEvent(event);
                order.apply(createdEvent);
                break;
            case "PaymentProcessedEvent":
                PaymentProcessedEvent paymentEvent = deserializeEvent(event);
                order.apply(paymentEvent);
                break;
            // ... más casos
        }
    }
}
```

### **Saga con Timeout y Compensación Automática**

```java
@Component
public class SagaTimeoutHandler {
    
    @Autowired
    private SagaRepository sagaRepository;
    
    @Scheduled(fixedRate = 60000) // Cada minuto
    public void checkSagaTimeouts() {
        LocalDateTime timeoutThreshold = LocalDateTime.now().minusMinutes(30);
        
        List<SagaInstance> timedOutSagas = sagaRepository
            .findByStatusInAndStartedAtBefore(
                Arrays.asList(SagaStatus.IN_PROGRESS, SagaStatus.COMPENSATING), 
                timeoutThreshold);
        
        for (SagaInstance saga : timedOutSagas) {
            handleSagaTimeout(saga);
        }
    }
    
    private void handleSagaTimeout(SagaInstance saga) {
        log.warn("Saga {} has timed out, initiating compensation", saga.getSagaId());
        
        try {
            if (saga.getStatus() == SagaStatus.IN_PROGRESS) {
                saga.setStatus(SagaStatus.COMPENSATING);
                saga.setTimeoutCompensation(true);
                sagaRepository.save(saga);
                
                // Iniciar compensación
                startCompensation(saga, new TimeoutException("Saga timeout"));
                
            } else if (saga.getStatus() == SagaStatus.COMPENSATING) {
                // Compensación también timeout
                saga.setStatus(SagaStatus.COMPENSATION_FAILED);
                saga.setError("Compensation timeout");
                sagaRepository.save(saga);
                
                // Enviar alerta para intervención manual
                alertService.sendSagaTimeoutAlert(saga);
            }
            
        } catch (Exception e) {
            log.error("Failed to handle saga timeout for saga {}", saga.getSagaId(), e);
        }
    }
}

// Monitor de sagas
@Component
public class SagaMonitor {
    
    private final MeterRegistry meterRegistry;
    private final Counter sagaCounter;
    private final Timer sagaDurationTimer;
    
    public SagaMonitor(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.sagaCounter = Counter.builder("saga.operations.total")
            .description("Total number of saga operations")
            .register(meterRegistry);
        this.sagaDurationTimer = Timer.builder("saga.duration")
            .description("Time taken to complete sagas")
            .register(meterRegistry);
    }
    
    public void recordSagaCompletion(SagaInstance saga, SagaStatus finalStatus) {
        sagaCounter.increment(Tags.of("status", finalStatus.name()));
        
        if (finalStatus == SagaStatus.COMPLETED) {
            Duration duration = Duration.between(saga.getStartedAt(), saga.getCompletedAt());
            sagaDurationTimer.record(duration);
        }
    }
}
```

---

## **Patrones Avanzados**

### **Saga con Escalación**
```java
@Component
public class SagaEscalationHandler {
    
    public void escalateSagaIfNeeded(SagaInstance saga) {
        if (shouldEscalate(saga)) {
            SagaEscalation escalation = createEscalation(saga);
            escalationService.createEscalation(escalation);
            
            // Notificar a equipos
            notificationService.notifyOperationsTeam(escalation);
        }
    }
    
    private boolean shouldEscalate(SagaInstance saga) {
        return saga.getRetryCount() > 5 ||
               saga.getCompensationAttempts() > 3 ||
               isBusinessCriticalSaga(saga);
    }
}
```

### **Compensación Paralela**
```java
@Component
public class ParallelCompensationService {
    
    public CompletableFuture<Void> compensateInParallel(SagaInstance saga) {
        List<Callable<Void>> compensationTasks = createCompensationTasks(saga);
        
        ExecutorService executor = Executors.newFixedThreadPool(4);
        
        try {
            List<Future<Void>> futures = executor.invokeAll(compensationTasks);
            
            // Esperar a que todas las compensaciones terminen
            for (Future<Void> future : futures) {
                future.get(); // Propagar excepciones
            }
            
        } catch (InterruptedException | ExecutionException e) {
            throw new CompensationException("Parallel compensation failed", e);
        } finally {
            executor.shutdown();
        }
        
        return CompletableFuture.completedFuture(null);
    }
}
```

---

## **Referencias Oficiales**

1. **Microsoft - Saga Pattern**  
   https://docs.microsoft.com/en-us/azure/architecture/patterns/saga

2. **AWS - Implementing the Saga Pattern**  
   https://docs.aws.amazon.com/prescriptive-guidance/latest/patterns/implementing-the-saga-pattern.html

3. **Microservices.io - Saga Pattern**  
   https://microservices.io/patterns/data/saga.html

4. **Eventuate - Sagas**  
   https://eventuate.io/docs/why-event-sourcing-microservices.html

5. **Axon Framework - Sagas**  
   https://docs.axoniq.io/reference-guide/axon-framework/sagas/
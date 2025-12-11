# Idempotencia en Microservicios

## **¿Qué es la Idempotencia?**

La **idempotencia** es una propiedad fundamental en sistemas distribuidos donde una operación puede ejecutarse múltiples veces con el mismo resultado. Una operación idempotente produce el mismo resultado independientemente de cuántas veces se ejecute con los mismos parámetros de entrada.

**Definición formal**: Una operación f es idempotente si y solo si f(f(x)) = f(x) para todos los valores x en el dominio de f.

---

## **Conceptos Fundamentales**

### Idempotencia vs. Seguridad
- **Idempotente**: La operación puede repetirse sin efectos secundarios no deseados
- **Segura**: La operación no cambia el estado del sistema (read-only)

<br>
<img src="./imgs/idempotency.png" style="max-width:700px" />



### Tipos de Idempotencia

#### 1. Idempotencia por Definición
```java
// Operación naturalmente idempotente
public class IdempotentOperation {
    public User getUserById(String userId) {
        return userRepository.findById(userId); // Siempre devuelve el mismo resultado
    }
}
```

#### 2. Idempotencia por Diseño
```java
// Operación diseñada para ser idempotente
public class OrderService {
    
    // Clave de idempotencia para evitar duplicados
    public Order createOrder(CreateOrderCommand command, String idempotencyKey) {
        
        // Verificar si ya existe una operación con esta clave
        if (idempotencyRepository.exists(idempotencyKey)) {
            return idempotencyRepository.getResult(idempotencyKey);
        }
        
        try {
            Order order = doCreateOrder(command);
            // Guardar el resultado con la clave de idempotencia
            idempotencyRepository.save(idempotencyKey, order);
            return order;
        } catch (Exception e) {
            // En caso de error, limpiar la entrada de idempotencia
            idempotencyRepository.remove(idempotencyKey);
            throw e;
        }
    }
}
```

---

## **Problemas que Resuelve la Idempotencia**

### 1. Reintentos Automáticos
Los clientes y middlewares (como proxies) pueden reintentar automáticamente operaciones fallidas.

```java
// Cliente con reintentos automáticos
public class ResilientHttpClient {
    
    public <T> T executeWithRetry(HttpRequest request, Class<T> responseType) {
        int maxRetries = 3;
        int attempt = 0;
        
        while (attempt < maxRetries) {
            try {
                return executeRequest(request, responseType);
            } catch (TransientException e) {
                attempt++;
                if (attempt >= maxRetries) {
                    throw new RetryExhaustedException("Max retries exceeded", e);
                }
                sleep(calculateBackoff(attempt));
            }
        }
        throw new UnreachableException("Should not reach here");
    }
}
```

### 2. Fallos de Red y Timeouts
Los timeouts pueden crear incertidumbre sobre si la operación se completó exitosamente.

```java
// Manejo de operaciones con timeout
public class PaymentService {
    
    public PaymentResult processPayment(PaymentRequest request) {
        try {
            // La operación puede timeout, pero el pago puede haberse procesado
            return externalPaymentGateway.charge(request);
        } catch (TimeoutException e) {
            // No sabemos si el pago se procesó
            // Consultar el estado de la transacción
            return checkPaymentStatus(request.getTransactionId());
        }
    }
}
```

### 3. Duplicación de Mensajes
En sistemas de mensajería, los mensajes pueden entregarse múltiples veces.

```java
// Consumidor de mensajes idempotente
@Component
public class OrderMessageConsumer {
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private MessageDeduplicator deduplicator;
    
    @EventListener
    public void handleOrderCreated(OrderCreatedEvent event) {
        String messageId = event.getMessageId();
        
        // Verificar si ya procesamos este mensaje
        if (deduplicator.isProcessed(messageId)) {
            return; // Ignorar mensaje duplicado
        }
        
        try {
            processOrder(event.getOrderId());
            deduplicator.markAsProcessed(messageId);
        } catch (Exception e) {
            // No marcar como procesado en caso de error
            throw e;
        }
    }
}
```

---

## **Estrategias de Implementación**

### 1. Claves de Idempotencia

#### Implementación Básica
```java
@Component
public class IdempotencyKeyManager {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    private static final String IDEMPOTENCY_PREFIX = "idempotency:";
    
    public <T> T executeWithIdempotency(String key, Supplier<T> operation, Duration ttl) {
        String redisKey = IDEMPOTENCY_PREFIX + key;
        
        // Verificar si ya existe una ejecución
        Object cachedResult = redisTemplate.opsForValue().get(redisKey);
        if (cachedResult != null) {
            return (T) cachedResult;
        }
        
        // Ejecutar la operación
        T result = operation.get();
        
        // Guardar el resultado
        redisTemplate.opsForValue().set(redisKey, result, ttl);
        
        return result;
    }
}
```

#### Uso en Servicio
```java
@Service
public class UserService {
    
    @Autowired
    private IdempotencyKeyManager idempotencyManager;
    
    public User createUser(CreateUserRequest request, String idempotencyKey) {
        return idempotencyManager.executeWithIdempotency(
            idempotencyKey,
            () -> doCreateUser(request),
            Duration.ofHours(1)
        );
    }
    
    private User doCreateUser(CreateUserRequest request) {
        // Lógica de creación de usuario
        User user = new User(request.getEmail(), request.getName());
        return userRepository.save(user);
    }
}
```

### 2. Operaciones de Upsert

#### Repository Pattern
```java
@Repository
public class UserRepository {
    
    public User upsertUser(User user) {
        // Implementación idempotente
        Optional<User> existing = findByEmail(user.getEmail());
        
        if (existing.isPresent()) {
            User existingUser = existing.get();
            existingUser.setName(user.getName());
            existingUser.setUpdatedAt(LocalDateTime.now());
            return save(existingUser);
        } else {
            user.setCreatedAt(LocalDateTime.now());
            return save(user);
        }
    }
}
```

#### SQL Upsert
```sql
-- PostgreSQL
INSERT INTO users (email, name, created_at, updated_at)
VALUES ($1, $2, NOW(), NOW())
ON CONFLICT (email) 
DO UPDATE SET 
    name = EXCLUDED.name,
    updated_at = NOW()
RETURNING *;

-- MySQL
INSERT INTO users (email, name, created_at, updated_at)
VALUES (?, ?, NOW(), NOW())
ON DUPLICATE KEY UPDATE 
    name = VALUES(name),
    updated_at = NOW();
```

### 3. Verificación de Estado

#### Patrón Check-Then-Act
```java
public class AccountService {
    
    public void transferMoney(String fromAccount, String toAccount, BigDecimal amount) {
        // Verificar si ya se ejecutó esta transferencia
        String transactionId = generateTransactionId(fromAccount, toAccount, amount);
        
        if (transferRepository.existsByTransactionId(transactionId)) {
            return; // Transferencia ya procesada
        }
        
        // Verificar saldo
        Account from = accountRepository.findById(fromAccount);
        if (from.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException();
        }
        
        // Ejecutar transferencia atómica
        try {
            doTransfer(from, toAccount, amount);
            transferRepository.save(new Transfer(transactionId, fromAccount, toAccount, amount));
        } catch (Exception e) {
            // Log error pero no revertir la transferencia ya ejecutada
            log.error("Transfer failed for transaction: {}", transactionId, e);
            throw e;
        }
    }
}
```

---

## **Casos de Uso Específicos**

### 1. APIs RESTful

#### Patrón PUT vs POST
```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    // PUT es naturalmente idempotente
    @PutMapping("/{userId}")
    public ResponseEntity<User> updateUser(@PathVariable String userId, 
                                          @RequestBody UserUpdateRequest request) {
        User updated = userService.updateUser(userId, request);
        return ResponseEntity.ok(updated);
    }
    
    // POST requiere claves de idempotencia para ser idempotente
    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody CreateUserRequest request,
                                          @RequestHeader("Idempotency-Key") String idempotencyKey) {
        User created = userService.createUser(request, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
```

#### Manejo de Headers HTTP
```java
@Component
public class IdempotencyInterceptor implements HandlerInterceptor {
    
    @Override
    public boolean preHandle(HttpServletRequest request, 
                           HttpServletResponse response, 
                           Object handler) {
        
        String idempotencyKey = request.getHeader("Idempotency-Key");
        if (idempotencyKey != null && !idempotencyKey.isEmpty()) {
            // Verificar si existe una respuesta previa
            CachedResponse cached = cacheService.get(idempotencyKey);
            if (cached != null) {
                writeCachedResponse(response, cached);
                return false; // No continuar con la ejecución
            }
        }
        
        return true;
    }
    
    @Override
    public void postHandle(HttpServletRequest request, 
                         HttpServletResponse response, 
                         Object handler, 
                         ModelAndView modelAndView) {
        
        String idempotencyKey = request.getHeader("Idempotency-Key");
        if (idempotencyKey != null && response.getStatus() < 400) {
            // Guardar respuesta para futuros requests
            cacheService.put(idempotencyKey, new CachedResponse(response));
        }
    }
}
```

### 2. Sistemas de Mensajería

#### Consumer Idempotente
```java
@Component
public class EventProcessingService {
    
    private final Set<String> processedEvents = ConcurrentHashMap.newKeySet();
    
    @EventListener
    public void handleUserRegistered(UserRegisteredEvent event) {
        String eventId = event.getEventId();
        
        // Verificación rápida en memoria
        if (!processedEvents.add(eventId)) {
            log.debug("Event {} already processed", eventId);
            return;
        }
        
        try {
            processUserRegistration(event.getUserId());
            
            // Persistir para recuperación después de restart
            eventRepository.markAsProcessed(eventId);
            
        } catch (Exception e) {
            // Remover del set para permitir reintento
            processedEvents.remove(eventId);
            log.error("Failed to process event {}", eventId, e);
            throw e;
        }
    }
}
```

#### Outbox Pattern
```java
@Entity
public class OutboxMessage {
    
    @Id
    @GeneratedValue
    private Long id;
    
    private String aggregateId;
    private String eventType;
    private String payload;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
    
    // Estado: PENDING, PROCESSED, FAILED
    @Enumerated(EnumType.STRING)
    private MessageStatus status;
    
    // Constructor y métodos
}

@Service
public class OutboxService {
    
    @Autowired
    private OutboxMessageRepository outboxRepository;
    
    @Autowired
    private EventPublisher eventPublisher;
    
    @Transactional
    public void publishEvent(String aggregateId, String eventType, Object eventData) {
        OutboxMessage message = new OutboxMessage();
        message.setAggregateId(aggregateId);
        message.setEventType(eventType);
        message.setPayload(serialize(eventData));
        message.setCreatedAt(LocalDateTime.now());
        message.setStatus(MessageStatus.PENDING);
        
        outboxRepository.save(message);
        // El commit de la transacción hace visible este mensaje
    }
    
    @Scheduled(fixedDelay = 1000)
    public void processPendingMessages() {
        List<OutboxMessage> pendingMessages = outboxRepository.findByStatus(MessageStatus.PENDING);
        
        for (OutboxMessage message : pendingMessages) {
            try {
                eventPublisher.publish(message.getEventType(), deserialize(message.getPayload()));
                message.setStatus(MessageStatus.PROCESSED);
                message.setProcessedAt(LocalDateTime.now());
            } catch (Exception e) {
                message.setStatus(MessageStatus.FAILED);
                log.error("Failed to process message {}", message.getId(), e);
            }
            outboxRepository.save(message);
        }
    }
}
```

### 3. Bases de Datos Distribuidas

#### Optimistic Locking
```java
@Entity
public class BankAccount {
    
    @Id
    private String accountId;
    
    private BigDecimal balance;
    
    @Version
    private Long version; // Para optimistic locking
    
    // Constructor y métodos
}

@Service
public class BankAccountService {
    
    public void transfer(String fromAccountId, String toAccountId, BigDecimal amount) {
        // Este método es idempotente debido al optimistic locking
        // Si se ejecuta dos veces con los mismos parámetros,
        // la segunda ejecución fallará por conflicto de versión
        
        Account fromAccount = accountRepository.findById(fromAccountId);
        Account toAccount = accountRepository.findById(toAccountId);
        
        if (fromAccount.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException();
        }
        
        fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
        toAccount.setBalance(toAccount.getBalance().add(amount));
        
        accountRepository.saveAll(Arrays.asList(fromAccount, toAccount));
    }
}
```

#### Constraints de Base de Datos
```sql
-- Constraint única para prevenir duplicados
CREATE TABLE user_preferences (
    user_id VARCHAR(255) NOT NULL,
    preference_key VARCHAR(255) NOT NULL,
    preference_value TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, preference_key),
    UNIQUE KEY unique_user_preference (user_id, preference_key)
);
```

---

## **Buenas Prácticas**

### 1. Diseño de APIs

#### Claves de Idempotencia
```java
@RestController
public class OrderController {
    
    @PostMapping("/orders")
    public ResponseEntity<Order> createOrder(
            @RequestBody CreateOrderRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        
        // Validar clave de idempotencia si se proporciona
        if (idempotencyKey != null && !isValidIdempotencyKey(idempotencyKey)) {
            return ResponseEntity.badRequest().build();
        }
        
        Order order = orderService.createOrder(request, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }
    
    private boolean isValidIdempotencyKey(String key) {
        // Validar formato de la clave
        return key != null && !key.trim().isEmpty() && key.length() <= 255;
    }
}
```

#### Códigos de Estado HTTP
```java
@ControllerAdvice
public class IdempotencyExceptionHandler {
    
    @ExceptionHandler(DuplicateIdempotencyKeyException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateKey(DuplicateIdempotencyKeyException e) {
        // 409 Conflict para claves de idempotencia duplicadas
        ErrorResponse error = new ErrorResponse("DUPLICATE_REQUEST", 
                                              "Request with this idempotency key already processed");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }
    
    @ExceptionHandler(InvalidIdempotencyKeyException.class)
    public ResponseEntity<ErrorResponse> handleInvalidKey(InvalidIdempotencyKeyException e) {
        // 400 Bad Request para claves inválidas
        ErrorResponse error = new ErrorResponse("INVALID_IDEMPOTENCY_KEY", 
                                              "The provided idempotency key is invalid");
        return ResponseEntity.badRequest().body(error);
    }
}
```

### 2. Gestión de Estado

#### TTL y Limpieza
```java
@Component
public class IdempotencyCacheManager {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    private static final Duration DEFAULT_TTL = Duration.ofHours(24);
    
    public <T> T executeWithIdempotency(String key, Supplier<T> operation, Duration customTtl) {
        Duration ttl = customTtl != null ? customTtl : DEFAULT_TTL;
        String cacheKey = buildCacheKey(key);
        
        // Verificar caché existente
        T cachedResult = getFromCache(cacheKey);
        if (cachedResult != null) {
            return cachedResult;
        }
        
        // Ejecutar operación
        T result = operation.get();
        
        // Guardar en caché con TTL
        saveToCache(cacheKey, result, ttl);
        
        return result;
    }
    
    @Scheduled(fixedDelay = 3600000) // Cada hora
    public void cleanupExpiredEntries() {
        // Limpiar entradas expiradas manualmente si es necesario
        // (Redis TTL maneja esto automáticamente)
    }
}
```

### 3. Monitoreo y Observabilidad

#### Métricas
```java
@Component
public class IdempotencyMetrics {
    
    private final Counter idempotentOperations;
    private final Counter duplicateOperations;
    private final Timer operationTimer;
    
    public IdempotencyMetrics(MeterRegistry meterRegistry) {
        this.idempotentOperations = Counter.builder("idempotent.operations.total")
                .description("Total number of idempotent operations")
                .register(meterRegistry);
        
        this.duplicateOperations = Counter.builder("duplicate.operations.total")
                .description("Total number of duplicate operations detected")
                .register(meterRegistry);
        
        this.operationTimer = Timer.builder("idempotent.operation.duration")
                .description("Duration of idempotent operations")
                .register(meterRegistry);
    }
    
    public void recordIdempotentOperation(String operationType, Duration duration) {
        idempotentOperations.increment(Tags.of("operation", operationType));
        operationTimer.record(duration, Tags.of("operation", operationType));
    }
    
    public void recordDuplicateOperation(String operationType) {
        duplicateOperations.increment(Tags.of("operation", operationType));
    }
}
```

#### Logging
```java
@Service
public class IdempotentOrderService {
    
    private static final Logger log = LoggerFactory.getLogger(IdempotentOrderService.class);
    
    public Order createOrder(CreateOrderRequest request, String idempotencyKey) {
        String operationId = UUID.randomUUID().toString();
        
        log.info("Starting order creation. OperationId: {}, IdempotencyKey: {}", 
                operationId, idempotencyKey);
        
        try {
            // Verificar si ya existe una operación con esta clave
            Optional<Order> existingOrder = orderRepository.findByIdempotencyKey(idempotencyKey);
            
            if (existingOrder.isPresent()) {
                log.info("Returning existing order for idempotency key: {}", idempotencyKey);
                return existingOrder.get();
            }
            
            // Crear nuevo orden
            Order newOrder = doCreateOrder(request);
            newOrder.setIdempotencyKey(idempotencyKey);
            orderRepository.save(newOrder);
            
            log.info("Order created successfully. OperationId: {}, OrderId: {}", 
                    operationId, newOrder.getId());
            
            return newOrder;
            
        } catch (Exception e) {
            log.error("Order creation failed. OperationId: {}, IdempotencyKey: {}", 
                    operationId, idempotencyKey, e);
            throw e;
        }
    }
}
```

---

## **Desafíos y Limitaciones**

### 1. Gestión de Estado
```java
// Problema: Estado inconsistente entre servicios
@Service
public class ProblematicService {
    
    public void processOperation(String operationId) {
        // Si el primer servicio procesa pero el segundo falla,
        // tenemos inconsistencia
        serviceA.process(operationId);
        serviceB.process(operationId); // Puede fallar
    }
}

// Solución: Saga Pattern
@Service
public class SagaOrderService {
    
    @Autowired
    private SagaOrchestrator sagaOrchestrator;
    
    public void createOrderWithSaga(CreateOrderRequest request) {
        Saga saga = new OrderCreationSaga(request);
        sagaOrchestrator.execute(saga);
    }
}
```

### 2. Concurrencia
```java
// Problema: Condiciones de carrera
@Service
public class ProblematicInventoryService {
    
    public void reserveItem(String itemId, int quantity) {
        // Dos threads pueden ejecutar esto simultáneamente
        // y ambos ver el mismo stock disponible
        Item item = itemRepository.findById(itemId);
        if (item.getAvailableQuantity() >= quantity) {
            item.setAvailableQuantity(item.getAvailableQuantity() - quantity);
            itemRepository.save(item);
        }
    }
}

// Solución: Locking optimista
@Service
public class OptimisticInventoryService {
    
    public void reserveItem(String itemId, int quantity) {
        Item item = itemRepository.findById(itemId);
        int currentVersion = item.getVersion();
        
        if (item.getAvailableQuantity() >= quantity) {
            item.setAvailableQuantity(item.getAvailableQuantity() - quantity);
            
            try {
                itemRepository.saveWithVersion(item, currentVersion);
            } catch (OptimisticLockException e) {
                // Reintentar con nueva versión
                reserveItem(itemId, quantity);
            }
        }
    }
}
```

### 3. Sincronización de Caché
```java
// Problema: Caché inconsistente
@Component
public class CacheProblematicService {
    
    @Cacheable("users")
    public User getUserById(String userId) {
        return userRepository.findById(userId);
    }
    
    // Si actualizamos el usuario, la caché puede quedar obsoleta
    public void updateUser(User user) {
        userRepository.save(user);
        cacheManager.evict("users", user.getId()); // Limpiar caché
    }
}
```

---

## **Herramientas y Librerías**

### 1. Spring Boot
```xml
<!-- Dependencies -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
```

```java
@Configuration
@EnableCaching
@EnableRedisRepositories
public class IdempotencyConfig {
    
    @Bean
    public CacheManager cacheManager() {
        RedisCacheManager.Builder builder = RedisCacheManager
                .RedisCacheManagerBuilder
                .fromConnectionFactory(redisConnectionFactory())
                .cacheDefaults(getCacheConfiguration(Duration.ofHours(1)));
        
        return builder.build();
    }
    
    private RedisCacheConfiguration getCacheConfiguration(Duration ttl) {
        return RedisCacheConfiguration
                .defaultCacheConfig()
                .entryTtl(ttl)
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()));
    }
}
```

### 2. Librerías Específicas
```java
// Apache Camel para manejo de idempotencia
@Component
public class CamelIdempotentRoute extends RouteBuilder {
    
    @Override
    public void configure() throws Exception {
        from("direct:order")
            .idempotent()
                .header("Idempotency-Key")
                .messageIdRepositoryRef("idempotentRepository")
            .to("bean:orderService?method=processOrder");
    }
    
    @Bean
    public IdempotencyRepository<String> idempotentRepository() {
        return new MemoryIdempotencyRepository();
    }
}
```

---

## **Ejemplos de Código Completo**

### 1. Servicio de Pagos Idempotente
```java
@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    
    @Autowired
    private PaymentService paymentService;
    
    @PostMapping
    public ResponseEntity<PaymentResult> processPayment(
            @RequestBody PaymentRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        
        if (idempotencyKey != null && !isValidKey(idempotencyKey)) {
            return ResponseEntity.badRequest().build();
        }
        
        PaymentResult result = paymentService.processPayment(request, idempotencyKey);
        return ResponseEntity.ok(result);
    }
    
    private boolean isValidKey(String key) {
        return key != null && key.length() >= 10 && key.length() <= 255;
    }
}

@Service
@Transactional
public class PaymentService {
    
    @Autowired
    private PaymentRepository paymentRepository;
    
    @Autowired
    private ExternalPaymentGateway paymentGateway;
    
    @Autowired
    private IdempotencyKeyManager idempotencyManager;
    
    public PaymentResult processPayment(PaymentRequest request, String idempotencyKey) {
        return idempotencyManager.executeWithIdempotency(
            buildIdempotencyKey(request, idempotencyKey),
            () -> doProcessPayment(request),
            Duration.ofHours(24)
        );
    }
    
    private PaymentResult doProcessPayment(PaymentRequest request) {
        // Verificar si ya existe un pago con estos datos
        Optional<Payment> existing = paymentRepository
                .findByTransactionId(request.getTransactionId());
        
        if (existing.isPresent()) {
            return PaymentResult.fromPayment(existing.get());
        }
        
        // Crear registro de pago pendiente
        Payment payment = new Payment();
        payment.setTransactionId(request.getTransactionId());
        payment.setAmount(request.getAmount());
        payment.setCurrency(request.getCurrency());
        payment.setStatus(PaymentStatus.PENDING);
        payment = paymentRepository.save(payment);
        
        try {
            // Procesar pago con gateway externo
            GatewayResult gatewayResult = paymentGateway.charge(request);
            
            // Actualizar estado del pago
            payment.setStatus(mapGatewayStatus(gatewayResult.getStatus()));
            payment.setGatewayTransactionId(gatewayResult.getTransactionId());
            payment.setProcessedAt(LocalDateTime.now());
            paymentRepository.save(payment);
            
            return PaymentResult.success(payment);
            
        } catch (PaymentException e) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setErrorMessage(e.getMessage());
            paymentRepository.save(payment);
            
            return PaymentResult.failure(payment, e.getMessage());
        }
    }
    
    private String buildIdempotencyKey(PaymentRequest request, String clientKey) {
        if (clientKey != null) {
            return "payment:" + clientKey;
        }
        // Generar clave basada en los datos del pago
        return "payment:" + request.getTransactionId() + ":" + request.getAmount();
    }
}

@Entity
public class Payment {
    
    @Id
    @GeneratedValue
    private Long id;
    
    private String transactionId;
    private BigDecimal amount;
    private String currency;
    
    @Enumerated(EnumType.STRING)
    private PaymentStatus status;
    
    private String gatewayTransactionId;
    private String errorMessage;
    
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
    
    // Constructors, getters, setters
}

public enum PaymentStatus {
    PENDING, COMPLETED, FAILED, CANCELLED
}
```

### 2. Consumer de Mensajes con Deduplicación
```java
@Component
public class OrderEventHandler {
    
    private static final Logger log = LoggerFactory.getLogger(OrderEventHandler.class);
    
    @Autowired
    private OrderService orderService;
    
    @Autowired
    private MessageDeduplicator deduplicator;
    
    @Autowired
    private OrderEventRepository eventRepository;
    
    @EventListener
    @Async("eventExecutor")
    public void handleOrderCreated(OrderCreatedEvent event) {
        String eventId = event.getEventId();
        
        // Verificar si ya procesamos este evento
        if (deduplicator.isProcessed(eventId)) {
            log.debug("Event {} already processed, skipping", eventId);
            return;
        }
        
        try {
            // Procesar el evento
            orderService.processOrderCreated(event.getOrderId());
            
            // Marcar como procesado
            deduplicator.markAsProcessed(eventId);
            eventRepository.save(new ProcessedEvent(eventId, LocalDateTime.now()));
            
            log.info("Successfully processed event {}", eventId);
            
        } catch (Exception e) {
            log.error("Failed to process event {}", eventId, e);
            
            // No marcar como procesado en caso de error
            // Permitir reintento
            throw e;
        }
    }
}

@Component
public class MessageDeduplicator {
    
    private final Set<String> processedMessages = ConcurrentHashMap.newKeySet();
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    private static final String PROCESSED_PREFIX = "processed:";
    
    public boolean isProcessed(String messageId) {
        // Verificación rápida en memoria
        if (processedMessages.contains(messageId)) {
            return true;
        }
        
        // Verificación persistente en Redis
        String redisKey = PROCESSED_PREFIX + messageId;
        Boolean exists = redisTemplate.hasKey(redisKey);
        
        if (Boolean.TRUE.equals(exists)) {
            processedMessages.add(messageId);
            return true;
        }
        
        return false;
    }
    
    public void markAsProcessed(String messageId) {
        // Agregar a set en memoria
        processedMessages.add(messageId);
        
        // Guardar en Redis con TTL de 24 horas
        String redisKey = PROCESSED_PREFIX + messageId;
        redisTemplate.opsForValue().set(redisKey, "processed", Duration.ofHours(24));
    }
    
    public void markAsFailed(String messageId) {
        // Remover de procesados para permitir reintento
        processedMessages.remove(messageId);
        
        String redisKey = PROCESSED_PREFIX + messageId;
        redisTemplate.delete(redisKey);
    }
}
```

---

## **Pros y Contras**

### Ventajas

1. **Robustez**: Los sistemas pueden manejar fallos de red y reintentos sin efectos secundarios
2. **Simplicidad del Cliente**: Los clientes no necesitan lógica compleja para manejar reintentos
3. **Escalabilidad**: Facilita el escalado horizontal sin problemas de coordinación
4. **Tolerancia a Fallos**: Mejora la resiliencia del sistema ante fallos temporales
5. **Debugging**: Facilita el análisis de logs y debugging de problemas

### Desventajas

1. **Complejidad Adicional**: Requiere implementación de lógica de idempotencia
2. **Overhead de Almacenamiento**: Necesidad de almacenar claves de idempotencia y resultados
3. **Latencia**: Puede agregar latencia debido a verificaciones de estado
4. **Gestión de Estado**: Complejidad en la gestión y limpieza de estado idempotente
5. **Casos Edge**: Algunos casos pueden ser difíciles de hacer idempotentes

---

## **Patrones Relacionados**

### 1. Circuit Breaker
```java
@Component
public class IdempotentCircuitBreakerService {
    
    @Autowired
    private CircuitBreaker circuitBreaker;
    
    public <T> T executeWithCircuitBreaker(String operationId, Supplier<T> operation) {
        return circuitBreaker.executeSupplier(() -> {
            // Verificar idempotencia dentro del circuit breaker
            return executeWithIdempotency(operationId, operation);
        });
    }
}
```

### 2. Saga Pattern
```java
public class OrderCreationSaga {
    
    private List<Step> steps = Arrays.asList(
        new ReserveInventoryStep(),
        new ProcessPaymentStep(),
        new CreateOrderStep(),
        new SendConfirmationStep()
    );
    
    public void execute(CreateOrderRequest request) {
        try {
            for (Step step : steps) {
                step.execute(request);
            }
        } catch (Exception e) {
            // Ejecutar compensaciones en orden inverso
            compensate(request);
        }
    }
}
```

### 3. Event Sourcing
```java
@Entity
public class Order {
    
    @Id
    private String orderId;
    
    @ElementCollection
    private List<OrderEvent> events = new ArrayList<>();
    
    public void handle(OrderCreatedEvent event) {
        events.add(event);
        apply(event);
    }
    
    public void handle(OrderUpdatedEvent event) {
        events.add(event);
        apply(event);
    }
    
    private void apply(OrderCreatedEvent event) {
        this.orderId = event.getOrderId();
        this.status = OrderStatus.CREATED;
        // Aplicar cambios del evento
    }
}
```

---

## **Conclusión**

La idempotencia es fundamental para construir sistemas distribuidos robustos y escalables. Aunque requiere una inversión inicial en diseño e implementación, proporciona beneficios significativos en términos de:

- **Confiabilidad**: Manejo elegante de fallos y reintentos
- **Escalabilidad**: Facilita el escalado horizontal
- **Mantenibilidad**: Simplifica la lógica del cliente y el debugging
- **User Experience**: Operaciones más confiables desde la perspectiva del usuario

La implementación exitosa de idempotencia requiere:
1. **Identificación de operaciones** que necesitan ser idempotentes
2. **Elección de estrategias** apropiadas (claves de idempotencia, upserts, etc.)
3. **Gestión cuidadosa del estado** y la concurrencia
4. **Monitoreo y observabilidad** para detectar problemas
5. **Testing exhaustivo** para verificar la idempotencia

La idempotencia no es solo una característica técnica, sino un principio de diseño que debe guiar toda la arquitectura de microservicios para crear sistemas verdaderamente resilientes.
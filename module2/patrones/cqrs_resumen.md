# CQRS (Command Query Responsibility Segregation)

## **¿Qué es CQRS?**

CQRS es un patrón de arquitectura que **separa las operaciones de lectura (Queries) de las operaciones de escritura (Commands)** en una aplicación. Esta separación permite optimizar cada tipo de operación según sus necesidades específicas.

### Conceptos Fundamentales

#### 1. **Commands (Comandos)**
- Representan **acciones** que cambian el estado del sistema
- Contienen datos necesarios para ejecutar una acción
- Nombrados en **imperativo** (CreateUser, UpdateOrder, DeleteProduct)
- Pueden fallar por validaciones o errores de negocio
- **NO devuelven datos** (solo estado de éxito/fracaso)

#### 2. **Queries (Consultas)**
- Representan **preguntas** que solo leen datos
- **NO modifican** el estado del sistema
- Nombrados como **sustantivos** (GetUserById, GetOrdersByCustomer, FindProducts)
- Siempre devuelven datos
- **NO tienen lógica de negocio** compleja

#### 3. **Separación de Modelos**

```
┌─────────────────────┐    ┌─────────────────────┐
│   WRITE MODEL       │    │    READ MODEL       │
│                     │    │                     │
│ • Domain Logic      │    │ • Denormalized Data │
│ • Business Rules    │    │ • Optimized Queries │
│ • Validation        │    │ • Fast Retrieval    │
│ • Complex Objects   │    │ • Simple Objects    │
│                     │    │                     │
└─────────────────────┘    └─────────────────────┘
```

#### 4. **Event Sourcing (Opcional)**
- Almacena cambios como secuencia de eventos
- Permite reconstrucción del estado
- Historial completo de cambios
- Base para auditoría y debugging

![CQRS](./imgs/cqrs.jpg)

---

## **Casos de Uso**

### 1. **Sistemas de Alta Performance**
- Aplicaciones con muchas más lecturas que escrituras
- Sistemas que requieren respuestas rápidas
- Apps con carga de lectura pesada (dashboards, reportes)

### 2. **Aplicaciones Escalables**
- Sistemas que necesitan escalar lectura y escritura independientemente
- Microservicios con diferentes patrones de acceso
- Aplicaciones distribuidas

### 3. **Sistemas con Lógica Compleja**
- Aplicaciones con reglas de negocio complejas
- Sistemas que requieren múltiples representaciones de datos
- Apps con diferentes vistas de los mismos datos

### 4. **Sistemas de E-commerce**
- Catálogo de productos (muchas lecturas, pocas escrituras)
- Carrito de compras (escrituras moderadas, muchas lecturas)
- Reportes de ventas (lecturas intensivas)

### 5. **Sistemas Financieros**
- Cuentas bancarias (escrituras críticas, lecturas frecuentes)
- Trading systems (múltiples vistas de mercado)
- Reporting financiero (consultas complejas)

---

## **Ejemplos**

### Ejemplo 1: Sistema de Gestión de Usuarios con CQRS

#### Command - Crear Usuario
```java
// Command para crear usuario
public class CreateUserCommand {
    private final String email;
    private final String password;
    private final String name;
    private final UUID commandId;
    
    public CreateUserCommand(String email, String password, String name) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email es requerido");
        }
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Contraseña debe tener al menos 8 caracteres");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Nombre es requerido");
        }
        
        this.email = email;
        this.password = password;
        this.name = name;
        this.commandId = UUID.randomUUID();
    }
    
    // Getters
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getName() { return name; }
    public UUID getCommandId() { return commandId; }
}

// Command para actualizar usuario
public class UpdateUserCommand {
    private final UUID userId;
    private final String name;
    private final String email;
    private final UUID commandId;
    
    public UpdateUserCommand(UUID userId, String name, String email) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.commandId = UUID.randomUUID();
    }
    
    // Getters
    public UUID getUserId() { return userId; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public UUID getCommandId() { return commandId; }
}
```

#### Query - Consultar Usuarios
```java
// Query para obtener usuario por ID
public class GetUserByIdQuery {
    private final UUID userId;
    
    public GetUserByIdQuery(UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID es requerido");
        }
        this.userId = userId;
    }
    
    public UUID getUserId() { return userId; }
}

// Query para buscar usuarios
public class SearchUsersQuery {
    private final String searchTerm;
    private final int page;
    private final int size;
    
    public SearchUsersQuery(String searchTerm, int page, int size) {
        this.searchTerm = searchTerm;
        this.page = Math.max(0, page);
        this.size = Math.max(1, Math.min(size, 100)); // Max 100 por página
    }
    
    public String getSearchTerm() { return searchTerm; }
    public int getPage() { return page; }
    public int getSize() { return size; }
}
```

#### DTOs para Transferencia de Datos
```java
// DTO para vista de lectura (User)
public class UserView {
    private final UUID id;
    private final String email;
    private final String name;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    
    public UserView(UUID id, String email, String name, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
    
    // Getters
    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getName() { return name; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}

// DTO para resultados paginados
public class PaginatedResult<T> {
    private final List<T> data;
    private final int page;
    private final int size;
    private final long total;
    private final int totalPages;
    
    public PaginatedResult(List<T> data, int page, int size, long total) {
        this.data = data;
        this.page = page;
        this.size = size;
        this.total = total;
        this.totalPages = (int) Math.ceil((double) total / size);
    }
    
    // Getters
    public List<T> getData() { return data; }
    public int getPage() { return page; }
    public int getSize() { return size; }
    public long getTotal() { return total; }
    public int getTotalPages() { return totalPages; }
}
```

#### Write Model - Modelo de Escritura
```java
// Entidad del dominio (Write Model)
public class User {
    private UUID id;
    private String email;
    private String password;
    private String name;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Constructor privado para el aggregate root
    private User(String email, String password, String name) {
        this.id = UUID.randomUUID();
        this.email = email;
        this.password = password;
        this.name = name;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    // Factory method para crear usuario
    public static User create(String email, String password, String name) {
        validateEmail(email);
        validatePassword(password);
        return new User(email, password, name);
    }
    
    // Métodos de comportamiento
    public void updateInfo(String name, String email) {
        if (name != null && !name.trim().isEmpty()) {
            this.name = name;
        }
        if (email != null && !email.trim().isEmpty()) {
            validateEmail(email);
            this.email = email;
        }
        this.updatedAt = LocalDateTime.now();
    }
    
    public void changePassword(String newPassword) {
        validatePassword(newPassword);
        this.password = newPassword;
        this.updatedAt = LocalDateTime.now();
    }
    
    // Métodos de validación privados
    private static void validateEmail(String email) {
        if (!email.contains("@")) {
            throw new IllegalArgumentException("Email debe contener @");
        }
    }
    
    private static void validatePassword(String password) {
        if (password.length() < 8) {
            throw new IllegalArgumentException("Contraseña debe tener al menos 8 caracteres");
        }
    }
    
    // Getters para eventos y persistencia
    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getName() { return name; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
```

#### Command Handler
```java
@Component
public class UserCommandHandler {
    
    private final UserRepository userRepository;
    private final EventPublisher eventPublisher;
    
    public UserCommandHandler(UserRepository userRepository, EventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
    }
    
    @EventHandler
    public void handle(CreateUserCommand command) {
        try {
            // Crear usuario usando el aggregate root
            User user = User.create(command.getEmail(), command.getPassword(), command.getName());
            
            // Guardar en el repositorio de escritura
            userRepository.save(user);
            
            // Publicar evento
            eventPublisher.publish(new UserCreatedEvent(
                user.getId(), 
                user.getEmail(), 
                user.getName(),
                user.getCreatedAt()
            ));
            
        } catch (IllegalArgumentException e) {
            throw new CommandExecutionException("Error creando usuario: " + e.getMessage(), e);
        }
    }
    
    @EventHandler
    public void handle(UpdateUserCommand command) {
        try {
            User user = userRepository.findById(command.getUserId())
                .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado"));
            
            user.updateInfo(command.getName(), command.getEmail());
            userRepository.save(user);
            
            eventPublisher.publish(new UserUpdatedEvent(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getUpdatedAt()
            ));
            
        } catch (UserNotFoundException e) {
            throw new CommandExecutionException("Usuario no encontrado: " + command.getUserId(), e);
        }
    }
}
```

#### Query Handler
```java
@Component
public class UserQueryHandler {
    
    private final UserReadRepository userReadRepository;
    
    public UserQueryHandler(UserReadRepository userReadRepository) {
        this.userReadRepository = userReadRepository;
    }
    
    @QueryHandler
    public UserView handle(GetUserByIdQuery query) {
        return userReadRepository.findById(query.getUserId())
            .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado: " + query.getUserId()));
    }
    
    @QueryHandler
    public PaginatedResult<UserView> handle(SearchUsersQuery query) {
        List<UserView> users = userReadRepository.search(
            query.getSearchTerm(),
            query.getPage(),
            query.getSize()
        );
        
        long total = userReadRepository.count(query.getSearchTerm());
        
        return new PaginatedResult<>(users, query.getPage(), query.getSize(), total);
    }
}
```

#### Event Handlers para Actualizar Read Model
```java
@Component
public class UserEventHandler {
    
    private final UserReadRepository userReadRepository;
    
    public UserEventHandler(UserReadRepository userReadRepository) {
        this.userReadRepository = userReadRepository;
    }
    
    @EventHandler
    public void handle(UserCreatedEvent event) {
        UserView userView = new UserView(
            event.getUserId(),
            event.getEmail(),
            event.getName(),
            event.getCreatedAt(),
            event.getCreatedAt()
        );
        
        userReadRepository.save(userView);
    }
    
    @EventHandler
    public void handle(UserUpdatedEvent event) {
        UserView existingUser = userReadRepository.findById(event.getUserId())
            .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado para actualización"));
        
        UserView updatedUser = new UserView(
            event.getUserId(),
            event.getEmail(),
            event.getName(),
            existingUser.getCreatedAt(),
            event.getUpdatedAt()
        );
        
        userReadRepository.save(updatedUser);
    }
}
```

#### Repository Interfaces
```java
// Write Repository
public interface UserRepository {
    Optional<User> findById(UUID id);
    void save(User user);
    boolean existsByEmail(String email);
}

// Read Repository
public interface UserReadRepository {
    Optional<UserView> findById(UUID id);
    List<UserView> search(String searchTerm, int page, int size);
    long count(String searchTerm);
}

// Implementación JPA para Read Model
@Repository
public class JpaUserReadRepository implements UserReadRepository {
    
    @Autowired
    private UserViewEntityRepository entityRepository;
    
    @Override
    public Optional<UserView> findById(UUID id) {
        return entityRepository.findById(id)
            .map(this::mapToView);
    }
    
    @Override
    public List<UserView> search(String searchTerm, int page, int size) {
        int offset = page * size;
        
        List<UserViewEntity> entities;
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            entities = entityRepository.findAll(PageRequest.of(page, size)).getContent();
        } else {
            entities = entityRepository.searchByTerm(searchTerm, PageRequest.of(page, size));
        }
        
        return entities.stream()
            .map(this::mapToView)
            .collect(Collectors.toList());
    }
    
    @Override
    public long count(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return entityRepository.count();
        } else {
            return entityRepository.countBySearchTerm(searchTerm);
        }
    }
    
    @Override
    public void save(UserView userView) {
        UserViewEntity entity = mapToEntity(userView);
        entityRepository.save(entity);
    }
    
    private UserView mapToView(UserViewEntity entity) {
        return new UserView(
            entity.getId(),
            entity.getEmail(),
            entity.getName(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
    
    private UserViewEntity mapToEntity(UserView userView) {
        UserViewEntity entity = new UserViewEntity();
        entity.setId(userView.getId());
        entity.setEmail(userView.getEmail());
        entity.setName(userView.getName());
        entity.setCreatedAt(userView.getCreatedAt());
        entity.setUpdatedAt(userView.getUpdatedAt());
        return entity;
    }
}
```

#### Controller
```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    private final CommandGateway commandGateway;
    private final QueryGateway queryGateway;
    
    public UserController(CommandGateway commandGateway, QueryGateway queryGateway) {
        this.commandGateway = commandGateway;
        this.queryGateway = queryGateway;
    }
    
    @PostMapping
    public CompletableFuture<ResponseEntity<UUID>> createUser(@RequestBody CreateUserRequest request) {
        CreateUserCommand command = new CreateUserCommand(
            request.getEmail(),
            request.getPassword(),
            request.getName()
        );
        
        return commandGateway.send(command)
            .thenApply(userId -> ResponseEntity.ok(userId));
    }
    
    @PutMapping("/{id}")
    public CompletableFuture<ResponseEntity<Void>> updateUser(
            @PathVariable UUID id, 
            @RequestBody UpdateUserRequest request) {
        
        UpdateUserCommand command = new UpdateUserCommand(
            id,
            request.getName(),
            request.getEmail()
        );
        
        return commandGateway.send(command)
            .thenApply(result -> ResponseEntity.ok().build());
    }
    
    @GetMapping("/{id}")
    public CompletableFuture<ResponseEntity<UserView>> getUser(@PathVariable UUID id) {
        GetUserByIdQuery query = new GetUserByIdQuery(id);
        
        return queryGateway.send(query)
            .thenApply(user -> ResponseEntity.ok(user));
    }
    
    @GetMapping("/search")
    public CompletableFuture<ResponseEntity<PaginatedResult<UserView>>> searchUsers(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        SearchUsersQuery query = new SearchUsersQuery(q, page, size);
        
        return queryGateway.send(query)
            .thenApply(result -> ResponseEntity.ok(result));
    }
}
```

### Ejemplo 2: Sistema de E-commerce Simplificado

```java
// Command para crear orden
public class CreateOrderCommand {
    private final UUID customerId;
    private final List<OrderItem> items;
    private final UUID commandId;
    
    public CreateOrderCommand(UUID customerId, List<OrderItem> items) {
        this.customerId = customerId;
        this.items = items;
        this.commandId = UUID.randomUUID();
    }
}

// Query para obtener órdenes del cliente
public class GetOrdersByCustomerQuery {
    private final UUID customerId;
    
    public GetOrdersByCustomerQuery(UUID customerId) {
        this.customerId = customerId;
    }
}

// Read Model - Vista optimizada para consultas
public class OrderSummaryView {
    private final UUID orderId;
    private final UUID customerId;
    private final String customerName;
    private final List<OrderItemView> items;
    private final Money total;
    private final OrderStatus status;
    private final LocalDateTime createdAt;
    
    // Constructor y getters
}

// Query Handler optimizado
@Component
public class OrderQueryHandler {
    
    private final OrderReadRepository orderReadRepository;
    
    @QueryHandler
    public List<OrderSummaryView> handle(GetOrdersByCustomerQuery query) {
        // Consulta optimizada - solo datos necesarios para la vista
        return orderReadRepository.findSummariesByCustomerId(query.getCustomerId());
    }
}
```

---

## **Ventajas**

### 1. **Optimización de Performance**
- Read models especializados para consultas rápidas
- Write models optimizados para operaciones complejas
- Posibilidad de usar diferentes bases de datos para lectura y escritura

### 2. **Escalabilidad Independiente**
- Escalar lectura y escritura por separado
- Optimizar cada modelo según sus necesidades
- Distribución de carga especializada

### 3. **Flexibilidad en Modelos**
- Read models pueden ser desnormalizados para consultas específicas
- Write models mantienen consistencia del dominio
- Diferentes representaciones para diferentes casos de uso

### 4. **Mantenibilidad**
- Separación clara de responsabilidades
- Lógica de lectura más simple
- Reglas de negocio aisladas en el write model

### 5. **Testabilidad**
- Testing independiente de commands y queries
- Mocks más simples de configurar
- Tests más rápidos para read models

---

## **Desventajas**

### 1. **Complejidad de Implementación**
- Necesidad de mantener múltiples modelos
- Sincronización entre write y read models
- Eventual consistency (consistencia eventual)

### 2. **Eventual Consistency**
- Los datos pueden no estar inmediatamente sincronizados
- Necesidad de manejar casos donde los datos no están actualizados
- Complejidad en UI para mostrar estados de sincronización

### 3. **Duplicación de Datos**
- Datos replicados en múltiples read models
- Espacio adicional de almacenamiento
- Proceso de sincronización necesario

### 4. **Curva de Aprendizaje**
- Concepto que requiere comprensión profunda
- Patrones adicionales (Event Sourcing, CQRS)
- Debugging más complejo con múltiples modelos

### 5. **Overhead Operacional**
- Más componentes para mantener
- Eventos y handlers adicionales
- Monitoreo más complejo

---

## **Buenas Prácticas**

### 1. **Separación Clara de Commands y Queries**
```java
// ✅ Command - Solo datos para acción
public class CreateProductCommand {
    private final String name;
    private final Money price;
    private final String category;
    // No métodos de negocio aquí
}

// ✅ Query - Solo parámetros de búsqueda
public class GetProductsByCategoryQuery {
    private final String category;
    private final int page;
    private final int size;
}
```

### 2. **Validación en Command Handlers**
```java
@Component
public class UserCommandHandler {
    
    @EventHandler
    public void handle(CreateUserCommand command) {
        // Validaciones de negocio antes de procesar
        if (userRepository.existsByEmail(command.getEmail())) {
            throw new EmailAlreadyExistsException("Email ya está en uso");
        }
        
        User user = User.create(command.getEmail(), command.getPassword(), command.getName());
        userRepository.save(user);
    }
}
```

### 3. **Read Models Optimizados**
```java
// ✅ Read model desnormalizado para consultas
public class UserSummaryView {
    private final UUID id;
    private final String fullName; // Combinado para búsquedas
    private final String email;
    private final int orderCount; // Precalculado
    private final Money totalSpent; // Precalculado
    
    // Constructor optimizado para lectura
}
```

### 4. **Eventos Bien Definidos**
```java
// ✅ Evento con toda la información necesaria
public class UserRegisteredEvent {
    private final UUID userId;
    private final String email;
    private final String name;
    private final LocalDateTime registeredAt;
    
    // Constructor
}
```

### 5. **Idempotencia en Event Handlers**
```java
@Component
public class UserEventHandler {
    
    @EventHandler
    public void handle(UserRegisteredEvent event) {
        // Verificar si ya se procesó el evento
        if (eventStore.hasProcessed(event.getEventId())) {
            return; // Evitar procesamiento duplicado
        }
        
        // Procesar evento
        processEvent(event);
        
        // Marcar como procesado
        eventStore.markAsProcessed(event.getEventId());
    }
}
```

### 6. **Manejo de Errores Consistente**
```java
// Excepciones específicas para CQRS
public class CommandExecutionException extends RuntimeException {
    public CommandExecutionException(String message) {
        super(message);
    }
}

public class QueryExecutionException extends RuntimeException {
    public QueryExecutionException(String message) {
        super(message);
    }
}
```

### 7. **Testing Independiente**
```java
// Test de Command Handler
@Test
public void shouldCreateUserSuccessfully() {
    // Given
    CreateUserCommand command = new CreateUserCommand("test@email.com", "password123", "John Doe");
    
    // When
    commandHandler.handle(command);
    
    // Then
    verify(userRepository).save(any(User.class));
    verify(eventPublisher).publish(any(UserRegisteredEvent.class));
}

// Test de Query Handler
@Test
public void shouldReturnUserById() {
    // Given
    UUID userId = UUID.randomUUID();
    GetUserByIdQuery query = new GetUserByIdQuery(userId);
    
    // When
    UserView result = queryHandler.handle(query);
    
    // Then
    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(userId);
}
```

### 8. **Monitorización y Observabilidad**
```java
// Métricas para Commands y Queries
@Component
public class CQRSMetrics {
    
    private final MeterRegistry meterRegistry;
    private final Timer commandTimer;
    private final Timer queryTimer;
    private final Counter errorCounter;
    
    public CQRSMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.commandTimer = Timer.builder("cqrs.commands.duration").register(meterRegistry);
        this.queryTimer = Timer.builder("cqrs.queries.duration").register(meterRegistry);
        this.errorCounter = Counter.builder("cqrs.errors").register(meterRegistry);
    }
    
    public <T> T measureCommand(Supplier<T> commandExecution) {
        return commandTimer.record(commandExecution);
    }
    
    public <T> T measureQuery(Supplier<T> queryExecution) {
        return queryTimer.record(queryExecution);
    }
}
```

---

## **Implementación con Spring Boot**

### Configuración CQRS con Spring
```java
@Configuration
@EnableCqrs
public class CQRSConfiguration {
    
    @Bean
    public CommandGateway commandGateway() {
        return new SimpleCommandGateway();
    }
    
    @Bean
    public QueryGateway queryGateway() {
        return new SimpleQueryGateway();
    }
    
    @Bean
    public EventPublisher eventPublisher() {
        return new EventPublisherImpl();
    }
}
```

### Async Processing para Eventos
```java
@Configuration
@EnableAsync
public class AsyncConfiguration {
    
    @Bean(name = "eventProcessor")
    public Executor eventProcessorExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("EventProcessor-");
        executor.initialize();
        return executor;
    }
}
```

---

## **Conclusión**

CQRS es especialmente efectivo para:

### **Aplicaciones Idóneas:**
- **Sistemas con alta carga de lectura** (>80% consultas)
- **Aplicaciones que requieren diferentes vistas** de los mismos datos
- **Sistemas escalables** que necesitan optimización independiente
- **Aplicaciones con lógica de negocio compleja** en escrituras

### **Aplicaciones NO Recomendadas:**
- **Sistemas simples** con CRUD básico
- **Aplicaciones con igual proporción** de lectura y escritura
- **Proyectos pequeños** donde la complejidad no se justifica
- **Sistemas en tiempo real** que requieren consistencia inmediata

### **Complementariedad con Otros Patrones:**
- **Arquitectura Hexagonal**: CQRS se integra perfectamente con puertos y adaptadores
- **Event Sourcing**: CQRS puede beneficiarse del almacenamiento de eventos
- **Microservicios**: CQRS facilita la separación de responsabilidades entre servicios

La implementación exitosa de CQRS requiere:
1. **Comprensión clara** de los casos de uso
2. **Disciplina** en la separación de responsabilidades
3. **Herramientas adecuadas** para manejo de eventos
4. **Monitoreo** de la consistencia eventual
5. **Testing exhaustivo** de ambos modelos


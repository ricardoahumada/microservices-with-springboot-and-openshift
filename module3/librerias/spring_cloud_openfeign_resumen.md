# Spring Cloud OpenFeign

## **¿Qué es Spring Cloud OpenFeign?**

Spring Cloud OpenFeign es un framework declarativo de cliente HTTP para aplicaciones Spring Boot que simplifica la comunicación entre microservicios. Proporciona una forma elegante y type-safe de realizar llamadas HTTP a servicios externos mediante interfaces Java anotadas.

**Características principales**:
- **Declarativo**: Define interfaces con anotaciones en lugar de código imperativo
- **Type-safe**: Utiliza interfaces Java tipadas para llamadas HTTP
- **Integración nativa**: Se integra perfectamente con Spring Boot y Spring Cloud
- **Load balancing**: Soporte integrado para client-side load balancing
- **Circuit breaker**: Integración con Resilience4j para tolerancia a fallos

## **Conceptos Fundamentales**

### Client HTTP Declarativo
OpenFeign permite definir clientes HTTP como interfaces Java con anotaciones, eliminando la necesidad de código boilerplate.

```java
// Cliente HTTP declarativo
@FeignClient(name = "user-service")
public interface UserServiceClient {
    
    @GetMapping("/api/users/{id}")
    User getUserById(@PathVariable("id") Long id);
    
    @PostMapping("/api/users")
    User createUser(@RequestBody CreateUserRequest request);
    
    @PutMapping("/api/users/{id}")
    User updateUser(@PathVariable("id") Long id, @RequestBody UpdateUserRequest request);
    
    @DeleteMapping("/api/users/{id}")
    void deleteUser(@PathVariable("id") Long id);
}
```

### Anotaciones Principales

#### @FeignClient
```java
// Configuración básica
@FeignClient(name = "user-service")
public interface UserClient { }

// Configuración avanzada
@FeignClient(
    name = "user-service",
    url = "https://api.example.com",
    configuration = FeignConfig.class,
    fallback = UserServiceFallback.class,
    fallbackFactory = UserServiceFallbackFactory.class
)
public interface AdvancedUserClient { }
```

#### Anotaciones de Mapeo HTTP
```java
@FeignClient(name = "order-service")
public interface OrderServiceClient {
    
    // GET requests
    @GetMapping("/api/orders")
    List<Order> getOrders();
    
    @GetMapping("/api/orders/{id}")
    Order getOrderById(@PathVariable Long id);
    
    // POST requests
    @PostMapping("/api/orders")
    Order createOrder(@RequestBody CreateOrderRequest request);
    
    // PUT requests
    @PutMapping("/api/orders/{id}/status")
    Order updateOrderStatus(@PathVariable Long id, @RequestBody UpdateStatusRequest request);
    
    // DELETE requests
    @DeleteMapping("/api/orders/{id}")
    void deleteOrder(@PathVariable Long id);
    
    // PATCH requests
    @PatchMapping("/api/orders/{id}")
    Order partialUpdate(@PathVariable Long id, @RequestBody PartialUpdateRequest request);
}
```

#### Parámetros y Headers
```java
@FeignClient(name = "product-service")
public interface ProductServiceClient {
    
    // Query parameters
    @GetMapping("/api/products")
    List<Product> getProducts(
        @RequestParam("category") String category,
        @RequestParam("minPrice") Double minPrice,
        @RequestParam("maxPrice") Double maxPrice
    );
    
    // Multiple query parameters
    @GetMapping("/api/products/search")
    List<Product> searchProducts(@RequestParam Map<String, String> searchParams);
    
    // Custom headers
    @GetMapping("/api/products/featured")
    List<Product> getFeaturedProducts(
        @RequestHeader("Authorization") String authorization,
        @RequestHeader("User-Agent") String userAgent
    );
    
    // Header templates
    @GetMapping("/api/products/trending")
    List<Product> getTrendingProducts(
        @RequestHeader("X-API-Key") String apiKey
    );
}
```

## **Configuración y Setup**

### Dependencias Maven
```xml
<!-- Spring Cloud OpenFeign -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
</dependency>

<!-- Para Spring Cloud 2020.0+ -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-feign</artifactId>
</dependency>

<!-- Dependencias adicionales útiles -->
<dependency>
    <groupId>io.github.openfeign</groupId>
    <artifactId>feign-gson</artifactId>
</dependency>

<dependency>
    <groupId>io.github.openfeign</groupId>
    <artifactId>feign-okhttp</artifactId>
</dependency>

<dependency>
    <groupId>io.github.openfeign</groupId>
    <artifactId>feign-slf4j</artifactId>
</dependency>
```

### Configuración en Application.yml
```yaml
# Configuración básica de Feign
feign:
  client:
    config:
      default:
        connect-timeout: 5000
        read-timeout: 10000
        logger-level: basic
        error-decoder: com.example.config.FeignErrorDecoder

# Configuración específica por cliente
feign:
  client:
    config:
      user-service:
        connect-timeout: 3000
        read-timeout: 5000
        logger-level: full
        retryer: feign.Retryer.Default

# Configuración de circuit breaker
resilience4j:
  feign:
    instances:
      user-service:
        minimum-number-of-calls: 5
        sliding-window-size: 10
        failure-rate-threshold: 50
```

### Habilitación en Spring Boot
```java
@SpringBootApplication
@EnableFeignClients
public class Application {
    
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}

// Configuración personalizada
@Configuration
@EnableFeignClients(basePackages = "com.example.clients")
public class FeignConfig {
    
    @Bean
    public Contract feignContract() {
        // Usar anotaciones de Spring MVC en lugar de anotaciones Feign por defecto
        return new SpringMvcContract();
    }
    
    @Bean
    public Encoder feignEncoder() {
        return new GsonEncoder();
    }
    
    @Bean
    public Decoder feignDecoder() {
        return new GsonDecoder();
    }
}
```

## **Casos de Uso Principales**

### 1. Comunicación Entre Microservicios
```java
// Servicio de órdenes que consume servicio de usuarios
@Service
public class OrderService {
    
    @Autowired
    private UserServiceClient userServiceClient;
    
    @Autowired
    private ProductServiceClient productServiceClient;
    
    public Order createOrder(CreateOrderRequest request) {
        // Verificar usuario
        User user = userServiceClient.getUserById(request.getUserId());
        if (user == null) {
            throw new UserNotFoundException("User not found: " + request.getUserId());
        }
        
        // Verificar productos
        List<Product> products = productServiceClient.getProductsByIds(request.getProductIds());
        
        // Crear orden
        Order order = buildOrder(request, user, products);
        return orderRepository.save(order);
    }
}
```

### 2. Integración con APIs Externas
```java
// Cliente para API de pagos externa
@FeignClient(
    name = "payment-gateway",
    url = "https://api.stripe.com/v1",
    configuration = StripeFeignConfig.class
)
public interface StripePaymentClient {
    
    @PostMapping("/charges")
    ChargeResponse createCharge(@RequestBody ChargeRequest request);
    
    @GetMapping("/charges/{id}")
    ChargeResponse getCharge(@PathVariable("id") String chargeId);
    
    @PostMapping("/refunds")
    RefundResponse createRefund(@RequestBody RefundRequest request);
}

// Cliente para API de email
@FeignClient(
    name = "email-service",
    url = "https://api.sendgrid.com/v3",
    configuration = SendGridFeignConfig.class
)
public interface EmailServiceClient {
    
    @PostMapping("/mail/send")
    EmailResponse sendEmail(@RequestBody SendEmailRequest request);
    
    @GetMapping("/messages")
    List<EmailMessage> getMessages(@RequestParam("limit") int limit);
}
```

### 3. Load Balancing Cliente
```java
// Cliente con load balancing automático
@FeignClient(name = "inventory-service")
public interface InventoryServiceClient {
    
    @GetMapping("/api/inventory/{productId}")
    InventoryStatus checkInventory(@PathVariable("productId") String productId);
    
    @PostMapping("/api/inventory/reserve")
    ReservationResponse reserveInventory(@RequestBody ReservationRequest request);
}

// Configuración de Ribbon para load balancing
@FeignClient(
    name = "user-service",
    configuration = RibbonConfig.class
)
public interface UserServiceClient {
    // ...
}

@Configuration
public class RibbonConfig {
    
    @Bean
    public IRule ribbonRule() {
        return new WeightedResponseTimeRule();
    }
    
    @Bean
    public IPing ribbonPing() {
        return new PingUrl();
    }
}
```

## **Ejemplos de Código Completo**

### 1. Cliente CRUD Completo
```java
// Interfaz del cliente
@FeignClient(
    name = "user-management-service",
    url = "https://api.userservice.com/v1",
    configuration = UserFeignConfig.class
)
public interface UserManagementClient {
    
    @GetMapping("/users")
    List<UserResponse> getUsers(
        @RequestParam(value = "page", defaultValue = "0") int page,
        @RequestParam(value = "size", defaultValue = "20") int size,
        @RequestParam(value = "sort", defaultValue = "id,desc") String sort
    );
    
    @GetMapping("/users/{id}")
    UserResponse getUserById(@PathVariable("id") Long id);
    
    @GetMapping("/users/email/{email}")
    UserResponse getUserByEmail(@PathVariable("email") String email);
    
    @PostMapping("/users")
    UserResponse createUser(@RequestBody CreateUserRequest request);
    
    @PutMapping("/users/{id}")
    UserResponse updateUser(
        @PathVariable("id") Long id, 
        @RequestBody UpdateUserRequest request
    );
    
    @PatchMapping("/users/{id}/status")
    UserResponse updateUserStatus(
        @PathVariable("id") Long id,
        @RequestBody UpdateStatusRequest request
    );
    
    @DeleteMapping("/users/{id}")
    void deleteUser(@PathVariable("id") Long id);
    
    @GetMapping("/users/{id}/orders")
    List<OrderResponse> getUserOrders(@PathVariable("id") Long id);
}

// Configuración del cliente
@Configuration
public class UserFeignConfig {
    
    @Bean
    public Decoder userDecoder() {
        return new GsonDecoder();
    }
    
    @Bean
    public Encoder userEncoder() {
        return new GsonEncoder();
    }
    
    @Bean
    public Contract feignContract() {
        return new SpringMvcContract();
    }
    
    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            // Agregar headers comunes
            requestTemplate.header("Content-Type", "application/json");
            requestTemplate.header("User-Agent", "MyApp/1.0");
            
            // Agregar token de autenticación si existe
            String authToken = SecurityContextHolder.getContext().getAuthentication().getCredentials();
            if (authToken != null) {
                requestTemplate.header("Authorization", "Bearer " + authToken);
            }
        };
    }
    
    @Bean
    public ErrorDecoder errorDecoder() {
        return new UserServiceErrorDecoder();
    }
}

// Manejo de errores personalizado
public class UserServiceErrorDecoder implements ErrorDecoder {
    
    private final Logger logger = LoggerFactory.getLogger(UserServiceErrorDecoder.class);
    
    @Override
    public Exception decode(String methodKey, Response response) {
        logger.error("Feign error occurred: {} - {}", response.status(), response.reason());
        
        switch (response.status()) {
            case 400:
                return new BadRequestException("Invalid request data");
            case 401:
                return new UnauthorizedException("Authentication required");
            case 403:
                return new ForbiddenException("Access denied");
            case 404:
                return new NotFoundException("User not found");
            case 409:
                return new ConflictException("User already exists");
            case 422:
                return new ValidationException("Validation failed");
            case 500:
                return new InternalServerErrorException("Internal server error");
            default:
                return new FeignException(response.status(), response.reason(), response.request(), response.body());
        }
    }
}

// DTOs y Request/Response classes
public class CreateUserRequest {
    private String email;
    private String firstName;
    private String lastName;
    private LocalDate birthDate;
    // getters, setters, constructors
}

public class UserResponse {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private UserStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    // getters, setters, constructors
}
```

### 2. Cliente con Circuit Breaker
```java
// Interfaz del cliente con fallback
@FeignClient(
    name = "payment-service",
    fallback = PaymentServiceFallback.class,
    fallbackFactory = PaymentServiceFallbackFactory.class
)
public interface PaymentServiceClient {
    
    @PostMapping("/payments/process")
    PaymentResponse processPayment(@RequestBody PaymentRequest request);
    
    @GetMapping("/payments/{id}")
    PaymentResponse getPaymentStatus(@PathVariable("id") String paymentId);
}

// Fallback class
@Component
public class PaymentServiceFallback implements PaymentServiceClient {
    
    private final Logger logger = LoggerFactory.getLogger(PaymentServiceFallback.class);
    
    @Override
    public PaymentResponse processPayment(PaymentRequest request) {
        logger.warn("Payment service unavailable, using fallback");
        
        PaymentResponse response = new PaymentResponse();
        response.setStatus(PaymentStatus.PENDING);
        response.setMessage("Payment processing is temporarily unavailable. Please try again later.");
        return response;
    }
    
    @Override
    public PaymentResponse getPaymentStatus(String paymentId) {
        logger.warn("Payment service unavailable, cannot check status for payment: {}", paymentId);
        
        PaymentResponse response = new PaymentResponse();
        response.setStatus(PaymentStatus.UNKNOWN);
        response.setMessage("Payment status unavailable due to service unavailability.");
        return response;
    }
}

// Fallback factory para más control
@Component
public class PaymentServiceFallbackFactory implements FallbackFactory<PaymentServiceClient> {
    
    private final Logger logger = LoggerFactory.getLogger(PaymentServiceFallbackFactory.class);
    
    @Override
    public PaymentServiceClient create(Throwable cause) {
        logger.error("Payment service fallback triggered by: {}", cause.getMessage(), cause);
        
        return new PaymentServiceClient() {
            @Override
            public PaymentResponse processPayment(PaymentRequest request) {
                logger.error("Processing payment fallback for amount: {}", request.getAmount());
                
                PaymentResponse response = new PaymentResponse();
                response.setStatus(PaymentStatus.FAILED);
                response.setErrorCode("SERVICE_UNAVAILABLE");
                response.setErrorMessage("Payment service is currently unavailable");
                return response;
            }
            
            @Override
            public PaymentResponse getPaymentStatus(String paymentId) {
                logger.error("Getting payment status fallback for payment: {}", paymentId);
                
                PaymentResponse response = new PaymentResponse();
                response.setStatus(PaymentStatus.UNKNOWN);
                response.setErrorCode("SERVICE_UNAVAILABLE");
                response.setErrorMessage("Cannot retrieve payment status");
                return response;
            }
        };
    }
}
```

### 3. Cliente Asíncrono
```java
// Cliente con llamadas asíncronas
@FeignClient(
    name = "notification-service",
    configuration = NotificationFeignConfig.class
)
public interface NotificationServiceClient {
    
    @GetMapping("/notifications/pending")
    CompletableFuture<List<Notification>> getPendingNotifications();
    
    @PostMapping("/notifications/send")
    CompletableFuture<NotificationResponse> sendNotification(@RequestBody NotificationRequest request);
    
    @PostMapping("/notifications/batch")
    CompletableFuture<BatchNotificationResponse> sendBatchNotifications(@RequestBody BatchNotificationRequest request);
}

// Configuración para async
@Configuration
public class NotificationFeignConfig {
    
    @Bean
    public ExecutorService executorService() {
        return Executors.newFixedThreadPool(10);
    }
    
    @Bean
    public Contract feignContract() {
        return new Contract.Default();
    }
}

// Uso del cliente asíncrono
@Service
public class NotificationService {
    
    @Autowired
    private NotificationServiceClient notificationClient;
    
    @Async
    public CompletableFuture<Void> sendUserWelcomeEmail(String userEmail) {
        NotificationRequest request = new NotificationRequest();
        request.setType(NotificationType.WELCOME);
        request.setRecipient(userEmail);
        request.setTemplate("welcome-email");
        
        return notificationClient.sendNotification(request)
            .thenAccept(response -> {
                if (response.isSuccess()) {
                    logger.info("Welcome email sent successfully to: {}", userEmail);
                } else {
                    logger.error("Failed to send welcome email to: {}", userEmail);
                }
            });
    }
    
    public CompletableFuture<List<Notification>> getMultipleNotifications() {
        return notificationClient.getPendingNotifications();
    }
}
```

## **Ventajas y Desventajas**

### Ventajas

1. **Simplicidad y Declaratividad**
   - Código limpio y mantenible
   - Definición de APIs como interfaces Java
   - Reducción significativa del código boilerplate

2. **Integración Nativa con Spring**
   - Integración perfecta con Spring Boot
   - Soporte para anotaciones Spring MVC
   - Configuración unificada con application.yml

3. **Type Safety**
   - Compile-time type checking
   - IDE support completo (autocompletado, refactoring)
   - Detección temprana de errores

4. **Flexibilidad y Extensibilidad**
   - Fácil personalización de configuración
   - Soporte para múltiples codecs (JSON, XML, etc.)
   - Integración con herramientas de observabilidad

5. **Características Avanzadas**
   - Load balancing automático
   - Circuit breaker integration
   - Retry mechanisms
   - Timeouts configurables

### Desventajas

1. **Complejidad de Configuración**
   - Configuración inicial puede ser compleja
   - Múltiples opciones de configuración pueden confundir
   - Debugging puede ser desafiante

2. **Performance Overhead**
   - Reflection y proxies dinámicos
   - Overhead adicional comparado con HTTP clients directos
   - Posible impacto en startup time

3. **Dependencias**
   - Acoplamiento fuerte con Spring ecosystem
   - Dependencias adicionales aumentan el tamaño del JAR
   - Version compatibility issues

4. **Limitaciones de Flexibilidad**
   - Algunos casos de uso específicos pueden requerir configuración personalizada
   - Menos control fino comparado con HTTP clients de bajo nivel
   - Dependencia de las anotaciones disponibles

5. **Testing Complexity**
   - Mocking interfaces Feign puede ser complejo
   - Requiere herramientas específicas para testing
   - Integration testing puede ser más difícil

## **Buenas Prácticas**

### 1. Organización de Clientes
```java
// ✅ BUENA PRÁCTICA: Separar clientes en packages específicos
package com.example.clients.user;
package com.example.clients.payment;
package com.example.clients.notification;

// Cliente bien estructurado
@FeignClient(
    name = "user-service",
    path = "/api/v1/users",  // Versioning en el path
    configuration = UserClientConfig.class
)
public interface UserClient {
    // Métodos específicos y bien nombrados
    @GetMapping("/{id}/profile")
    UserProfile getUserProfile(@PathVariable("id") Long userId);
}

// ❌ MALA PRÁCTICA: Mezclar múltiples servicios en una interfaz
@FeignClient(name = "api-gateway")
public interface MixedClient {
    @GetMapping("/users/{id}")
    User getUser(@PathVariable Long id);
    
    @PostMapping("/orders")
    Order createOrder(@RequestBody OrderRequest request);
}
```

### 2. Manejo de Errores
```java
@FeignClient(name = "order-service")
public interface OrderClient {
    
    @GetMapping("/orders/{id}")
    Order getOrder(@PathVariable Long id);
}

// ✅ BUENA PRÁCTICA: Error decoder personalizado
@Component
public class OrderServiceErrorDecoder implements ErrorDecoder {
    
    @Override
    public Exception decode(String methodKey, Response response) {
        try {
            String errorBody = Util.toString(response.body().asInputStream());
            OrderServiceError error = new Gson().fromJson(errorBody, OrderServiceError.class);
            
            return new OrderServiceException(
                error.getCode(),
                error.getMessage(),
                error.getDetails()
            );
        } catch (Exception e) {
            return new FeignException(response.status(), response.reason(), response.request(), response.body());
        }
    }
}

// ❌ MALA PRÁCTICA: No manejar errores específicamente
@FeignClient(name = "order-service")
public interface BadOrderClient {
    @GetMapping("/orders/{id}")
    Order getOrder(@PathVariable Long id);  // Puede lanzar FeignException genérica
}
```

### 3. Configuración de Timeouts
```yaml
# ✅ BUENA PRÁCTICA: Configuración granular por cliente
feign:
  client:
    config:
      default:
        connect-timeout: 3000
        read-timeout: 10000
      payment-service:
        connect-timeout: 5000
        read-timeout: 30000  # Pagos pueden tomar más tiempo
      notification-service:
        connect-timeout: 1000
        read-timeout: 5000   # Notificaciones deben ser rápidas
```

### 4. Logging y Monitoring
```java
// ✅ BUENA PRÁCTICA: Configurar logging apropiadamente
@Configuration
public class FeignLoggingConfig {
    
    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.BASIC; // En producción
        // return Logger.Level.FULL; // Solo para desarrollo/debugging
    }
}

// Configuración de métricas
@Component
public class FeignMetricsInterceptor implements RequestInterceptor {
    
    @Override
    public void apply(RequestTemplate template) {
        Timer.Sample sample = Timer.start(meterRegistry);
        
        try {
            template.header("X-Request-Start", String.valueOf(System.currentTimeMillis()));
            template.header("X-Request-ID", UUID.randomUUID().toString());
        } finally {
            sample.stop(Timer.builder("feign.requests")
                .description("Feign client requests")
                .register(meterRegistry));
        }
    }
}
```

### 5. Configuration Management
```java
// ✅ BUENA PRÁCTICA: Configuración centralizada
@ConfigurationProperties(prefix = "app.feign.clients")
public class FeignClientProperties {
    
    private Map<String, ClientConfig> clients = new HashMap<>();
    
    public Map<String, ClientConfig> getClients() { return clients; }
    public void setClients(Map<String, ClientConfig> clients) { this.clients = clients; }
    
    public static class ClientConfig {
        private int connectTimeout = 3000;
        private int readTimeout = 10000;
        private String loggerLevel = "BASIC";
        private boolean retryEnabled = true;
        
        // getters, setters
    }
}

// ❌ MALA PRÁCTICA: Configuración hardcodeada
@FeignClient(name = "user-service")
public interface HardcodedClient {
    @GetMapping("/users/{id}")
    User getUser(@PathVariable Long id);  // Sin configuración flexible
}
```

### 6. Async y Reactive Patterns
```java
// ✅ BUENA PRÁCTICA: Soporte para programación reactiva
@FeignClient(name = "reactive-service")
public interface ReactiveServiceClient {
    
    @GetMapping("/data")
    Mono<DataResponse> getData();
    
    @PostMapping("/process")
    Flux<ProcessResult> processBatch(@RequestBody BatchRequest request);
}

// Configuración reactiva
@Configuration
public class ReactiveFeignConfig {
    
    @Bean
    public Contract reactiveContract() {
        return new ReactiveContract();
    }
}

// ✅ BUENA PRÁCTICA: Cliente asíncrono bien configurado
@Async
public CompletableFuture<User> getUserAsync(Long userId) {
    return userClient.getUser(userId)
        .thenApply(user -> {
            // Procesamiento adicional si es necesario
            return enrichUserData(user);
        });
}
```

### 7. Testing Strategies
```java
// ✅ BUENA PRÁCTICA: Testing con Feign Test
@ExtendWith(MockitoExtension.class)
class UserClientTest {
    
    @Mock
    private UserServiceClient userClient;
    
    @InjectMocks
    private UserService userService;
    
    @Test
    void shouldGetUserSuccessfully() {
        // Given
        Long userId = 1L;
        User expectedUser = createTestUser();
        when(userClient.getUserById(userId)).thenReturn(expectedUser);
        
        // When
        User result = userService.getUser(userId);
        
        // Then
        assertThat(result).isEqualTo(expectedUser);
        verify(userClient).getUserById(userId);
    }
}

// ✅ BUENA PRÁCTICA: Integration testing con @MockServer
@SpringBootTest
@TestPropertySource(properties = {
    "feign.client.config.user-service.url=http://localhost:${mock.server.port}"
})
@DirtiesContext
class UserClientIntegrationTest {
    
    @Autowired
    private UserServiceClient userClient;
    
    @Test
    void shouldHandleUserCreation() {
        // Mock server configuration
        mockServer.expect(requestTo("http://localhost:8080/api/users"))
                 .andRespond(withSuccess("{\"id\":1,\"name\":\"John\"}", MediaType.APPLICATION_JSON));
        
        // Test execution
        UserRequest request = new UserRequest("John", "john@example.com");
        User result = userClient.createUser(request);
        
        // Assertions
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("John");
    }
}
```

## **Referencias Oficiales**

1. **Spring Cloud OpenFeign Documentation**
   - https://docs.spring.io/spring-cloud-openfeign/docs/current/reference/html/
   - Documentación oficial completa de Spring Cloud OpenFeign

2. **Spring Cloud OpenFeign GitHub Repository**
   - https://github.com/spring-cloud/spring-cloud-openfeign
   - Código fuente y ejemplos oficiales

3. **OpenFeign Core Documentation**
   - https://github.com/OpenFeign/feign
   - Documentación del proyecto OpenFeign base

4. **Spring Cloud Reference Guide**
   - https://docs.spring.io/spring-cloud/docs/current/reference/html/
   - Guía de referencia completa de Spring Cloud

5. **Spring Boot Actuator with Feign**
   - https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html
   - Integración con Spring Boot Actuator para monitoring
# Logback

## ¿Qué es Logback?

**Logback** es un framework de logging para Java que sirve como successor de Log4j. Es el framework de logging por defecto en Spring Boot y ofrece un rendimiento superior, configuración flexible y características avanzadas como logging condicional, compresión automática de archivos y purging automática.

### Conceptos Fundamentales

**Logger**: Componente que produce mensajes de log según un nivel específico (TRACE, DEBUG, INFO, WARN, ERROR).

**Appender**: Componente que define dónde se escriben los mensajes de log (consola, archivo, socket, base de datos).

**Layout**: Componente que formatea los mensajes de log según un patrón específico.

**Level**: Nivel de severidad para controlar qué mensajes se procesan (TRACE < DEBUG < INFO < WARN < ERROR).

**Filter**: Componente que permite filtrar mensajes basado en criterios específicos.

---

## **Herramientas Principales**

### **Core Module (logback-core)**
- Base del framework de logging
- Implementación de loggers, appenders y layouts básicos
- Manejo de configuración y inicialización

### **Classic Module (logback-classic)**
- Implementación de SLF4J binding
- Integración con Java APIs de logging
- Configuración avanzada y filtros

### **Access Module (logback-access)**
- Logging para Servlets containers
- Integración con Tomcat, Jetty, JBoss
- Logging de requests HTTP

---

## **Casos de Uso**

### **Aplicaciones Java Standalone**
- Logging de aplicaciones desktop
- Logging de servicios standalone
- Aplicaciones batch y ETL

### **Aplicaciones Web**
- Logging en Spring Boot (por defecto)
- Logging en aplicaciones Java EE
- Microservicios con Spring Framework

### **Aplicaciones Empresariales**
- Sistemas de misión crítica
- Aplicaciones de alta disponibilidad
- Sistemas de trading financiero

### **Debugging y Desarrollo**
- Debugging local de aplicaciones
- Desarrollo de APIs REST
- Testing y QA

### **Producción y Monitoreo**
- Logging en ambiente de producción
- Auditoría de operaciones
- Compliance y regulaciones

---

## **Configuración**

### **Archivo de Configuración (logback.xml)**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <!-- Console Appender -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
            <charset>UTF-8</charset>
        </encoder>
    </appender>

    <!-- File Appender -->
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>/var/log/myapp/application.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>/var/log/myapp/application.%d{yyyy-MM-dd}.%i.log.gz</fileNamePattern>
            <timeBasedFileNamingAndTriggeringPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedFNATP">
                <maxFileSize>100MB</maxFileSize>
            </timeBasedFileNamingAndTriggeringPolicy>
            <maxHistory>30</maxHistory>
            <totalSizeCap>3GB</totalSizeCap>
        </rollingPolicy>
        <encoder>
            <pattern>%date %level [%thread] %logger{10} [%file:%line] - %msg%n</pattern>
            <charset>UTF-8</charset>
        </encoder>
    </appender>

    <!-- Async Appender -->
    <appender name="ASYNC" class="ch.qos.logback.classic.AsyncAppender">
        <appender-ref ref="FILE"/>
        <queueSize>1024</queueSize>
        <discardingThreshold>0</discardingThreshold>
    </appender>

    <!-- Database Appender -->
    <appender name="DB" class="ch.qos.logback.db.DBAppender">
        <connectionSource class="ch.qos.logback.db.DataSourceConnectionSource">
            <dataSource class="com.zaxxer.hikari.HikariDataSource">
                <driver-class-name>org.postgresql.Driver</driver-class-name>
                <jdbc-url>jdbc:postgresql://localhost:5432/logs</jdbc-url>
                <username>loguser</username>
                <password>logpass</password>
            </dataSource>
        </connectionSource>
        <insertHeaders>false</insertHeaders>
        <insertCData>false</insertCData>
        <insertSeq>false</insertSeq>
        <insertTime>false</insertTime>
    </appender>

    <!-- SMTP Appender -->
    <appender name="EMAIL" class="ch.qos.logback.classic.net.SMTPAppender">
        <smtpHost>smtp.gmail.com</smtpHost>
        <smtpPort>587</smtpPort>
        <username>alerts@company.com</username>
        <password>app-password</password>
        <subject>ALERT: %logger{20} - %msg</subject>
        <to>devops@company.com</to>
        <from>myapp@company.com</from>
        <asynchronousSending>false</asynchronousSending>
        <charsetEncoding>UTF-8</charsetEncoding>
        <layout class="ch.qos.logback.classic.html.HTMLLayout">
            <pattern>%d{HH:mm:ss.SSS} %-5level %logger{35} - %msg%n</pattern>
        </layout>
    </appender>

    <!-- Rolling Policy Configuration -->
    <rollingPolicy class="ch.qos.logback.core.rolling.FixedWindowRollingPolicy">
        <fileNamePattern>/var/log/myapp/archive/application.%i.log.zip</fileNamePattern>
        <minIndex>1</minIndex>
        <maxIndex>10</maxIndex>
    </rollingPolicy>

    <!-- Level Filter -->
    <filter class="ch.qos.logback.classic.filter.LevelFilter">
        <level>ERROR</level>
        <onMatch>ACCEPT</onMatch>
        <onMismatch>DENY</onMismatch>
    </filter>

    <!-- Logger Configuration -->
    <logger name="com.company.myapp" level="DEBUG" additivity="false">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="FILE"/>
    </logger>

    <logger name="org.springframework" level="WARN"/>
    <logger name="org.hibernate" level="WARN"/>
    <logger name="org.apache" level="WARN"/>
    <logger name="com.zaxxer" level="INFO"/>

    <!-- Root Logger -->
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="ASYNC"/>
    </root>

    <!-- JMX Configuration -->
    <jmxConfigurator/>
</configuration>
```

### **Configuración para Diferentes Ambientes**

```xml
<!-- logback-spring.xml para Spring Boot -->
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <!-- Profile-based configuration -->
    <springProfile name="development">
        <root level="DEBUG">
            <appender-ref ref="CONSOLE"/>
        </root>
    </springProfile>

    <springProfile name="staging">
        <root level="INFO">
            <appender-ref ref="FILE"/>
            <appender-ref ref="EMAIL"/>
        </root>
    </springProfile>

    <springProfile name="production">
        <root level="WARN">
            <appender-ref ref="ASYNC"/>
            <appender-ref ref="EMAIL"/>
        </root>
    </springProfile>

    <!-- Console configuration for all profiles -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
            <providers>
                <timestamp/>
                <logLevel/>
                <loggerName/>
                <message/>
                <mdc/>
                <arguments/>
                <stackTrace/>
            </providers>
        </encoder>
    </appender>

    <!-- Async configuration for performance -->
    <appender name="ASYNC_FILE" class="ch.qos.logback.classic.AsyncAppender">
        <queueSize>1024</queueSize>
        <discardingThreshold>0</discardingThreshold>
        <includeCallerData>true</includeCallerData>
        <appender-ref ref="FILE"/>
    </appender>
</configuration>
```

### **Configuración Programática**

```java
@Configuration
public class LogbackConfig {
    
    @EventListener
    public void handleContextRefresh(ContextRefreshedEvent event) {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        
        // Configurar appender programáticamente
        ConsoleAppender<ILoggingEvent> consoleAppender = new ConsoleAppender<>();
        consoleAppender.setContext(loggerContext);
        consoleAppender.setName("console");
        
        // Configurar encoder
        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(loggerContext);
        encoder.setPattern("%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n");
        encoder.setCharset(StandardCharsets.UTF_8);
        encoder.start();
        
        consoleAppender.setEncoder(encoder);
        consoleAppender.start();
        
        // Configurar logger
        Logger logger = loggerContext.getLogger("com.company.myapp");
        logger.addAppender(consoleAppender);
        logger.setLevel(Level.DEBUG);
        logger.setAdditive(false);
    }
    
    // Configurar filtro personalizado
    @Bean
    public FilterRegistrationBean<MarkerFilter> loggingFilter() {
        FilterRegistrationBean<MarkerFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new MarkerFilter());
        registrationBean.addUrlPatterns("/api/*");
        return registrationBean;
    }
}
```

---

## **Ejemplos en Java**

### **Configuración Básica**

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

public class LoggingExample {
    
    private static final Logger logger = LoggerFactory.getLogger(LoggingExample.class);
    private static final Marker PERFORMANCE_MARKER = MarkerFactory.getMarker("PERFORMANCE");
    private static final Marker SECURITY_MARKER = MarkerFactory.getMarker("SECURITY");
    
    public void demonstrateLogging() {
        // Logging básico
        logger.trace("This is a trace message");
        logger.debug("Debugging information");
        logger.info("Information message");
        logger.warn("Warning message");
        logger.error("Error message", exception);
        
        // Logging con parámetros
        String userId = "12345";
        logger.info("User logged in: {}", userId);
        
        // Logging con múltiples parámetros
        logger.info("User {} performed action {} at {}", userId, action, timestamp);
        
        // Logging con marker
        logger.info(PERFORMANCE_MARKER, "Request processed in {} ms", duration);
        logger.warn(SECURITY_MARKER, "Suspicious activity detected: {}", activity);
        
        // Logging condicional
        if (logger.isDebugEnabled()) {
            logger.debug("Expensive operation result: {}", expensiveOperation());
        }
    }
    
    // Logger con diferentes nombres
    public class ServiceLogger {
        private static final Logger logger = LoggerFactory.getLogger(ServiceLogger.class);
        
        public void logServiceCall(String serviceName, String method, Object result) {
            logger.info("SERVICE_CALL - Service: {}, Method: {}, Result: {}", 
                       serviceName, method, result);
        }
    }
}
```

### **Configuración en Spring Boot**

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

@Component
public class ApplicationLogger {
    
    private static final Logger logger = LoggerFactory.getLogger(ApplicationLogger.class);
    private static final Marker API_MARKER = MarkerFactory.getMarker("API");
    
    @EventListener
    public void handleApplicationStarted(ApplicationReadyEvent event) {
        logger.info("Application started successfully with logback");
    }
    
    public void logApiRequest(String endpoint, String method, String userId) {
        logger.info(API_MARKER, "API_REQUEST - Endpoint: {}, Method: {}, User: {}", 
                   endpoint, method, userId);
    }
    
    public void logException(String operation, Exception exception) {
        logger.error("EXCEPTION in {}: {}", operation, exception.getMessage(), exception);
    }
    
    // Logging de negocio específico
    public void logBusinessEvent(String event, Map<String, Object> data) {
        logger.info("BUSINESS_EVENT - Event: {}, Data: {}", event, data);
    }
}

// Configuración de logging en application.properties
logging.level.com.company.myapp=DEBUG
logging.level.org.springframework=WARN
logging.level.org.hibernate=WARN
logging.file.name=/var/log/myapp/application.log
logging.pattern.console=%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n
```

### **Custom Layout y Appender**

```java
// Custom Layout
public class JsonLayout extends LayoutBase<ILoggingEvent> {
    
    private ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public String doLayout(ILoggingEvent event) {
        Map<String, Object> logEntry = new HashMap<>();
        logEntry.put("timestamp", new Date(event.getTimeStamp()));
        logEntry.put("level", event.getLevel().toString());
        logEntry.put("logger", event.getLoggerName());
        logEntry.put("message", event.getFormattedMessage());
        logEntry.put("thread", event.getThreadName());
        logEntry.put("timestampMillis", event.getTimeStamp());
        
        // Añadir MDC si existe
        Map<String, String> mdc = event.getMDCPropertyMap();
        if (mdc != null && !mdc.isEmpty()) {
            logEntry.put("mdc", mdc);
        }
        
        try {
            return objectMapper.writeValueAsString(logEntry) + "\n";
        } catch (JsonProcessingException e) {
            return "{\"error\": \"Failed to serialize log entry\"}\n";
        }
    }
}

// Custom Appender
public class CustomDatabaseAppender extends AppenderBase<ILoggingEvent> {
    
    private DataSource dataSource;
    private String tableName = "application_logs";
    
    @Override
    protected void append(ILoggingEvent event) {
        try (Connection connection = dataSource.getConnection()) {
            String sql = String.format("INSERT INTO %s (timestamp, level, logger, message, thread, exception) VALUES (?, ?, ?, ?, ?, ?)", tableName);
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setTimestamp(1, new Timestamp(event.getTimeStamp()));
                stmt.setString(2, event.getLevel().toString());
                stmt.setString(3, event.getLoggerName());
                stmt.setString(4, event.getFormattedMessage());
                stmt.setString(5, event.getThreadName());
                stmt.setString(6, event.getThrowableProxy() != null ? event.getThrowableProxy().getMessage() : null);
                
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            addError("Failed to insert log entry to database", e);
        }
    }
    
    // Getters y setters para configuración
    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }
}
```

### **MDC (Mapped Diagnostic Context)**

```java
public class MDCExample {
    
    private static final Logger logger = LoggerFactory.getLogger(MDCExample.class);
    
    public void demonstrateMDC() {
        // Añadir información al contexto
        MDC.put("userId", "12345");
        MDC.put("sessionId", "abc-def-ghi");
        MDC.put("requestId", "req-12345");
        
        try {
            // Los logs incluirán automáticamente la información MDC
            logger.info("User performing action");
            logger.debug("Processing request");
            
            // Método helper para MDC
            processRequest();
            
        } finally {
            // Limpiar MDC al finalizar
            MDC.clear();
        }
    }
    
    private void processRequest() {
        // El MDC está disponible en este contexto
        logger.info("Processing business logic");
    }
}

// Filter para configurar MDC automáticamente
@Component
public class MDCFilter implements Filter {
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
            throws IOException, ServletException {
        
        try {
            // Añadir información de request al MDC
            MDC.put("requestId", UUID.randomUUID().toString());
            MDC.put("remoteAddr", request.getRemoteAddr());
            
            if (request instanceof HttpServletRequest) {
                HttpServletRequest httpRequest = (HttpServletRequest) request;
                MDC.put("method", httpRequest.getMethod());
                MDC.put("uri", httpRequest.getRequestURI());
                MDC.put("userAgent", httpRequest.getHeader("User-Agent"));
            }
            
            chain.doFilter(request, response);
            
        } finally {
            MDC.clear();
        }
    }
}
```

### **Logging en Microservicios**

```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    private static final Marker REQUEST_MARKER = MarkerFactory.getMarker("REQUEST");
    
    @Autowired
    private UserService userService;
    
    @GetMapping("/{id}")
    public ResponseEntity<User> getUser(@PathVariable String id) {
        long startTime = System.currentTimeMillis();
        
        try {
            logger.info(REQUEST_MARKER, "GET /api/users/{} - User request", id);
            
            User user = userService.getUserById(id);
            
            long duration = System.currentTimeMillis() - startTime;
            logger.info(REQUEST_MARKER, "GET /api/users/{} - Response sent in {}ms", id, duration);
            
            return ResponseEntity.ok(user);
            
        } catch (UserNotFoundException e) {
            logger.warn("GET /api/users/{} - User not found", id);
            return ResponseEntity.notFound().build();
            
        } catch (Exception e) {
            logger.error("GET /api/users/{} - Internal server error", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody UserCreateRequest request) {
        try {
            logger.info("POST /api/users - Creating user with email: {}", request.getEmail());
            
            User user = userService.createUser(request);
            
            logger.info("POST /api/users - User created successfully with id: {}", user.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(user);
            
        } catch (ValidationException e) {
            logger.warn("POST /api/users - Validation failed: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
            
        } catch (Exception e) {
            logger.error("POST /api/users - Failed to create user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

// Logging interceptor para trazabilidad
@Component
public class LoggingInterceptor implements HandlerInterceptor {
    
    private static final Logger logger = LoggerFactory.getLogger(LoggingInterceptor.class);
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String requestId = UUID.randomUUID().toString();
        MDC.put("requestId", requestId);
        
        logger.info("REQUEST_START - {} {} - User-Agent: {}", 
                   request.getMethod(), request.getRequestURI(), request.getHeader("User-Agent"));
        
        return true;
    }
    
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) {
        logger.info("REQUEST_END - {} {} - Status: {}", 
                   request.getMethod(), request.getRequestURI(), response.getStatus());
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        if (ex != null) {
            logger.error("REQUEST_ERROR - {} {} - Exception: {}", 
                        request.getMethod(), request.getRequestURI(), ex.getMessage(), ex);
        }
        
        MDC.clear();
    }
}
```

---

## **Ventajas y Desventajas**

### **Ventajas**

**Rendimiento Superior**
- Implementación más rápida que Log4j
- Logging asíncrono con AsyncAppender
- Minimización de I/O bloqueante

**Configuración Flexible**
- Configuración XML, Groovy o programática
- Logging condicional basado en perfiles
- Configuración dinámica vía JMX

**Características Avanzadas**
- Rolling automático de archivos
- Compresión automática de logs antiguos
- Filtros y masks para datos sensibles

**Integración Nativa**
- SLF4J binding por defecto
- Integración con Spring Boot
- Soporte para frameworks populares

**Logging Estructurado**
- Soporte para JSON layouts
- Integración con sistemas de logging centralizado
- MDC para trazabilidad

### **Desventajas**

**Complejidad de Configuración**
- Curva de aprendizaje para configuraciones avanzadas
- Múltiples opciones de configuración
- Debugging de configuración complejo

**Dependencias**
- Requiere SLF4J para funcionamiento
- Dependencias adicionales para algunas características
- Posibles conflictos de versiones

**Overhead de Memoria**
- Rolling de archivos consume memoria
- AsyncAppender tiene overhead de queue
- Múltiples appenders aumentan uso de memoria

**Curva de Aprendizaje**
- Conceptos avanzados requieren experiencia
- Debugging de problemas de logging complejo
- Optimización requiere conocimiento profundo

---

## **Buenas Prácticas**

### **1. Niveles de Logging Apropiados**

```java
// ✅ Usar niveles correctos
logger.trace("Method entry: {}", methodName);
logger.debug("Processing item: {}", item);
logger.info("User {} logged in", userId);
logger.warn("Rate limit exceeded for user: {}", userId);
logger.error("Failed to process payment", exception);

// ❌ Evitar mal uso de niveles
logger.info("Debug info: {}", debugData); // Usar DEBUG
logger.error("This is just info", exception); // Usar INFO
logger.debug("Error occurred", exception); // Usar ERROR
```

### **2. Configuración por Ambiente**

```xml
<!-- logback-spring.xml -->
<configuration>
    <!-- Development -->
    <springProfile name="dev">
        <root level="DEBUG">
            <appender-ref ref="CONSOLE"/>
        </root>
    </springProfile>
    
    <!-- Staging -->
    <springProfile name="staging">
        <root level="INFO">
            <appender-ref ref="FILE"/>
            <appender-ref ref="CONSOLE"/>
        </root>
    </springProfile>
    
    <!-- Production -->
    <springProfile name="prod">
        <root level="WARN">
            <appender-ref ref="ASYNC_FILE"/>
            <appender-ref ref="EMAIL"/>
        </root>
    </springProfile>
</configuration>
```

### **3. Performance Considerations**

```java
// ✅ Logging asíncrono para mejor performance
<appender name="ASYNC_FILE" class="ch.qos.logback.classic.AsyncAppender">
    <queueSize>1024</queueSize>
    <discardingThreshold>0</discardingThreshold>
    <includeCallerData>false</includeCallerData>
    <appender-ref ref="FILE"/>
</appender>

// ✅ Condicional logging para operaciones costosas
if (logger.isDebugEnabled()) {
    logger.debug("Expensive operation result: {}", expensiveOperation());
}

// ✅ Usar placeholders en lugar de concatenación
logger.info("User {} performed action {} at {}", userId, action, timestamp);
```

### **4. Seguridad en Logging**

```java
// ✅ Evitar log de datos sensibles
public void logUserAction(String userId, String action, String password) {
    // ❌ No hacer esto
    logger.info("User {} performed {} with password {}", userId, action, password);
    
    // ✅ Hacer esto
    logger.info("User {} performed {}", userId, action);
}

// ✅ Usar filters para masking
<filter class="ch.qos.logback.classic.filter.MarkerFilter">
    <marker>SENSITIVE_DATA</marker>
    <onMatch>DENY</onMatch>
</filter>
```

### **5. Structured Logging**

```java
// ✅ Logging estructurado con JSON
public class StructuredLogger {
    
    private static final Logger logger = LoggerFactory.getLogger(StructuredLogger.class);
    
    public void logEvent(String eventType, Map<String, Object> data) {
        Map<String, Object> logData = new HashMap<>();
        logData.put("event_type", eventType);
        logData.put("timestamp", Instant.now().toEpochMilli());
        logData.putAll(data);
        
        logger.info("EVENT: {}", logData);
    }
    
    public void logApiCall(String endpoint, String method, int statusCode, long duration) {
        Map<String, Object> logData = Map.of(
            "endpoint", endpoint,
            "method", method,
            "status_code", statusCode,
            "duration_ms", duration
        );
        
        logger.info("API_CALL: {}", logData);
    }
}
```

### **6. Error Handling**

```java
// ✅ Manejo adecuado de excepciones en logging
public class SafeLogger {
    
    private static final Logger logger = LoggerFactory.getLogger(SafeLogger.class);
    
    public void logSafely(String message, Object... args) {
        try {
            logger.info(message, args);
        } catch (Exception e) {
            // Fallback logging si falla el logging principal
            System.err.println("Failed to log: " + message);
        }
    }
    
    public void logException(String operation, Exception exception) {
        try {
            logger.error("Exception in {}: {}", operation, exception.getMessage(), exception);
        } catch (Exception e) {
            // Fallback logging
            System.err.println("Failed to log exception: " + exception.getMessage());
        }
    }
}
```

### **7. Monitoring y Alertas**

```xml
<!-- Configurar alertas por email -->
<appender name="EMAIL_ALERTS" class="ch.qos.logback.classic.net.SMTPAppender">
    <smtpHost>smtp.company.com</smtpHost>
    <smtpPort>587</smtpPort>
    <username>alerts@company.com</username>
    <password>app-password</password>
    <subject>[ALERT] Application Issues Detected</subject>
    <to>ops-team@company.com</to>
    <from>app@company.com</from>
    
    <!-- Filtro para enviar solo errores críticos -->
    <filter class="ch.qos.logback.classic.filter.LevelFilter">
        <level>ERROR</level>
        <onMatch>ACCEPT</onMatch>
        <onMismatch>DENY</onMismatch>
    </filter>
    
    <layout class="ch.qos.logback.classic.html.HTMLLayout"/>
</appender>
```

---

## **Referencias Oficiales**

1. **Logback Documentation**  
   https://logback.qos.ch/documentation.html

2. **Logback Manual**  
   https://logback.qos.ch/manual/index.html

3. **Logback Configuration**  
   https://logback.qos.ch/manual/configuration.html

4. **Spring Boot Logging**  
   https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.logging

5. **SLF4J Documentation**  
   https://www.slf4j.org/manual.html
# Istio

Istio es un service mesh de código abierto que proporciona una plataforma completa para conectar, asegurar y monitorear microservicios. Desarrollado por Google, IBM y Lyft, Istio se ha convertido en el service mesh más popular, ofreciendo capacidades avanzadas de gestión de tráfico, seguridad, observabilidad y control de políticas.

## **¿Qué es Istio?**

Istio es una plataforma de service mesh que extiende Kubernetes con capacidades avanzadas de networking, seguridad y observabilidad. Utiliza sidecars (Envoy proxies) junto a cada servicio para interceptar todo el tráfico de red, mientras que el control plane gestiona la configuración y políticas globalmente.

### Características Principales

- **Envoy Proxy**: Sidecar proxy de alto rendimiento basado en Envoy
- **Control Plane**: Gestión centralizada con Pilot, Citadel, Galley
- **Traffic Management**: Control granular del tráfico con VirtualServices
- **Security**: mTLS automático y políticas de autorización
- **Observability**: Métricas, logs y tracing sin instrumentación
- **Policy Enforcement**: Control de acceso y rate limiting
- **Multi-cluster**: Soporte para múltiples clusters de Kubernetes

## **Conceptos Clave**

### Arquitectura de Istio
Istio sigue una arquitectura split entre Control Plane y Data Plane:

- **Control Plane**: 
  - **Pilot**: Gestión de tráfico y configuración
  - **Citadel**: Gestión de seguridad y certificados
  - **Galley**: Validación y distribución de configuración
  - **Istiod**: Componente unificado en versiones recientes

- **Data Plane**:
  - **Envoy Proxies**: Sidecars que manejan el tráfico real

### Envoy Proxy
Proxy de alto rendimiento que maneja:
- Load balancing y routing
- Circuit breaking y retry policies
- mTLS y autenticación
- Métricas y observabilidad
- Rate limiting y throttling

```java
// Aplicación Java sin modificaciones para Istio
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {
    
    @Autowired
    private UsuarioService usuarioService;
    
    @GetMapping("/{id}")
    public ResponseEntity<Usuario> obtenerUsuario(@PathVariable Long id) {
        // Istio maneja automáticamente:
        // - Load balancing entre instancias
        // - Circuit breaking
        // - Retries con backoff exponencial
        // - Timeouts
        // - mTLS
        // - Métricas de request
        
        Usuario usuario = usuarioService.obtenerPorId(id);
        return ResponseEntity.ok(usuario);
    }
    
    @PostMapping
    public ResponseEntity<Usuario> crearUsuario(@RequestBody UsuarioRequest request) {
        // Istio aplica políticas de seguridad
        // Rate limiting
        // Observabilidad automática
        
        Usuario usuario = usuarioService.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(usuario);
    }
}
```

### Traffic Management
Control granular del tráfico con CRDs (Custom Resource Definitions):

```yaml
# VirtualService para routing avanzado
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: usuario-service-routing
spec:
  http:
  - match:
    - headers:
        x-user-type:
          exact: "premium"
    route:
    - destination:
        host: usuario-service
        subset: premium  # Usuarios premium van a subset premium
  - match:
    - uri:
        prefix: "/api/usuarios/admin"
    route:
    - destination:
        host: usuario-service
        subset: admin  # Endpoints admin van a subset admin
  - route:
    - destination:
        host: usuario-service
        subset: default
```

### Security
Seguridad basada en mTLS y políticas:

```yaml
# PeerAuthentication para mTLS estricto
apiVersion: security.istio.io/v1beta1
kind: PeerAuthentication
metadata:
  name: default
  namespace: default
spec:
  mtls:
    mode: STRICT

# AuthorizationPolicy para control de acceso
apiVersion: security.istio.io/v1beta1
kind: AuthorizationPolicy
metadata:
  name: usuario-service-authz
spec:
  selector:
    matchLabels:
      app: usuario-service
  rules:
  - from:
    - source:
        principals: 
        - "cluster.local/ns/default/sa/api-gateway"
        - "cluster.local/ns/default/sa/pedido-service"
    to:
    - operation:
        methods: ["GET", "POST"]
  - from:
    - source:
        namespaces: ["admin"]
    to:
    - operation:
        paths: ["/api/usuarios/admin/*"]
```

## **Casos de Uso**

### 1. Gestión Avanzada de Tráfico
- **Canary deployments**: Despliegues graduales seguros
- **A/B testing**: División de tráfico para testing
- **Traffic mirroring**: Mirroring para testing sin impacto
- **Circuit breaking**: Protección automática contra servicios fallidos

```yaml
# Configuración de canary deployment
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: usuario-service-canary
spec:
  http:
  - match:
    - headers:
        x-canary:
          exact: "true"
    route:
    - destination:
        host: usuario-service
        subset: v2
      weight: 10  # 10% del tráfico va a v2
  - route:
    - destination:
        host: usuario-service
        subset: v1
      weight: 90

---
apiVersion: networking.istio.io/v1beta1
kind: DestinationRule
metadata:
  name: usuario-service-dr
spec:
  host: usuario-service
  subsets:
  - name: v1
    labels:
      version: v1
    trafficPolicy:
      connectionPool:
        tcp:
          maxConnections: 100
        http:
          http1MaxPendingRequests: 50
  - name: v2
    labels:
      version: v2
    trafficPolicy:
      connectionPool:
        tcp:
          maxConnections: 50  # Más restrictivo para v2
```

### 2. Seguridad Empresarial
- **Zero-trust security**: Modelo de seguridad sin confianza implícita
- **mTLS automático**: Cifrado automático entre servicios
- **Fine-grained access control**: Control de acceso granular
- **Compliance**: Soporte para requerimientos de compliance

### 3. Observabilidad Avanzada
- **Distributed tracing**: Tracing distribuido automático
- **Custom metrics**: Métricas personalizadas sin instrumentación
- **Access logs**: Logs detallados de acceso
- **Grafana integration**: Dashboards predefinidos

### 4. Multi-Cloud y Hybrid
- **Multi-cluster**: Gestión de múltiples clusters
- **Hybrid deployments**: Aplicaciones en diferentes entornos
- **Traffic splitting**: División de tráfico entre entornos
- **Unified policies**: Políticas consistentes across entornos

## **Ejemplos en Java**

### Configuración de Aplicación para Istio

```java
@SpringBootApplication
@EnableConfigurationProperties
public class IstioUsuarioServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(IstioUsuarioServiceApplication.class, args);
    }
}

// Configuración optimizada para Istio
@Configuration
public class IstioConfiguration {
    
    @Bean
    @LoadBalanced
    public RestTemplate istioAwareRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        
        // Configuración para trabajar con Istio
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);  // Timeout más largo para Istio
        factory.setReadTimeout(10000);
        restTemplate.setRequestFactory(factory);
        
        // Interceptor para headers de Istio
        restTemplate.setInterceptors(List.of(
            new IstioHeadersInterceptor()
        ));
        
        return restTemplate;
    }
    
    @Bean
    public WebClient istioAwareWebClient() {
        HttpClient httpClient = HttpClient.create()
            .responseTimeout(Duration.ofSeconds(10))
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000);
        
        return WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .filter(istioHeadersFilter())
            .build();
    }
}

// Interceptor para headers de Istio
@Component
public class IstioHeadersInterceptor implements ClientHttpRequestInterceptor {
    
    @Override
    public ClientHttpResponse intercept(HttpRequest request, 
                                       byte[] body, 
                                       ClientHttpRequestExecution execution) 
            throws IOException {
        
        // Añadir headers requeridos por Istio
        request.getHeaders().add("x-user-id", getCurrentUserId());
        request.getHeaders().add("x-request-id", UUID.randomUUID().toString());
        request.getHeaders().add("x-forwarded-for", getClientIp());
        
        return execution.execute(request, body);
    }
    
    private String getCurrentUserId() {
        // Obtener desde JWT, headers, etc.
        return SecurityContextHolder.getContext()
            .getAuthentication().getName();
    }
    
    private String getClientIp() {
        // Obtener IP del cliente
        return "127.0.0.1"; // Implementar lógica real
    }
}

// Filtro para WebClient
private ExchangeFilterFunction istioHeadersFilter() {
    return (request, next) -> {
        // Añadir headers para Istio
        ClientRequest modifiedRequest = ClientRequest.from(request)
            .header("x-user-id", getCurrentUserId())
            .header("x-request-id", UUID.randomUUID().toString())
            .build();
            
        return next.exchange(modifiedRequest);
    };
}
```

### Health Checks Optimizados para Istio

```java
@Component
public class IstioHealthIndicator implements HealthIndicator {
    
    @Override
    public Health health() {
        Health.Builder healthBuilder = Health.up();
        
        try {
            // Verificar dependencias críticas
            boolean databaseHealthy = checkDatabase();
            boolean cacheHealthy = checkCache();
            boolean externalServicesHealthy = checkExternalServices();
            
            healthBuilder
                .withDetail("database", databaseHealthy ? "UP" : "DOWN")
                .withDetail("cache", cacheHealthy ? "UP" : "DOWN")
                .withDetail("external_services", externalServicesHealthy ? "UP" : "DOWN")
                .withDetail("istio_injected", isIstioInjected())
                .withDetail("proxy_health", getProxyHealth());
            
            // Determinar estado general
            if (databaseHealthy && cacheHealthy) {
                if (externalServicesHealthy) {
                    return healthBuilder.build();
                } else {
                    return healthBuilder
                        .withDetail("status", "degraded")
                        .status(Status.UP)
                        .build();
                }
            } else {
                return healthBuilder.down().build();
            }
            
        } catch (Exception e) {
            return healthBuilder.down()
                .withException(e)
                .withDetail("error", e.getMessage())
                .build();
        }
    }
    
    private boolean checkDatabase() {
        try {
            // Verificación rápida de base de datos
            return true; // Implementar verificación real
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean checkCache() {
        try {
            // Verificación rápida de cache
            return true; // Implementar verificación real
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean checkExternalServices() {
        try {
            // Verificar servicios externos críticos
            // Por ejemplo, servicios de payment, email, etc.
            return true; // Implementar verificación real
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean isIstioInjected() {
        // Verificar si Istio está inyectado
        return System.getenv("ISTIO_META_") != null;
    }
    
    private String getProxyHealth() {
        try {
            // Verificar health del proxy Envoy
            String healthEndpoint = "http://localhost:15020/healthz/ready";
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.getForEntity(healthEndpoint, String.class);
            return response.getStatusCode() == HttpStatus.OK ? "UP" : "DOWN";
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }
}

// Endpoint específico para Istio
@RestController
public class IstioController {
    
    @GetMapping("/istio-health")
    public ResponseEntity<Map<String, Object>> istioHealth() {
        Map<String, Object> health = new HashMap<>();
        
        health.put("service", "usuario-service");
        health.put("version", getVersion());
        health.put("istio_injected", isIstioInjected());
        health.put("proxy_type", "envoy");
        health.put("mesh_version", getMeshVersion());
        health.put("timestamp", Instant.now());
        
        return ResponseEntity.ok(health);
    }
    
    private boolean isIstioInjected() {
        return System.getenv("ISTIO_META_") != null ||
               System.getenv("ISTIO_PROXY_VERSION") != null;
    }
    
    private String getVersion() {
        return getClass().getPackage().getImplementationVersion();
    }
    
    private String getMeshVersion() {
        return System.getenv("ISTIO_PROXY_VERSION");
    }
}
```

### Configuración de Métricas para Istio

```java
// Métricas que Istio automáticamente recolecta
@RestController
public class MetricsController {
    
    @Autowired
    private MeterRegistry meterRegistry;
    
    @GetMapping("/actuator/metrics")
    public Map<String, Object> getMetrics(@RequestParam(required = false) List<String> names) {
        Map<String, Object> result = new HashMap<>();
        
        // Métricas personalizadas que Istio puede recolectar
        if (names == null || names.isEmpty()) {
            result.put("usuario_service_uptime_seconds", getUptimeSeconds());
            result.put("database_connections_active", getActiveDatabaseConnections());
            result.put("cache_hit_rate", getCacheHitRate());
            result.put("external_service_calls_total", getExternalServiceCalls());
        } else {
            names.forEach(name -> {
                switch (name) {
                    case "usuario_service_uptime_seconds":
                        result.put(name, getUptimeSeconds());
                        break;
                    case "database_connections_active":
                        result.put(name, getActiveDatabaseConnections());
                        break;
                    // Más métricas...
                }
            });
        }
        
        return result;
    }
    
    private double getUptimeSeconds() {
        return (System.currentTimeMillis() - getStartTime()) / 1000.0;
    }
    
    private int getActiveDatabaseConnections() {
        // Obtener del pool de conexiones
        return 10; // Ejemplo
    }
    
    private double getCacheHitRate() {
        // Calcular hit rate del cache
        return 0.85; // 85%
    }
    
    private long getExternalServiceCalls() {
        // Contador de llamadas a servicios externos
        return meterRegistry.counter("external_service_calls_total").count();
    }
}

// Configuración de métricas para Prometheus (Istio)
@Configuration
public class PrometheusMetricsConfig {
    
    @Bean
    public MeterRegistry prometheusMeterRegistry() {
        return new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
    }
    
    @Bean
    public Counter businessOperationsCounter(MeterRegistry meterRegistry) {
        return Counter.builder("business_operations_total")
            .description("Total de operaciones de negocio")
            .register(meterRegistry);
    }
    
    @Bean
    public Timer businessOperationTimer(MeterRegistry meterRegistry) {
        return Timer.builder("business_operation_duration_seconds")
            .description("Duración de operaciones de negocio")
            .register(meterRegistry);
    }
}

// Uso de métricas en servicios
@Service
public class UsuarioService {
    
    private final Counter businessOperationsCounter;
    private final Timer businessOperationTimer;
    
    public UsuarioService(Counter businessOperationsCounter, 
                         Timer businessOperationTimer) {
        this.businessOperationsCounter = businessOperationsCounter;
        this.businessOperationTimer = businessOperationTimer;
    }
    
    public Usuario crearUsuario(UsuarioRequest request) {
        return businessOperationTimer.record(() -> {
            // Lógica de creación
            Usuario usuario = createUsuarioLogic(request);
            
            // Incrementar contador
            businessOperationsCounter.increment(
                "operation", "create_usuario",
                "user_type", request.getType().toString(),
                "region", request.getRegion());
            
            return usuario;
        });
    }
}
```

### Configuración de Client para Istio

```java
// Cliente HTTP configurado para trabajar con Istio
@Component
public class IstioUsuarioClient {
    
    private final WebClient webClient;
    
    public IstioUsuarioClient(WebClient webClient) {
        this.webClient = webClient;
    }
    
    public Mono<Usuario> getUsuario(Long id) {
        return webClient
            .get()
            .uri("http://usuario-service/usuarios/{id}", id)
            .header("x-user-id", getCurrentUserId())
            .header("x-request-id", UUID.randomUUID().toString())
            .retrieve()
            .onStatus(HttpStatus::is5xxServerError, response -> {
                return response.bodyToMono(String.class)
                    .flatMap(errorBody -> {
                        logger.error("Service error: {}", errorBody);
                        return Mono.error(new ServiceUnavailableException("Service temporarily unavailable"));
                    });
            })
            .onStatus(HttpStatus::is4xxClientError, response -> {
                return response.bodyToMono(String.class)
                    .flatMap(errorBody -> {
                        logger.warn("Client error: {}", errorBody);
                        return Mono.error(new UserNotFoundException("User not found"));
                    });
            })
            .bodyToMono(Usuario.class)
            .timeout(Duration.ofSeconds(5))
            .retryWhen(Retry.backoff(3, Duration.ofMillis(100))
                .filter(this::isRetryableError)
                .onRetryExhaustedThrow((backoff, retrySignal) -> {
                    throw new ServiceUnavailableException("Service failed after retries");
                }))
            .onErrorResume(throwable -> {
                logger.warn("Istio mesh fallback triggered for usuario {}", id, throwable);
                return getFallbackUsuario(id);
            });
    }
    
    private boolean isRetryableError(Throwable throwable) {
        return throwable instanceof TimeoutException ||
               throwable instanceof ConnectException ||
               (throwable instanceof WebClientResponseException && 
                ((WebClientResponseException) throwable).getStatusCode().is5xxServerError());
    }
    
    private Mono<Usuario> getFallbackUsuario(Long id) {
        // Fallback manejado por Istio circuit breaker
        return Mono.just(Usuario.builder()
            .id(id)
            .nombre("Usuario Temporal")
            .email("temporal@example.com")
            .estado(EstadoUsuario.TEMPORAL)
            .build());
    }
    
    private String getCurrentUserId() {
        return SecurityContextHolder.getContext()
            .getAuthentication().getName();
    }
}
```

### Configuración de Deployment para Istio

```yaml
# Deployment con sidecar injection
apiVersion: apps/v1
kind: Deployment
metadata:
  name: usuario-service
  labels:
    app: usuario-service
    version: v1
spec:
  replicas: 3
  selector:
    matchLabels:
      app: usuario-service
  template:
    metadata:
      labels:
        app: usuario-service
        version: v1
      annotations:
        # Configuración para Istio
        sidecar.istio.io/inject: "true"
        prometheus.io/scrape: "true"
        prometheus.io/port: "8080"
        prometheus.io/path: "/actuator/prometheus"
        # Configuración de traffic policy
        traffic.sidecar.istio.io/includeInboundPorts: "8080"
        traffic.sidecar.istio.io/includeOutboundIPRanges: "*"
    spec:
      containers:
      - name: usuario-service
        image: empresa/usuario-service:1.0.0
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "istio,production"
        - name: JAVA_OPTS
          value: "-Xms256m -Xmx512m -Djava.net.preferIPv4Stack=true"
        - name: POD_NAME
          valueFrom:
            fieldRef:
              fieldPath: metadata.name
        - name: POD_NAMESPACE
          valueFrom:
            fieldRef:
              fieldPath: metadata.namespace
        resources:
          requests:
            memory: "256Mi"
            cpu: "250m"
          limits:
            memory: "512Mi"
            cpu: "500m"
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 30
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
        startupProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
          failureThreshold: 30

---
# Service para Istio
apiVersion: v1
kind: Service
metadata:
  name: usuario-service
  labels:
    app: usuario-service
spec:
  type: ClusterIP
  ports:
  - port: 8080
    targetPort: 8080
    protocol: TCP
    name: http
  selector:
    app: usuario-service

---
# Gateway para acceso externo
apiVersion: networking.istio.io/v1beta1
kind: Gateway
metadata:
  name: usuario-service-gateway
spec:
  selector:
    istio: ingressgateway
  servers:
  - port:
      number: 80
      name: http
      protocol: HTTP
    hosts:
    - "usuario-service.example.com"
  - port:
      number: 443
      name: https
      protocol: HTTPS
    tls:
      mode: SIMPLE
      credentialName: usuario-service-tls
    hosts:
    - "usuario-service.example.com"

---
# VirtualService para routing externo
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: usuario-service-vs
spec:
  hosts:
  - "usuario-service.example.com"
  gateways:
  - usuario-service-gateway
  http:
  - match:
    - uri:
        prefix: "/api/usuarios"
    route:
    - destination:
        host: usuario-service
        port:
          number: 8080
    retries:
      attempts: 3
      perTryTimeout: 2s
      baseDelay: 0.1s
    timeout: 10s
    fault:
      delay:
        percentage:
          value: 0.1
        fixedDelay: 5s
```

## **Ventajas y Desventajas**

### Ventajas
- **Ecosistema maduro**: Proyecto estable con amplia adopción
- **Kubernetes nativo**: Integración perfecta con Kubernetes
- **Arquitectura robusta**: Separación clara entre control y data plane
- **Extensibilidad**: APIs y CRDs para extensibilidad
- **Multi-cloud**: Soporte para múltiples proveedores cloud
- **Rich tooling**: Herramientas completas de gestión y observabilidad
- **Community active**: Comunidad activa y soporte empresarial
- **Performance**: Envoy proxy de alto rendimiento

### Desventajas
- **Complejidad**: Curva de aprendizaje pronunciada
- **Resource overhead**: Consumo significativo de CPU y memoria
- **Latencia**: Overhead de latencia por los sidecars
- **Vendor dependencies**: Dependencia del ecosistema Istio
- **Configuration complexity**: Configuración puede ser muy compleja
- **Debugging difficulty**: Debugging puede ser desafiante
- **Learning curve**: Requiere conocimiento especializado
- **Upgrades**: Actualizaciones pueden ser complejas

## **Buenas Prácticas**

### Instalación y Configuración
- **Namespace selection**: Usar namespaces específicos para service mesh
- **Resource allocation**: Asignar recursos apropiados para control plane
- **Security hardening**: Configurar security policies apropiadamente
- **Monitoring setup**: Configurar monitoring desde el inicio

```yaml
# Namespace con sidecar injection habilitado
apiVersion: v1
kind: Namespace
metadata:
  name: usuario-service-ns
  labels:
    istio-injection: enabled
    # Configuración de red
    topology.istio.io/network: default-network

---
# ResourceQuota para namespace
apiVersion: v1
kind: ResourceQuota
metadata:
  name: usuario-service-quota
  namespace: usuario-service-ns
spec:
  hard:
    requests.cpu: "2"
    requests.memory: 4Gi
    limits.cpu: "4"
    limits.memory: 8Gi
    pods: "10"
```

### Seguridad
- **mTLS strict mode**: Habilitar mTLS estricto en todos los namespaces
- **Authorization policies**: Implementar políticas de autorización granulares
- **Certificate rotation**: Configurar rotación automática de certificados
- **Security scanning**: Escanear imágenes regularmente

```yaml
# PeerAuthentication estricto para namespace
apiVersion: security.istio.io/v1beta1
kind: PeerAuthentication
metadata:
  name: default
  namespace: usuario-service-ns
spec:
  mtls:
    mode: STRICT
  portLevelMtls:
    8080:
      mode: STRICT

---
# AuthorizationPolicy granular
apiVersion: security.istio.io/v1beta1
kind: AuthorizationPolicy
metadata:
  name: usuario-service-authz
  namespace: usuario-service-ns
spec:
  selector:
    matchLabels:
      app: usuario-service
  rules:
  - from:
    - source:
        principals: 
        - "cluster.local/ns/default/sa/api-gateway"
        - "cluster.local/ns/usuario-service-ns/sa/usuario-service"
    to:
    - operation:
        methods: ["GET", "POST"]
        paths: ["/api/usuarios/*"]
  - from:
    - source:
        namespaces: ["admin"]
    to:
    - operation:
        methods: ["GET", "POST", "PUT", "DELETE"]
        paths: ["/api/usuarios/admin/*"]
  - from:
    - source:
        notPrincipals: 
        - "cluster.local/ns/default/sa/guest"
    to:
    - operation:
        methods: ["GET"]
        paths: ["/api/usuarios/public/*"]
```

### Traffic Management
- **Circuit breaking**: Configurar circuit breaking apropiadamente
- **Retry policies**: Usar retry policies con backoff exponencial
- **Timeout configuration**: Configurar timeouts realistas
- **Load balancing**: Usar load balancing algorithms apropiados

```yaml
# DestinationRule con circuit breaking
apiVersion: networking.istio.io/v1beta1
kind: DestinationRule
metadata:
  name: usuario-service-circuit-breaker
spec:
  host: usuario-service
  trafficPolicy:
    connectionPool:
      tcp:
        maxConnections: 100  # Máximo conexiones TCP
      http:
        http1MaxPendingRequests: 50  # Máximo requests pendientes
        maxRequestsPerConnection: 2  # Requests por conexión
        maxRetries: 3
    circuitBreaker:
      consecutiveErrors: 5  # Errores consecutivos para abrir circuito
      interval: 30s         # Intervalo de evaluación
      baseEjectionTime: 30s # Tiempo de ejection
      maxEjectionPercent: 50 # Porcentaje máximo de ejection
      minHealthPercent: 50   # Porcentaje mínimo saludable
    loadBalancer:
      simple: LEAST_CONN  # Least connections load balancing
```

### Observabilidad
- **Custom metrics**: Definir métricas específicas del negocio
- **Tracing integration**: Integrar con Jaeger, Zipkin
- **Logging configuration**: Configurar logging estructurado
- **Alerting rules**: Configurar alertas apropiadas

```yaml
# ServiceMonitor para Prometheus
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: usuario-service-monitor
  namespace: usuario-service-ns
  labels:
    app: usuario-service
spec:
  selector:
    matchLabels:
      app: usuario-service
  endpoints:
  - port: http
    path: /actuator/prometheus
    interval: 30s
    scrapeTimeout: 10s
    honorLabels: true

---
# PodMonitor para sidecars
apiVersion: monitoring.coreos.com/v1
kind: PodMonitor
metadata:
  name: istio-proxy-monitor
  namespace: usuario-service-ns
spec:
  selector:
    matchLabels:
      app: usuario-service
  podMetricsEndpoints:
  - port: http-envoy-prom
    interval: 30s
    path: /stats/prometheus
```

### Deployment Strategies
- **Canary deployments**: Usar canary para releases importantes
- **Blue-green deployments**: Blue-green para zero-downtime
- **Traffic mirroring**: Usar mirroring para testing
- **Progressive delivery**: Implementar entrega progresiva

```yaml
# Configuración de canary deployment
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: usuario-service-canary
spec:
  http:
  - match:
    - headers:
        x-canary:
          exact: "true"
    - headers:
        user-id:
          regex: ".*premium.*"
    route:
    - destination:
        host: usuario-service
        subset: v2
      weight: 20
  - route:
    - destination:
        host: usuario-service
        subset: v1
      weight: 80
  - fault:
    delay:
      percentage:
        value: 1.0
      fixedDelay: 5s
    route:
    - destination:
        host: usuario-service
        subset: v1
```

### Troubleshooting
- **Debug mode**: Habilitar modo debug temporalmente
- **Proxy logs**: Configurar logging detallado de proxies
- **Metrics inspection**: Inspeccionar métricas de Istio
- **Traffic analysis**: Analizar patrones de tráfico

```yaml
# Configuración de debug temporal
apiVersion: v1
kind: ConfigMap
metadata:
  name: istio-proxy-config
  namespace: usuario-service-ns
data:
  mesh: |
    defaultConfig:
      # Configuración temporal para debugging
      proxyStatsMatcher:
        inclusionRegexps:
        - ".*"
      extraStatTags:
      - url_label
      - method
      - response_code
```

---

## **Referencias Oficiales**

1. **Istio Official Documentation**: https://istio.io/latest/docs/
2. **Istio GitHub Repository**: https://github.com/istio/istio
3. **Istio Concepts Guide**: https://istio.io/latest/docs/concepts/
4. **Istio Tasks and Tutorials**: https://istio.io/latest/docs/tasks/
5. **Istio API Reference**: https://istio.io/latest/docs/reference/config/
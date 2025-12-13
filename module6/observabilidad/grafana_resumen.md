# Grafana

## ¿Qué es Grafana?

**Grafana** es una plataforma de visualización y observabilidad open source que permite crear, explorar y compartir dashboards interactivos para métricas, logs y traces. Soporta múltiples fuentes de datos como Prometheus, InfluxDB, Elasticsearch, Loki, PostgreSQL y muchos más, proporcionando una interfaz unificada para el monitoreo y análisis de sistemas.

### Conceptos Fundamentales

**Data Sources**: Conexiones a diferentes sistemas de almacenamiento de métricas, logs o traces (Prometheus, InfluxDB, Elasticsearch, etc.).

**Dashboard**: Colección de paneles (panels) que muestran visualizaciones de datos con configuraciones específicas.

**Panel**: Componente individual de visualización que muestra datos específicos con gráficos, tablas o alertas.

**Query**: Consulta enviada a data sources para obtener datos específicos con filtros y transformaciones.

**Alert**: Notificación configurada que se activa cuando ciertas condiciones se cumplen.

---

## **Herramientas Principales**

### **Grafana Core**
- Dashboard engine y visualización
- Alerting system
- User management y permissions
- Plugin system

### **Grafana Enterprise**
- Data source proxy avanzado
- Distributed tracing
- Usage analytics
- Enhanced security

### **Grafana Cloud**
- SaaS version de Grafana
- Managed Prometheus y Loki
- Hosted Grafana con plugins premium
- SLA y soporte empresarial

### **Grafana Plugins**
- Data source plugins
- Panel plugins
- App plugins
- Dashboard plugins

---

## **Casos de Uso**

### **Infrastructure Monitoring**
- Monitoreo de servidores y redes
- Métricas de CPU, memoria, disco
- Alertas de performance
- Capacity planning

### **Application Performance Monitoring (APM)**
- Métricas de aplicaciones
- Latency y throughput
- Error rates y availability
- User experience metrics

### **Business Intelligence**
- KPIs de negocio
- Dashboards ejecutivos
- Reportes automatizados
- Data exploration

### **Security Monitoring**
- SIEM dashboards
- Threat detection
- Compliance monitoring
- Incident response

### **DevOps y SRE**
- Service level objectives (SLOs)
- Error budgets
- Incident management
- Postmortem analysis

### **IoT y Edge Monitoring**
- IoT device monitoring
- Edge computing metrics
- Sensor data visualization
- Real-time dashboards

---

## **Configuración**

### **Grafana Configuration (grafana.ini)**

```ini
# Configuración básica
[paths]
data = /var/lib/grafana/
logs = /var/log/grafana
plugins = /var/lib/grafana/plugins
provisioning = /etc/grafana/provisioning

[server]
protocol = http
http_addr = 0.0.0.0
http_port = 3000
domain = grafana.company.com
root_url = https://grafana.company.com/
serve_from_sub_path = false

# Configuración de seguridad
[security]
admin_user = admin
admin_password = admin_password
secret_key = SW2YcwTIb9zpOOhoPsMm
disable_gravatar = false
data_source_proxy_whitelist = grafana.company.com
cookie_secure = true
cookie_samesite = strict

# Configuración de base de datos
[database]
type = postgres
host = postgres:5432
name = grafana
user = grafana
password = grafana_password
ssl_mode = disable
max_idle_conn = 2
max_open_conn = 0
conn_max_lifetime = 14400

# Configuración de sesión
[session]
provider = database
provider_config = tables:sessions,sessions_cleanup,sec_key_headers,sec_key_headers_cleanup,per_org_user,per_org_headers,per_org_headers_cleanup,activation,star
cookie_name = grafana_sess
cookie_secure = false
session_life_time = 86400
gc_interval_time = 31536000
conn_max_lifetime = 14400

# Configuración de usuarios
[users]
allow_sign_up = false
allow_org_create = false
auto_assign_org = true
auto_assign_org_role = Viewer
verify_email_enabled = false
login_hint = email or username
password_hint = password

# Configuración de equipos
[auth]
login_cookie_name = grafana_user_auth
login_maximum_inactive_lifetime_duration = 
login_maximum_lifetime_duration = 
token_rotation_interval_minutes = 10
disable_login_form = false
disable_signout_menu = 
signout_redirect_url = 

# Configuración de alertas
[alerting]
enabled = true
execute_alerts = true
error_or_timeout = alerting
nodata_or_nullvalues = no_data
concurrent_render_limit = 5
evaluation_timeout_seconds = 30
notification_timeout_seconds = 30
max_attempts = 3
min_interval_seconds = 1

# Configuración de dashboards
[dashboards]
default_home_dashboard_path = 
min_refresh_interval = 5s
versions_to_keep = 20
concurrent_refresh_limit = 5

# Configuración de métricas
[metrics]
enabled = true
interval_seconds = 10
disable_metrics = false
disable_total_stats = false

# Configuración de analytics
[analytics]
reporting_enabled = false
check_for_updates = false
google_analytics_ua_id = 
google_tag_api_id = 
rudderstack_write_key = 
rudderstack_data_plane_url = 
rudderstack_sdk_url = 

# Configuración de plugins
[plugins]
allow_loading_unsigned_plugins = 
marketplace_url = https://grafana.com/grafana/plugins/

# Configuración de log
[log]
mode = console file
level = info
filters = 

[log.file]
path = /var/log/grafana/grafana.log
log_rotate = true
max_lines = 1000000
max_size_shift = 28
daily_rotate = 7

[log.console]
level = info

[log.syslog]
level = info
network = 
address = 
facility = 
tag = 

# Configuración de feature flags
[feature_toggles]
enable = ngalert
prometheusRemoteWrite = 
prometheusRemoteRead = 
influxdbBackendMigration = 
newDBLibrary = 
validateDashboardsOnSave = 
enablePanelTreeView = 
enableServiceAccount = 
disableEnvelopeEncryption = 

# Configuración de query history
[query_history]
enabled = false

# Configuración de exploration
[exploration]
enabled = true

# Configuración de annotations
[annotations]
enabled = true
cleanupjob_batchsize = 100
cleanupjob_timeout = 30
max_age = 86400
max_tags = 100
max_annotations_to_keep = 100000

# Configuración de external image storage
[external_image_storage]
provider = 

[external_image_storage.s3]
endpoint = 
path_style_access = 
bucket_url = 
bucket = 
region = 
path = 
acl = 
signed_urls = 
server_side_encryption = 
secret_key = 
path_style_access = 

# Configuración de rendering
[rendering]
server_url = 
callback_url = 
concurrent_render_request_limit = 30
rendering_ignore_https_errors = false

# Configuración de enterprise
[enterprise]
license_path = 

# Configuración de feature management
[feature_management]
homepage = 
updated_update_nag =
```

### **Docker Compose para Grafana Stack**

```yaml
version: '3.8'

services:
  grafana:
    image: grafana/grafana:10.2.0
    ports:
      - "3000:3000"
    environment:
      - GF_SECURITY_ADMIN_USER=admin
      - GF_SECURITY_ADMIN_PASSWORD=admin123
      - GF_USERS_ALLOW_SIGN_UP=false
      - GF_INSTALL_PLUGINS=grafana-piechart-panel
    volumes:
      - grafana_data:/var/lib/grafana
      - ./grafana/provisioning:/etc/grafana/provisioning
      - ./grafana/dashboards:/var/lib/grafana/dashboards
      - ./grafana/plugins:/var/lib/grafana/plugins
    networks:
      - monitoring
    restart: unless-stopped
    deploy:
      resources:
        limits:
          memory: 1G
          cpus: '0.5'
        reservations:
          memory: 512M
          cpus: '0.25'

  prometheus:
    image: prom/prometheus:v2.47.0
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
      - '--web.console.libraries=/etc/prometheus/console_libraries'
      - '--web.console.templates=/etc/prometheus/consoles'
      - '--storage.tsdb.retention.time=200h'
      - '--web.enable-lifecycle'
    ports:
      - "9090:9090"
    volumes:
      - ./prometheus/prometheus.yml:/etc/prometheus/prometheus.yml
      - prometheus_data:/prometheus
    networks:
      - monitoring
    restart: unless-stopped

  loki:
    image: grafana/loki:2.9.3
    command: -config.file=/etc/loki/local-config.yaml
    ports:
      - "3100:3100"
    volumes:
      - ./loki:/etc/loki
      - loki_data:/loki
    networks:
      - monitoring
    restart: unless-stopped

  promtail:
    image: grafana/promtail:2.9.3
    command: -config.file=/etc/promtail/config.yml
    volumes:
      - ./promtail:/etc/promtail
      - /var/log:/var/log:ro
      - /var/lib/docker/containers:/var/lib/docker/containers:ro
      - /etc/machine-id:/etc/machine-id:ro
    networks:
      - monitoring
    depends_on:
      - loki
    restart: unless-stopped

  tempo:
    image: grafana/tempo:2.3.1
    command: -config.file=/etc/tempo.yaml
    ports:
      - "3200:3200"  # tempo
      - "4317:4317"  # otlp grpc
      - "4318:4318"  # otlp http
    volumes:
      - ./tempo:/etc/tempo
      - tempo_data:/var/tempo
    networks:
      - monitoring
    restart: unless-stopped

volumes:
  grafana_data:
  prometheus_data:
  loki_data:
  tempo_data:

networks:
  monitoring:
    driver: bridge
```

### **Data Sources Configuration**

```yaml
# grafana/provisioning/datasources/datasources.yml
apiVersion: 1

datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://prometheus:9090
    isDefault: true
    editable: true
    jsonData:
      httpMethod: POST
      queryTimeout: 60s
      timeInterval: 15s
    version: 1

  - name: Loki
    type: loki
    access: proxy
    url: http://loki:3100
    editable: true
    jsonData:
      maxLines: 1000
      timeout: 60s
    version: 1

  - name: Tempo
    type: tempo
    access: proxy
    url: http://tempo:3200
    editable: true
    jsonData:
      tracesToLogs:
        datasourceUid: 'loki'
        tags: ['job', 'instance', 'pod', 'namespace']
        mappedTags: [{ key: 'service.name', value: 'service' }]
        mapTagNamesEnabled: false
        spanStartTimeShift: '1h'
        spanEndTimeShift: '1h'
        filterByTraceID: false
        filterBySpanID: false
      tracesToMetrics:
        datasourceUid: 'prometheus'
        tags: [{ key: 'service.name', value: 'service' }, { key: 'job' }]
        queries:
          - name: 'Sample query'
            query: 'sum(rate(traces_spanmetrics_latency_bucket{$$__tags}[5m]))'
      serviceMap:
        datasourceUid: 'prometheus'
      nodeGraph:
        enabled: true
    version: 1

  - name: Elasticsearch
    type: elasticsearch
    access: proxy
    url: http://elasticsearch:9200
    database: "logs-*"
    editable: true
    jsonData:
      esVersion: 8
      timeField: "@timestamp"
      interval: "Daily"
      logMessageField: "message"
      logLevelField: "level"
    version: 1

  - name: InfluxDB
    type: influxdb
    access: proxy
    url: http://influxdb:8086
    database: "telegraf"
    editable: true
    jsonData:
      version: "InfluxQL"
      timeInterval: "15s"
    version: 1

  - name: PostgreSQL
    type: postgres
    access: proxy
    url: postgres:5432
    database: "monitoring"
    user: "grafana"
    editable: true
    jsonData:
      sslmode: "disable"
      maxOpenConns: 5
      maxIdleConns: 2
      connMaxLifetime: 14400
    version: 1
```

### **Dashboard Provisioning**

```yaml
# grafana/provisioning/dashboards/dashboards.yml
apiVersion: 1

providers:
  - name: 'default'
    orgId: 1
    folder: ''
    type: file
    disableDeletion: false
    updateIntervalSeconds: 10
    allowUiUpdates: true
    options:
      path: /var/lib/grafana/dashboards

  - name: 'custom'
    orgId: 1
    folder: 'Custom'
    type: file
    disableDeletion: false
    updateIntervalSeconds: 10
    allowUiUpdates: true
    options:
      path: /var/lib/grafana/dashboards/custom
```

### **Alerting Configuration**

```yaml
# grafana/provisioning/notifiers/notifiers.yml
apiVersion: 1

notifiers:
  - name: 'Slack'
    type: slack
    uid: slack-notifier
    orgId: 1
    isDefault: false
    settings:
      url: 'https://hooks.slack.com/services/YOUR/SLACK/WEBHOOK'
      recipient: '#alerts'
      username: Grafana
      iconEmoji: ':chart_with_upwards_trend:'

  - name: 'Email'
    type: email
    uid: email-notifier
    orgId: 1
    isDefault: false
    settings:
      fromAddress: 'grafana@company.com'
      fromName: 'Grafana'
      subject: 'Alert: {{ .GroupLabels.app }}'
      toAddresses: ['ops-team@company.com']

  - name: 'PagerDuty'
    type: pagerduty
    uid: pagerduty-notifier
    orgId: 1
    isDefault: false
    settings:
      integrationKey: 'YOUR_PAGERDUTY_INTEGRATION_KEY'
      severity: 'critical'

  - name: 'Webhook'
    type: webhook
    uid: webhook-notifier
    orgId: 1
    isDefault: false
    settings:
      url: 'https://api.company.com/alerts'
      username: 'grafana'
      password: 'api_password'
      httpMethod: 'POST'
```

---

## **Ejemplos de Configuración**

### **Application Performance Dashboard**

```json
{
  "dashboard": {
    "id": null,
    "title": "Application Performance",
    "tags": ["application", "performance"],
    "timezone": "browser",
    "refresh": "5s",
    "time": {
      "from": "now-1h",
      "to": "now"
    },
    "panels": [
      {
        "id": 1,
        "title": "Request Rate",
        "type": "graph",
        "targets": [
          {
            "expr": "sum(rate(http_requests_total[5m])) by (service)",
            "legendFormat": "{{service}}",
            "refId": "A"
          }
        ],
        "yAxes": [
          {
            "label": "Requests/sec",
            "min": 0
          }
        ],
        "gridPos": {
          "h": 8,
          "w": 12,
          "x": 0,
          "y": 0
        }
      },
      {
        "id": 2,
        "title": "Response Time",
        "type": "graph",
        "targets": [
          {
            "expr": "histogram_quantile(0.95, sum(rate(http_request_duration_seconds_bucket[5m])) by (le, service))",
            "legendFormat": "95th percentile - {{service}}",
            "refId": "A"
          },
          {
            "expr": "histogram_quantile(0.50, sum(rate(http_request_duration_seconds_bucket[5m])) by (le, service))",
            "legendFormat": "50th percentile - {{service}}",
            "refId": "B"
          }
        ],
        "yAxes": [
          {
            "label": "Seconds",
            "min": 0
          }
        ],
        "gridPos": {
          "h": 8,
          "w": 12,
          "x": 12,
          "y": 0
        }
      },
      {
        "id": 3,
        "title": "Error Rate",
        "type": "graph",
        "targets": [
          {
            "expr": "sum(rate(http_requests_total{status=~\"5..\"}[5m])) / sum(rate(http_requests_total[5m])) * 100",
            "legendFormat": "Error Rate %",
            "refId": "A"
          }
        ],
        "yAxes": [
          {
            "label": "Percentage",
            "min": 0,
            "max": 100
          }
        ],
        "gridPos": {
          "h": 8,
          "w": 12,
          "x": 0,
          "y": 8
        }
      },
      {
        "id": 4,
        "title": "Active Connections",
        "type": "graph",
        "targets": [
          {
            "expr": "sum(http_connections_active) by (service)",
            "legendFormat": "{{service}}",
            "refId": "A"
          }
        ],
        "yAxes": [
          {
            "label": "Connections",
            "min": 0
          }
        ],
        "gridPos": {
          "h": 8,
          "w": 12,
          "x": 12,
          "y": 8
        }
      }
    ],
    "templating": {
      "list": [
        {
          "name": "service",
          "type": "query",
          "query": "label_values(http_requests_total, service)",
          "multi": true,
          "includeAll": true
        }
      ]
    },
    "annotations": {
      "list": [
        {
          "name": "Deployments",
          "datasource": "Prometheus",
          "enable": true,
          "expr": "changes(process_start_time_seconds[1m]) > 0",
          "iconColor": "green",
          "step": "60"
        }
      ]
    }
  }
}
```

### **Infrastructure Monitoring Dashboard**

```json
{
  "dashboard": {
    "title": "Infrastructure Overview",
    "tags": ["infrastructure", "monitoring"],
    "panels": [
      {
        "title": "CPU Usage",
        "type": "graph",
        "targets": [
          {
            "expr": "100 - (avg by (instance) (irate(node_cpu_seconds_total{mode=\"idle\"}[5m])) * 100)",
            "legendFormat": "{{instance}}"
          }
        ],
        "yAxes": [
          {
            "label": "CPU %",
            "min": 0,
            "max": 100
          }
        ]
      },
      {
        "title": "Memory Usage",
        "type": "graph",
        "targets": [
          {
            "expr": "100 - (avg by (instance) (node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes * 100))",
            "legendFormat": "{{instance}}"
          }
        ],
        "yAxes": [
          {
            "label": "Memory %",
            "min": 0,
            "max": 100
          }
        ]
      },
      {
        "title": "Disk Usage",
        "type": "graph",
        "targets": [
          {
            "expr": "100 - (avg by (instance) (node_filesystem_avail_bytes{fstype!=\"tmpfs\"} / node_filesystem_size_bytes * 100))",
            "legendFormat": "{{instance}} - {{mountpoint}}"
          }
        ],
        "yAxes": [
          {
            "label": "Disk %",
            "min": 0,
            "max": 100
          }
        ]
      },
      {
        "title": "Network Traffic",
        "type": "graph",
        "targets": [
          {
            "expr": "sum by (instance) (rate(node_network_receive_bytes_total[5m]))",
            "legendFormat": "RX - {{instance}}"
          },
          {
            "expr": "sum by (instance) (rate(node_network_transmit_bytes_total[5m]))",
            "legendFormat": "TX - {{instance}}"
          }
        ]
      }
    ]
  }
}
```

### **Business Metrics Dashboard**

```json
{
  "dashboard": {
    "title": "Business Metrics",
    "tags": ["business", "kpi"],
    "panels": [
      {
        "title": "Revenue (Last 24h)",
        "type": "stat",
        "targets": [
          {
            "expr": "sum(revenue_total_24h)",
            "format": "table",
            "refId": "A"
          }
        ],
        "fieldConfig": {
          "defaults": {
            "unit": "currencyUSD",
            "decimals": 2
          }
        }
      },
      {
        "title": "Active Users",
        "type": "stat",
        "targets": [
          {
            "expr": "sum(active_users_5m)",
            "refId": "A"
          }
        ]
      },
      {
        "title": "Conversion Rate",
        "type": "graph",
        "targets": [
          {
            "expr": "sum(rate(conversions_total[5m])) / sum(rate(pageviews_total[5m])) * 100",
            "legendFormat": "Conversion Rate %"
          }
        ],
        "yAxes": [
          {
            "label": "Percentage",
            "min": 0,
            "max": 10
          }
        ]
      },
      {
        "title": "Revenue by Product",
        "type": "table",
        "targets": [
          {
            "expr": "sum(revenue_total_1h) by (product)",
            "format": "table",
            "refId": "A"
          }
        ],
        "transformations": [
          {
            "id": "organize",
            "options": {
              "excludeByName": {},
              "indexByName": {},
              "renameByName": {
                "product": "Product",
                "Value": "Revenue (1h)"
              }
            }
          }
        ]
      }
    ]
  }
}
```

---

## **Ejemplos en Java**

### **Custom Data Source Plugin**

```java
import org.grafana.plugins.datasource.GrafanaDataSourcePlugin;
import org.grafana.plugins.datasource.GrafanaDataSourceSettings;
import org.grafana.plugins.datasource.GrafanaQuery;
import org.grafana.plugins.datasource.GrafanaDataSource;
import org.grafana.plugins.datasource.GrafanaDataSourceResult;

public class CustomDataSource extends GrafanaDataSource {
    
    public CustomDataSource(GrafanaDataSourceSettings settings) {
        super(settings);
    }
    
    @Override
    public GrafanaDataSourceResult query(GrafanaQuery query) {
        try {
            // Parse query
            String sql = buildSqlQuery(query);
            
            // Execute query against data source
            List<Map<String, Object>> data = executeQuery(sql);
            
            // Transform to Grafana format
            return transformResults(data, query);
            
        } catch (Exception e) {
            throw new RuntimeException("Query failed", e);
        }
    }
    
    private String buildSqlQuery(GrafanaQuery query) {
        StringBuilder sql = new StringBuilder("SELECT ");
        
        // Add time column
        sql.append("timestamp, ");
        
        // Add value columns
        for (String field : query.getSelectedFields()) {
            sql.append(field).append(", ");
        }
        
        sql.append("FROM metrics ");
        sql.append("WHERE timestamp >= ? AND timestamp <= ? ");
        
        // Add filters
        for (Map<String, Object> filter : query.getFilters()) {
            sql.append("AND ").append(filter.get("key"))
               .append(" = ? ");
        }
        
        sql.append("ORDER BY timestamp");
        
        return sql.toString();
    }
    
    private List<Map<String, Object>> executeQuery(String sql) {
        // Implementation depends on your data source
        // Could be JDBC, REST API, etc.
        return Collections.emptyList();
    }
    
    private GrafanaDataSourceResult transformResults(List<Map<String, Object>> data, GrafanaQuery query) {
        List<GrafanaDataFrame> frames = new ArrayList<>();
        
        GrafanaDataFrame frame = new GrafanaDataFrame(query.getRefId());
        frame.setTimeColumn("timestamp");
        
        // Add fields
        for (String field : query.getSelectedFields()) {
            frame.addField(field, "number");
        }
        
        // Add data points
        for (Map<String, Object> row : data) {
            Object timestamp = row.get("timestamp");
            frame.addPoint(timestamp);
            
            for (String field : query.getSelectedFields()) {
                frame.addValue(field, row.get(field));
            }
        }
        
        frames.add(frame);
        return new GrafanaDataSourceResult(frames);
    }
}

// Plugin registration
public class CustomDataSourcePlugin extends GrafanaDataSourcePlugin {
    public CustomDataSourcePlugin() {
        super("custom-datasource");
    }
    
    @Override
    public Class<? extends GrafanaDataSource> getDataSourceClass() {
        return CustomDataSource.class;
    }
}
```

### **Grafana Client Integration**

```java
import org.grafana.grafana4j.GrafanaClient;
import org.grafana.grafana4j.models.*;

@Configuration
public class GrafanaClientConfig {
    
    @Value("${grafana.url:http://localhost:3000}")
    private String grafanaUrl;
    
    @Value("${grafana.api.key:}")
    private String apiKey;
    
    @Bean
    public GrafanaClient grafanaClient() {
        return new GrafanaClient(grafanaUrl, apiKey);
    }
}

@Service
public class GrafanaService {
    
    private final GrafanaClient grafanaClient;
    
    public GrafanaService(GrafanaClient grafanaClient) {
        this.grafanaClient = grafanaClient;
    }
    
    public void createDashboard(String title, String folder) {
        DashboardDTO dashboard = new DashboardDTO();
        dashboard.setTitle(title);
        dashboard.setTags(Arrays.asList("auto-generated"));
        dashboard.setRefresh("5s");
        dashboard.setTime(new DashboardTimeDTO()
            .from("now-1h")
            .to("now"));
        
        // Add panels
        List<PanelDTO> panels = new ArrayList<>();
        
        // CPU usage panel
        PanelDTO cpuPanel = new PanelDTO()
            .id(1)
            .title("CPU Usage")
            .type("graph")
            .targets(Arrays.asList(
                new PanelQueryDTO()
                    .expr("100 - (avg by (instance) (irate(node_cpu_seconds_total{mode=\"idle\"}[5m])) * 100))")
                    .legendFormat("{{instance}}")
            ))
            .gridPos(new PanelGridPosDTO().h(8).w(12).x(0).y(0));
        
        panels.add(cpuPanel);
        
        dashboard.setPanels(panels);
        
        grafanaClient.dashboards().createDashboard(dashboard, folder);
    }
    
    public void createAnnotation(String dashboardId, String text, String tags) {
        AnnotationDTO annotation = new AnnotationDTO()
            .dashboardId(Long.parseLong(dashboardId))
            .text(text)
            .tags(Arrays.asList(tags.split(",")))
            .time(System.currentTimeMillis());
        
        grafanaClient.annotations().createAnnotation(annotation);
    }
    
    public List<FolderDTO> getFolders() {
        return grafanaClient.folders().listFolders();
    }
    
    public List<DashboardSummaryDTO> getDashboards(String folder) {
        return grafanaClient.dashboards().listDashboards(folder);
    }
    
    public void updateAlertRule(String name, String query, String conditions) {
        AlertDTO alert = new AlertDTO()
            .name(name)
            .forDuration("5m")
            .conditions(Arrays.asList(conditions))
            .data(Arrays.asList(
                new AlertQueryDTO()
                    .refId("A")
                    .query(query)
            ));
        
        grafanaClient.alerts().createAlert(alert);
    }
    
    public void exportDashboard(String dashboardId) {
        DashboardDTO dashboard = grafanaClient.dashboards().getDashboard(dashboardId);
        
        // Save to file or database
        try (ObjectMapper mapper = new ObjectMapper()) {
            mapper.writerWithDefaultPrettyPrinter()
                .writeValue(new File("dashboard-export.json"), dashboard);
        } catch (IOException e) {
            throw new RuntimeException("Failed to export dashboard", e);
        }
    }
}
```

### **Custom Panel Plugin**

```java
import org.grafana.plugins.panel.GrafanaPanel;
import org.grafana.plugins.panel.GrafanaPanelOptions;
import org.grafana.plugins.panel.GrafanaPanelData;

public class BusinessKpiPanel extends GrafanaPanel {
    
    @Override
    public void init(GrafanaPanelOptions options) {
        setOptions(options);
        setWidth("100%");
        setHeight("400px");
    }
    
    @Override
    public void render(GrafanaPanelData data) {
        clear();
        
        // Create KPI display
        StringBuilder html = new StringBuilder();
        html.append("<div class='kpi-panel'>");
        
        for (QueryResult result : data.getResults()) {
            if (!result.getData().isEmpty()) {
                DataPoint point = result.getData().get(0);
                
                String value = String.valueOf(point.getValue());
                String label = result.getRefId();
                
                html.append("<div class='kpi-item'>");
                html.append("<div class='kpi-value'>").append(value).append("</div>");
                html.append("<div class='kpi-label'>").append(label).append("</div>");
                html.append("</div>");
            }
        }
        
        html.append("</div>");
        
        getElement().setInnerHTML(html.toString());
        
        // Add styling
        addStyle(".kpi-panel { display: flex; gap: 20px; }");
        addStyle(".kpi-item { text-align: center; }");
        addStyle(".kpi-value { font-size: 2em; font-weight: bold; color: #007bff; }");
        addStyle(".kpi-label { font-size: 0.9em; color: #666; }");
    }
    
    @Override
    public PanelOptions getDefaultOptions() {
        return PanelOptions.builder()
            .showTitle(true)
            .showLegend(false)
            .build();
    }
}

// Plugin registration
public class BusinessKpiPanelPlugin extends GrafanaPanelPlugin {
    public BusinessKpiPanelPlugin() {
        super("business-kpi-panel");
    }
    
    @Override
    public Class<? extends GrafanaPanel> getPanelClass() {
        return BusinessKpiPanel.class;
    }
}
```

### **Alert Management**

```java
import org.grafana.grafana4j.models.*;

@Service
public class AlertManagementService {
    
    private final GrafanaClient grafanaClient;
    
    public AlertManagementService(GrafanaClient grafanaClient) {
        this.grafanaClient = grafanaClient;
    }
    
    public void createApplicationAlert(String name, String service, String metric, double threshold) {
        // Create alert rule
        AlertRuleDTO alertRule = new AlertRuleDTO()
            .name(name)
            .forDuration("5m")
            .labels(Map.of(
                "service", service,
                "severity", "warning"
            ))
            .annotations(Map.of(
                "summary", String.format("High %s detected for %s", metric, service),
                "description", String.format("%s is above threshold: %s", metric, threshold)
            ));
        
        // Create conditions
        AlertConditionDTO condition = new AlertConditionDTO()
            .refId("A")
            .queryString(metric + " > " + threshold)
            .operator(AlertOperator.AND)
            .evaluator(new AlertEvaluatorDTO().type(AlertEvaluatorType.GT).params(threshold))
            .reducer(new AlertReducerDTO().type(AlertReducerType.AVG).params(60));
        
        alertRule.setConditions(List.of(condition));
        
        // Create target
        AlertTargetDTO target = new AlertTargetDTO()
            .refId("A")
            .dataSourceUid(getDataSourceUid("Prometheus"))
            .query(metric + " > " + threshold);
        
        alertRule.setTargets(List.of(target));
        
        grafanaClient.alertRules().createAlertRule(alertRule);
    }
    
    public void createBusinessAlert(String name, String description, List<String> notifications) {
        AlertRuleDTO alertRule = new AlertRuleDTO()
            .name(name)
            .forDuration("2m")
            .labels(Map.of(
                "type", "business",
                "severity", "critical"
            ))
            .annotations(Map.of(
                "description", description
            ));
        
        grafanaClient.alertRules().createAlertRule(alertRule);
        
        // Configure notifications
        for (String notification : notifications) {
            configureNotification(alertRule.getName(), notification);
        }
    }
    
    public void configureNotification(String alertName, String notificationChannel) {
        // Get notification policy
        NotificationPolicyDTO policy = new NotificationPolicyDTO()
            .name("default")
            .matchers(List.of(
                new MatcherDTO().label("alertname").value(alertName)
            ))
            .groupBy("alertname")
            .groupWait("10s")
            .groupInterval("10s")
            .repeatInterval("1h");
        
        grafanaClient.notifications().createPolicy(policy);
    }
    
    public List<AlertRuleDTO> getAlertRules() {
        return grafanaClient.alertRules().listAlertRules();
    }
    
    public void pauseAlertRule(String ruleId) {
        grafanaClient.alertRules().pauseAlertRule(ruleId);
    }
    
    public void resumeAlertRule(String ruleId) {
        grafanaClient.alertRules().resumeAlertRule(ruleId);
    }
    
    private String getDataSourceUid(String name) {
        List<DataSourceDTO> dataSources = grafanaClient.dataSources().listDataSources();
        return dataSources.stream()
            .filter(ds -> ds.getName().equals(name))
            .findFirst()
            .map(DataSourceDTO::getUid)
            .orElseThrow(() -> new RuntimeException("Data source not found: " + name));
    }
}
```

---

## **Ventajas y Desventajas**

### **Ventajas**

**Multi-Data Source Support**
- Soporte para Prometheus, InfluxDB, Elasticsearch, etc.
- Query unificado sobre múltiples fuentes
- Flexibilidad en elección de backend

**Rich Visualization**
- Amplia variedad de tipos de gráficos
- Paneles personalizables
- Templating y variables dinámicas

**Alerting Integrado**
- Sistema de alertas robusto
- Múltiples canales de notificación
- Alert management centralizado

**Extensibilidad**
- Plugin system potente
- Data source plugins
- Panel plugins customizados

**Collaboration Features**
- Dashboards compartibles
- User management y permissions
- Team organization

**Open Source**
- Código abierto con comunidad activa
- Sin vendor lock-in
- Costos de licenciamiento bajos

### **Desventajas**

**Complexity in Large Deployments**
- Configuración compleja para grandes instalaciones
- Performance tuning requerido
- Resource consumption significativo

**Query Language Limitations**
- No SQL nativo para algunas fuentes
- Learning curve para LogQL, PromQL
- Limitações em aggregations complexas

**Dashboard Management**
- Versioning de dashboards limitado
- Conflict resolution manual
- Backup/restore de dashboards complejo

**Enterprise Features Cost**
- Características avanzadas requieren licencia enterprise
- Distributed tracing limitado en OSS
- Advanced security features pagos

**Data Source Limitations**
- Algunos data sources tienen funcionalidades limitadas
- Performance issues con data sources lentos
- Query timeouts en datasets grandes

---

## **Buenas Prácticas**

### **1. Dashboard Design**

```json
{
  "dashboard": {
    "title": "Application Monitoring - Best Practices",
    "description": "Dashboard following Grafana best practices",
    "tags": ["application", "best-practices"],
    "refresh": "30s",
    "time": {
      "from": "now-6h",
      "to": "now"
    },
    "templating": {
      "list": [
        {
          "name": "environment",
          "type": "custom",
          "options": [
            {"text": "Production", "value": "prod", "selected": true},
            {"text": "Staging", "value": "staging", "selected": false},
            {"text": "Development", "value": "dev", "selected": false}
          ]
        }
      ]
    }
  }
}
```

### **2. Performance Optimization**

```yaml
# Query optimization
queries:
  - name: "CPU Usage"
    expr: "avg(rate(cpu_usage_seconds_total[5m])) by (instance)"
    # ✅ Good: Use rate() for counters
    # ✅ Good: Use 5m rate to reduce noise
    
  - name: "Memory Usage"
    expr: "node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes * 100"
    # ✅ Good: Direct calculation
    
  - name: "Bad Query Example"
    expr: "cpu_usage_seconds_total"  # ❌ Bad: Raw counter without rate
```

### **3. Alerting Strategy**

```yaml
# Well-defined alerting rules
groups:
- name: application.rules
  rules:
  - alert: HighErrorRate
    expr: |
      (
        sum(rate(http_requests_total{status=~"5.."}[5m])) by (service)
        /
        sum(rate(http_requests_total[5m])) by (service)
      ) > 0.05
    for: 2m
    labels:
      severity: warning
    annotations:
      summary: "High error rate detected"
      description: "Service {{ $labels.service }} has error rate > 5% for 2 minutes"

  - alert: HighLatency
    expr: |
      histogram_quantile(0.95, 
        sum(rate(http_request_duration_seconds_bucket{service="api"}[5m])) by (le)
      ) > 0.5
    for: 5m
    labels:
      severity: critical
    annotations:
      summary: "High latency detected"
      description: "API 95th percentile latency > 500ms for 5 minutes"
```

### **4. Security Configuration**

```ini
# grafana.ini security settings
[security]
admin_user = admin
admin_password = strong_password_here
secret_key = very_long_random_secret_key_here

# Cookie security
cookie_secure = true
cookie_samesite = strict

# Data source proxy whitelist
data_source_proxy_whitelist = 
  grafana.company.com
  prometheus.company.com
  loki.company.com

[users]
allow_sign_up = false
allow_org_create = false
auto_assign_org = true
auto_assign_org_role = Viewer

[auth]
login_cookie_name = grafana_user_auth
login_maximum_inactive_lifetime_duration = 
login_maximum_lifetime_duration = 
token_rotation_interval_minutes = 10
```

### **5. Backup Strategy**

```bash
#!/bin/bash
# Grafana backup script

BACKUP_DIR="/backup/grafana"
DATE=$(date +%Y%m%d_%H%M%S)

# Create backup directory
mkdir -p "$BACKUP_DIR/$DATE"

# Backup Grafana database
pg_dump -h postgres -U grafana grafana > "$BACKUP_DIR/$DATE/grafana_db.sql"

# Backup dashboards
tar -czf "$BACKUP_DIR/$DATE/dashboards.tar.gz" /var/lib/grafana/dashboards/

# Backup configuration
cp /etc/grafana/grafana.ini "$BACKUP_DIR/$DATE/"

# Backup provisioning
tar -czf "$BACKUP_DIR/$DATE/provisioning.tar.gz" /etc/grafana/provisioning/

# Backup plugins
tar -czf "$BACKUP_DIR/$DATE/plugins.tar.gz" /var/lib/grafana/plugins/

echo "Backup completed: $BACKUP_DIR/$DATE"
```

### **6. Monitoring Grafana**

```yaml
# Grafana metrics configuration
scrape_configs:
  - job_name: 'grafana'
    static_configs:
      - targets: ['grafana:3000']
    metrics_path: '/metrics'
    scrape_interval: 15s

# Useful Grafana metrics queries
queries:
  - name: "Grafana Request Rate"
    expr: "sum(rate(grafana_http_request_total[5m])) by (handler, method)"

  - name: "Grafana Query Duration"
    expr: "histogram_quantile(0.95, sum(rate(grafana_http_request_duration_seconds_bucket[5m])) by (le, handler))"

  - name: "Active Dashboards"
    expr: "grafana_dashboard_active"

  - name: "Active Users"
    expr: "grafana_session_logins_total"
```

### **7. User Management**

```yaml
# Team and user organization
teams:
  - name: "DevOps"
    permissions:
      - dashboards: read
      - dashboards: write
      - alerts: read
    users:
      - admin@grafana.company.com (Admin)
      - devops1@grafana.company.com (Editor)
      - devops2@grafana.company.com (Editor)

  - name: "Business"
    permissions:
      - dashboards: read
    users:
      - business1@grafana.company.com (Viewer)
      - business2@grafana.company.com (Viewer)

  - name: "Developers"
    permissions:
      - dashboards: read
      - dashboards: write
      - alerts: read
    users:
      - dev1@grafana.company.com (Editor)
      - dev2@grafana.company.com (Editor)
```

---

## **Referencias Oficiales**

1. **Grafana Documentation**  
   https://grafana.com/docs/grafana/latest/

2. **Grafana Dashboards**  
   https://grafana.com/docs/grafana/latest/dashboards/

3. **Grafana Alerting**  
   https://grafana.com/docs/grafana/latest/alerting/

4. **Grafana Plugins**  
   https://grafana.com/docs/grafana/latest/plugins/

5. **Grafana API**  
   https://grafana.com/docs/grafana/latest/developers/http_api/
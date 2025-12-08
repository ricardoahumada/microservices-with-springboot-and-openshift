# Modulo 7: CI/CD con Jenkins y Helm en OpenShift

## Estructura del Proyecto

```
mutualidad-platform/
├── afiliado-service/
│   ├── Jenkinsfile              # Pipeline declarativo
│   ├── Dockerfile
│   └── src/
├── notificacion-service/
│   ├── Jenkinsfile
│   └── ...
├── validacion-service/
│   ├── Jenkinsfile
│   └── ...
└── charts/
    ├── afiliado-service-chart/
    │   ├── Chart.yaml
    │   ├── values.yaml           # Valores por defecto
    │   ├── values-dev.yaml       # Valores DEV
    │   ├── values-qa.yaml        # Valores QA
    │   ├── values-prod.yaml      # Valores PROD
    │   └── templates/
    │       ├── deployment.yaml
    │       ├── service.yaml
    │       ├── configmap.yaml    # Configuracion externalizada
    │       └── route.yaml        # OpenShift Route
    └── ...
```

## Requisitos Previos

- Docker Desktop
- Kubernetes/Minikube (o OpenShift)
- Helm 3.x
- Java 11+
- Maven 3.6+
- kubectl (u oc CLI)

---

## Ejercicio 1: Jenkinsfile Declarativo (20 min)

### 1.1 Estructura del Jenkinsfile

El Jenkinsfile en `afiliado-service/Jenkinsfile` incluye:

- **agent kubernetes**: Pod template con contenedores maven, docker, helm
- **parameters**: ENVIRONMENT (dev/qa/prod), SKIP_TESTS
- **stages**: Checkout, Build & Test, Docker Build, Deploy, Verify
- **post actions**: Notificaciones success/failure

### 1.2 Revisar el Pipeline

```groovy
pipeline {
    agent {
        kubernetes {
            yaml '''
                apiVersion: v1
                kind: Pod
                spec:
                  containers:
                  - name: maven
                    image: maven:3.9-eclipse-temurin-11
                  - name: docker
                    image: docker:24-cli
                  - name: helm
                    image: alpine/helm:3.14.0
            '''
        }
    }
    
    stages {
        stage('Build & Test') { ... }
        stage('Docker Build & Push') { ... }
        stage('Deploy to OpenShift') { ... }
    }
}
```

### 1.3 Simular Ejecucion Local

Sin Jenkins, simula los pasos manualmente:

```bash
cd module7/solutions/mutualidad-platform/afiliado-service

# Build
mvn clean package -DskipTests

# Docker build
docker build -t mutualidad/afiliado-service:1.0.0-1 .

# Helm deploy (en Minikube)
cd ../charts/afiliado-service-chart
helm upgrade --install afiliado . \
    --values values-dev.yaml \
    --set image.tag=1.0.0-1
```

---

## Ejercicio 2: Helm Chart Multi-Ambiente (20 min)

### 2.1 Archivos de Values por Ambiente

Cada chart tiene valores diferenciados:

| Archivo | Replicas | Resources | PostgreSQL |
|---------|----------|-----------|------------|
| values-dev.yaml | 1 | 500m/512Mi | dev123 |
| values-qa.yaml | 2 | 1000m/1Gi | qa_secure_123 |
| values-prod.yaml | 3 | 2000m/2Gi | existingSecret |

### 2.2 Desplegar en Diferentes Ambientes

```bash
cd charts/afiliado-service-chart

# Desplegar en DEV
helm upgrade --install afiliado-dev . \
    --namespace mutualidad-dev \
    --values values-dev.yaml \
    --create-namespace

# Desplegar en QA
helm upgrade --install afiliado-qa . \
    --namespace mutualidad-qa \
    --values values-qa.yaml \
    --create-namespace

# Desplegar en PROD (con tag especifico)
helm upgrade --install afiliado-prod . \
    --namespace mutualidad-prod \
    --values values-prod.yaml \
    --set image.tag=1.0.0 \
    --create-namespace
```

### 2.3 Verificar Diferencias

```bash
# Comparar templates generados
helm template afiliado . -f values-dev.yaml > /tmp/dev.yaml
helm template afiliado . -f values-prod.yaml > /tmp/prod.yaml
diff /tmp/dev.yaml /tmp/prod.yaml
```

---

## Ejercicio 3: Rollback y Gestion de Releases (20 min)

### 3.1 Escenario de Incidente

Simular un despliegue fallido y ejecutar rollback:

```bash
# Estado inicial - version estable
helm upgrade --install afiliado . \
    --namespace mutualidad-dev \
    --values values-dev.yaml \
    --set image.tag=1.0.0

# Simular nueva version (con "bug")
helm upgrade afiliado . \
    --namespace mutualidad-dev \
    --values values-dev.yaml \
    --set image.tag=1.1.0-buggy
```

### 3.2 Detectar Problema

```bash
# Ver estado del pod
kubectl get pods -n mutualidad-dev -l app.kubernetes.io/name=afiliado-service

# Ver logs
kubectl logs -n mutualidad-dev -l app.kubernetes.io/name=afiliado-service
```

### 3.3 Ejecutar Rollback

```bash
# Ver historial de releases
helm history afiliado -n mutualidad-dev

# REVISION  STATUS      DESCRIPTION
# 1         superseded  Install complete
# 2         deployed    Upgrade complete

# Rollback a revision 1
helm rollback afiliado 1 -n mutualidad-dev

# Verificar estado
helm status afiliado -n mutualidad-dev
```

### 3.4 Verificar Recuperacion

```bash
# Confirmar version restaurada
helm get values afiliado -n mutualidad-dev

# Test de salud
kubectl port-forward svc/afiliado-afiliado-service 8081:80 -n mutualidad-dev &
curl http://localhost:8081/actuator/health
```

---

## Diferencias DEV vs QA vs PROD

| Aspecto | DEV | QA | PROD |
|---------|-----|----|----|
| Replicas | 1 | 2 | 3 |
| CPU Limit | 500m | 1000m | 2000m |
| Memory Limit | 512Mi | 1Gi | 2Gi |
| Log Level | DEBUG | INFO | WARN |
| Autoscaling | No | Si (2-4) | Si (3-10) |
| TLS | No | No | Si |

---

## Comandos Utiles

### Helm

```bash
# Listar releases
helm list -A

# Ver valores actuales
helm get values afiliado -n mutualidad-dev

# Dry-run antes de desplegar
helm upgrade --install afiliado . --dry-run --debug

# Desinstalar
helm uninstall afiliado -n mutualidad-dev
```

---

## Limpieza

```bash
# Eliminar releases
helm uninstall afiliado-dev -n mutualidad-dev
helm uninstall afiliado-qa -n mutualidad-qa

# Eliminar namespaces
kubectl delete namespace mutualidad-dev mutualidad-qa
```

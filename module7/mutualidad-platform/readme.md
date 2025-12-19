# Modulo 7: CI/CD con Jenkins y Helm en OpenShift

## Estructura del Proyecto

```
mutualidad-platform/
├── docker-compose.yml           # Jenkins local para CI/CD
├── jenkins.md                   # Guia detallada de Jenkins
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
    │       ├── configmap.yaml
    │       └── route.yaml        # OpenShift Route
    └── ...
```

## Requisitos Previos

- Docker Desktop
- Cuenta en DockerHub
- Acceso a cluster OpenShift
- oc CLI

---

## Arquitectura

```
┌─────────────────────┐         ┌──────────────┐         ┌─────────────────────────┐
│   Jenkins (Local)   │         │   DockerHub  │         │       OpenShift         │
│  docker-compose.yml │         │              │         │                         │
│                     │  push   │ ricardoahumada/│  pull  │  ┌───────────────────┐  │
│  Build -> Docker ───┼────────>│ *-service    │<────────┼──│ mutualidad-platform│  │
│                     │         │              │         │  └───────────────────┘  │
└─────────────────────┘         └──────────────┘         └─────────────────────────┘
```

**Flujo:**
1. Jenkins construye la imagen localmente
2. Push a DockerHub (`ricardoahumada/*`)
3. Helm despliega en OpenShift usando imagenes de DockerHub

---

## Configuracion Rapida

### 1. Iniciar Jenkins

```bash
cd module7/solutions/mutualidad-platform

# Iniciar Jenkins
docker-compose up -d

# Obtener password inicial
docker exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword

# Acceder a http://localhost:8080
```

### 2. Instalar Herramientas en Jenkins

```bash
docker exec -it jenkins bash

# Instalar herramientas
apt-get update && apt-get install -y docker.io
curl -sL https://mirror.openshift.com/pub/openshift-v4/clients/ocp/stable/openshift-client-linux.tar.gz | tar xz -C /usr/local/bin
curl -fsSL https://get.helm.sh/helm-v3.14.0-linux-amd64.tar.gz | tar xz -C /tmp && mv /tmp/linux-amd64/helm /usr/local/bin/

# Verificar
docker --version && oc version --client && helm version

exit
```

### 3. Configurar Credenciales en Jenkins

Ir a **Manage Jenkins > Credentials > System > Global credentials**:

| ID | Tipo | Descripcion |
|----|------|-------------|
| `dockerhub-credentials` | Username/Password | Usuario: `ricardoahumada`, Password: tu password |
| `openshift-token` | Secret text | Token de OpenShift |

### 4. Conectar a OpenShift

```bash
# Obtener token desde consola web: User menu > Copy login command
oc login --token=<tu-token> --server=<tu-server>

# Crear proyectos
oc new-project mutualidad-dev
oc new-project mutualidad-qa
oc new-project mutualidad-prod
```

### 5. Crear Pipeline en Jenkins

1. **New Item** > Nombre: `afiliado-service-pipeline` > **Pipeline**
2. En **Pipeline**:
   - Definition: **Pipeline script from SCM**
   - SCM: **Git**
   - Repository URL: URL de tu repositorio
   - Script Path: `afiliado-service/Jenkinsfile`
3. **Save** y **Build with Parameters**

---

## Imagenes en DockerHub

Los Jenkinsfiles publican en:

| Servicio | Imagen DockerHub |
|----------|------------------|
| afiliado-service | `ricardoahumada/afiliado-service` |
| notificacion-service | `ricardoahumada/notificacion-service` |
| validacion-service | `ricardoahumada/validacion-service` |

---

## Ejercicio 1: Jenkinsfile Declarativo (20 min)

### 1.1 Estructura del Jenkinsfile

El Jenkinsfile incluye:

- **parameters**: ENVIRONMENT (dev/qa/prod), SKIP_TESTS
- **stages**: Checkout, Build & Test, Docker Build & Push, Deploy to OpenShift, Verify
- **post actions**: Notificaciones success/failure

### 1.2 Flujo del Pipeline

```
Checkout -> Build Maven -> Docker Build -> Push DockerHub -> Deploy OpenShift -> Verify
```

### 1.3 Ejecutar Pipeline

1. En Jenkins, seleccionar el pipeline
2. Click **Build with Parameters**
3. Seleccionar ENVIRONMENT: `dev`
4. Click **Build**

### 1.4 Ejecucion Manual (sin Jenkins)

```bash
cd module7/solutions/mutualidad-platform/afiliado-service

# Build
mvn clean package -DskipTests

# Docker build y push
docker build -t ricardoahumada/afiliado-service:1.0.0 .
docker push ricardoahumada/afiliado-service:1.0.0

# Deploy en OpenShift
cd ../charts/afiliado-service-chart
helm upgrade --install afiliado-service . \
    --namespace mutualidad-dev \
    --values values-dev.yaml \
    --set image.tag=1.0.0
```

---

## Ejercicio 2: Helm Chart Multi-Ambiente (20 min)

### 2.1 Archivos de Values por Ambiente

| Archivo | Replicas | Resources | 
|---------|----------|-----------|
| values-dev.yaml | 1 | 500m/512Mi |
| values-qa.yaml | 2 | 1000m/1Gi |
| values-prod.yaml | 3 | 2000m/2Gi |

### 2.2 Desplegar en Diferentes Ambientes

```bash
cd charts/afiliado-service-chart

# DEV
helm upgrade --install afiliado-dev . \
    --namespace mutualidad-dev \
    --values values-dev.yaml

# QA
helm upgrade --install afiliado-qa . \
    --namespace mutualidad-qa \
    --values values-qa.yaml

# PROD
helm upgrade --install afiliado-prod . \
    --namespace mutualidad-prod \
    --values values-prod.yaml \
    --set image.tag=1.0.0
```

---

## Ejercicio 3: Rollback y Gestion de Releases (20 min)

### 3.1 Simular Despliegue Fallido

```bash
# Version estable
helm upgrade --install afiliado . \
    --namespace mutualidad-dev \
    --values values-dev.yaml \
    --set image.tag=1.0.0

# Version con bug
helm upgrade afiliado . \
    --namespace mutualidad-dev \
    --values values-dev.yaml \
    --set image.tag=1.1.0-buggy
```

### 3.2 Ejecutar Rollback

```bash
# Ver historial
helm history afiliado -n mutualidad-dev

# Rollback
helm rollback afiliado 1 -n mutualidad-dev

# Verificar
helm status afiliado -n mutualidad-dev
```

---

## Comandos Utiles

### Jenkins

```bash
# Iniciar
docker-compose up -d

# Ver logs
docker-compose logs -f jenkins

# Parar
docker-compose down
```

### OpenShift

```bash
# Login
oc login --token=<tu-token> --server=<tu-server>

# Ver proyectos
oc projects

# Ver pods
oc get pods -n mutualidad-dev

# Ver routes
oc get routes -n mutualidad-dev
```

### Helm

```bash
# Listar releases
helm list -A

# Ver valores
helm get values afiliado -n mutualidad-dev

# Desinstalar
helm uninstall afiliado -n mutualidad-dev
```

---

## Limpieza

```bash
# Eliminar releases
helm uninstall afiliado-dev -n mutualidad-dev
helm uninstall afiliado-qa -n mutualidad-qa

# Eliminar namespaces en OpenShift
oc delete project mutualidad-dev mutualidad-qa

# Parar Jenkins
docker-compose down -v
```

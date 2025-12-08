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
- Jenkins (para Ejercicio 1)

---

## Configuracion de Charts: Minikube vs OpenShift

### Minikube

#### Datos necesarios

| Dato | Como obtenerlo | Donde configurar |
|------|----------------|------------------|
| Cluster IP | `minikube ip` | No necesario (usa ClusterIP) |
| Namespace | Crear con `kubectl create namespace` | `--namespace` en helm |
| Ingress | `minikube addons enable ingress` | `values-*.yaml` -> ingress.enabled |
| NodePort | Automatico al habilitar | `values-*.yaml` -> service.type: NodePort |

#### Configuracion en values-dev.yaml para Minikube

```yaml
# Desactivar Route (es solo OpenShift)
route:
  enabled: false

# Usar NodePort o Ingress para acceso externo
service:
  type: NodePort
  nodePort: 30080  # Puerto 30000-32767

# O usar Ingress
ingress:
  enabled: true
  className: nginx
  hosts:
    - host: afiliado.local
      paths:
        - path: /
          pathType: Prefix
```

#### Comandos Minikube

```bash
# Iniciar cluster
minikube start --cpus=4 --memory=8192

# Habilitar addons necesarios
minikube addons enable ingress
minikube addons enable metrics-server

# Obtener IP del cluster
minikube ip

# Acceder a servicio NodePort
minikube service afiliado-afiliado-service -n mutualidad-dev

# Tunnel para LoadBalancer (en otra terminal)
minikube tunnel
```

### OpenShift

#### Datos necesarios

| Dato | Como obtenerlo | Donde configurar |
|------|----------------|------------------|
| API Server | `oc whoami --show-server` | Jenkinsfile -> OPENSHIFT_URL |
| Token | `oc whoami -t` | Jenkins Credentials |
| Registry | `oc get route -n openshift-image-registry` | Jenkinsfile -> REGISTRY |
| Project/Namespace | `oc new-project mutualidad-dev` | `--namespace` en helm |
| Route Host | `*.apps.<cluster-domain>` | `values-*.yaml` -> route.host |

#### Configuracion en values-prod.yaml para OpenShift

```yaml
# Habilitar Route (solo OpenShift)
route:
  enabled: true
  host: afiliado-prod.apps.cluster.example.com
  tls:
    enabled: true
    termination: edge

# Service interno (Route expone externamente)
service:
  type: ClusterIP
  port: 80

# Seguridad OpenShift
securityContext:
  runAsNonRoot: true
  # OpenShift asigna UID automaticamente
```

#### Comandos OpenShift

```bash
# Login
oc login https://api.cluster.example.com:6443 -u developer -p password

# Crear proyecto
oc new-project mutualidad-dev

# Obtener token para Jenkins
oc create sa jenkins -n mutualidad-dev
oc policy add-role-to-user edit -z jenkins -n mutualidad-dev
oc sa get-token jenkins -n mutualidad-dev

# Ver routes
oc get routes -n mutualidad-dev

# Acceder a la aplicacion
curl https://afiliado-dev.apps.cluster.example.com/actuator/health
```

---

## Jenkins en Minikube desplegando en OpenShift

Esta configuración permite ejecutar Jenkins localmente en Minikube mientras despliega las aplicaciones en un cluster OpenShift remoto.

```
┌─────────────────┐                    ┌─────────────────────────┐
│    Minikube     │                    │       OpenShift         │
│  ┌───────────┐  │   HTTPS + Token    │  ┌───────────────────┐  │
│  │  Jenkins  │──┼───────────────────▶│  │ mutualidad-platform│  │
│  └───────────┘  │   helm + oc CLI    │  └───────────────────┘  │
└─────────────────┘                    └─────────────────────────┘
```

### Paso 1: Obtener credenciales de OpenShift

```bash
# Login en OpenShift
oc login https://api.tu-cluster.example.com:6443 -u tu-usuario -p tu-password

# Crear ServiceAccount para Jenkins
oc new-project mutualidad-dev
oc create sa jenkins-deployer -n mutualidad-dev
oc policy add-role-to-user edit -z jenkins-deployer -n mutualidad-dev

# Obtener token (OpenShift 4.11+)
oc create token jenkins-deployer -n mutualidad-dev --duration=8760h

# O para versiones anteriores:
oc sa get-token jenkins-deployer -n mutualidad-dev

# Obtener URL del API Server
oc whoami --show-server
# Ejemplo: https://api.cluster.example.com:6443

# Obtener CA certificate (opcional, para TLS)
oc config view --raw -o jsonpath='{.clusters[0].cluster.certificate-authority-data}' | base64 -d > openshift-ca.crt
```

### Paso 2: Configurar credenciales en Jenkins

1. Ir a **Manage Jenkins** > **Credentials** > **System** > **Global credentials**
2. Crear:

| ID | Tipo | Valor |
|----|------|-------|
| `openshift-token` | **Secret text** | Token obtenido en Paso 1 |
| `openshift-ca` | **Secret file** | Archivo openshift-ca.crt (opcional) |

### Paso 3: Instalar oc CLI en Jenkins

El contenedor helm del Jenkinsfile necesita `oc`. Actualizar el pod template:

```groovy
// En Jenkinsfile, actualizar el contenedor helm:
- name: helm
  image: alpine/helm:3.14.0
  command: ['cat']
  tty: true
```

Cambiar a imagen con oc + helm:

```groovy
- name: deploy
  image: quay.io/openshift/origin-cli:4.14
  command: ['cat']
  tty: true
```

O instalar oc dinámicamente en el stage:

```groovy
stage('Deploy to OpenShift') {
    steps {
        container('helm') {
            withCredentials([string(credentialsId: 'openshift-token', variable: 'OC_TOKEN')]) {
                sh '''
                    # Instalar oc CLI
                    curl -sL https://mirror.openshift.com/pub/openshift-v4/clients/ocp/stable/openshift-client-linux.tar.gz | tar xz
                    chmod +x oc kubectl
                    
                    # Login en OpenShift
                    ./oc login https://api.tu-cluster.example.com:6443 \
                        --token=$OC_TOKEN \
                        --insecure-skip-tls-verify=true
                    
                    # Verificar conexion
                    ./oc whoami
                    ./oc project mutualidad-dev
                    
                    # Desplegar con Helm
                    helm upgrade --install afiliado-service ./charts/afiliado-service-chart \
                        --namespace mutualidad-dev \
                        --values ./charts/afiliado-service-chart/values-dev.yaml \
                        --set image.tag=${BUILD_NUMBER}
                '''
            }
        }
    }
}
```

### Paso 4: Configurar Registry de OpenShift

Para push de imágenes al registry interno de OpenShift:

```bash
# Obtener ruta del registry
oc get route default-route -n openshift-image-registry -o jsonpath='{.spec.host}'
# Ejemplo: default-route-openshift-image-registry.apps.cluster.example.com

# Crear secret para pull desde OpenShift
oc create secret docker-registry regcred \
    --docker-server=default-route-openshift-image-registry.apps.cluster.example.com \
    --docker-username=jenkins-deployer \
    --docker-password=$(oc create token jenkins-deployer -n mutualidad-dev) \
    -n mutualidad-dev

# Vincular al ServiceAccount default
oc secrets link default regcred --for=pull -n mutualidad-dev
```

En Jenkinsfile, usar este registry:

```groovy
environment {
    REGISTRY = 'default-route-openshift-image-registry.apps.cluster.example.com'
    IMAGE_NAME = "${REGISTRY}/mutualidad-dev/afiliado-service"
}
```

### Paso 5: Verificar conectividad

Desde un pod en Minikube, verificar que puede alcanzar OpenShift:

```bash
# Crear pod de prueba
kubectl run test-oc --rm -it --image=quay.io/openshift/origin-cli:4.14 -- /bin/bash

# Dentro del pod:
oc login https://api.tu-cluster.example.com:6443 --token=<tu-token> --insecure-skip-tls-verify
oc get projects
exit
```

---

## Ejercicio 1: Jenkinsfile Declarativo (20 min)

> **Nota**: Este ejercicio REQUIERE Jenkins instalado y configurado.

### 1.1 Estructura del Jenkinsfile

El Jenkinsfile en `afiliado-service/Jenkinsfile` incluye:

- **agent kubernetes**: Pod template con contenedores maven, docker, helm
- **parameters**: ENVIRONMENT (dev/qa/prod), SKIP_TESTS
- **stages**: Checkout, Build & Test, Docker Build, Deploy, Verify
- **post actions**: Notificaciones success/failure

### 1.2 Configurar Jenkins para el Pipeline

#### Paso 1: Crear Credenciales en Jenkins

1. Ir a **Manage Jenkins** > **Credentials** > **System** > **Global credentials**
2. Crear las siguientes credenciales:

| ID | Tipo | Descripcion |
|----|------|-------------|
| `docker-registry-creds` | Username/Password | Credenciales de quay.io o Docker Hub |
| `openshift-token` | Secret text | Token de OpenShift (`oc sa get-token jenkins`) |
| `kubeconfig` | Secret file | Archivo ~/.kube/config para Minikube |

#### Paso 2: Crear Pipeline Job

1. **New Item** > Nombre: `afiliado-service-pipeline` > **Pipeline**
2. En **Pipeline**:
   - Definition: **Pipeline script from SCM**
   - SCM: **Git**
   - Repository URL: URL de tu repositorio
   - Script Path: `afiliado-service/Jenkinsfile`
3. Guardar y ejecutar **Build Now**

### 1.3 Revisar el Pipeline

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

### 1.4 Simular Ejecucion Local (sin Jenkins)

Si no tienes Jenkins, simula los pasos manualmente:

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

> **Nota**: Este ejercicio NO requiere Jenkins. Solo necesitas Helm y kubectl/oc.

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

> **Nota**: Este ejercicio NO requiere Jenkins. Solo necesitas Helm y kubectl/oc.

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

### Instalar Jenkins

#### Opcion 1: Docker (Desarrollo Local)

```bash
# Crear volumen para persistencia
docker volume create jenkins_home

# Ejecutar Jenkins con Docker-in-Docker
docker run -d \
    --name jenkins \
    -p 8080:8080 \
    -p 50000:50000 \
    -v jenkins_home:/var/jenkins_home \
    -v /var/run/docker.sock:/var/run/docker.sock \
    jenkins/jenkins:lts

# Obtener password inicial
docker exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword

# Acceder a http://localhost:8080
```

#### Opcion 2: Helm en Kubernetes/Minikube

```bash
# Agregar repositorio
helm repo add jenkins https://charts.jenkins.io
helm repo update

# Instalar Jenkins
helm install jenkins jenkins/jenkins \
    --namespace jenkins \
    --create-namespace \
    --set controller.serviceType=NodePort

# Obtener password admin
kubectl exec -n jenkins svc/jenkins -c jenkins -- cat /run/secrets/additional/chart-admin-password

# Acceder (Minikube)
minikube service jenkins -n jenkins
```

#### Opcion 3: OpenShift (OperatorHub)

```bash
# Desde consola web:
# 1. OperatorHub > Buscar "Jenkins"
# 2. Instalar Jenkins Operator
# 3. Crear instancia de Jenkins

# O via CLI:
oc new-project jenkins
oc new-app jenkins-ephemeral  # Template basico
oc get route jenkins -n jenkins
```

### Configurar Jenkins para Kubernetes

Despues de instalar, configura el plugin de Kubernetes:

1. **Manage Jenkins** > **Manage Nodes and Clouds** > **Configure Clouds**
2. **Add a new cloud** > **Kubernetes**
3. Configurar:
   - Kubernetes URL: `https://kubernetes.default.svc` (interno) o URL externa
   - Credentials: ServiceAccount token o kubeconfig
   - Jenkins URL: `http://jenkins.jenkins.svc:8080`

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

### Minikube

```bash
# Iniciar con recursos adecuados
minikube start --cpus=4 --memory=8192 --driver=docker

# Dashboard
minikube dashboard

# IP del cluster
minikube ip

# Acceder a servicio
minikube service <nombre-servicio> -n <namespace>
```

### OpenShift

```bash
# Login
oc login <api-url> -u <user> -p <password>

# Ver proyectos
oc projects

# Cambiar proyecto
oc project mutualidad-dev

# Ver pods
oc get pods

# Ver routes
oc get routes
```

---

## Resumen de Requisitos por Ejercicio

| Ejercicio | Jenkins | Helm | kubectl/oc | Docker |
|-----------|---------|------|------------|--------|
| 1. Jenkinsfile Declarativo | **SI** | SI | SI | SI |
| 2. Helm Multi-Ambiente | NO | **SI** | SI | NO* |
| 3. Rollback y Releases | NO | **SI** | SI | NO* |

*Docker solo necesario si quieres construir imagenes nuevas. Puedes usar imagenes publicas para practicar.

---

## Limpieza

```bash
# Eliminar releases
helm uninstall afiliado-dev -n mutualidad-dev
helm uninstall afiliado-qa -n mutualidad-qa

# Eliminar namespaces
kubectl delete namespace mutualidad-dev mutualidad-qa

# Parar Jenkins (Docker)
docker stop jenkins && docker rm jenkins

# Eliminar Jenkins (Helm)
helm uninstall jenkins -n jenkins
```

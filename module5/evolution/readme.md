# Modulo 5: Contenerizacion y Empaquetado con Helm

## Estructura del Proyecto

```
mutualidad-platform/
├── docker-compose.yml              # Kafka + Zookeeper (desarrollo local)
├── afiliado-service/               # Producer (puerto 8081)
│   ├── Dockerfile
│   ├── .dockerignore
│   └── src/
├── notificacion-service/           # Consumer (puerto 8082)
│   ├── Dockerfile
│   ├── .dockerignore
│   └── src/
├── validacion-service/             # Consumer con DLQ (puerto 8083)
│   ├── Dockerfile
│   ├── .dockerignore
│   └── src/
└── charts/
    ├── afiliado-service-chart/     # Chart individual
    ├── notificacion-service-chart/ # Chart individual
    ├── validacion-service-chart/   # Chart individual
    └── mutualidad-platform-chart/  # Umbrella chart
```

## Requisitos Previos

- Docker Desktop
- kubectl
- Helm 3.x
- Minikube o Docker Desktop Kubernetes
- Java 11+
- Maven 3.6+

---

## Ejercicio 1: Dockerfile Multi-Stage (40 min)

### 1.1 Dockerfile Basico (15 min)

Genera el Dockerfile de `afiliado-service/Dockerfile`:

```dockerfile
# Etapa 1: Build
FROM maven:3.9-eclipse-temurin-11-alpine AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

# Etapa 2: Runtime
FROM eclipse-temurin:11-jre-alpine
WORKDIR /app
...
```

### 1.2 Construir Imagenes Docker (25 min)

```bash
cd module5/solutions/mutualidad-platform

# Construir afiliado-service
cd afiliado-service
docker build -t mutualidad/afiliado-service:1.0.0 .
cd ..

# Construir notificacion-service
cd notificacion-service
docker build -t mutualidad/notificacion-service:1.0.0 .
cd ..

# Construir validacion-service
cd validacion-service
docker build -t mutualidad/validacion-service:1.0.0 .
cd ..
```

### Verificar Imagenes

```bash
docker images | grep mutualidad
```

Resultado esperado:
```
mutualidad/afiliado-service      1.0.0   abc123   ~200MB
mutualidad/notificacion-service  1.0.0   def456   ~200MB
mutualidad/validacion-service    1.0.0   ghi789   ~200MB
```

### Probar Contenedor Localmente

```bash
# Levantar Kafka primero
docker compose up -d

# Ejecutar afiliado-service
docker run -d --name afiliado-test \
  --network mutualidad-platform_default \
  -p 8081:8081 \
  -e SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:29092 \
  mutualidad/afiliado-service:1.0.0

# Verificar logs
docker logs -f afiliado-test
```

---

## Ejercicio 2: Chart de Helm Basico (40 min)

### 2.1 Estructura del Chart (20 min)

Revisar la estructura de `charts/afiliado-service-chart/`:

```
afiliado-service-chart/
├── Chart.yaml          # Metadatos
├── values.yaml         # Valores por defecto
└── templates/
    ├── _helpers.tpl    # Funciones helper
    ├── deployment.yaml # Deployment de K8s
    └── service.yaml    # Service de K8s
```

### 2.2 Validar Charts (20 min)

```bash
cd charts

# instalar dependencias
helm dependency build afiliado-service-chart
helm dependency build notificacion-service-chart
helm dependency build validacion-service-chart

# Validar sintaxis
helm lint afiliado-service-chart
helm lint notificacion-service-chart
helm lint validacion-service-chart
```

Resultado esperado:
```
==> Linting afiliado-service-chart
[INFO] Chart.yaml: icon is recommended
1 chart(s) linted, 0 chart(s) failed
```

### Generar Templates (dry-run)

```bash
helm template my-afiliado afiliado-service-chart
```

---

## Ejercicio 3: Despliegue con Helm (40 min)

### 3.1 Preparar Cluster Kubernetes

```bash
# Iniciar Minikube (si no esta corriendo)
minikube start

# O usar Docker Desktop Kubernetes
# (habilitar en Docker Desktop > Settings > Kubernetes)

# Verificar conexion
kubectl cluster-info
```

### 3.2 Cargar Imagenes en Minikube

```bash
# Si usas Minikube, cargar imagenes locales
minikube image load mutualidad/afiliado-service:1.0.0
minikube image load mutualidad/notificacion-service:1.0.0
minikube image load mutualidad/validacion-service:1.0.0
```

### 3.3 Desplegar Charts Individuales

```bash
cd charts

# Actualizar dependencias (PostgreSQL)
helm dependency update afiliado-service-chart

# Instalar afiliado-service
helm install afiliado afiliado-service-chart

# Verificar pods
kubectl get pods -l app.kubernetes.io/name=afiliado-service
kubectl get pods
```

Resultado esperado:
```
NAME                               READY   STATUS    RESTARTS   AGE
afiliado-afiliado-service-xxx      1/1     Running   0          2m
afiliado-afiliado-service-yyy      1/1     Running   0          2m
afiliado-postgresql-0              1/1     Running   0          2m
```

### 3.4 Verificar Servicios

```bash
kubectl get svc
```

Resultado esperado:
```
NAME                        TYPE        CLUSTER-IP      PORT(S)
afiliado-afiliado-service   ClusterIP   10.96.xxx.xxx   80/TCP
afiliado-postgresql         ClusterIP   10.96.xxx.xxx   5432/TCP
```

### 3.5 Probar Conectividad

```bash
# Port-forward al servicio
kubectl port-forward svc/afiliado-afiliado-service 8081:80

# En otra terminal
curl http://localhost:8081/actuator/health
```

Resultado esperado:
```json
{
  "status": "UP"
}
```

---

## Ejercicio 4: Umbrella Chart (Opcional)

### 4.1 Desplegar Plataforma Completa

```bash
cd charts/mutualidad-platform-chart

# Actualizar dependencias
helm dependency update

# Instalar plataforma completa
helm install mutualidad . --namespace mutualidad --create-namespace
```

### 4.2 Verificar Despliegue

```bash
kubectl get all -n mutualidad
```

Resultado esperado:
```
NAME                                              READY   STATUS
pod/mutualidad-afiliado-service-xxx               1/1     Running
pod/mutualidad-notificacion-service-xxx           1/1     Running
pod/mutualidad-validacion-service-xxx             1/1     Running
pod/mutualidad-kafka-0                            1/1     Running
pod/mutualidad-postgresql-0                       1/1     Running
```

---

## Ejercicio 5: Upgrade y Rollback

### 5.1 Actualizar Release

```bash
# Cambiar replicas a 3
helm upgrade afiliado afiliado-service-chart --set replicaCount=3

# Verificar
kubectl get pods -l app.kubernetes.io/name=afiliado-service
```

### 5.2 Ver Historial

```bash
helm history afiliado
```

Resultado esperado:
```
REVISION  STATUS      DESCRIPTION
1         superseded  Install complete
2         deployed    Upgrade complete
```

### 5.3 Rollback

```bash
# Volver a revision 1
helm rollback afiliado 1

# Verificar
kubectl get pods -l app.kubernetes.io/name=afiliado-service
```

---

## Comandos Utiles

### Docker

```bash
# Listar imagenes
docker images | grep mutualidad

# Ver logs de contenedor
docker logs -f <container-id>

# Eliminar imagenes
docker rmi mutualidad/afiliado-service:1.0.0
```

### Helm

```bash
# Listar releases
helm list

# Ver valores de un release
helm get values afiliado

# Ver manifests generados
helm get manifest afiliado

# Desinstalar
helm uninstall afiliado
```

### Kubectl

```bash
# Ver pods
kubectl get pods

# Describir pod
kubectl describe pod <pod-name>

# Ver logs
kubectl logs -f <pod-name>

# Ejecutar shell en pod
kubectl exec -it <pod-name> -- /bin/sh
```

---

## Limpieza

```bash
# Eliminar releases de Helm
helm uninstall afiliado
helm uninstall mutualidad -n mutualidad

# Eliminar namespace
kubectl delete namespace mutualidad

# Detener Docker Compose
docker-compose down -v

# Eliminar imagenes Docker
docker rmi mutualidad/afiliado-service:1.0.0
docker rmi mutualidad/notificacion-service:1.0.0
docker rmi mutualidad/validacion-service:1.0.0
```

---

## Troubleshooting

### Pod en estado ImagePullBackOff

```bash
# Diagnosticar el problema
kubectl describe pod <pod-name>
# Revisar seccion "Events" al final para ver el error exacto
```

**Para imagenes locales (mutualidad/*):**
```bash
# Verificar que la imagen existe localmente
docker images | grep mutualidad

# Si usas Minikube, cargar imagen
minikube image load mutualidad/afiliado-service:1.0.0
```

**Para imagenes de Bitnami (postgresql, kafka):**

El error suele ser por rate limit de Docker Hub. Soluciones:

```bash
# Opcion 1: Descargar manualmente en Minikube
minikube ssh
docker pull bitnami/postgresql:15.4.0
exit

# Opcion 2: Especificar version en values.yaml
# Editar charts/afiliado-service-chart/values.yaml:
postgresql:
  image:
    tag: "15.4.0"

# Opcion 3: Usar pull secret de Docker Hub
kubectl create secret docker-registry dockerhub-secret \
  --docker-server=https://index.docker.io/v1/ \
  --docker-username=<tu-usuario> \
  --docker-password=<tu-token>
```

### Pod en estado CrashLoopBackOff

```bash
# Ver logs del pod
kubectl logs <pod-name>

# Describir pod para ver eventos
kubectl describe pod <pod-name>
```

### Helm install falla

```bash
# Validar chart
helm lint <chart-path>

# Ver templates generados
helm template <release-name> <chart-path> --debug
```

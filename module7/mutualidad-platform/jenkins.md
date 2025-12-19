# Jenkins - Guia de Configuracion y Uso

## 1. Iniciar Jenkins con Docker Compose

```bash
cd module7/solutions/mutualidad-platform

# Iniciar Jenkins
docker-compose up -d

# Ver logs (esperar a que inicie completamente)
docker-compose logs -f jenkins

# Obtener password inicial de admin
docker exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword
```

Acceder a **http://localhost:8080** e introducir el password inicial.

---

## 2. Configuracion Inicial de Jenkins

### 2.1 Setup Wizard

1. Seleccionar **Install suggested plugins**
2. Crear usuario administrador
3. Configurar URL de Jenkins: `http://localhost:8080`

### 2.2 Instalar Plugins Adicionales

Ir a **Manage Jenkins > Plugins > Available plugins** e instalar:

- **Pipeline**
- **Pipeline Utility Steps** (para leer pom.xml)
- **Git**

Reiniciar Jenkins despues de instalar.

### 2.3 Instalar Herramientas en el Contenedor

```bash
# Entrar al contenedor Jenkins
docker exec -it jenkins bash

# Instalar Docker CLI y Maven
apt-get update && apt-get install -y docker.io maven

# Instalar oc CLI (OpenShift)
curl -sL https://mirror.openshift.com/pub/openshift-v4/clients/ocp/stable/openshift-client-linux.tar.gz | tar xz -C /usr/local/bin

# Instalar Helm
curl -fsSL https://get.helm.sh/helm-v3.14.0-linux-amd64.tar.gz | tar xz -C /tmp
mv /tmp/linux-amd64/helm /usr/local/bin/

# Instalar kubectl
curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
chmod +x kubectl && mv kubectl /usr/local/bin/

# Verificar instalaciones
docker --version
mvn --version
oc version --client
helm version
kubectl version --client

exit
```

### 2.4 Configurar Credenciales

Ir a **Manage Jenkins > Credentials > System > Global credentials > Add Credentials**

| ID | Tipo | Valor |
|----|------|-------|
| `dockerhub-credentials` | Username with password | Usuario: `ricardoahumada`, Password: tu password de DockerHub |
| `openshift-token` | Secret text | Token de OpenShift (obtener con `oc whoami -t`) |

### 2.5 Configurar Variable de Entorno OPENSHIFT_SERVER

Ir a **Manage Jenkins > System > Global properties > Environment variables**:

| Nombre | Valor |
|--------|-------|
| `OPENSHIFT_SERVER` | `https://api.tu-cluster.example.com:6443` |

### 2.6 Configurar Conexion a OpenShift

Antes de ejecutar pipelines, autenticar Jenkins con OpenShift:

```bash
docker exec -it jenkins bash

# Login en OpenShift
oc login --token=<tu-token> --server=<tu-server>

# Verificar conexion
oc whoami
oc get projects

exit
```

---

## 3. Crear y Ejecutar Pipelines

### 3.1 Crear Pipeline Job

1. En Jenkins, click **New Item**
2. Nombre: `afiliado-service-pipeline`
3. Tipo: **Pipeline**
4. Click **OK**

### 3.2 Configurar Pipeline

En la seccion **Pipeline**:

**Definition**: Pipeline script from SCM

| Campo | Valor |
|-------|-------|
| SCM | Git |
| Repository URL | URL de tu repositorio Git |
| Branch | `*/main` o `*/master` |
| Script Path | `afiliado-service/Jenkinsfile` |

Click **Save**

### 3.3 Ejecutar Pipeline

1. Click en **Build with Parameters**
2. Seleccionar:
   - **ENVIRONMENT**: `dev`, `qa` o `prod`
   - **SKIP_TESTS**: marcar si deseas saltar tests
3. Click **Build**

### 3.4 Repetir para Otros Servicios

Crear pipelines adicionales:

| Pipeline Name | Script Path |
|---------------|-------------|
| `notificacion-service-pipeline` | `notificacion-service/Jenkinsfile` |
| `validacion-service-pipeline` | `validacion-service/Jenkinsfile` |

---

## 4. Flujo del Pipeline

Cada Jenkinsfile ejecuta:

```
Checkout -> Build & Test -> Docker Build -> Push DockerHub -> Deploy OpenShift -> Verify
```

1. **Checkout**: Clona el repositorio
2. **Build & Test**: Compila con Maven
3. **Docker Build**: Construye imagen `ricardoahumada/<service>:<version>`
4. **Push DockerHub**: Publica imagen en DockerHub
5. **Deploy OpenShift**: Despliega con Helm en namespace `mutualidad-<env>`
6. **Verify**: Verifica estado del deployment

---

## 5. Comandos Utiles

```bash
# Ver estado de Jenkins
docker-compose ps

# Ver logs
docker-compose logs -f jenkins

# Reiniciar Jenkins
docker-compose restart jenkins

# Parar Jenkins
docker-compose down

# Parar y eliminar volumenes
docker-compose down -v
```

---

## 6. Troubleshooting

### Error: Cannot connect to Docker daemon

```bash
# Verificar permisos del socket
docker exec -it jenkins bash
ls -la /var/run/docker.sock

# Si es necesario, ajustar permisos
chmod 666 /var/run/docker.sock
```

### Error: oc command not found

```bash
# Reinstalar oc dentro del contenedor
docker exec -it jenkins bash
curl -sL https://mirror.openshift.com/pub/openshift-v4/clients/ocp/stable/openshift-client-linux.tar.gz | tar xz -C /usr/local/bin
```

### Error: Unauthorized en DockerHub

Verificar que las credenciales `dockerhub-credentials` estan correctamente configuradas en Jenkins.

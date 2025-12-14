# OpenShift - Resumen Educativo

OpenShift es una plataforma de contenedores e infraestructura como servicio (IaaS) desarrollada por Red Hat, construida sobre Kubernetes. Proporciona una plataforma completa para desarrollo, despliegue y gestión de aplicaciones contenedorizadas, con herramientas integradas para CI/CD, monitoreo y escalado automático.

## **¿Qué es OpenShift?**

OpenShift es una plataforma de containers-as-a-service (CaaS) que combina la flexibilidad de Kubernetes con herramientas empresariales de desarrollo y operaciones. Ofrece una experiencia DevOps completa con integración nativa de CI/CD, gestión de código fuente, automatización y herramientas de monitoreo.

### Características Principales

- **Kubernetes nativo**: Basado en Kubernetes con funcionalidades extendidas
- **CI/CD integrado**: Herramientas nativas para integración y entrega continua
- **Developer Console**: Interfaz web intuitiva para desarrolladores
- **Source-to-Image (S2I)**: Construcción automática desde código fuente
- **Template system**: Plantillas reutilizables para aplicaciones
- **Multi-tenancy**: Aislamiento y seguridad a nivel empresarial

## **Conceptos Clave**

### Pods
Los **pods** son la unidad mínima de despliegue en OpenShift:
- Contienen uno o más contenedores
- Comparten red y almacenamiento
- Tienen生命周期 compartido
- Se crean, escalan y eliminan como unidad

### Services
Los **services** proporcionan acceso estable a pods:
- Load balancing automático
- Descubrimiento de servicios
- Múltiples tipos: ClusterIP, NodePort, LoadBalancer
- Configuración de ports y protocols

### Routes
Las **routes** exponen servicios al exterior:
- Equivalentes a Ingress en Kubernetes
- Gestión de certificados SSL/TLS
- Balanceo de carga L7
- Políticas de seguridad

### BuildConfigs y ImageStreams
- **BuildConfigs**: Definen cómo construir aplicaciones
- **ImageStreams**: Gestión de versiones de imágenes
- **Triggers**: Construcción automática por eventos
- **Webhooks**: Integración con sistemas externos

### Deployments y ReplicaSets
- **Deployments**: Gestión declarativa de aplicaciones
- **ReplicaSets**: Garantiza número de réplicas
- **Rollback**: Reversión automática a versiones anteriores
- **Scaling**: Escalado manual y automático

### Projects y Namespaces
- **Projects**: Espacios de trabajo para equipos
- **Namespaces**: Aislamiento de recursos
- **RBAC**: Control de acceso basado en roles
- **Quotas**: Límites de recursos por proyecto

## **Casos de Uso**

### 1. Aplicaciones Microservicios
- **Despliegue distribuido**: Múltiples servicios independientes
- **Escalado granular**: Escalado por servicio individual
- **Gestión de dependencias**: Service mesh integrado
- **Comunicación entre servicios**: Routes y services

### 2. CI/CD Nativo
- **Source-to-Image**: Construcción automática desde código
- **Pipeline as Code**: Jenkins integrado nativamente
- **GitOps**: Gestión declarativa desde repositorio
- **Automatización completa**: Desde commit hasta producción

### 3. Aplicaciones Empresariales
- **Multi-tenancy**: Aislamiento por departamentos
- **Compliance**: Políticas de seguridad integradas
- **Monitoreo**: Prometheus y Grafana integrados
- **Logging**: ELK stack integrado

### 4. DevOps y SRE
- **Infraestructura como Código**: YAML templates
- **Automatización**: Scripts y webhooks
- **Observabilidad**: Métricas, logs y tracing
- **SLO/SLI**: Gestión de objetivos de nivel de servicio

## **Configuración**

### Instalación Básica

#### Instalación con Red Hat Subscription
```bash
# Instalar herramientas de OpenShift
subscription-manager repos --enable=rhel-7-server-ose-4.9-rpms
yum install -y openshift-origin-client-tools

# Instalar con Ansible
ansible-playbook -i inventory/hosts \
  /usr/share/ansible/openshift-ansible/playbooks/prerequisites.yml

# Configurar cluster
oc cluster up --public-hostname=openshift.empresa.com
```

#### Instalación con Minishift (Desarrollo)
```bash
# Instalar Minishift
curl -L https://github.com/minishift/minishift/releases/download/v1.34.3/minishift-1.34.3-linux-amd64.tgz | tar xz
sudo mv minishift-1.34.3-linux-amd64/minishift /usr/local/bin/

# Iniciar cluster local
minishift start --memory=4096 --cpus=2
```

### Configuración de Proyectos

#### Creación de Proyecto
```bash
# Crear proyecto
oc new-project mi-aplicacion-dev --display-name="Mi Aplicación Desarrollo"

# Configurar permisos
oc policy add-role-to-user edit developer -n mi-aplicacion-dev
oc policy add-role-to-user view developer -n mi-aplicacion-dev

# Configurar límites de recursos
oc create -f - <<EOF
apiVersion: v1
kind: ResourceQuota
metadata:
  name: mi-aplicacion-quota
spec:
  hard:
    requests.cpu: "2"
    requests.memory: 4Gi
    limits.cpu: "4"
    limits.memory: 8Gi
    pods: "10"
EOF
```

### Configuración de Builds

#### BuildConfig con S2I
```yaml
apiVersion: build.openshift.io/v1
kind: BuildConfig
metadata:
  name: java-app-build
  labels:
    app: java-app
spec:
  source:
    type: Git
    git:
      uri: https://github.com/empresa/java-app.git
      ref: main
    contextDir: /
  strategy:
    type: Source
    sourceStrategy:
      from:
        kind: ImageStreamTag
        name: java:11
        namespace: openshift
      env:
        - name: MAVEN_CUSTOM_OPTS
          value: "-Dmaven.repo.local=/tmp/artifacts/m2"
        - name: MAVEN_OPTS
          value: "-Xmx512m -XX:MaxPermSize=256m"
  output:
    to:
      kind: ImageStreamTag
      name: java-app:latest
  triggers:
    - type: ConfigChange
    - type: ImageChange
      imageChange: {}
    - type: GitHub
      github:
        secret: my-github-webhook-secret
  postCommit:
    script: mvn test
```

#### BuildConfig con Docker
```yaml
apiVersion: build.openshift.io/v1
kind: BuildConfig
metadata:
  name: docker-app-build
spec:
  source:
    type: Git
    git:
      uri: https://github.com/empresa/docker-app.git
  strategy:
    type: Docker
    dockerStrategy:
      dockerfilePath: Dockerfile
      buildArgs:
        - name: VERSION
          value: "1.0.0"
  output:
    to:
      kind: ImageStreamTag
      name: docker-app:latest
  triggers:
    - type: ConfigChange
```

### Configuración de Deployments

#### DeploymentConfig Básico
```yaml
apiVersion: apps.openshift.io/v1
kind: DeploymentConfig
metadata:
  name: java-app
spec:
  replicas: 3
  selector:
    app: java-app
  template:
    metadata:
      labels:
        app: java-app
    spec:
      containers:
      - name: java-app
        image: java-app:latest
        ports:
        - containerPort: 8080
          protocol: TCP
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
        - name: DATABASE_URL
          valueFrom:
            secretKeyRef:
              name: db-credentials
              key: url
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "500m"
        livenessProbe:
          httpGet:
            path: /health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /ready
            port: 8080
          initialDelaySeconds: 5
          periodSeconds: 5
  triggers:
  - type: ConfigChange
  - type: ImageChange
    imageChangeParams:
      automatic: true
      containerNames:
      - java-app
      from:
        kind: ImageStreamTag
        name: java-app:latest
```

### Configuración de Services y Routes

#### Service
```yaml
apiVersion: v1
kind: Service
metadata:
  name: java-app-service
spec:
  selector:
    app: java-app
  ports:
  - name: http
    port: 80
    targetPort: 8080
    protocol: TCP
  type: ClusterIP
```

#### Route con TLS
```yaml
apiVersion: route.openshift.io/v1
kind: Route
metadata:
  name: java-app-route
spec:
  host: java-app.empresa.com
  to:
    kind: Service
    name: java-app-service
    weight: 100
  port:
    targetPort: 8080
  wildcardPolicy: None
  tls:
    termination: reencrypt
    insecureEdgeTerminationPolicy: Redirect
```

## **Ejemplos con Java**

### Aplicación Spring Boot en OpenShift

#### Dockerfile para Spring Boot
```dockerfile
FROM openjdk:11-jre-slim

WORKDIR /app

# Copiar JAR
COPY target/myapp-*.jar app.jar

# Variables de entorno
ENV JAVA_OPTS="-Xmx512m -XX:+UseG1GC"

# Exponer puerto
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Comando de inicio
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

#### Template de OpenShift
```yaml
apiVersion: template.openshift.io/v1
kind: Template
metadata:
  name: spring-boot-app
  annotations:
    description: "Template for Spring Boot application"
    tags: "java,springboot,microservice"
objects:
- apiVersion: v1
  kind: ImageStream
  metadata:
    name: spring-boot-app
- apiVersion: build.openshift.io/v1
  kind: BuildConfig
  metadata:
    name: spring-boot-app
  spec:
    source:
      type: Git
      git:
        uri: ${GIT_REPO}
        ref: ${GIT_REF}
    strategy:
      type: Source
      sourceStrategy:
        from:
          kind: ImageStreamTag
          name: java:11
    output:
      to:
        kind: ImageStreamTag
        name: spring-boot-app:latest
- apiVersion: apps.openshift.io/v1
  kind: DeploymentConfig
  metadata:
    name: spring-boot-app
  spec:
    replicas: 1
    selector:
      app: spring-boot-app
    template:
      metadata:
        labels:
          app: spring-boot-app
      spec:
        containers:
        - name: spring-boot-app
          image: spring-boot-app:latest
          ports:
          - containerPort: 8080
          env:
          - name: SPRING_PROFILES_ACTIVE
            value: ${SPRING_PROFILE}
          - name: DATABASE_URL
            valueFrom:
              secretKeyRef:
                name: ${SECRET_NAME}
                key: url
          resources:
            requests:
              memory: "512Mi"
              cpu: "250m"
            limits:
              memory: "1Gi"
              cpu: "500m"
    triggers:
    - type: ConfigChange
- apiVersion: v1
  kind: Service
  metadata:
    name: spring-boot-app
  spec:
    selector:
      app: spring-boot-app
    ports:
    - port: 80
      targetPort: 8080
    type: ClusterIP
- apiVersion: route.openshift.io/v1
  kind: Route
  metadata:
    name: spring-boot-app
  spec:
    host: ${HOSTNAME}
    to:
      kind: Service
      name: spring-boot-app
    tls:
      termination: edge
parameters:
- name: GIT_REPO
  displayName: Git Repository
  value: https://github.com/empresa/spring-boot-app.git
- name: GIT_REF
  displayName: Git Reference
  value: main
- name: SPRING_PROFILE
  displayName: Spring Profile
  value: production
- name: SECRET_NAME
  displayName: Database Secret Name
  value: db-credentials
- name: HOSTNAME
  displayName: Application Hostname
  value: app.empresa.com
```

### Pipeline CI/CD con Jenkins en OpenShift

#### Jenkinsfile para OpenShift
```groovy
pipeline {
    agent {
        kubernetes {
            label 'maven-pod'
            containerTemplate {
                name 'maven',
                     image 'maven:3.8.6-openjdk-11',
                     ttyEnabled true,
                     command 'cat'
                envVars([
                    envVar(key: 'MAVEN_OPTS', value: '-Dmaven.repo.local=/tmp/m2'),
                    envVar(key: 'JAVA_OPTS', value: '-Xmx1024m')
                ])
            }
        }
    }
    
    environment {
        NEXUS_URL = credentials('nexus-url')
        NEXUS_REPO = 'maven-releases'
        OPENSHIFT_TOKEN = credentials('openshift-token')
        PROJECT_NAME = 'mi-aplicacion'
        IMAGE_TAG = "${BUILD_NUMBER}"
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        
        stage('Build') {
            steps {
                container('maven') {
                    sh '''
                        mvn clean compile -DskipTests
                        mvn package -DskipTests
                    '''
                }
            }
        }
        
        stage('Test') {
            steps {
                container('maven') {
                    sh 'mvn test'
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }
        
        stage('Quality Gate') {
            steps {
                container('maven') {
                    sh '''
                        mvn sonar:sonar \
                            -Dsonar.projectKey=${PROJECT_NAME} \
                            -Dsonar.host.url=${SONARQUBE_URL} \
                            -Dsonar.login=${SONAR_TOKEN}
                    '''
                }
            }
        }
        
        stage('Push to Nexus') {
            when {
                branch 'main'
            }
            steps {
                container('maven') {
                    sh '''
                        mvn deploy \
                            -DskipTests \
                            -DrepositoryId=nexus \
                            -Durl=https://${NEXUS_URL}/repository/${NEXUS_REPO}
                    '''
                }
            }
        }
        
        stage('Build OpenShift Image') {
            steps {
                script {
                    sh '''
                        oc login ${OPENSHIFT_URL} --token=${OPENSHIFT_TOKEN}
                        oc project ${PROJECT_NAME}-dev
                        oc start-build java-app --wait
                    '''
                }
            }
        }
        
        stage('Deploy to Dev') {
            steps {
                script {
                    sh '''
                        oc rollout latest dc/java-app
                        oc rollout status dc/java-app
                    '''
                }
            }
        }
        
        stage('Integration Tests') {
            steps {
                script {
                    sh '''
                        sleep 60  # Esperar a que la app esté lista
                        curl -f http://java-app-dev.${DOMAIN}/actuator/health
                    '''
                }
            }
        }
        
        stage('Deploy to Staging') {
            when {
                branch 'main'
            }
            steps {
                script {
                    sh '''
                        oc project ${PROJECT_NAME}-staging
                        oc tag ${PROJECT_NAME}-dev/java-app:latest \
                              ${PROJECT_NAME}-staging/java-app:staging
                        oc rollout latest dc/java-app
                        oc rollout status dc/java-app
                    '''
                }
            }
        }
        
        stage('E2E Tests') {
            when {
                branch 'main'
            }
            steps {
                script {
                    sh '''
                        sleep 60
                        curl -f http://java-app-staging.${DOMAIN}/actuator/health
                        # Ejecutar tests de integración aquí
                    '''
                }
            }
        }
        
        stage('Deploy to Production') {
            when {
                branch 'main'
            }
            input {
                message "Deploy to production?"
                ok "Deploy"
                parameters {
                    choice(name: 'ENVIRONMENT', choices: ['production'], description: 'Environment to deploy to')
                }
            }
            steps {
                script {
                    sh '''
                        oc project ${PROJECT_NAME}-prod
                        oc tag ${PROJECT_NAME}-staging/java-app:staging \
                              ${PROJECT_NAME}-prod/java-app:prod
                        oc rollout latest dc/java-app
                        oc rollout status dc/java-app
                    '''
                }
            }
        }
    }
    
    post {
        always {
            sh 'oc logout'
            cleanWs()
        }
        success {
            slackSend channel: '#devops',
                      color: 'good',
                      message: "✅ Pipeline ${BUILD_NUMBER} for ${BRANCH_NAME} completed successfully"
        }
        failure {
            slackSend channel: '#devops',
                      color: 'danger',
                      message: "❌ Pipeline ${BUILD_NUMBER} for ${BRANCH_NAME} failed"
        }
    }
}
```

### Configuración de Helm en OpenShift

#### Chart.yaml
```yaml
apiVersion: v2
name: spring-boot-app
description: A Helm chart for Spring Boot application
version: 1.0.0
appVersion: "1.0"

dependencies:
- name: postgresql
  version: ~10.3.0
  repository: https://charts.bitnami.com/bitnami
  condition: postgresql.enabled
```

#### values.yaml
```yaml
replicaCount: 3

image:
  repository: java-app
  tag: latest
  pullPolicy: IfNotPresent

service:
  type: ClusterIP
  port: 80
  targetPort: 8080

ingress:
  enabled: true
  className: nginx
  hosts:
    - host: app.empresa.com
      paths:
        - path: /
          pathType: Prefix

resources:
  limits:
    cpu: 500m
    memory: 1Gi
  requests:
    cpu: 250m
    memory: 512Mi

autoscaling:
  enabled: true
  minReplicas: 2
  maxReplicas: 10
  targetCPUUtilizationPercentage: 80

postgresql:
  enabled: true
  auth:
    database: appdb
    username: appuser
    password: apppassword
```

#### Deployment.yaml (Helm Template)
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: {{ include "spring-boot-app.fullname" . }}
  labels:
    {{- include "spring-boot-app.labels" . | nindent 4 }}
spec:
  {{- if not .Values.autoscaling.enabled }}
  replicas: {{ .Values.replicaCount }}
  {{- end }}
  selector:
    matchLabels:
      {{- include "spring-boot-app.selectorLabels" . | nindent 6 }}
  template:
    metadata:
      {{- with .Values.podAnnotations }}
      annotations:
        {{- toYaml . | nindent 8 }}
      {{- end }}
      labels:
        {{- include "spring-boot-app.selectorLabels" . | nindent 8 }}
    spec:
      {{- with .Values.imagePullSecrets }}
      imagePullSecrets:
        {{- toYaml . | nindent 8 }}
      {{- end }}
      serviceAccountName: {{ include "spring-boot-app.serviceAccountName" . }}
      securityContext:
        {{- toYaml .Values.podSecurityContext | nindent 8 }}
      containers:
      - name: {{ .Chart.Name }}
        securityContext:
          {{- toYaml .Values.securityContext | nindent 12 }}
        image: "{{ .Values.image.repository }}:{{ .Values.image.tag | default .Chart.AppVersion }}"
        imagePullPolicy: {{ .Values.image.pullPolicy }}
        ports:
        - name: http
          containerPort: 8080
          protocol: TCP
        {{- if .Values.livenessProbe }}
        livenessProbe:
          {{- toYaml .Values.livenessProbe | nindent 10 }}
        {{- end }}
        {{- if .Values.readinessProbe }}
        readinessProbe:
          {{- toYaml .Values.readinessProbe | nindent 10 }}
        {{- end }}
        resources:
          {{- toYaml .Values.resources | nindent 10 }}
        {{- with .Values.volumeMounts }}
        volumeMounts:
          {{- toYaml . | nindent 10 }}
        {{- end }}
      {{- with .Values.volumes }}
      volumes:
        {{- toYaml . | nindent 8 }}
      {{- end }}
```

## **Ventajas y Desventajas**

### Ventajas
- **Kubernetes nativo**: Funcionalidades completas de K8s + herramientas adicionales
- **CI/CD integrado**: Herramientas nativas para pipelines
- **Developer-friendly**: Interfaz intuitiva para desarrolladores
- **Enterprise-ready**: Características de seguridad y compliance
- **Multi-tenant**: Aislamiento robusto para equipos
- **Ecosystem completo**: Herramientas integradas de monitoreo y logging
- **Vendor support**: Soporte oficial de Red Hat
- **Automation**: Automatización completa del ciclo de vida

### Desventajas
- **Complejidad**: Curva de aprendizaje pronunciada
- **Recursos**: Consume más recursos que Kubernetes vanilla
- **Costos**: Licenciamiento puede ser costoso
- **Vendor lock-in**: Dependencia de Red Hat
- **Actualizaciones**: Ciclos de actualización más lentos
- **Configuración**: Configuración inicial puede ser compleja
- **Performance**: Overhead adicional comparado con K8s vanilla

## **Buenas Prácticas**

### Seguridad
- **RBAC**: Implementar control de acceso granular
- **Secrets**: Gestión segura de credenciales
- **Network Policies**: Segmentación de red
- **Security Contexts**: Configuraciones de seguridad por contenedor
- **Image Scanning**: Escaneo de vulnerabilidades en imágenes

```yaml
apiVersion: v1
kind: SecurityContextConstraints
metadata:
  name: restricted-scc
allowPrivilegeEscalation: false
allowHostDirVolumePlugin: false
allowHostIPC: false
allowHostNetwork: false
allowHostPID: false
allowHostPorts: false
allowPrivilegedContainer: false
allowedCapabilities: []
defaultAddCapabilities: []
fsGroup:
  type: MustRunAs
groups:
- system:authenticated
readOnlyRootFilesystem: false
requiredDropCapabilities:
- ALL
runAsUser:
  type: MustRunAsNonRoot
seLinuxContext:
  type: MustRunAs
volumes:
- configMap
- downwardAPI
- emptyDir
- persistentVolumeClaim
- projected
- secret
```

### Escalabilidad
- **HPA**: Horizontal Pod Autoscaler
- **VPA**: Vertical Pod Autoscaler
- **Cluster Autoscaling**: Escalado automático de nodos
- **Resource Quotas**: Límites por proyecto

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: java-app-hpa
spec:
  scaleTargetRef:
    apiVersion: apps.openshift.io/v1
    kind: DeploymentConfig
    name: java-app
  minReplicas: 2
  maxReplicas: 20
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 80
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
```

### Monitoreo
- **Prometheus**: Métricas nativas
- **Grafana**: Dashboards de monitoreo
- **Jaeger**: Distributed tracing
- **ELK**: Logging centralizado

```yaml
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: java-app-monitor
  labels:
    app: java-app
spec:
  selector:
    matchLabels:
      app: java-app
  endpoints:
  - port: http
    path: /actuator/prometheus
    interval: 30s
```

### Gestión de Configuración
- **ConfigMaps**: Configuración no sensible
- **Secrets**: Datos sensibles
- **Templates**: Plantillas reutilizables
- **Helm Charts**: Gestión declarativa

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: app-config
data:
  application.properties: |
    server.port=8080
    spring.profiles.active=production
    logging.level.com.empresa=INFO
  logback.xml: |
    <?xml version="1.0" encoding="UTF-8"?>
    <configuration>
        <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
            <encoder>
                <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
            </encoder>
        </appender>
        <logger name="com.empresa" level="INFO"/>
        <root level="INFO">
            <appender-ref ref="STDOUT"/>
        </root>
    </configuration>
```

### Desarrollo y Testing
- **Source-to-Image**: Construcción automática desde código
- **Hot Deploy**: Recarga rápida en desarrollo
- **Health Checks**: Probes de salud y readiness
- **CI/CD Integration**: Pipelines automatizados

---

## **Referencias Oficiales**

1. **OpenShift Documentation**: https://docs.openshift.com/
2. **OpenShift CLI Reference**: https://docs.openshift.com/cli/latest/
3. **OpenShift GitHub Repository**: https://github.com/openshift/origin
4. **OpenShift Blog**: https://www.redhat.com/en/openshift
5. **OpenShift Interactive Learning Portal**: https://learn.openshift.com/
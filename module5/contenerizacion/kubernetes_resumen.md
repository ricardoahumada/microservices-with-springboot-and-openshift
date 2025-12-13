# Kubernetes

## ¿Qué es Kubernetes?

**Kubernetes** (también conocido como "K8s") es una plataforma de orquestación de contenedores de código abierto que automatiza el despliegue, escalado y gestión de aplicaciones contenedorizadas. Diseñada por Google y ahora mantenida por la Cloud Native Computing Foundation (CNCF), Kubernetes se ha convertido en el estándar de la industria para la gestión de microservicios y aplicaciones en la nube.

### Conceptos Fundamentales

**Pod**: Unidad más pequeña de Kubernetes que puede contener uno o más contenedores que comparten almacenamiento y red.

**Deployment**: Recurso que gestiona la implementación y actualización de pods de manera declarativa.

**Service**: Abstracción que define un conjunto lógico de pods y una política de acceso a ellos.

**ConfigMap**: Recurso para almacenar datos de configuración no confidenciales en pares clave-valor.

**Secret**: Recurso para almacenar información sensible como contraseñas, tokens SSH, etc.

**PersistentVolume**: Recurso de almacenamiento de red o local que proporciona almacenamiento persistente.

**Namespace**: Recurso para organizar y aislar recursos de Kubernetes en clusters.

---

## **Herramientas Principales**

### **kubectl**
- CLI principal para interactuar con clusters de Kubernetes
- Gestión de recursos, despliegue, debugging
- Comandos para crear, modificar y eliminar recursos

### **Helm**
- Gestor de paquetes para Kubernetes
- Simplifica instalación y gestión de aplicaciones complejas
- Charts para definir, instalar y actualizar aplicaciones

### **Dashboard**
- Interfaz web para gestión de clusters
- Visualización de recursos y métricas
- Gestión de aplicaciones y deployments

### **Kompose**
- Herramienta para convertir Docker Compose a Kubernetes
- Migra configuraciones de Docker Compose
- Genera manifests de Kubernetes automáticamente

---

## **Comandos Principales**

### **Gestión de Cluster**

```bash
# Ver información del cluster
kubectl cluster-info

# Ver nodos del cluster
kubectl get nodes

# Describir nodo
kubectl describe node node-name

# Ver información detallada del cluster
kubectl version

# Ver eventos del cluster
kubectl get events --all-namespaces

# Ver logs del sistema
kubectl logs -n kube-system -l component=kube-proxy
```

### **Gestión de Pods**

```bash
# Crear pod desde archivo YAML
kubectl apply -f pod.yaml

# Ver pods en namespace default
kubectl get pods

# Ver pods en todos los namespaces
kubectl get pods --all-namespaces

# Ver pods con más detalles
kubectl get pods -o wide

# Describir pod
kubectl describe pod pod-name

# Ver logs del pod
kubectl logs pod-name

# Ver logs de contenedor específico
kubectl logs pod-name -c container-name

# Seguir logs en tiempo real
kubectl logs -f pod-name

# Ejecutar comando en pod
kubectl exec -it pod-name -- /bin/bash

# Ejecutar comando específico
kubectl exec pod-name -- ls -la

# Copiar archivos a/desde pod
kubectl cp pod-name:/path/to/file ./local-file
kubectl cp ./local-file pod-name:/path/to/destination

# Eliminar pod
kubectl delete pod pod-name

# Eliminar forzado
kubectl delete pod pod-name --grace-period=0 --force
```

### **Gestión de Deployments**

```bash
# Crear deployment
kubectl apply -f deployment.yaml

# Ver deployments
kubectl get deployments

# Describir deployment
kubectl describe deployment deployment-name

# Ver estado del deployment
kubectl rollout status deployment/deployment-name

# Ver historial de deployments
kubectl rollout history deployment/deployment-name

# Rollback a revisión anterior
kubectl rollout undo deployment/deployment-name

# Rollback a revisión específica
kubectl rollout undo deployment/deployment-name --to-revision=2

# Escalar deployment
kubectl scale deployment deployment-name --replicas=3

# Ver réplicas del deployment
kubectl get rs

# Actualizar deployment
kubectl set image deployment/deployment-name container-name=image:new-tag

# Establecer variables de entorno
kubectl set env deployment/deployment-name ENV_VAR=value

# Exponer deployment como servicio
kubectl expose deployment deployment-name --port=80 --type=LoadBalancer
```

### **Gestión de Services**

```bash
# Crear servicio
kubectl apply -f service.yaml

# Ver servicios
kubectl get services

# Ver servicios con endpoints
kubectl get svc -o wide

# Describir servicio
kubectl describe service service-name

# Ver endpoints del servicio
kubectl get endpoints

# Editar servicio
kubectl edit service service-name

# Eliminar servicio
kubectl delete service service-name

# Exponer deployment como servicio
kubectl expose deployment nginx --port=80 --target-port=8080
```

### **Gestión de ConfigMaps y Secrets**

```bash
# Crear ConfigMap
kubectl create configmap app-config --from-file=config.properties

# Crear ConfigMap desde literal
kubectl create configmap app-config --from-literal=key1=value1 --from-literal=key2=value2

# Ver ConfigMaps
kubectl get configmaps

# Ver contenido de ConfigMap
kubectl get configmap app-config -o yaml

# Crear Secret
kubectl create secret generic app-secrets --from-literal=password=secret123

# Crear Secret desde archivo
kubectl create secret generic app-secrets --from-file=./secrets/password.txt

# Ver Secrets
kubectl get secrets

# Ver contenido de Secret (decode)
kubectl get secret app-secrets -o yaml

# Decodificar Secret
echo "c2VjcmV0MTIz" | base64 -d
```

### **Gestión de Namespaces**

```bash
# Crear namespace
kubectl create namespace dev

# Ver namespaces
kubectl get namespaces

# Cambiar namespace por defecto
kubectl config set-context --current --namespace=dev

# Ver pods en namespace específico
kubectl get pods -n namespace-name

# Eliminar namespace
kubectl delete namespace namespace-name

# Crear recurso en namespace específico
kubectl apply -f resource.yaml -n namespace-name
```

### **Debugging y Monitoreo**

```bash
# Ver eventos
kubectl get events

# Ver eventos de un recurso específico
kubectl describe pod pod-name | grep Events

# Ver métricas de pods (requiere metrics-server)
kubectl top pods

# Ver métricas de nodos
kubectl top nodes

# Ver recursos en el cluster
kubectl get all

# Ver uso de recursos
kubectl describe node node-name

# Ver logs de múltiples pods
kubectl logs -l app=myapp --tail=50

# Port forward para debugging
kubectl port-forward pod-name 8080:80

# Port forward a servicio
kubectl port-forward svc/my-service 8080:80
```

---

## **Recursos Kubernetes (YAMLS)**

### **Pod**

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: myapp-pod
  labels:
    app: myapp
    version: v1
  namespace: default
spec:
  containers:
  - name: myapp-container
    image: nginx:alpine
    ports:
    - containerPort: 80
      name: http
    env:
    - name: NODE_ENV
      value: "production"
    - name: DATABASE_URL
      valueFrom:
        secretKeyRef:
          name: db-credentials
          key: database-url
    resources:
      requests:
        memory: "64Mi"
        cpu: "250m"
      limits:
        memory: "128Mi"
        cpu: "500m"
    volumeMounts:
    - name: app-config
      mountPath: /app/config
    - name: app-logs
      mountPath: /app/logs
    livenessProbe:
      httpGet:
        path: /health
        port: 80
      initialDelaySeconds: 30
      periodSeconds: 10
    readinessProbe:
      httpGet:
        path: /ready
        port: 80
      initialDelaySeconds: 5
      periodSeconds: 5
  volumes:
  - name: app-config
    configMap:
      name: app-config
  - name: app-logs
    emptyDir: {}
  restartPolicy: Always
  nodeSelector:
    kubernetes.io/os: linux
  tolerations:
  - key: "node.kubernetes.io/not-ready"
    operator: "Exists"
    effect: "NoExecute"
    tolerationSeconds: 300
```

### **Deployment**

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: myapp-deployment
  namespace: default
  labels:
    app: myapp
    component: web
spec:
  replicas: 3
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 1
  selector:
    matchLabels:
      app: myapp
      component: web
  template:
    metadata:
      labels:
        app: myapp
        component: web
        version: v1
    spec:
      containers:
      - name: myapp
        image: myapp:1.0.0
        ports:
        - containerPort: 3000
          name: http
        env:
        - name: NODE_ENV
          value: "production"
        - name: DATABASE_URL
          valueFrom:
            secretKeyRef:
              name: db-secrets
              key: database-url
        - name: REDIS_URL
          valueFrom:
            configMapKeyRef:
              name: app-config
              key: redis-url
        resources:
          requests:
            memory: "128Mi"
            cpu: "100m"
          limits:
            memory: "256Mi"
            cpu: "200m"
        volumeMounts:
        - name: app-storage
          mountPath: /app/data
        livenessProbe:
          httpGet:
            path: /health
            port: 3000
          initialDelaySeconds: 30
          periodSeconds: 10
          timeoutSeconds: 5
          failureThreshold: 3
        readinessProbe:
          httpGet:
            path: /ready
            port: 3000
          initialDelaySeconds: 5
          periodSeconds: 5
          timeoutSeconds: 3
          failureThreshold: 3
        securityContext:
          runAsNonRoot: true
          runAsUser: 1000
          allowPrivilegeEscalation: false
          readOnlyRootFilesystem: true
      volumes:
      - name: app-storage
        persistentVolumeClaim:
          claimName: app-pvc
      securityContext:
        fsGroup: 1000
      nodeSelector:
        node-type: application
      affinity:
        podAntiAffinity:
          preferredDuringSchedulingIgnoredDuringExecution:
          - weight: 100
            podAffinityTerm:
              labelSelector:
                matchExpressions:
                - key: app
                  operator: In
                  values:
                  - myapp
              topologyKey: kubernetes.io/hostname
```

### **Service**

```yaml
apiVersion: v1
kind: Service
metadata:
  name: myapp-service
  namespace: default
  labels:
    app: myapp
    component: web
spec:
  type: ClusterIP
  selector:
    app: myapp
    component: web
  ports:
  - name: http
    port: 80
    targetPort: 3000
    protocol: TCP
  sessionAffinity: None
---
apiVersion: v1
kind: Service
metadata:
  name: myapp-service-lb
  namespace: default
  labels:
    app: myapp
    component: web
  annotations:
    service.beta.kubernetes.io/aws-load-balancer-type: "nlb"
spec:
  type: LoadBalancer
  selector:
    app: myapp
    component: web
  ports:
  - name: http
    port: 80
    targetPort: 3000
    protocol: TCP
  - name: https
    port: 443
    targetPort: 3000
    protocol: TCP
---
apiVersion: v1
kind: Service
metadata:
  name: myapp-headless
  namespace: default
spec:
  clusterIP: None
  selector:
    app: myapp
  ports:
  - name: http
    port: 80
    targetPort: 3000
```

### **ConfigMap**

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: app-config
  namespace: default
  labels:
    app: myapp
data:
  # Configuración de aplicación
  app.properties: |
    server.port=3000
    server.host=0.0.0.0
    logging.level.root=INFO
    logging.level.com.myapp=DEBUG
    
  database.properties: |
    db.host=postgres-service
    db.port=5432
    db.name=myapp
    db.pool.min=5
    db.pool.max=20
    
  redis.properties: |
    redis.host=redis-service
    redis.port=6379
    redis.timeout=5000
    redis.pool.max=10
    
  nginx.conf: |
    server {
        listen 80;
        server_name localhost;
        
        location / {
            proxy_pass http://myapp-service;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
        }
    }
```

### **Secret**

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: app-secrets
  namespace: default
  labels:
    app: myapp
type: Opaque
data:
  # Los valores deben estar en base64
  database-password: cGFzc3dvcmQxMjM=  # password123
  jwt-secret: eW91ci1qd3Qtc2VjcmV0LWtleQ==  # your-jwt-secret-key
  api-key: YWJjZGVmZ2hpams=  # abcdefghijk
stringData:
  # Alternativamente, usar stringData para valores en texto plano
  database-username: admin
  redis-password: redis123
---
apiVersion: v1
kind: Secret
metadata:
  name: tls-certificates
  namespace: default
type: kubernetes.io/tls
data:
  tls.crt: LS0tLS1CRUdJTi...  # Certificado TLS en base64
  tls.key: LS0tLS1CRUdJTi...  # Clave privada en base64
```

### **PersistentVolume y PersistentVolumeClaim**

```yaml
apiVersion: v1
kind: PersistentVolume
metadata:
  name: app-pv
  labels:
    type: local
spec:
  storageClassName: manual
  capacity:
    storage: 10Gi
  accessModes:
    - ReadWriteOnce
  persistentVolumeReclaimPolicy: Retain
  hostPath:
    path: "/mnt/data/app"
---
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: app-pvc
  namespace: default
spec:
  storageClassName: manual
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 10Gi
  selector:
    matchLabels:
      type: local
---
# StorageClass para AWS EBS
apiVersion: storage.k8s.io/v1
kind: StorageClass
metadata:
  name: ebs-sc
provisioner: ebs.csi.aws.com
parameters:
  type: gp3
  iops: "3000"
  throughput: "125"
  encrypted: "true"
allowVolumeExpansion: true
reclaimPolicy: Delete
```

### **Ingress**

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: myapp-ingress
  namespace: default
  annotations:
    nginx.ingress.kubernetes.io/rewrite-target: /
    nginx.ingress.kubernetes.io/ssl-redirect: "true"
    nginx.ingress.kubernetes.io/force-ssl-redirect: "true"
    cert-manager.io/cluster-issuer: "letsencrypt-prod"
    nginx.ingress.kubernetes.io/rate-limit: "100"
    nginx.ingress.kubernetes.io/rate-limit-window: "1m"
spec:
  tls:
  - hosts:
    - myapp.example.com
    secretName: myapp-tls
  rules:
  - host: myapp.example.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: myapp-service
            port:
              number: 80
      - path: /api/v1
        pathType: Prefix
        backend:
          service:
            name: api-service
            port:
              number: 8080
```

---

## **Ejemplos de Configuración Completa**

### **Aplicación Full-Stack en Kubernetes**

```yaml
# Aplicación completa con múltiples servicios
apiVersion: v1
kind: Namespace
metadata:
  name: myapp

---
# ConfigMap para configuración de aplicación
apiVersion: v1
kind: ConfigMap
metadata:
  name: myapp-config
  namespace: myapp
data:
  database.properties: |
    db.host=postgres-service
    db.port=5432
    db.name=myapp
    db.ssl=true
  redis.properties: |
    redis.host=redis-service
    redis.port=6379
    redis.timeout=5000
  app.properties: |
    server.port=8080
    server.context-path=/api
    management.endpoints.web.exposure.include=health,info,metrics

---
# Secrets para credenciales
apiVersion: v1
kind: Secret
metadata:
  name: myapp-secrets
  namespace: myapp
type: Opaque
stringData:
  db-password: "secure_password_123"
  jwt-secret: "super_secret_jwt_key_456"
  redis-password: "redis_secure_789"

---
# Deployment para base de datos PostgreSQL
apiVersion: apps/v1
kind: Deployment
metadata:
  name: postgres-deployment
  namespace: myapp
spec:
  replicas: 1
  selector:
    matchLabels:
      app: postgres
  template:
    metadata:
      labels:
        app: postgres
    spec:
      containers:
      - name: postgres
        image: postgres:15-alpine
        ports:
        - containerPort: 5432
        env:
        - name: POSTGRES_DB
          value: "myapp"
        - name: POSTGRES_USER
          value: "admin"
        - name: POSTGRES_PASSWORD
          valueFrom:
            secretKeyRef:
              name: myapp-secrets
              key: db-password
        volumeMounts:
        - name: postgres-storage
          mountPath: /var/lib/postgresql/data
        resources:
          requests:
            memory: "256Mi"
            cpu: "250m"
          limits:
            memory: "512Mi"
            cpu: "500m"
        livenessProbe:
          exec:
            command:
            - pg_isready
            - -U
            - admin
            - -d
            - myapp
          initialDelaySeconds: 30
          periodSeconds: 10
      volumes:
      - name: postgres-storage
        persistentVolumeClaim:
          claimName: postgres-pvc

---
# Service para PostgreSQL
apiVersion: v1
kind: Service
metadata:
  name: postgres-service
  namespace: myapp
spec:
  selector:
    app: postgres
  ports:
  - port: 5432
    targetPort: 5432
  clusterIP: None  # Headless service

---
# PersistentVolumeClaim para PostgreSQL
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: postgres-pvc
  namespace: myapp
spec:
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 20Gi
  storageClassName: gp3

---
# Deployment para Redis
apiVersion: apps/v1
kind: Deployment
metadata:
  name: redis-deployment
  namespace: myapp
spec:
  replicas: 1
  selector:
    matchLabels:
      app: redis
  template:
    metadata:
      labels:
        app: redis
    spec:
      containers:
      - name: redis
        image: redis:7-alpine
        ports:
        - containerPort: 6379
        command:
        - redis-server
        - --appendonly
        - "yes"
        - --requirepass
        - $(REDIS_PASSWORD)
        env:
        - name: REDIS_PASSWORD
          valueFrom:
            secretKeyRef:
              name: myapp-secrets
              key: redis-password
        volumeMounts:
        - name: redis-storage
          mountPath: /data
        resources:
          requests:
            memory: "128Mi"
            cpu: "100m"
          limits:
            memory: "256Mi"
            cpu: "200m"
      volumes:
      - name: redis-storage
        persistentVolumeClaim:
          claimName: redis-pvc

---
# Service para Redis
apiVersion: v1
kind: Service
metadata:
  name: redis-service
  namespace: myapp
spec:
  selector:
    app: redis
  ports:
  - port: 6379
    targetPort: 6379
  clusterIP: None  # Headless service

---
# PersistentVolumeClaim para Redis
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: redis-pvc
  namespace: myapp
spec:
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 5Gi
  storageClassName: gp3

---
# Deployment para aplicación principal
apiVersion: apps/v1
kind: Deployment
metadata:
  name: myapp-deployment
  namespace: myapp
spec:
  replicas: 3
  selector:
    matchLabels:
      app: myapp
  template:
    metadata:
      labels:
        app: myapp
        version: v1
    spec:
      containers:
      - name: myapp
        image: myapp:1.0.0
        ports:
        - containerPort: 8080
          name: http
        env:
        - name: NODE_ENV
          value: "production"
        - name: DATABASE_URL
          value: "postgresql://admin:$(DB_PASSWORD)@postgres-service:5432/myapp"
        - name: REDIS_URL
          value: "redis://:$(REDIS_PASSWORD)@redis-service:6379"
        - name: JWT_SECRET
          valueFrom:
            secretKeyRef:
              name: myapp-secrets
              key: jwt-secret
        envFrom:
        - configMapRef:
            name: myapp-config
        - secretRef:
            name: myapp-secrets
        resources:
          requests:
            memory: "256Mi"
            cpu: "200m"
          limits:
            memory: "512Mi"
            cpu: "500m"
        volumeMounts:
        - name: app-logs
          mountPath: /app/logs
        livenessProbe:
          httpGet:
            path: /api/health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
          timeoutSeconds: 5
          failureThreshold: 3
        readinessProbe:
          httpGet:
            path: /api/ready
            port: 8080
          initialDelaySeconds: 5
          periodSeconds: 5
          timeoutSeconds: 3
          failureThreshold: 3
        securityContext:
          runAsNonRoot: true
          runAsUser: 1000
          allowPrivilegeEscalation: false
          readOnlyRootFilesystem: true
      volumes:
      - name: app-logs
        emptyDir: {}
      nodeSelector:
        node-type: application
      affinity:
        podAntiAffinity:
          preferredDuringSchedulingIgnoredDuringExecution:
          - weight: 100
            podAffinityTerm:
              labelSelector:
                matchExpressions:
                - key: app
                  operator: In
                  values:
                  - myapp
              topologyKey: kubernetes.io/hostname

---
# Service para aplicación principal
apiVersion: v1
kind: Service
metadata:
  name: myapp-service
  namespace: myapp
  labels:
    app: myapp
spec:
  type: ClusterIP
  selector:
    app: myapp
  ports:
  - port: 80
    targetPort: 8080
    protocol: TCP
    name: http

---
# Ingress para exponer aplicación
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: myapp-ingress
  namespace: myapp
  annotations:
    nginx.ingress.kubernetes.io/rewrite-target: /
    nginx.ingress.kubernetes.io/ssl-redirect: "true"
    nginx.ingress.kubernetes.io/force-ssl-redirect: "true"
    cert-manager.io/cluster-issuer: "letsencrypt-prod"
spec:
  tls:
  - hosts:
    - myapp.example.com
    secretName: myapp-tls
  rules:
  - host: myapp.example.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: myapp-service
            port:
              number: 80
```

---

## **Buenas Prácticas**

### **1. Seguridad**

```yaml
# Pod Security Context
apiVersion: v1
kind: Pod
metadata:
  name: secure-pod
spec:
  securityContext:
    runAsNonRoot: true
    runAsUser: 1000
    fsGroup: 1000
    seccompProfile:
      type: RuntimeDefault
  containers:
  - name: app
    image: myapp:latest
    securityContext:
      allowPrivilegeEscalation: false
      readOnlyRootFilesystem: true
      capabilities:
        drop:
        - ALL
        add:
        - NET_BIND_SERVICE
    resources:
      requests:
        memory: "64Mi"
        cpu: "100m"
      limits:
        memory: "128Mi"
        cpu: "200m"
```

### **2. Gestión de Recursos**

```yaml
# Resource Requests y Limits
apiVersion: v1
kind: Pod
metadata:
  name: resource-managed-pod
spec:
  containers:
  - name: app
    image: myapp:latest
    resources:
      requests:
        memory: "128Mi"
        cpu: "100m"
      limits:
        memory: "256Mi"
        cpu: "200m"
    # Horizontal Pod Autoscaler
---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: myapp-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: myapp-deployment
  minReplicas: 2
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
```

### **3. Health Checks**

```yaml
# Liveness, Readiness y Startup Probes
apiVersion: v1
kind: Pod
metadata:
  name: healthy-pod
spec:
  containers:
  - name: app
    image: myapp:latest
    livenessProbe:
      httpGet:
        path: /health
        port: 8080
      initialDelaySeconds: 30
      periodSeconds: 10
      timeoutSeconds: 5
      failureThreshold: 3
    readinessProbe:
      httpGet:
        path: /ready
        port: 8080
      initialDelaySeconds: 5
      periodSeconds: 5
      timeoutSeconds: 3
      failureThreshold: 3
    startupProbe:
      httpGet:
        path: /startup
        port: 8080
      initialDelaySeconds: 10
      periodSeconds: 5
      timeoutSeconds: 3
      failureThreshold: 30
```

### **4. Configuración y Secrets**

```yaml
# Configuración desde ConfigMap y Secret
apiVersion: v1
kind: Pod
metadata:
  name: configured-pod
spec:
  containers:
  - name: app
    image: myapp:latest
    env:
    # Desde ConfigMap
    - name: APP_CONFIG
      valueFrom:
        configMapKeyRef:
          name: app-config
          key: app.properties
    # Desde Secret
    - name: DB_PASSWORD
      valueFrom:
        secretKeyRef:
          name: app-secrets
          key: db-password
    # Desde Secret como archivo
    - name: TLS_CERT
      valueFrom:
        secretKeyRef:
          name: tls-certificates
          key: tls.crt
    envFrom:
    - configMapRef:
        name: app-config
    - secretRef:
        name: app-secrets
    volumeMounts:
    - name: config-volume
      mountPath: /app/config
    - name: secret-volume
      mountPath: /app/secrets
      readOnly: true
  volumes:
  - name: config-volume
    configMap:
      name: app-config
  - name: secret-volume
    secret:
      secretName: app-secrets
```

### **5. Networking**

```yaml
# Network Policies
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: app-network-policy
  namespace: default
spec:
  podSelector:
    matchLabels:
      app: myapp
  policyTypes:
  - Ingress
  - Egress
  ingress:
  - from:
    - podSelector:
        matchLabels:
          app: nginx-ingress
    - namespaceSelector:
        matchLabels:
          name: monitoring
    ports:
    - protocol: TCP
      port: 8080
  egress:
  - to:
    - podSelector:
        matchLabels:
          app: postgres
    ports:
    - protocol: TCP
      port: 5432
  - to:
    - podSelector:
        matchLabels:
          app: redis
    ports:
    - protocol: TCP
      port: 6379
```

### **6. Storage**

```yaml
# Persistent Volumes con diferentes backends
# AWS EBS
apiVersion: storage.k8s.io/v1
kind: StorageClass
metadata:
  name: ebs-sc
provisioner: ebs.csi.aws.com
parameters:
  type: gp3
  iops: "3000"
  throughput: "125"
  encrypted: "true"
allowVolumeExpansion: true
reclaimPolicy: Delete

---
# PersistentVolumeClaim
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: app-pvc
spec:
  accessModes:
    - ReadWriteOnce
  storageClassName: ebs-sc
  resources:
    requests:
      storage: 10Gi
```

### **7. Monitoring y Observability**

```yaml
# ServiceMonitor para Prometheus
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: myapp-monitor
  namespace: monitoring
spec:
  selector:
    matchLabels:
      app: myapp
  endpoints:
  - port: http
    path: /metrics
    interval: 30s
    scrapeTimeout: 10s

---
# PodMonitor para scraping de pods
apiVersion: monitoring.coreos.com/v1
kind: PodMonitor
metadata:
  name: myapp-pod-monitor
  namespace: monitoring
spec:
  selector:
    matchLabels:
      app: myapp
  podMetricsEndpoints:
  - port: http
    path: /metrics
    interval: 30s
```

### **8. Multi-Environment Configuration**

```yaml
# Kustomize para diferentes ambientes
# base/kustomization.yaml
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization
resources:
- deployment.yaml
- service.yaml
- configmap.yaml
namespace: myapp

---
# overlays/dev/kustomization.yaml
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization
bases:
- ../../base
patches:
- target:
    kind: Deployment
    name: myapp-deployment
  patch: |
    - op: replace
      path: /spec/replicas
      value: 2
    - op: replace
      path: /spec/template/spec/containers/0/resources/limits/memory
      value: "256Mi"

---
# overlays/prod/kustomization.yaml
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization
bases:
- ../../base
patches:
- target:
    kind: Deployment
    name: myapp-deployment
  patch: |
    - op: replace
      path: /spec/replicas
      value: 5
    - op: replace
      path: /spec/template/spec/containers/0/resources/limits/memory
      value: "1Gi"
```

---

## **Referencias Oficiales**

1. **Kubernetes Documentation**  
   https://kubernetes.io/docs/

2. **kubectl Reference**  
   https://kubernetes.io/docs/reference/kubectl/

3. **Kubernetes Concepts**  
   https://kubernetes.io/docs/concepts/

4. **Kubernetes API Reference**  
   https://kubernetes.io/docs/reference/kubernetes-api/

5. **Kubernetes Tutorials**  
   https://kubernetes.io/docs/tutorials/
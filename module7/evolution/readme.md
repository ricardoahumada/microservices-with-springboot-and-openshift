# Guia de Evolucion - Module 7: CI/CD con Jenkins y OpenShift

## Descripcion General

Esta guia describe como evolucionar el proyecto base `mutualidad-platform-base` hacia la solucion completa `mutualidad-platform`, implementando pipelines CI/CD con Jenkins, Helm Charts multi-ambiente y estrategias de rollback.

---

## Ejercicio 1: Jenkinsfile Declarativo con Agente Kubernetes

### Objetivo
Implementar un pipeline CI/CD completo usando sintaxis declarativa de Jenkins con agente Kubernetes.

### Punto de Partida
- `mutualidad-platform-base/*/Jenkinsfile` - Contiene estructura basica con TODOs

### Tareas

#### 1.1 Configurar Agente Kubernetes
```groovy
agent {
    kubernetes {
        yaml '''
apiVersion: v1
kind: Pod
spec:
  containers:
  - name: maven
    image: maven:3.9-eclipse-temurin-11
    command: ["sleep", "infinity"]
    volumeMounts:
    - name: maven-cache
      mountPath: /root/.m2
  - name: docker
    image: docker:24-cli
    command: ["sleep", "infinity"]
  - name: helm
    image: alpine/helm:3.14.0
    command: ["sleep", "infinity"]
'''
    }
}
```

#### 1.2 Definir Parametros
```groovy
parameters {
    choice(name: 'ENVIRONMENT', 
           choices: ['dev', 'qa', 'prod'], 
           description: 'Target environment')
    booleanParam(name: 'SKIP_TESTS', 
                 defaultValue: false, 
                 description: 'Skip unit tests')
}
```

#### 1.3 Implementar Stages
- **Checkout**: Obtener codigo y calcular `GIT_COMMIT_SHORT`
- **Build & Test**: Usar `container('maven')` para ejecutar Maven
- **Code Analysis**: Condicion `when` para qa/prod
- **Docker Build**: Usar `container('docker')` para build/push
- **Deploy**: Usar `container('helm')` para `helm upgrade --install`
- **Verify**: Validar despliegue con `helm status`

### Solucion de Referencia
Ver `mutualidad-platform/afiliado-service/Jenkinsfile`

---

## Ejercicio 2: Helm Chart Multi-Ambiente

### Objetivo
Configurar Helm Charts con values diferenciados por ambiente (dev, qa, prod).

### Punto de Partida
- `mutualidad-platform-base/charts/*/values.yaml` - Configuracion base

### Tareas

#### 2.1 Crear values-dev.yaml
```yaml
replicaCount: 1
image:
  repository: quay.io/mutualidad/afiliado-service
  tag: latest
resources:
  limits:
    cpu: 500m
    memory: 512Mi
  requests:
    cpu: 100m
    memory: 256Mi
```

#### 2.2 Crear values-qa.yaml
```yaml
replicaCount: 2
resources:
  limits:
    cpu: 1000m
    memory: 1Gi
```

#### 2.3 Crear values-prod.yaml
```yaml
replicaCount: 3
resources:
  limits:
    cpu: 2000m
    memory: 2Gi
autoscaling:
  enabled: true
  minReplicas: 3
  maxReplicas: 10
```

#### 2.4 Usar en Pipeline
```groovy
def valuesFile = "values-${params.ENVIRONMENT}.yaml"
helm upgrade --install ${APP_NAME} . \
    --values ${valuesFile} \
    --set image.tag=${IMAGE_TAG}
```

### Solucion de Referencia
Ver `mutualidad-platform/charts/*/values-*.yaml`

---

## Ejercicio 3: Estrategia de Rollback

### Objetivo
Implementar rollback automatico y manual usando Helm.

### Tareas

#### 3.1 Rollback Automatico con --atomic
```groovy
helm upgrade --install ${APP_NAME} . \
    --atomic \
    --timeout 5m
```
> `--atomic` automaticamente hace rollback si el despliegue falla

#### 3.2 Stage de Rollback Manual
```groovy
stage('Rollback') {
    when {
        expression { params.ACTION == 'rollback' }
    }
    steps {
        container('helm') {
            sh """
                helm rollback ${APP_NAME} ${params.REVISION} \
                    -n ${OPENSHIFT_PROJECT}
            """
        }
    }
}
```

#### 3.3 Listar Historial de Releases
```bash
helm history ${APP_NAME} -n ${NAMESPACE}
```

#### 3.4 Parametros Adicionales
```groovy
parameters {
    choice(name: 'ACTION', 
           choices: ['deploy', 'rollback'], 
           description: 'Action to perform')
    string(name: 'REVISION', 
           defaultValue: '', 
           description: 'Revision for rollback')
}
```

### Solucion de Referencia
Ver `mutualidad-platform/*/Jenkinsfile` - seccion Deploy to OpenShift

---

## Estructura de Archivos

```
module7/solutions/
├── mutualidad-platform-base/     # Version con TODOs (ejercicio)
│   ├── afiliado-service/
│   │   └── Jenkinsfile           # Pipeline con TODOs
│   ├── notificacion-service/
│   │   └── Jenkinsfile
│   ├── validacion-service/
│   │   └── Jenkinsfile
│   └── charts/
│       └── */values.yaml         # Solo valores base
│
├── mutualidad-platform/          # Solucion completa
│   ├── afiliado-service/
│   │   └── Jenkinsfile           # Pipeline implementado
│   ├── notificacion-service/
│   │   └── Jenkinsfile
│   ├── validacion-service/
│   │   └── Jenkinsfile
│   └── charts/
│       └── */
│           ├── values.yaml
│           ├── values-dev.yaml
│           ├── values-qa.yaml
│           └── values-prod.yaml
│
└── evolution/
    └── readme.md                 # Esta guia
```

---

## Checklist de Validacion

### Ejercicio 1 - Jenkinsfile
- [ ] Agente Kubernetes configurado con 3 containers
- [ ] Parametros ENVIRONMENT y SKIP_TESTS definidos
- [ ] Variables de entorno correctas
- [ ] Stage Build usa container maven
- [ ] Stage Docker usa container docker
- [ ] Stage Deploy usa container helm con --atomic
- [ ] Post actions implementados

### Ejercicio 2 - Multi-Ambiente
- [ ] values-dev.yaml con 1 replica, recursos minimos
- [ ] values-qa.yaml con 2 replicas, recursos medios
- [ ] values-prod.yaml con 3+ replicas, autoscaling
- [ ] Pipeline selecciona values segun ENVIRONMENT

### Ejercicio 3 - Rollback
- [ ] Flag --atomic en helm upgrade
- [ ] Parametro ACTION con opcion rollback
- [ ] Stage condicional para rollback
- [ ] Comando helm rollback implementado

---

## Comandos Utiles

```bash
# Ver historial de releases
helm history afiliado-service -n mutualidad-dev

# Rollback a revision anterior
helm rollback afiliado-service 2 -n mutualidad-dev

# Ver valores actuales
helm get values afiliado-service -n mutualidad-dev

# Dry-run de despliegue
helm upgrade --install afiliado-service . \
    --values values-dev.yaml \
    --dry-run
```

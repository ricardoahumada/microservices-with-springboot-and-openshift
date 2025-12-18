# Charts Avanzadas - Multi-Entorno

Charts de Helm configuradas para despliegue en dos entornos:
- **DEV**: Minikube con Ingress
- **PROD**: OpenShift con Routes

## Estructura

```
charts-adv/
├── afiliado-service-chart/
│   ├── values.yaml          # Valores base
│   ├── values-dev.yaml      # Minikube (Ingress, 1 replica, recursos minimos)
│   ├── values-prod.yaml     # OpenShift (Route, HPA, recursos prod)
│   └── templates/
│       ├── deployment.yaml
│       ├── service.yaml
│       ├── ingress.yaml     # Solo dev
│       ├── route.yaml       # Solo prod
│       └── hpa.yaml         # Solo prod
├── validacion-service-chart/
├── notificacion-service-chart/
└── README.md
```

## Uso

### Despliegue en DEV (Minikube)

```bash
# Habilitar ingress en Minikube
minikube addons enable ingress

# Instalar servicio
cd charts-adv/afiliado-service-chart
helm dependency build
helm install afiliado-dev . -f values.yaml -f values-dev.yaml

# Verificar
kubectl get ingress
kubectl get pods
```

### Despliegue en PROD (OpenShift)

```bash
# Login a OpenShift
oc login --token=<token> --server=<server>

# Instalar servicio
cd charts-adv/afiliado-service-chart
helm dependency build
helm install afiliado-prod . -f values.yaml -f values-prod.yaml

# Verificar
oc get routes
oc get pods
oc get hpa
```

## Diferencias entre Entornos

| Aspecto | DEV (Minikube) | PROD (OpenShift) |
|---------|----------------|------------------|
| Replicas | 1 | 2 (con HPA 2-5) |
| CPU Request | 100m | 250m |
| Memory Request | 128Mi | 256Mi |
| CPU Limit | 200m | 500m |
| Memory Limit | 256Mi | 512Mi |
| Exposicion | Ingress (nginx) | Route |
| TLS | No | Si (edge) |
| Autoscaling | No | Si |
| Profile Spring | dev,kubernetes | prod,kubernetes |

## Comandos Utiles

### Verificar templates generados

```bash
# Dev
helm template my-release . -f values.yaml -f values-dev.yaml

# Prod
helm template my-release . -f values.yaml -f values-prod.yaml
```

### Actualizar despliegue

```bash
# Dev
helm upgrade afiliado-dev . -f values.yaml -f values-dev.yaml

# Prod
helm upgrade afiliado-prod . -f values.yaml -f values-prod.yaml
```

### Desinstalar

```bash
helm uninstall afiliado-dev
helm uninstall afiliado-prod
```

## Configurar /etc/hosts (Dev)

Para acceder a los servicios en Minikube:

```bash
# Obtener IP de Minikube
minikube ip

# Agregar a /etc/hosts
echo "$(minikube ip) afiliado.dev.local validacion.dev.local notificacion.dev.local" | sudo tee -a /etc/hosts
```

## Notas

- Las Routes de OpenShift usan `route.openshift.io/v1` API
- El HPA usa `autoscaling/v2` API
- PostgreSQL embebido en ambos entornos
- Los endpoints de observabilidad son los mismos en ambos entornos

# Docker Compose

## ¿Qué es Docker Compose?

**Docker Compose** es una herramienta para definir y ejecutar aplicaciones Docker multi-contenedor. Utiliza archivos YAML para configurar todos los servicios de la aplicación y permite iniciar, parar y reconstruir todos los servicios con un solo comando, simplificando significativamente la gestión de aplicaciones complejas.

### Conceptos Fundamentales

**Services**: Contenedores individuales definidos en docker-compose.yml. Cada service puede tener su propia imagen, configuración y dependencias.

**Volumes**: Mecanismo para persistir datos generados y utilizados por contenedores Docker.

**Networks**: Redes virtuales que permiten comunicación entre contenedores y aislamiento de tráfico.

**Environment Variables**: Variables de configuración que pueden ser pasadas a los contenedores.

**Dependencies**: Relaciones entre servicios que aseguran el orden de inicio (depends_on).

---

## **Herramientas Principales**

### **docker-compose CLI**
- Herramienta principal para gestión de aplicaciones multi-contenedor
- Comando base: `docker-compose`
- Versión actual: v2 (docker compose sin guión)

### **Docker Compose File (docker-compose.yml)**
- Archivo de configuración principal
- Formato YAML
- Define servicios, redes, volúmenes y configuraciones

### **docker-compose.override.yml**
- Archivo opcional que sobrescribe configuración base
- Útil para desarrollo local
- Se aplica automáticamente junto con docker-compose.yml

### **Environment Files (.env)**
- Archivos de variables de entorno
- Simplifican configuración por entorno
- Soportan diferentes valores por entorno

---

## **Comandos Principales**

### **Gestión de Servicios**

```bash
# Iniciar todos los servicios
docker-compose up

# Iniciar en segundo plano (detached)
docker-compose up -d

# Reconstruir y reiniciar servicios
docker-compose up -d --build

# Iniciar servicios específicos
docker-compose up service1 service2

# Parar servicios
docker-compose down

# Parar y eliminar contenedores, redes y volúmenes anónimos
docker-compose down -v

# Parar y eliminar todo, incluyendo volúmenes con nombre
docker-compose down -v --rmi all

# Reiniciar servicios
docker-compose restart

# Reiniciar servicio específico
docker-compose restart service-name
```

### **Gestión de Estado**

```bash
# Ver estado de servicios
docker-compose ps

# Ver logs de todos los servicios
docker-compose logs

# Ver logs de servicio específico
docker-compose logs service-name

# Seguir logs en tiempo real
docker-compose logs -f

# Ver últimos logs (tail)
docker-compose logs --tail=100 service-name

# Ver logs con timestamps
docker-compose logs -t service-name
```

### **Ejecución de Comandos**

```bash
# Ejecutar comando en servicio
docker-compose exec service-name comando

# Ejecutar con usuario específico
docker-compose exec -u root service-name bash

# Ejecutar comando único sin entrar al contenedor
docker-compose run --rm service-name npm test

# Ejecutar servicio temporal para tareas
docker-compose run --rm app npm install
```

### **Escalado y Balanceado**

```bash
# Escalar servicios
docker-compose up -d --scale app=3

# Escalar servicio específico
docker-compose up -d --scale service-name=2

# Configurar replicas en docker-compose.yml
# services:
#   app:
#     deploy:
#       replicas: 3
```

### **Desarrollo y Debugging**

```bash
# Ver configuración construida
docker-compose config

# Validar docker-compose.yml
docker-compose config --quiet

# Listar contenedores
docker-compose ps -a

# Ver redes
docker-compose networks

# Ver volúmenes
docker-compose volumes

# Ver estadísticas
docker-compose top
```

### **Limpieza y Mantenimiento**

```bash
# Eliminar contenedores detenidos
docker-compose rm

# Eliminar contenedores detenidos sin confirmación
docker-compose rm -f

# Eliminar volúmenes no utilizados
docker-compose down -v

# Limpiar imágenes no utilizadas
docker image prune -a

# Sistema completo de limpieza
docker system prune -a
```

---

## **Estructura de docker-compose.yml**

### **Versión y Servicios Básicos**

```yaml
# Versión del formato de Compose
version: '3.8'

# Definición de servicios
services:
  # Servicio de aplicación web
  web:
    # Imagen a utilizar
    image: nginx:latest
    
    # Build context (alternativa a image)
    build: .
    
    # Puerto a exponer
    ports:
      - "80:80"
      - "443:443"
    
    # Variables de entorno
    environment:
      - NGINX_HOST=localhost
      - NGINX_PORT=80
    
    # Dependencias
    depends_on:
      - db
      - redis
    
    # Redes
    networks:
      - frontend
      - backend
    
    # Volúmenes
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf
      - ./html:/usr/share/nginx/html
    
    # Reinicio automático
    restart: unless-stopped
    
    # Comando a ejecutar
    command: nginx -g "daemon off;"
    
    # Usuario
    user: nginx
    
    # Health check
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 40s

  # Servicio de base de datos
  db:
    image: postgres:15-alpine
    
    environment:
      - POSTGRES_DB=myapp
      - POSTGRES_USER=admin
      - POSTGRES_PASSWORD=secure_password
    
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./init.sql:/docker-entrypoint-initdb.d/init.sql
    
    ports:
      - "5432:5432"
    
    restart: unless-stopped
    
    # Configuración de despliegue
    deploy:
      resources:
        limits:
          cpus: '0.5'
          memory: 512M
        reservations:
          cpus: '0.25'
          memory: 256M

# Definición de volúmenes
volumes:
  postgres_data:
    driver: local

# Definición de redes
networks:
  frontend:
    driver: bridge
    ipam:
      config:
        - subnet: 172.20.0.0/16
  backend:
    driver: bridge
    internal: true
```

### **Configuración Avanzada**

```yaml
version: '3.8'

services:
  # Aplicación con configuración compleja
  app:
    build:
      context: .
      dockerfile: Dockerfile.prod
      args:
        - BUILD_ENV=production
        - API_VERSION=v1.2.0
    
    image: myapp:1.0.0
    
    container_name: myapp-container
    
    ports:
      - "3000:3000"
      - "9229:9229"  # Debug port
    
    environment:
      - NODE_ENV=production
      - DATABASE_URL=postgresql://admin:password@db:5432/myapp
      - REDIS_URL=redis://redis:6379
      - API_KEY=${API_KEY}
    
    env_file:
      - .env.production
      - .env.local
    
    volumes:
      - ./uploads:/app/uploads
      - ./logs:/app/logs
      - /app/node_modules  # Anonymous volume
    
    networks:
      - app-network
    
    depends_on:
      db:
        condition: service_healthy
      redis:
        condition: service_started
    
    restart: unless-stopped
    
    deploy:
      replicas: 2
      placement:
        constraints:
          - node.role == worker
      resources:
        limits:
          cpus: '1.0'
          memory: 1G
        reservations:
          cpus: '0.5'
          memory: 512M
      restart_policy:
        condition: on-failure
        delay: 5s
        max_attempts: 3
        window: 120s
    
    logging:
      driver: "json-file"
      options:
        max-size: "10m"
        max-file: "3"
    
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:3000/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 40s
    
    labels:
      - "traefik.enable=true"
      - "traefik.http.routers.app.rule=Host(`myapp.local`)"
      - "traefik.http.services.app.loadbalancer.server.port=3000"
    
    sysctls:
      - net.core.somaxconn=65535
    
    ulimits:
      nproc: 65535
      nofile:
        soft: 20000
        hard: 40000

  # Servicio con configuración de red avanzada
  nginx:
    image: nginx:alpine
    
    ports:
      - "80:80"
      - "443:443"
    
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf
      - ./ssl:/etc/ssl/certs
      - ./html:/usr/share/nginx/html
    
    networks:
      app-network:
        ipv4_address: 172.20.0.10
    
    depends_on:
      - app
    
    restart: unless-stopped
    
    # Configuración de logging
    logging:
      driver: "syslog"
      options:
        syslog-address: "tcp://log-server:514"
        tag: "nginx"

  # Servicio de monitoreo
  prometheus:
    image: prom/prometheus:latest
    
    ports:
      - "9090:9090"
    
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml
      - prometheus_data:/prometheus
    
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
      - '--web.console.libraries=/etc/prometheus/console_libraries'
      - '--web.console.templates=/etc/prometheus/consoles'
      - '--storage.tsdb.retention.time=200h'
      - '--web.enable-lifecycle'
    
    networks:
      - monitoring
    
    restart: unless-stopped

# Volúmenes con configuración avanzada
volumes:
  postgres_data:
    driver: local
    driver_opts:
      type: none
      o: bind
      device: /data/postgres
  
  prometheus_data:
    driver: local

# Redes con configuración IPAM
networks:
  app-network:
    driver: bridge
    ipam:
      driver: default
      config:
        - subnet: 172.20.0.0/16
          gateway: 172.20.0.1
          ip_range: 172.20.1.0/24
  
  monitoring:
    driver: bridge
    internal: true
```

---

## **Ejemplos de Configuración**

### **Aplicación Full-Stack**

```yaml
# docker-compose.yml completo para aplicación web
version: '3.8'

services:
  # Frontend
  frontend:
    build:
      context: ./frontend
      dockerfile: Dockerfile
    ports:
      - "3000:3000"
    environment:
      - REACT_APP_API_URL=http://localhost:3001/api
    depends_on:
      - backend
    networks:
      - app-network

  # Backend API
  backend:
    build:
      context: ./backend
      dockerfile: Dockerfile
    ports:
      - "3001:3001"
    environment:
      - NODE_ENV=production
      - DATABASE_URL=postgresql://admin:password@db:5432/myapp
      - JWT_SECRET=${JWT_SECRET}
      - REDIS_URL=redis://redis:6379
    env_file:
      - .env.production
    volumes:
      - ./uploads:/app/uploads
      - ./logs:/app/logs
    depends_on:
      - db
      - redis
    networks:
      - app-network
    restart: unless-stopped

  # Base de datos PostgreSQL
  db:
    image: postgres:15-alpine
    environment:
      - POSTGRES_DB=myapp
      - POSTGRES_USER=admin
      - POSTGRES_PASSWORD=${DB_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./init.sql:/docker-entrypoint-initdb.d/init.sql
      - ./backups:/backups
    ports:
      - "5432:5432"
    networks:
      - app-network
    restart: unless-stopped
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U admin -d myapp"]
      interval: 30s
      timeout: 10s
      retries: 3

  # Cache Redis
  redis:
    image: redis:7-alpine
    command: redis-server --appendonly yes
    volumes:
      - redis_data:/data
    ports:
      - "6379:6379"
    networks:
      - app-network
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 30s
      timeout: 10s
      retries: 3

  # Servidor web Nginx
  nginx:
    image: nginx:alpine
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf
      - ./ssl:/etc/ssl/certs
      - ./static:/var/www/static
    depends_on:
      - frontend
      - backend
    networks:
      - app-network
    restart: unless-stopped

  # Job processor
  worker:
    build:
      context: ./backend
      dockerfile: Dockerfile.worker
    environment:
      - NODE_ENV=production
      - DATABASE_URL=postgresql://admin:password@db:5432/myapp
      - REDIS_URL=redis://redis:6379
    volumes:
      - ./logs:/app/logs
    depends_on:
      - db
      - redis
    networks:
      - app-network
    restart: unless-stopped
    deploy:
      replicas: 2

volumes:
  postgres_data:
  redis_data:

networks:
  app-network:
    driver: bridge
```

### **Aplicación con Microservicios**

```yaml
# docker-compose para arquitectura de microservicios
version: '3.8'

services:
  # API Gateway
  api-gateway:
    image: nginx:alpine
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./gateway/nginx.conf:/etc/nginx/nginx.conf
      - ./gateway/ssl:/etc/ssl
    networks:
      - gateway-network
      - user-service-network
      - order-service-network
      - payment-service-network

  # Servicio de usuarios
  user-service:
    build: ./services/user-service
    environment:
      - DATABASE_URL=postgresql://admin:password@user-db:5432/users
      - REDIS_URL=redis://redis:6379
    depends_on:
      - user-db
      - redis
    networks:
      - user-service-network
      - redis-network
    restart: unless-stopped

  # Base de datos del servicio de usuarios
  user-db:
    image: postgres:15-alpine
    environment:
      - POSTGRES_DB=users
      - POSTGRES_USER=admin
      - POSTGRES_PASSWORD=password
    volumes:
      - user_db_data:/var/lib/postgresql/data
    networks:
      - user-service-network
    restart: unless-stopped

  # Servicio de órdenes
  order-service:
    build: ./services/order-service
    environment:
      - DATABASE_URL=postgresql://admin:password@order-db:5432/orders
      - USER_SERVICE_URL=http://user-service:3000
      - PAYMENT_SERVICE_URL=http://payment-service:3000
    depends_on:
      - order-db
      - user-service
      - payment-service
    networks:
      - order-service-network
      - user-service-network
      - payment-service-network
    restart: unless-stopped

  # Base de datos del servicio de órdenes
  order-db:
    image: postgres:15-alpine
    environment:
      - POSTGRES_DB=orders
      - POSTGRES_USER=admin
      - POSTGRES_PASSWORD=password
    volumes:
      - order_db_data:/var/lib/postgresql/data
    networks:
      - order-service-network
    restart: unless-stopped

  # Servicio de pagos
  payment-service:
    build: ./services/payment-service
    environment:
      - STRIPE_SECRET_KEY=${STRIPE_SECRET_KEY}
      - WEBHOOK_SECRET=${WEBHOOK_SECRET}
    networks:
      - payment-service-network
    restart: unless-stopped

  # Redis compartido
  redis:
    image: redis:7-alpine
    command: redis-server --appendonly yes
    volumes:
      - redis_data:/data
    networks:
      - redis-network
    restart: unless-stopped

  # Servicio de monitoreo
  prometheus:
    image: prom/prometheus:latest
    ports:
      - "9090:9090"
    volumes:
      - ./monitoring/prometheus.yml:/etc/prometheus/prometheus.yml
      - prometheus_data:/prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
      - '--web.console.libraries=/etc/prometheus/console_libraries'
      - '--web.console.templates=/etc/prometheus/consoles'
    networks:
      - monitoring-network
    restart: unless-stopped

  # Grafana para visualización
  grafana:
    image: grafana/grafana:latest
    ports:
      - "3000:3000"
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin
    volumes:
      - grafana_data:/var/lib/grafana
    networks:
      - monitoring-network
    restart: unless-stopped

volumes:
  user_db_data:
  order_db_data:
  redis_data:
  prometheus_data:
  grafana_data:

networks:
  gateway-network:
    driver: bridge
  user-service-network:
    driver: bridge
  order-service-network:
    driver: bridge
  payment-service-network:
    driver: bridge
  redis-network:
    driver: bridge
  monitoring-network:
    driver: bridge
```

---

## **Buenas Prácticas**

### **1. Estructura de Archivos**

```yaml
# Estructura recomendada de archivos
# .
# ├── docker-compose.yml          # Configuración principal
# ├── docker-compose.override.yml # Configuración desarrollo
# ├── docker-compose.prod.yml     # Configuración producción
# ├── docker-compose.test.yml     # Configuración tests
# ├── .env                        # Variables de entorno
# ├── .env.development           # Variables desarrollo
# ├── .env.production            # Variables producción
# ├── .env.test                  # Variables test
# └── .dockerignore              # Archivos excluidos

# docker-compose.override.yml para desarrollo
version: '3.8'

services:
  app:
    build: .
    ports:
      - "3000:3000"
      - "9229:9229"  # Debug port
    environment:
      - NODE_ENV=development
    volumes:
      - ./src:/app/src  # Hot reload
      - /app/node_modules  # Anonymous volume

  db:
    ports:
      - "5432:5432"  # Exponer puerto en desarrollo
```

### **2. Variables de Entorno**

```yaml
# .env (variables comunes)
APP_NAME=myapp
APP_VERSION=1.0.0
NODE_ENV=development

# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=myapp
DB_USER=admin
DB_PASSWORD=secure_password

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# JWT
JWT_SECRET=your-jwt-secret-key

# docker-compose.yml
version: '3.8'

services:
  app:
    image: ${APP_NAME}:${APP_VERSION}
    environment:
      - NODE_ENV=${NODE_ENV}
      - DATABASE_URL=postgresql://${DB_USER}:${DB_PASSWORD}@${DB_HOST}:${DB_PORT}/${DB_NAME}
      - REDIS_URL=redis://${REDIS_HOST}:${REDIS_PORT}
      - JWT_SECRET=${JWT_SECRET}
    env_file:
      - .env
```

### **3. Gestión de Salud**

```yaml
services:
  app:
    build: .
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:3000/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 40s
    
  db:
    image: postgres:15-alpine
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U admin -d myapp"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 30s

  # Usar health checks en depends_on
  app:
    depends_on:
      db:
        condition: service_healthy
```

### **4. Logging y Monitoreo**

```yaml
services:
  app:
    logging:
      driver: "json-file"
      options:
        max-size: "10m"
        max-file: "3"
        labels: "service=app"

  # Centralized logging con Fluentd
  fluentd:
    image: fluent/fluentd:latest
    volumes:
      - ./fluentd.conf:/fluentd/etc/fluent.conf
    ports:
      - "24224:24224"
      - "24224:24224/udp"
    networks:
      - logging-network

  # Prometeus para métricas
  prometheus:
    image: prom/prometheus:latest
    ports:
      - "9090:9090"
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml
      - prometheus_data:/prometheus
    networks:
      - monitoring-network

  # Grafana para visualización
  grafana:
    image: grafana/grafana:latest
    ports:
      - "3000:3000"
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin
    volumes:
      - grafana_data:/var/lib/grafana
    networks:
      - monitoring-network
```

### **5. Seguridad**

```yaml
services:
  app:
    # Usuario no root
    user: "1000:1000"
    
    # Read-only root filesystem
    read_only: true
    
    # Capacidades del sistema
    cap_drop:
      - ALL
    cap_add:
      - CHOWN
      - SETGID
      - SETUID
    
    # Sysctls
    sysctls:
      - net.core.somaxconn=65535
    
    # ULimits
    ulimits:
      nofile:
        soft: 20000
        hard: 40000
    
    # Secrets (no usar environment variables para secrets)
    secrets:
      - db_password
    environment:
      - DB_PASSWORD_FILE=/run/secrets/db_password

secrets:
  db_password:
    file: ./secrets/db_password.txt
```

### **6. Configuración por Entorno**

```yaml
# docker-compose.yml (base)
version: '3.8'

services:
  app:
    image: myapp:1.0.0
    environment:
      - NODE_ENV=production
    deploy:
      replicas: 2
      resources:
        limits:
          cpus: '1.0'
          memory: 1G

  db:
    image: postgres:15-alpine
    deploy:
      resources:
        limits:
          cpus: '0.5'
          memory: 512M

# docker-compose.override.yml (desarrollo)
version: '3.8'

services:
  app:
    environment:
      - NODE_ENV=development
    volumes:
      - ./src:/app/src  # Hot reload
    ports:
      - "3000:3000"
      - "9229:9229"
    command: npm run dev

  db:
    ports:
      - "5432:5432"  # Exponer para desarrollo

# docker-compose.prod.yml (producción)
version: '3.8'

services:
  app:
    deploy:
      replicas: 5
      placement:
        constraints:
          - node.role == worker
      resources:
        limits:
          cpus: '2.0'
          memory: 2G
    restart_policy:
      condition: any
      delay: 5s
      max_attempts: 3

  nginx:
    ports:
      - "80:80"
      - "443:443"
```

### **7. Optimización de Red**

```yaml
# Redes optimizadas
networks:
  # Red externa para servicios que necesitan internet
  frontend:
    driver: bridge
    ipam:
      driver: default
      config:
        - subnet: 172.20.0.0/16
  
  # Red interna para comunicación entre servicios
  backend:
    driver: bridge
    internal: true
  
  # Red dedicada para bases de datos
  database:
    driver: bridge
    internal: true
    ipam:
      driver: default
      config:
        - subnet: 172.21.0.0/16

services:
  nginx:
    networks:
      - frontend
      - backend

  app:
    networks:
      - backend
      - database

  db:
    networks:
      - database
```

---

## **Referencias Oficiales**

1. **Docker Compose Documentation**  
   https://docs.docker.com/compose/

2. **Compose File Reference**  
   https://docs.docker.com/compose/compose-file/

3. **Docker Compose CLI Reference**  
   https://docs.docker.com/compose/reference/

4. **Docker Compose in Production**  
   https://docs.docker.com/compose/production/

5. **Docker Compose Best Practices**  
   https://docs.docker.com/develop/dev-best-practices/
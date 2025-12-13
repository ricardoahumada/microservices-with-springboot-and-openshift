# Docker

## ¿Qué es Docker?

**Docker** es una plataforma de contenerización que permite desarrollar, enviar y ejecutar aplicaciones en contenedores ligeros y portátiles. Los contenedores empaquetan una aplicación junto con todas sus dependencias, bibliotecas y archivos de configuración, garantizando que la aplicación funcione consistentemente en cualquier entorno donde se ejecute.

### Conceptos Fundamentales

**Contenedor**: Entorno aislado que ejecuta una aplicación con sus dependencias. Ligero porque comparte el kernel del sistema operativo host.

**Imagen**: Plantilla de solo lectura que contiene todo lo necesario para ejecutar una aplicación (código, runtime, herramientas, librerías).

**Dockerfile**: Archivo de texto con instrucciones para crear una imagen de Docker.

**Registry**: Repositorio donde se almacenan y distribuyen las imágenes de Docker (Docker Hub, ECR, GCR).

**Docker Daemon**: Servicio en segundo plano que gestiona contenedores, imágenes, redes y volúmenes.

---

## **Herramientas Principales**

### **Docker Engine**
- Motor principal de Docker
- Gestiona contenedores, imágenes y redes
- Disponible para Linux, Windows y macOS

### **Docker CLI**
- Interfaz de línea de comandos
- Comando principal: `docker`
- Gestión de imágenes, contenedores y servicios

### **Docker Compose**
- Herramienta para definir aplicaciones multi-contenedor
- Utiliza archivos YAML para configuración
- Simplifica el desarrollo y despliegue

### **Docker Swarm**
- Orquestador de contenedores nativo
- Clustering de múltiples hosts Docker
- Gestión de servicios escalables

---

## **Comandos Principales**

### **Gestión de Imágenes**

```bash
# Construir imagen desde Dockerfile
docker build -t nombre-imagen:version .

# Descargar imagen desde registry
docker pull nombre-imagen:tag

# Subir imagen a registry
docker push nombre-usuario/nombre-imagen:tag

# Listar imágenes locales
docker images

# Eliminar imagen
docker rmi nombre-imagen:tag

# Ejecutar imagen
docker run [opciones] nombre-imagen

# Ejecutar con nombre personalizado
docker run --name mi-contenedor -p 8080:80 nginx:latest

# Ejecutar en background
docker run -d --name mi-app -p 8080:3000 mi-app

# Ejecutar con variables de entorno
docker run -e VAR1=valor1 -e VAR2=valor2 --name mi-app mi-app

# Ejecutar con volumen
docker run -v /ruta/host:/ruta/contenedor --name mi-app mi-app

# Ejecutar con red personalizada
docker run --network mi-red --name mi-app mi-app
```

### **Gestión de Contenedores**

```bash
# Listar contenedores en ejecución
docker ps

# Listar todos los contenedores
docker ps -a

# Iniciar contenedor parado
docker start mi-contenedor

# Parar contenedor
docker stop mi-contenedor

# Reiniciar contenedor
docker restart mi-contenedor

# Eliminar contenedor
docker rm mi-contenedor

# Eliminar contenedor forzadamente
docker rm -f mi-contenedor

# Ver logs de contenedor
docker logs mi-contenedor

# Seguir logs en tiempo real
docker logs -f mi-contenedor

# Ejecutar comando en contenedor
docker exec mi-contenedor comando

# Abrir shell en contenedor
docker exec -it mi-contenedor /bin/bash

# Inspeccionar contenedor
docker inspect mi-contenedor

# Estadísticas de contenedores
docker stats
```

### **Gestión de Redes**

```bash
# Crear red personalizada
docker network create mi-red

# Listar redes
docker network ls

# Conectar contenedor a red
docker network connect mi-red mi-contenedor

# Desconectar contenedor de red
docker network disconnect mi-red mi-contenedor

# Inspeccionar red
docker network inspect mi-red

# Eliminar red
docker network rm mi-red
```

### **Gestión de Volúmenes**

```bash
# Crear volumen
docker volume create mi-volumen

# Listar volúmenes
docker volume ls

# Inspeccionar volumen
docker volume inspect mi-volumen

# Eliminar volumen
docker volume rm mi-volumen

# Limpiar volúmenes no utilizados
docker volume prune
```

---

## **Dockerfile y Configuración**

### **Dockerfile Básico**

```dockerfile
# Usar imagen base oficial
FROM node:18-alpine

# Directorio de trabajo
WORKDIR /app

# Copiar package.json primero (para aprovechar cache de Docker)
COPY package*.json ./

# Instalar dependencias
RUN npm install

# Copiar código fuente
COPY . .

# Exponer puerto
EXPOSE 3000

# Comando por defecto
CMD ["npm", "start"]

# Usuario no root (buena práctica)
USER node

# Metadata
LABEL version="1.0" maintainer="equipo-dev@empresa.com"
```

### **Multi-stage Build**

```dockerfile
# Etapa de construcción
FROM node:18-alpine AS builder

WORKDIR /app
COPY package*.json ./
RUN npm ci --only=production

# Copiar código
COPY . .

# Construir aplicación
RUN npm run build

# Etapa de producción (imagen más pequeña)
FROM nginx:alpine AS production

# Copiar build desde builder
COPY --from=builder /app/dist /usr/share/nginx/html

# Configurar nginx
COPY nginx.conf /etc/nginx/nginx.conf

# Exponer puerto
EXPOSE 80

# Comando
CMD ["nginx", "-g", "daemon off;"]
```

### **Optimización de Imágenes**

```dockerfile
# Usar imágenes específicas de versión
FROM node:18.17.0-alpine

# Combinar comandos RUN para reducir capas
RUN apk add --no-cache \
    curl \
    && npm install -g pm2 \
    && rm -rf /var/cache/apk/*

# Limpiar cache de npm
RUN npm ci --only=production && npm cache clean --force

# Usar .dockerignore
# node_modules
# .git
# .gitignore
# Dockerfile
# .dockerignore
```

---

## **Ejemplos de YAMLS**

### **Aplicación Node.js con Dockerfile**

```yaml
# Dockerfile
FROM node:18-alpine

WORKDIR /app

COPY package*.json ./
RUN npm install --only=production

COPY . .

EXPOSE 3000

USER node

CMD ["npm", "start"]

# docker-compose.yml
version: '3.8'

services:
  app:
    build: .
    ports:
      - "3000:3000"
    environment:
      - NODE_ENV=production
      - DB_HOST=db
    depends_on:
      - db
    networks:
      - app-network

  db:
    image: postgres:15-alpine
    environment:
      - POSTGRES_DB=myapp
      - POSTGRES_USER=admin
      - POSTGRES_PASSWORD=password
    volumes:
      - postgres_data:/var/lib/postgresql/data
    networks:
      - app-network

volumes:
  postgres_data:

networks:
  app-network:
    driver: bridge
```

### **Aplicación con Múltiples Servicios**

```yaml
# docker-compose.yml
version: '3.8'

services:
  frontend:
    build: ./frontend
    ports:
      - "80:80"
    depends_on:
      - backend
    environment:
      - API_URL=http://backend:3000
    networks:
      - app-network

  backend:
    build: ./backend
    ports:
      - "3000:3000"
    environment:
      - DATABASE_URL=postgresql://admin:password@db:5432/myapp
      - REDIS_URL=redis://redis:6379
    depends_on:
      - db
      - redis
    networks:
      - app-network

  db:
    image: postgres:15-alpine
    environment:
      - POSTGRES_DB=myapp
      - POSTGRES_USER=admin
      - POSTGRES_PASSWORD=password
    volumes:
      - postgres_data:/var/lib/postgresql/data
    networks:
      - app-network

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    networks:
      - app-network

  nginx:
    image: nginx:alpine
    ports:
      - "443:443"
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf
      - ./ssl:/etc/nginx/ssl
    depends_on:
      - frontend
      - backend
    networks:
      - app-network

volumes:
  postgres_data:

networks:
  app-network:
    driver: bridge
```

---

## **Buenas Prácticas**

### **1. Optimización de Imágenes**

```dockerfile
# ✅ Usar imágenes específicas y pequeñas
FROM node:18-alpine

# ❌ Evitar imágenes :latest
# FROM node:latest

# ✅ Combinar comandos RUN
RUN apk add --no-cache curl && \
    npm install -g pm2

# ❌ Múltiples comandos RUN separados
# RUN apk add curl
# RUN npm install -g pm2

# ✅ Usar .dockerignore
# node_modules
# .git
# *.md
# .env
```

### **2. Seguridad**

```dockerfile
# ✅ Usuario no root
USER node

# ✅ No exponer secretos en imagen
# En lugar de: ENV API_KEY=123456789
# Usar: ARG API_KEY
# Y configurar en runtime

# ✅ Escaneo de vulnerabilidades
# Usar herramientas como Trivy, Clair
RUN apk add --no-cache --virtual .build-deps \
    curl && \
    curl -sSf https://get.trivy.sh | sh

# ✅ Mantener imágenes actualizadas
# Renov定期 actualizaciones de base images
```

### **3. Multi-stage Builds**

```dockerfile
# ✅ Separar build y runtime
FROM node:18-alpine AS builder

WORKDIR /app
COPY package*.json ./
RUN npm ci

COPY . .
RUN npm run build

FROM node:18-alpine AS runtime

WORKDIR /app
COPY --from=builder /app/dist ./dist
COPY --from=builder /app/node_modules ./node_modules
COPY package*.json ./

EXPOSE 3000
CMD ["node", "dist/index.js"]
```

### **4. Variables de Entorno**

```yaml
# docker-compose.yml con variables
version: '3.8'

services:
  app:
    build: .
    environment:
      - NODE_ENV=production
      - DB_HOST=db
      - DB_USER=${DB_USER:-admin}
      - DB_PASSWORD=${DB_PASSWORD}
    env_file:
      - .env
    ports:
      - "${APP_PORT:-3000}:3000"
    networks:
      - app-network
```

### **5. Health Checks**

```dockerfile
FROM node:18-alpine

WORKDIR /app

COPY package*.json ./
RUN npm install

COPY . .

EXPOSE 3000

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:3000/health || exit 1

CMD ["npm", "start"]
```

### **6. Logging y Monitoreo**

```yaml
# docker-compose.yml con logging
version: '3.8'

services:
  app:
    build: .
    logging:
      driver: "json-file"
      options:
        max-size: "10m"
        max-file: "3"
    environment:
      - NODE_ENV=production
    networks:
      - app-network

  # Log aggregation
  fluentd:
    image: fluent/fluentd:latest
    volumes:
      - ./fluentd.conf:/fluentd/etc/fluent.conf
    ports:
      - "24224:24224"
    networks:
      - app-network
```

### **7. Gestión de Redes**

```yaml
# Redes isoladas
version: '3.8'

services:
  app:
    build: .
    networks:
      - frontend
      - backend

  db:
    image: postgres:15-alpine
    networks:
      - backend
    # Solo accesible desde backend network

  redis:
    image: redis:7-alpine
    networks:
      - backend

  nginx:
    image: nginx:alpine
    ports:
      - "80:80"
    networks:
      - frontend

networks:
  frontend:
    driver: bridge
  backend:
    driver: bridge
    internal: true
```

---

## **Comandos Avanzados**

### **Docker Compose Avanzado**

```bash
# Iniciar servicios en segundo plano
docker-compose up -d

# Reconstruir servicios
docker-compose up -d --build

# Ver logs de todos los servicios
docker-compose logs -f

# Ejecutar comando en servicio
docker-compose exec app npm test

# Escalar servicios
docker-compose up -d --scale app=3

# Parar y eliminar todo
docker-compose down

# Parar y eliminar con volúmenes
docker-compose down -v

# Ver estado de servicios
docker-compose ps
```

### **Debugging y Monitoreo**

```bash
# Ver procesos en contenedor
docker top mi-contenedor

# Estadísticas detalladas
docker stats --no-stream

# Ver uso de disco
docker system df

# Limpiar recursos no utilizados
docker system prune -a

# Ver eventos de Docker
docker events --filter container=mi-contenedor

# Copiar archivos entre host y contenedor
docker cp mi-contenedor:/ruta/archivo ./archivo
```

### **Migración y Backup**

```bash
# Crear imagen desde contenedor
docker commit mi-contenedor mi-imagen:v1.0

# Exportar contenedor como tar
docker export mi-contenedor > contenedor.tar

# Importar imagen desde tar
docker import contenedor.tar mi-imagen:v1.0

# Backup de volúmenes
docker run --rm -v volumen-host:/data -v $(pwd):/backup \
  alpine tar czf /backup/volumen-backup.tar.gz /data

# Restaurar volumen
docker run --rm -v volumen-host:/data -v $(pwd):/backup \
  alpine tar xzf /backup/volumen-backup.tar.gz -C /
```

---

## **Referencias Oficiales**

1. **Docker Documentation**  
   https://docs.docker.com/

2. **Docker CLI Reference**  
   https://docs.docker.com/engine/reference/commandline/cli/

3. **Docker Compose Documentation**  
   https://docs.docker.com/compose/

4. **Docker Best Practices**  
   https://docs.docker.com/develop/dev-best-practices/

5. **Docker Hub Registry**  
   https://hub.docker.com/
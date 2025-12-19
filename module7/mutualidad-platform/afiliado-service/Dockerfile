# ====================================
# Etapa 1: Build
# ====================================
FROM maven:3.9-eclipse-temurin-11-alpine AS builder

WORKDIR /app

# Copiar POM primero para aprovechar cache de dependencias
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copiar código fuente y compilar
COPY src ./src
RUN mvn clean package -DskipTests -B

# ====================================
# Etapa 2: Runtime
# ====================================
FROM eclipse-temurin:11-jre-alpine

WORKDIR /app

# Crear usuario no privilegiado
RUN addgroup --system --gid 1001 appgroup && \
    adduser --system --uid 1001 --ingroup appgroup appuser

# Instalar curl para health checks
RUN apk add --no-cache curl

# Crear directorio de logs (permisos para OpenShift con UID aleatorio)
RUN mkdir -p /app/logs && chmod 777 /app/logs

# Copiar JAR desde etapa de build
COPY --from=builder /app/target/*.jar app.jar

# Cambiar propietario
RUN chown appuser:appgroup app.jar

# Cambiar a usuario no privilegiado
USER appuser

# Puerto de la aplicación
EXPOSE 8081

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8081/actuator/health || exit 1

# Configuración JVM optimizada para contenedores
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]

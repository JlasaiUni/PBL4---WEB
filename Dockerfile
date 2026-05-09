# ============================================================
#  MULTI-STAGE BUILD
#  Stage 1 – Build con Maven + Java 21
#  Stage 2 – Runtime con JRE Alpine (imagen mínima)
# ============================================================

# ── Stage 1: Build ───────────────────────────────────────────
FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /build

# Copiar solo el pom.xml primero para aprovechar la caché de capas
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Copiar el código fuente y compilar
COPY src ./src
RUN mvn package -DskipTests -q

# ── Stage 2: Runtime ─────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime

# Usuario no-root para mayor seguridad
RUN addgroup -S spring && adduser -S spring -G spring
USER spring

WORKDIR /app

# Copiar el JAR desde la etapa de build
COPY --from=builder /build/target/springboot-fullstack-template-1.0.0.jar app.jar

# Puerto expuesto
EXPOSE 8080

# Variables de entorno por defecto (sobreescribir en docker-compose o en runtime)
ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-Xms256m -Xmx512m"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

# Stage 1: Build the application using Maven
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /gym-trainer-be
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src src
RUN mvn clean package -DskipTests

# Stage 2: Extract layers for optimization
FROM eclipse-temurin:17-jre AS optimizer
WORKDIR /gym-trainer-be
COPY --from=builder /gym-trainer-be/target/*.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract

# Stage 3: Final Production Image
FROM eclipse-temurin:17-jre
WORKDIR /gym-trainer-be

# Install curl for healthchecks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Security: Create non-root user
RUN groupadd --system spring && \
    useradd --system --gid spring spring
USER spring:spring

# Copy extracted layers (NOT the JAR file)
COPY --from=optimizer /gym-trainer-be/dependencies/ ./
COPY --from=optimizer /gym-trainer-be/spring-boot-loader/ ./
COPY --from=optimizer /gym-trainer-be/snapshot-dependencies/ ./
COPY --from=optimizer /gym-trainer-be/application/ ./

# JVM Tuning
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"

ENV SPRING_PROFILES_ACTIVE=docker

EXPOSE 8080

# Use exec to properly handle graceful shutdown
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
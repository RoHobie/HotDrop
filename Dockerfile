# Stage 1: Build React + TypeScript Frontend
FROM node:20-alpine AS frontend-builder
WORKDIR /app/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci --prefer-offline
COPY frontend/ ./
RUN npm run build

# Stage 2: Build Spring Boot JAR & Extract Layers
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY pom.xml mvnw ./
COPY .mvn .mvn
RUN chmod +x ./mvnw && ./mvnw dependency:go-offline -B
COPY src ./src
COPY --from=frontend-builder /app/src/main/resources/static ./src/main/resources/static
RUN ./mvnw clean package -DskipTests
RUN java -Djarmode=tools -jar target/*.jar extract --layers --destination /app/extracted && \
    cd /app/extracted/application && mv *.jar app.jar

# Stage 2.5: Pre-generate Class Data Sharing (CDS) Archive
RUN cd /app/extracted && \
    mkdir -p /app/run && \
    cp -r dependencies/* /app/run/ && \
    cp -r spring-boot-loader/* /app/run/ 2>/dev/null || true && \
    cp -r snapshot-dependencies/* /app/run/ 2>/dev/null || true && \
    cp -r application/* /app/run/ && \
    cd /app/run && \
    java -XX:ArchiveClassesAtExit=application.jsa -Dspring.context.exit=onRefresh -jar app.jar || true

# Stage 3: Minimal JRE Runtime with Layer Caching & CDS
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Separate layers for Docker build caching
COPY --from=builder /app/extracted/dependencies/ ./
COPY --from=builder /app/extracted/spring-boot-loader/ ./
COPY --from=builder /app/extracted/snapshot-dependencies/ ./
COPY --from=builder /app/extracted/application/ ./
COPY --from=builder /app/run/application.jsa ./

EXPOSE 8080
ENTRYPOINT ["java", "-XX:SharedArchiveFile=application.jsa", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]

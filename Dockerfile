FROM maven:3.8.1-openjdk-11 AS builder
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:11-jre-jammy
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends curl \
  && rm -rf /var/lib/apt/lists/*
RUN groupadd -r appuser && useradd -r -g appuser appuser
COPY --from=builder /build/target/payment-service-*.jar payment-service.jar
RUN mkdir -p logs && chown -R appuser:appuser /app
USER appuser
EXPOSE 8005
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 CMD curl -f http://localhost:8005/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "payment-service.jar"]

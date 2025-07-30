# Stage 1: Build the application
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app

# Copy the entire project structure to handle the multi-module build
COPY .. .

# Build the payment-status-svc module
RUN mvn -f payment-status-svc/pom.xml clean package -DskipTests

# Stage 2: Create the production image
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy the runnable artifacts from the builder stage
COPY --from=builder /app/payment-status-svc/target/quarkus-app .

EXPOSE 8083

CMD ["java", "-jar", "quarkus-run.jar"]

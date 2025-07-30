# Running the Application with Docker

This document provides instructions on how to run the `payment-status-svc` application using Docker and Docker Compose for both production and development environments.

## Production Mode

To build and run the application in a production-like environment, use the provided `docker-compose.yml` file. This will start both the `payment-status-svc` application and a PostgreSQL database.

From the `payment-status-svc` directory, run the following command:

```bash
docker-compose up --build
```

This command will:
1.  Build the Docker image for the `payment-status-svc` using the provided `Dockerfile`.
2.  Start a PostgreSQL container.
3.  Start the `payment-status-svc` container, connected to the PostgreSQL container.

The application will be accessible on port `8083`.

## Development Mode

For development, you can run the application in a Docker container with hot-reloading enabled. This allows you to see your code changes without rebuilding the image.

From the root of the `CSV-Payments-PoC` project, run the following command:

```bash
docker run -it --rm -p 8083:8083 -v "$(pwd)":/app maven:3.9-eclipse-temurin-21 \
  mvn -f /app/payment-status-svc/pom.xml quarkus:dev
```

This command will:
1.  Start a Maven container.
2.  Mount your local project directory into the container.
3.  Start the Quarkus application in development mode, which enables hot-reloading.

The application will be accessible on port `8083`.


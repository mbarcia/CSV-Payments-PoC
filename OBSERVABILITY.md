# Observability Stack Documentation

## Overview

This document provides instructions on how to use the observability stack for the CSV Payments Processing Application. The stack includes:

- **Prometheus**: For collecting and storing metrics
- **Grafana**: For visualizing metrics and creating dashboards
- **Tempo**: For distributed tracing
- **Loki**: For log aggregation
- **OpenTelemetry Collector**: For collecting telemetry data from services

## Prerequisites

- Docker and Docker Compose
- curl (for testing)

## Starting the Observability Stack

To start just the observability stack (without the application services):

```bash
docker-compose -f observability-only.yml up -d
```

To start the full application with the observability stack:

```bash
docker-compose up -d
```

## Accessing the Components

Once the stack is running, you can access the components at the following URLs:

- **Grafana**: http://localhost:3000
  - Username: admin
  - Password: admin

- **Prometheus**: http://localhost:9090

- **Tempo**: http://localhost:3200

- **Loki**: http://localhost:3100

## Grafana Dashboards

The application includes a pre-configured Grafana dashboard that visualizes key metrics:

1. Navigate to http://localhost:3000
2. Log in with admin/admin
3. Go to "Dashboards" in the left sidebar
4. Select "CSV Payments Pipeline Dashboard"

The dashboard includes panels for:

- Requests per Second by Service
- Average Response Time by Service
- JVM Heap Memory Usage
- JVM GC Time Percentage

## Custom Metrics

The application exposes custom metrics to demonstrate reactive programming concepts:

- **Backpressure Events**: Count of backpressure events triggered by rate limiting
- **Retry Attempts**: Number of retry attempts with exponential back-off
- **Lazy Evaluation**: Metrics showing the benefits of lazy stream processing

## Distributed Tracing

OpenTelemetry provides distributed tracing capabilities that allow you to:

1. Follow a request as it flows through multiple services
2. Identify bottlenecks and performance issues
3. Understand the impact of retry mechanisms
4. Visualize the lazy evaluation of streams across services

To view traces in Grafana:

1. Navigate to http://localhost:3000
2. Go to "Explore" in the left sidebar
3. Select "Tempo" as the data source
4. Use the search functionality to find traces

## Log Aggregation

Logs from all services are collected by the OpenTelemetry Collector and stored in Loki. To view logs in Grafana:

1. Navigate to http://localhost:3000
2. Go to "Explore" in the left sidebar
3. Select "Loki" as the data source
4. Use the search functionality to find logs

## Metrics Endpoints

Each service exposes a `/q/metrics` endpoint that provides Prometheus-formatted metrics:

- Input CSV File Processing Service: http://localhost:8081/q/metrics
- Payments Processing Service: http://localhost:8082/q/metrics
- Payment Status Service: http://localhost:8083/q/metrics
- Output CSV File Processing Service: http://localhost:8084/q/metrics
- Data Persistence Service: http://localhost:8085/q/metrics
- Orchestrator Service: http://localhost:8080/q/metrics

## Testing the Stack

To test if all components are working correctly, run:

```bash
./test-observability.sh
```

This script will:
1. Start the observability stack
2. Wait for services to initialize
3. Check the health of each component
4. Report the status of each service

## Stopping the Stack

To stop the observability stack:

```bash
docker-compose -f observability-only.yml down
```

To stop the full application with the observability stack:

```bash
docker-compose down
```

## Troubleshooting

### Common Issues

1. **Services not starting**: Check if Docker has enough resources (CPU, memory)
2. **Grafana dashboards not loading**: Ensure the dashboard JSON files are correctly mounted
3. **Metrics not appearing**: Verify that services are correctly configured to export metrics
4. **Traces not appearing**: Check that the OpenTelemetry SDK is correctly configured in services

### Checking Service Logs

To check the logs of a specific service:

```bash
docker-compose logs <service-name>
```

For example:
```bash
docker-compose logs prometheus
docker-compose logs grafana
```

### Port Conflicts

If you encounter port conflicts, you can modify the port mappings in the docker-compose files:

1. `docker-compose.yml` for the full application
2. `observability-only.yml` for just the observability stack

### Data Persistence

The following directories are used for data persistence:

- `prometheus_data`: Prometheus metrics storage
- `grafana_data`: Grafana configuration and dashboards
- `tempo_data`: Tempo trace storage

These directories are mounted as Docker volumes and will persist data between container restarts.
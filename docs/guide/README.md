# Pipeline Framework Guide

Welcome to the Pipeline Framework guide! This guide will help you understand how to use the framework to build reactive pipeline processing systems.

## What You'll Learn

This guide covers everything you need to know about the Pipeline Framework:

1. **Getting Started**: Setting up the framework in your project
2. **Creating Pipeline Steps**: Building your first pipeline steps
3. **Application Structure**: Structuring pipeline applications
4. **Backend Services**: Creating backend services that implement pipeline steps
5. **Orchestrator Services**: Building orchestrator services that coordinate pipelines
6. **Pipeline Compilation**: Understanding how the annotation processor works
7. **Error Handling & DLQ**: Managing errors and dead letter queues
8. **Observability**: Monitoring and observing pipeline applications

## Prerequisites

Before diving into the Pipeline Framework, make sure you have:

- Java 21 or higher
- Maven 3.8+
- A Quarkus-based project (the framework is designed to work with Quarkus)
- Basic understanding of reactive programming concepts

## Quick Start

To quickly get started with the Pipeline Framework:

1. Add the framework dependencies to your `pom.xml`
2. Create your first pipeline step with the `@PipelineStep` annotation
3. Implement the step interface with your business logic
4. Build your project to trigger automatic adapter generation
5. Run your application to execute the pipeline

## Next Steps

Dive into the specific guides to learn more about each aspect of the Pipeline Framework:

- [Getting Started](/guide/getting-started): Detailed setup instructions
- [Creating Pipeline Steps](/guide/creating-steps): Building your first steps
- [Application Structure](/guide/application-structure): Structuring complex applications
- [Backend Services](/guide/backend-services): Implementing backend services
- [Orchestrator Services](/guide/orchestrator-services): Coordinating pipelines
- [Pipeline Compilation](/guide/pipeline-compilation): Understanding code generation
- [Error Handling & DLQ](/guide/error-handling): Managing failures
- [Observability](/guide/observability): Monitoring your applications

Happy pipelining!
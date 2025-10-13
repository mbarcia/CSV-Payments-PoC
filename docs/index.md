---
layout: home

hero:
  name: The Pipeline Framework
  text: Reactive Pipeline Processing
  tagline: Build scalable, resilient pipeline applications with Quarkus and Mutiny
  image:
    src: /logo.png
    alt: The Pipeline Framework
  actions:
    - theme: brand
      text: Get Started
      link: /guide/getting-started
    - theme: alt
      text: View on GitHub
      link: https://github.com/mbarcia/pipelineframework
    - theme: alt
      text: Design with Canvas
      link: https://app.pipelineframework.org

features:
  - title: Reactive by Design
    details: Built on Mutiny for non-blocking, high-performance applications
  - title: Annotation Driven
    details: Simple annotations generate complex infrastructure automatically
  - title: Visual Design
    details: Use the Canvas designer to create and configure pipelines visually
  - title: Observability First
    details: Built-in metrics, tracing, and logging support
  - title: Resilient by Default
    details: Comprehensive error handling with dead letter queues
---

<Callout type="tip" title="Visual Pipeline Designer Available">
The Pipeline Framework includes a visual canvas designer at <a href="https://app.pipelineframework.org" target="_blank">https://app.pipelineframework.org</a> that allows you to create and configure your pipelines using an intuitive drag-and-drop interface. This tool makes it easier to visualize the flow between steps and manage complex pipeline configurations.
</Callout>

## Introduction

The Pipeline Framework is a powerful tool for building reactive pipeline processing systems. It simplifies the development of distributed systems by providing a consistent way to create, configure, and deploy pipeline steps.

## Key Features

- **Reactive Programming**: Built on top of Mutiny for non-blocking operations
- **Annotation-Based Configuration**: Simplifies adapter generation with `@PipelineStep`
- **gRPC and REST Support**: Automatically generates adapters for both communication protocols
- **Modular Design**: Clear separation between runtime and deployment components
- **Auto-Generation**: Generates necessary infrastructure at build time
- **Observability**: Built-in metrics, tracing, and logging support
- **Error Handling**: Comprehensive error handling with DLQ support
- **Concurrency Control**: Virtual threads and backpressure management

## Getting Started

New to The Pipeline Framework? Start with our [Getting Started](/guide/getting-started) guide to learn the basics.

## Guides

To get started with The Pipeline Framework, explore these guides:

### Getting Started
- [Getting Started](/guide/getting-started.html): Setting up the framework in your project
- [Creating Pipeline Steps](/guide/creating-steps.html): Building your first pipeline steps

### Application Development
- [Application Structure](/guide/application-structure.html): Structuring pipeline applications
- [Backend Services](/guide/backend-services.html): Creating backend services that implement pipeline steps
- [Orchestrator Services](/guide/orchestrator-services.html): Building orchestrator services that coordinate pipelines

### Advanced Topics
- [Pipeline Compilation](/guide/pipeline-compilation.html): Understanding how the annotation processor works
- [Error Handling & DLQ](/guide/error-handling.html): Managing errors and dead letter queues
- [Observability](/guide/observability.html): Monitoring and observing pipeline applications

### Reference
- [Architecture](/reference/architecture.html): Deep dive into the framework architecture
- [Framework Overview](/FRAMEWORK_OVERVIEW.html): Complete architecture and comparison to original spec
- [Reference Implementation](/REFERENCE_IMPLEMENTATION.html): Complete implementation guide with examples
- [YAML Configuration Schema](/YAML_SCHEMA.html): Complete YAML schema documentation
- [Canvas Designer Guide](/CANVAS_GUIDE.html): Complete Canvas usage guide
- [Java-Centered Types](/JAVA_CENTERED_TYPES.html): Java-first approach with protobuf mapping

This approach reduces boilerplate code and ensures consistency across your pipeline steps.
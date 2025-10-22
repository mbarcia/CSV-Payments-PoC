# Introduction

The Pipeline Framework is a powerful tool for building reactive pipeline processing systems. It simplifies the development of distributed systems by providing a consistent way to create, configure, and deploy pipeline steps.

<Callout type="tip" title="Visual Pipeline Designer">
The Pipeline Framework includes a visual canvas designer at <a href="https://app.pipelineframework.org" target="_blank">https://app.pipelineframework.org</a> that allows you to create and configure your pipelines using an intuitive drag-and-drop interface. Simply design your pipeline visually, click "Download Application", and you'll get a complete ZIP file with all the generated source code - no command-line tools needed!
</Callout>

## Key Features

- **Reactive Programming**: Built on top of Mutiny for non-blocking operations
- **Visual Design Canvas**: Create and configure pipelines with the visual designer at <a href="https://app.pipelineframework.org" target="_blank">https://app.pipelineframework.org</a>
- **Annotation-Based Configuration**: Simplifies adapter generation with `@PipelineStep`
- **gRPC and REST Support**: Automatically generates adapters for both communication protocols
- **Modular Design**: Clear separation between runtime and deployment components

## Framework Overview

For complete documentation of the framework architecture, implementation details, and reference implementations, see the complete documentation files in the main repository:

- [Framework Overview](/FRAMEWORK_OVERVIEW.html) - Complete architecture and comparison to original spec
- [Reference Implementation](/REFERENCE_IMPLEMENTATION.html) - Complete implementation guide with examples
- [YAML Configuration Schema](/YAML_SCHEMA.html) - Complete YAML schema documentation
- [Canvas Designer Guide](/CANVAS_GUIDE.html) - Complete Canvas usage guide
- [Java-Centered Types](/JAVA_CENTERED_TYPES.html) - Comprehensive Java-first approach with automatic protobuf mapping

## How It Works

The framework allows you to define pipeline steps as simple classes annotated with `@PipelineStep`. The framework automatically generates the necessary adapters at build time, eliminating the need for manual configuration.

### Getting Started
- [Quick Start](/guide/quick-start): Get started quickly with the visual Canvas designer
- [Canvas Designer Guide](/CANVAS_GUIDE.html): Complete guide to using the visual designer
- [Using the Template Generator](/guide/using-template-generator): Advanced usage of the template generator

### Application Development
- [Application Structure](/guide/application-structure): Structuring pipeline applications
- [Backend Services](/guide/backend-services): Creating backend services that implement pipeline steps
- [Orchestrator Services](/guide/orchestrator-services): Building orchestrator services that coordinate pipelines

### Advanced Topics
- [Pipeline Compilation](/guide/pipeline-compilation): Understanding how the annotation processor works
- [Error Handling & DLQ](/guide/error-handling): Managing errors and dead letter queues
- [Observability](/guide/observability): Monitoring and observing pipeline applications

### Reference
- [Architecture](/reference/architecture): Deep dive into the framework architecture

This approach reduces boilerplate code and ensures consistency across your pipeline steps.
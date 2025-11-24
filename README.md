# The Pipeline Framework

[![Maven Central](https://img.shields.io/maven-central/v/org.pipelineframework/pipelineframework.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22org.pipelineframework%22%20AND%20a:%22pipelineframework%22)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Java 21+](https://img.shields.io/badge/Java-21+-brightgreen.svg)](https://adoptium.net/)
[![Quarkus](https://img.shields.io/badge/Quarkus-3.29.4-orange)](https://quarkus.io)
[![CodeRabbit](https://img.shields.io/coderabbit/prs/github/mbarcia/pipelineframework?label=CodeRabbit&color=purple
)](https://coderabbit.ai)

The Pipeline Framework is a powerful framework for building reactive pipeline processing systems. It simplifies the development of distributed systems by providing a consistent way to create, configure, and deploy pipeline steps with automatic gRPC and REST adapter generation.

## 🚀 Quick Start

### Visual Designer
The fastest way to get started is using our visual canvas designer:
- Visit [https://app.pipelineframework.org](https://app.pipelineframework.org)
- Design your pipeline visually
- Download the complete source code for your pipeline application

## 📋 For Different Audiences

### Product Owners
- **Microservices Architecture**: Each pipeline step can be deployed and scaled independently
- **Data Integrity**: Immutability ensures that all transformations are preserved, providing robust data integrity
- **Audit Trail**: Every input transformation is permanently recorded, enabling complete auditability
- **Reliability**: Built-in dead letter queue for error handling ensures resilient processing

### Architects
- **Immutability by Design**: No database updates during pipeline execution - only appends/preserves
- **Kubernetes & Serverless Ready**: Native builds enable quick start-up times for Kubernetes and serverless platforms (AWS Lambda, Google Cloud Run)
- **Cost Efficiency**: Reduced resource consumption with native builds and reactive processing
- **Scalability**: Each pipeline step can be scaled independently based on demand
- **gRPC & REST Flexibility**: High-performance gRPC for throughput-intensive tasks, REST for ease of integration
- **Multiple Persistence Options**: Choose from reactive (Panache) or virtual thread-based persistence models
- **Rich Step Types**: Support for OneToOne, OneToMany, ManyToOne, ManyToMany, and SideEffect processing patterns

### Developers
- **Monorepo Support**: Complete pipeline can be debugged step-by-step in a single repository
- **Compile-Time Safety**: All entities in common package provide type safety across steps
- **Reactive Processing**: Mutiny's event/worker thread model for efficient concurrency
- **Build-Time Generation**: Automatic gRPC and REST adapter generation at build time
- **gRPC & REST Flexibility**: Fast gRPC for high-throughput scenarios, REST for ease of development and integration
- **TLS Ready**: Reference implementation includes TLS configuration out of the box
- **Multiple Processing Patterns**: OneToOne, OneToMany, ManyToOne, ManyToMany, SideEffect and blocking variants
- **Health Monitoring**: Built-in health check capabilities for infrastructure monitoring

### QA Engineers
- **Unit Testing Excellence**: Each step deliberately kept small, making unit tests easy to write and fast to run
- **Deterministic Behavior**: Immutability ensures consistent test results across runs
- **AI-Assisted Development**: Modular structure is ideal for AI coding assistants
- **Focused Testing**: Each step's limited scope enables comprehensive testing
- **Multiple Execution Modes**: Test with different processing patterns (OneToOne, OneToMany, etc.) to validate behavior
- **Health Check Testing**: Built-in health monitoring provides additional testable endpoints

### CTOs
- **Cost Reduction**: Native builds and reactive processing reduce infrastructure costs
- **Developer Productivity**: Rapid development with visual designer and auto-generation
- **Operational Excellence**: Built-in observability, health checks, and error handling
- **Technology Modernization**: Reactive, cloud-native architecture with industry standards
- **Risk Mitigation**: Dead letter queue and multiple processing patterns provide resilience

## ✨ Key Features

- **Reactive Programming**: Built on top of Quarkus, Mutiny and Vert.x for non-blocking operations
- **Immutable Architecture**: No database updates during pipeline execution - only appends/preserves
- **Annotation-Based Configuration**: Simplifies adapter generation with `@PipelineStep`
- **gRPC & REST Flexibility**: Automatic adapter generation for fast gRPC or easy REST integration
- **Visual Design Canvas**: Create and configure pipelines with the visual designer
- **Modular Design**: Clear separation between runtime and deployment components
- **Auto-Generation**: Generates necessary infrastructure at build time
- **Observability**: Built-in metrics, tracing, and logging support
- **Error Handling**: Comprehensive error handling with DLQ support
- **Multiple Processing Patterns**: OneToOne, OneToMany, ManyToOne, ManyToMany, SideEffect and blocking variants
- **Health Monitoring**: Built-in health check capabilities
- **Concurrency Control**: Reactive processing with backpressure management
- **Cost Optimization**: Native builds for fast startup and reduced resource consumption
- **Multiple Persistence Models**: Choose from reactive or virtual thread-based persistence

## 📚 Documentation

- **[Full Documentation](https://pipelineframework.org)** - Complete documentation site
- **[Getting Started Guide](https://pipelineframework.org/guide/getting-started)** - Step-by-step tutorial
- **[Canvas Designer Guide](https://pipelineframework.org/CANVAS_GUIDE)** - Visual design tool
- **[API Reference](https://pipelineframework.org/annotations/pipeline-step)** - Annotation documentation

## 🏗️ Architecture

The framework follows a modular architecture:

```text
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   Orchestrator  │───▶│ Backend Service  │───▶│ Backend Service │
│   (Coordinates) │    │     (Step 1)     │    │     (Step 2)    │
└─────────────────┘    └──────────────────┘    └─────────────────┘
         │                        │                       │
         ▼                        ▼                       ▼
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│    Input Data   │    │   Processing     │    │   Output Data   │
│                 │    │   Pipeline       │    │                 │
└─────────────────┘    └──────────────────┘    └─────────────────┘
```

Each pipeline step is implemented as an independent service that can be scaled and deployed separately.

## 🛠️ Example Implementation

Here's a simple pipeline step implementation:

```java
@PipelineStep(
    order = 1,
    inputType = PaymentRecord.class,
    outputType = PaymentStatus.class,
    stepType = StepOneToOne.class,
    backendType = GenericGrpcReactiveServiceAdapter.class,
    autoPersist = true,
    runOnVirtualThreads = true
)
@ApplicationScoped
public class ProcessPaymentService implements ReactiveStreamingClientService<PaymentRecord, PaymentStatus> {
    
    @Override
    public Uni<PaymentStatus> process(PaymentRecord input) {
        // Business logic here
        PaymentStatus status = new PaymentStatus();
        // Process the payment record
        return Uni.createFrom().item(status);
    }
}
```

## 🤝 Contributing

Besides the obvious development contributions, anything, (yes! especially silly questions) are a contribution to this project.
Ask questions, test existing functionality, propose new features, engage in constructive debate. Thank you in advance!

## 📄 License

This project is licensed under the Apache License, Version 2.0 - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- Built with [Quarkus](https://quarkus.io) - Supersonic Subatomic Java
- Powered by [Mutiny](https://smallrye.io/smallrye-mutiny/) - Reactive programming toolkit
- Inspired by reactive, functional and OOP patters, helped by platform and devops experience 

---

<p align="center">
  Made with ❤️ by the Pipeline Framework team
</p>

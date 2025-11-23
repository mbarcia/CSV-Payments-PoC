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

### Programmatic Development
Add the framework to your Maven project:

```xml
<dependency>
  <groupId>org.pipelineframework</groupId>
  <artifactId>pipelineframework</artifactId>
  <version>0.9.0</version>
</dependency>
```

## ✨ Key Features

- **Reactive Programming**: Built on top of Quarkus, Mutiny and Vert.x for non-blocking operations
- **Annotation-Based Configuration**: Simplifies adapter generation with `@PipelineStep`
- **gRPC and REST Support**: Automatically generates adapters for both communication protocols  
- **Visual Design Canvas**: Create and configure pipelines with the visual designer
- **Modular Design**: Clear separation between runtime and deployment components
- **Test Integration**: Built-in support for unit and integration tests with Testcontainers
- **Auto-Generation**: Generates necessary infrastructure at build time
- **Observability**: Built-in metrics, tracing, and logging support
- **Error Handling**: Comprehensive error handling with DLQ support
- **Concurrency Control**: Virtual threads and backpressure management

## 📚 Documentation

- **[Full Documentation](https://pipelineframework.org)** - Complete documentation site
- **[Getting Started Guide](https://pipelineframework.org/guide/getting-started)** - Step-by-step tutorial
- **[Framework Overview](https://pipelineframework.org/FRAMEWORK_OVERVIEW)** - Architecture and concepts
- **[Canvas Designer Guide](https://pipelineframework.org/CANVAS_GUIDE)** - Visual design tool
- **[API Reference](https://pipelineframework.org/annotations/pipeline-step)** - Annotation documentation

## 🏗️ Architecture

The framework follows a modular architecture:

```
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

## 🧪 Testing

The framework includes comprehensive testing support:

```java
@QuarkusTest
class ProcessPaymentServiceTest {
    
    @Test
    void testProcessPayment() {
        // Your test implementation
    }
}
```

## 🚀 Building

To build the framework:

```bash
./mvnw clean install
```

To build the documentation:

```bash
cd docs
npm install
npm run build
```

## 🤝 Contributing

We welcome contributions! Here's how you can help:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Make your changes
4. Commit your changes (`git commit -m 'Add amazing feature'`)
5. Push to the branch (`git push origin feature/amazing-feature`)
6. Open a Pull Request

Check out our [Contributing Guide](CONTRIBUTING.md) for more details.

## 📄 License

This project is licensed under the Apache License, Version 2.0 - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- Built with [Quarkus](https://quarkus.io) - Supersonic Subatomic Java
- Powered by [Mutiny](https://smallrye.io/smallrye-mutiny/) - Reactive programming toolkit
- Inspired by reactive design patterns and microservices architecture principles

---

<p align="center">
  Made with ❤️ by the Pipeline Framework team
</p>
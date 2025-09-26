# Introduction

The Pipeline Framework is a powerful tool for building reactive pipeline processing systems. It simplifies the development of distributed systems by providing a consistent way to create, configure, and deploy pipeline steps.

## Key Features

- **Reactive Programming**: Built on top of Mutiny for non-blocking operations
- **Annotation-Based Configuration**: Simplifies adapter generation with `@PipelineStep`
- **gRPC and REST Support**: Automatically generates adapters for both communication protocols
- **Modular Design**: Clear separation between runtime and deployment components

## How It Works

The framework allows you to define pipeline steps as simple classes annotated with `@PipelineStep`. The framework automatically generates the necessary adapters at build time, eliminating the need for manual configuration.

# Introduction

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

## How It Works

The framework allows you to define pipeline steps as simple classes annotated with `@PipelineStep`. The framework automatically generates the necessary adapters at build time, eliminating the need for manual configuration.

```java
@PipelineStep(
   order = 1,
   stub = MyGrpc.MyStub.class,
   inboundMapper = FooRequestToDomainMapper.class,
   outboundMapper = DomainToBarResponseMapper.class
)
public class MyPipelineStep implements StepOneToOne<FooRequest, BarResponse> {
    @Override
    public Uni<BarResponse> apply(Uni<FooRequest> request) {
        // Your implementation here
        return request.map(req -> {
            // Transform the request
            return new BarResponse();
        });
    }
}
```

<Callout type="info" title="Getting Started">
New to The Pipeline Framework? Start with our <a href="/guide/getting-started">Getting Started</a> guide to learn the basics.
</Callout>

## Guides

To get started with The Pipeline Framework, explore these guides:

### Getting Started
- [Getting Started](/guide/getting-started): Setting up the framework in your project
- [Creating Pipeline Steps](/guide/creating-steps): Building your first pipeline steps

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
# Pipeline Framework

A framework for building reactive pipeline processing systems.

## Annotation-Based Automatic Adapter Generation

This framework now supports annotation-based automatic generation of gRPC and REST adapters, which simplifies the development of pipeline steps by eliminating the disconnect between step configuration and adapter configuration.

### New Annotations

#### @PipelineStep

The `@PipelineStep` annotation is used to mark a class as a pipeline step. This annotation enables automatic generation of gRPC and REST adapters.

```java
@PipelineStep(
   order = 1,
   autoPersist = true,
   debug = true,
   recoverOnFailure = true,
   stub = MyGrpc.MyStub.class,
   inboundMapper = FooRequestToDomainMapper.class,
   outboundMapper = DomainToBarResponseMapper.class
)
```

#### @MapperForStep

The `@MapperForStep` annotation is used to mark mapper classes that handle conversions between different representations of the same entity: domain, DTO, and gRPC formats.

```java
@MapperForStep(
    order = 3,
    grpcType = PaymentsProcessingSvc.AckPaymentSent.class,
    domainType = AckPaymentSent.class
)
```

### Benefits

1. **Reduced Boilerplate**: Developers no longer need to manually configure adapters
2. **Consistent Configuration**: Configuration is defined in one place (the step implementation)
3. **Improved Developer Experience**: Simpler, more intuitive API
4. **Reduced Errors**: Less manual configuration reduces the chance of errors
5. **Better Maintainability**: Configuration changes only need to be made in one place
6. **Architectural Integrity**: Maintains proper separation of concerns between orchestrator and service modules

### Module Structure

- `runtime`: Contains the annotations, mapper interfaces, and generic adapter classes
- `deployment`: Contains the Quarkus build processor that scans for annotations and generates adapter beans

### Usage

Developers only need to:

1. Annotate their service class with `@PipelineStep`
2. Annotate their mapper classes with `@MapperForStep`
3. Implement the mapper interfaces (`InboundMapper`, `OutboundMapper`)
4. Implement the service interface (`StepOneToOne`, etc.)

The framework automatically generates and registers the adapter beans at build time.
# Proposal: Annotation-Based Automatic Adapter Generation

## Overview

This proposal outlines a solution to simplify the development of pipeline steps by using annotations to automatically generate gRPC and REST adapters. 
This approach will eliminate the disconnect between step configuration and adapter configuration, making it easier for developers to create and maintain pipeline steps.

## Motivation

In pipeline-framework, we provide gRPC adapter classes for users to build their own @GrpcClient annotated endpoints. These adapter classes provide a `remoteProcess()` functional interface that wraps the `process()` interface of the backend services.
The adapter takes 2 Mappers: one for converting the input gRPC type to domain type and the other to convert the output domain type to gRPC. 
Then it also injects a backend service to wrap the "process()" functional interface call. 
I've now built annotation classes so that the backend service can be annotated with more information. 
Proposed additional information is: the input/output domain types, the input/output gRPC types. 
Each mapper is also annotated, and I'm proposing to add: the domain type, the grpc type, and the DTO type (for REST endpoints).
Now what I would like to do next is make users' lives easier by registering a grpc client class in CDI at runtime, so the developers don't need to write all this boilerplate code.

## Current Challenges

1. **Configuration Disconnect**: Developers must configure steps in two separate places:
   - In the step implementation (using `liveConfig().overrides()`)
   - In the gRPC/REST adapters (by overriding `getStepConfig()`)

2. **Boilerplate Code**: Developers must manually create and maintain adapter classes with significant boilerplate code.

3. **Error-Prone**: Manual configuration increases the likelihood of errors and inconsistencies.

## Proposed Solution

### 1. New Annotations

#### @PipelineStep

The `@PipelineStep` annotation is used to mark a class as a pipeline step. This annotation enables automatic generation of gRPC and REST adapters.

```java
@PipelineStep(
    order = 1,
    autoPersist = true,
    debug = true,
    recoverOnFailure = true,
    inputType = String.class,
    outputType = CsvPaymentsInputFile.class
)
```

The annotation supports the following configuration options:

1. `order`: The order of this step in the pipeline (default: 0)
2. `autoPersist`: Whether to enable auto-persistence for this step (default: false)
3. `debug`: Whether to enable debug mode for this step (default: false)
4. `recoverOnFailure`: Whether to enable failure recovery for this step (default: false)
5. `inputType`: The input type for this pipeline step (default: Object.class)
6. `outputType`: The output type for this pipeline step (default: Object.class)

#### @MapperForStep

```java
@MapperForStep(
    order = 3,
    grpc = PaymentsProcessingSvc.AckPaymentSent.class,
    dto = AckPaymentSentDto.class,
    domain = AckPaymentSent.class
)
```

This annotation marks a mapper class and provides metadata about the entity it maps between different representations (domain, DTO, gRPC).

### 2. Automatic Configuration

The framework will automatically:
1. Read configuration from the `@PipelineStep` annotation
2. Find its associated @MapperForStep mapper(s)
3. Know which gRPC input/output types belong to it
2. Make this configuration available to adapters through the `StepConfigProvider`
3. Enable features like auto-persistence based on the annotation values
4. Crucially, generate an adapter bean that does:
   gRPC in  → mapper.fromGrpc() → domain → service.process() → domain out → mapper.toGrpc() → gRPC out

   so that the developer only writes the service + mapper classes, and your framework wires the rest.

This is exactly the kind of thing Quarkus “deployment” modules are made for.

## Implementation Details

1.	At build time (deployment module):
   •	Scan all classes annotated with @PipelineStep to gather the metadata:
   •	backend service class
   •	gRPC stub type, inputType, outputType
   •	maybe qualifiers, etc.
   •	Scan all classes annotated with @MapperForStep to build a mapping of step → mapper (and gRPC/DTO/domain types).
2. For each discovered step:
   •	generate or register a CDI bean of type io.grpc.BindableService (or your adapter interface) that implements the gRPC service and uses the mapper + backend service under the hood.
3. The developer only writes:
   •	@PipelineStep service with domain logic
   •	@MapperForStep mapper(s)
   •	no gRPC adapter at all.

### How to register that adapter bean
```java
beans.produce(SyntheticBeanBuildItem.configure(MyGeneratedGrpcAdapter.class)
    .types(BindableService.class, MyGeneratedGrpcAdapter.class)
    .scope(ApplicationScoped.class)
    .setRuntimeInit()
    .supplier(ctx -> {
        var mapper = ctx.beanInstance(mapperClass);
        var service = ctx.beanInstance(serviceClass);
        return new MyGeneratedGrpcAdapter(mapper, service);
    })
    .done());
```

Because you’re not sure of the concrete adapter class ahead of time, during the build step, emit a small .java or .class implementing BindableService per step (Quarkus has GeneratedBeanBuildItem for this).
Then register that generated class as a bean.

```java
@BuildStep
void registerPipelineSteps(CombinedIndexBuildItem index,
                           BuildProducer<SyntheticBeanBuildItem> beans) {
    // collect all mappers first
    Map<Class<?>, Class<?>> stepToMapper = ...;

    for (ClassInfo ci : index.getIndex().getKnownClasses()) {
        if (ci.hasAnnotation(PipelineStep.class.getName())) {
            var ann = ci.annotation(PipelineStep.class.getName());
            String serviceClassName = ci.name().toString();
            Class<?> serviceClass = Class.forName(serviceClassName);
            Class<?> mapperClass = stepToMapper.get(serviceClass);

            beans.produce(SyntheticBeanBuildItem.configure(GenericGrpcAdapter.class)
                .types(BindableService.class) // so Quarkus picks it up
                .scope(ApplicationScoped.class)
                .setRuntimeInit()
                .supplier(ctx -> {
                    Object service = ctx.beanInstance(serviceClass);
                    Object mapper = ctx.beanInstance(mapperClass);
                    return new GenericGrpcAdapter(service, mapper, ann.inputType(), ann.outputType());
                })
                .done());
        }
    }
}
```

GenericGrpcAdapter in your runtime module would implement the gRPC BindableService interface, delegate to the mapper and backend service, and know how to call the stub.

You can assume a service is gRPC-enabled if the stub property is present in the annotation.

The developer only does:
```java
@PipelineStep(inputType = MyRequest.class,
              outputType = MyResponse.class,
              stub = MyGrpc.MyStub.class)
@ApplicationScoped
public class MyStepService {
    public DomainOut process(DomainIn in) { … }
}

@MapperForStep(inputType = MyRequest.class,
                outputType = MyResponse.class,
                domainInputType = DomainIn.class,
                domainOutputType = DomainOut.class)
@Mapper(componentModel = "cdi")
public interface MyStepMapper {
    DomainIn fromGrpc(MyRequest req);
    MyResponse toGrpc(DomainOut out);
}
```

Your extension generates and registers the adapter.
When the app runs, Quarkus already has a BindableService bean for gRPC server startup — no boilerplate.

TL;DR
•	Use Quarkus’s deployment module to scan @PipelineStep and @MapperForStep.
•	Register either a generic adapter bean or generate one per step with SyntheticBeanBuildItem.
•	That bean implements the actual gRPC endpoint, doing the mapping + service call internally.
•	Developers only annotate; your framework wires it.

## Benefits

1. **Reduced Boilerplate**: Developers no longer need to manually configure adapters
2. **Consistent Configuration**: Configuration is defined in one place (the step implementation)
3. **Improved Developer Experience**: Simpler, more intuitive API
4. **Reduced Errors**: Less manual configuration reduces the chance of errors
5. **Better Maintainability**: Configuration changes only need to be made in one place
6. **Architectural Integrity**: Maintains proper separation of concerns between orchestrator and service modules

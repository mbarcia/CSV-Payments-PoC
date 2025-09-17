# Proposal: Annotation-Based Automatic Adapter Generation

## Overview

This proposal outlines a solution to simplify the development of pipeline steps by using annotations to automatically generate gRPC and REST adapters. This approach will eliminate the disconnect between step configuration and adapter configuration, making it easier for developers to create and maintain pipeline steps.

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
2. Make this configuration available to adapters through the `StepConfigProvider`
3. Enable features like auto-persistence based on the annotation values

### 3. Adapter Generation (Future Enhancement)

In a future phase, we could implement automatic generation of:
- gRPC service adapters
- REST resource adapters
- Configuration wiring

## Implementation Details

### Step 1: Annotation Support (Completed)

- Created `@PipelineStep` and `@MapperForStep` annotations
- Modified `ConfigurableStep` to read configuration from annotations
- Updated adapters to automatically access step configuration

### Step 2: Configuration Bridge (Completed)

- Created `StepConfigProvider` to bridge configuration between steps and adapters
- Modified `GrpcReactiveServiceAdapter` and `RestReactiveServiceAdapter` to use automatic configuration
- Updated existing adapters to set the step class (for adapters in the orchestrator module) or use direct configuration (for adapters in service modules)

### Step 3: Developer Experience Improvements (Completed)

- Updated existing steps and mappers to use the new annotations
- Simplified adapter implementations by removing manual configuration
- Added `@MapperForStep` annotations to all mapper classes in the common module with correct entity type information
- Maintained proper architectural boundaries (no circular dependencies)

## Benefits

1. **Reduced Boilerplate**: Developers no longer need to manually configure adapters
2. **Consistent Configuration**: Configuration is defined in one place (the step implementation)
3. **Improved Developer Experience**: Simpler, more intuitive API
4. **Reduced Errors**: Less manual configuration reduces the chance of errors
5. **Better Maintainability**: Configuration changes only need to be made in one place
6. **Architectural Integrity**: Maintains proper separation of concerns between orchestrator and service modules

## Future Enhancements

1. **Automatic Adapter Generation**: Use annotation processing to automatically generate gRPC and REST adapters
2. **Configuration Validation**: Validate that step orders match between steps and mappers
3. **IDE Support**: Provide IDE plugins for better developer experience
4. **Documentation Generation**: Automatically generate documentation from annotations

## Migration Path

1. Existing steps can gradually adopt the new annotations
2. No breaking changes to existing code
3. Backward compatibility maintained for manually configured adapters
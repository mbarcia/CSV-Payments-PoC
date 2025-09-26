# Proposal: Out-of-the-box default Pipeline CLI application

## Overview

I want to provide user of the framework with an out-of-the-box CLI application to run the steps configured through 
@pipeline-framework/runtime/src/main/java/io/github/mbarcia/pipeline/annotation/PipelineStep.java annotations on the services.
This application needs to be annotated with QuarkusMain and also with Command (from picocli), as seen on                                                    
@orchestrator-svc/src/main/java/io/github/mbarcia/csv/CsvPaymentsApplication.java . However,    
@orchestrator-svc/ belongs to the "reference implementation", and I want to provide this as     
part of the pipeline framework features, so that developers don't need to write ANY             
application class. Conversely, if the developer does want to write his/her own application class, they       
should be able to do so. 

So far, efforts to provide this feature have resulted in frustration, as the introduction of such a class with a QuarkusMain annotation interferes with the backend services which are NOT supposed to "run" like that. 
There is currently an implementation in @pipeline-framework/deployment/src/main/java/io/github/mbarcia/pipeline/processor/PipelineProcessor.java that works with PipelineApplication.
But I don't like with this design. I believe the application class should be 100% generated, and instruct the developer with a (generated documentation as part of the build) list of steps that they can inject.


## Proposed Solution

PipelineApplication should be a generic class in the framework, and it should:
- inject a PipelineRunner, which is also a generic class
- be extensible by the developer
- incorporate the stopwatch, the latch sync mechanism, the exiter, as can be seen in CsvApplication
- be 100% generic (not depending on CSV Payments at all)

Notes:
- PipelineRunner should also be extensible or replaceable by the developer if they so wish/need.

Now, enlisting the help of PipelineProcessor, 
module, create a concrete class for the orchestrator that
- injects all the newly generated steps
- assembles a list with all the steps and pass it on to the runner.
- is annotated with QuarkusMain
- is annotated with Command

They key here is when NOT to generate the class. So, to do that, we'll use the application.properties
`pipeline.generate-cli=true`

and we'll ask before we generate the CLI class
```java
class MyProcessor {
  @BuildStep
  void generateCliClass(BuildTimeConfig config, BuildProducer<GeneratedClassBuildItem> classes) {
    if (!config.generateCli) {
        return; // skip
    }
    // … generate CLI class here
  }
}
```

The current implementation of generatePipelineApplicationClass() can be scrapped if needed (although I'm pretty sure 
a good chunk will be still useful). Also, the current implementation of PipelineApplication or even PipelineRunner 
can be scrapped as needed.

## Module layout

```markdown
pipeline-framework/
├─ runtime/
│ ├─ META-INF extension descriptor
│ ├─ @PipelineStep annotation and StepConfigProvider
│ ├─ config classes
│ ├─ BaseEntity and persistence
│ ├─ Mapper inbound and outbound interfaces
│ ├─ Holy trinity of GenericGrpcAdapter(s)
│ ├─ PipelineRunner
│ ├─ PipelineApplication
│ └─ Steps hierarchy (s)
└─ deployment/
└─ PipelineProcessor.java (Quarkus build steps for the grpc adapters and pipeline steps)
 ```

- runtime is what the users depend on.
- deployment is discovered automatically by Quarkus

## Important

The framework must NOT have any dependencies on the csv payments system.
When running `mvn`, skip the formatting by `-Dspotless.formatting=false`

## Benefits

1. **Reduced Boilerplate**: Developers no longer need to manually configure adapters
3. **Improved Developer Experience**: Simpler, more intuitive API

## Key Knowledge
- **Technology Stack**: Java 21 with preview features, Quarkus 3.28.0.CR1, Maven, gRPC, Mutiny reactive streams
- **Architecture**: Pipeline Framework with runtime and deployment modules; runtime contains annotations and base classes, deployment contains Quarkus build processors
- **Core Components**: @PipelineStep annotation, GenericGrpcAdapter hierarchy, Steps hierarchy, PipelineProcessor (server-side and client-side)
- **Key Convention**: Generated classes should follow naming pattern `{OriginalClassName}GrpcService` and `{OriginalClassName}Step`
- **Build Command**: `mvn clean compile -Dmaven.test.skip=true -Dspotless.check.skip=true`
- **Testing Approach**: Use run-e2e-test.sh for integration testing of the complete pipeline

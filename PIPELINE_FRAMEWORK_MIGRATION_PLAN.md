# Pipeline Framework Migration Plan

## Current State Analysis

We currently have two approaches for generating pipeline grpcService implementations:

1. **PipelineProcessor** (Quarkus build step approach):
   - Uses Quarkus build steps and Jandex for annotation scanning
   - Generates step adapters and StepsRegistry at build time
   - Integrates with Quarkus CDI system through SyntheticBeanBuildItem
   - Works with the recorder pattern for runtime instantiation

2. **ClientStepProcessor** (Annotation processor approach):
   - Uses stando g to generate client step implementations
   - Generates client steps with proper CDI annotations (@GrpcClient, @Inject)
   - Places generated classes in the same module as the annotated service

## Issues Identified

1. **CDI Integration Complexity**: The annotation processor approach struggles with proper CDI integration for the generated classes
2. **Class Loading**: Generated classes need to be properly recognized by Quarkus's CDI container for dependency injection to work
3. **StepsRegistry Generation**: The annotation processor needs to generate a registry that can be used by the PipelineRunner
4. **Architecture Separation**: Generated client steps should ideally be in the orchestrator module, not scattered across service modules

## Goals

1. Replace the PipelineProcessor with the ClientStepProcessor approach entirely
2. Eliminate the Jandex-based annotation scanning
3. Remove the complex recorder pattern for StepsRegistry instantiation
4. Generate all pipeline components (client steps, server steps, registry) through annotation processing
5. Ensure proper CDI integration for all generated components
6. Maintain backward compatibility with existing code

## Proposed Solution

### Phase 1: Enhance ClientStepProcessor

1. **Modify ClientStepProcessor** to:
   - Track all generated client step class names
   - Generate a StepsRegistry implementation class at the end of processing
   - Place generated classes in appropriate packages for CDI discovery

2. **StepsRegistry Generation**:
   - Generate a concrete implementation of StepsRegistry interface
   - Use proper CDI annotations (@ApplicationScoped)
   - Implement getSteps() method that returns instances of all generated client steps

3. **CDI Integration**:
   - Ensure generated classes have proper CDI scope annotations
   - Use @GrpcClient and @Inject annotations correctly
   - Make sure classes are discoverable by Quarkus's CDI scanning

### Phase 2: Remove Old PipelineProcessor

1. **Delete PipelineProcessor.java**:
   - Remove the entire file and its associated build steps
   - Eliminate Jandex-based annotation scanning
   - Remove SyntheticBeanBuildItem usage

2. **Remove Recorder Classes**:
   - Delete StepsRegistryRecorder.java
   - Remove all reflection-based instantiation code

### Phase 3: Update PipelineRunner

1. **Simplify PipelineRunner**:
   - Remove reflection-based registry instantiation
   - Use direct CDI injection of StepsRegistry
   - Simplify the step retrieval logic

## Implementation Details

### ClientStepProcessor Modifications

```java
// Add field to track generated client steps
private final List<String> generatedClientStepClassNames = new ArrayList<>();

// Modify process method to generate StepsRegistry after all client steps
@Override
public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    // Existing client step generation logic
    
    // After processing all @PipelineStep annotations, generate StepsRegistry
    if (roundEnv.processingOver() && !generatedClientStepClassNames.isEmpty()) {
        generateStepsRegistry();
    }
    
    return true;
}

// Generate StepsRegistry implementation
private void generateStepsRegistry() throws IOException {
    String packageName = "org.pipelineframework.generated";
    String className = "StepsRegistryImpl";
    
    // Create StepsRegistryImpl class with @ApplicationScoped annotation
    TypeSpec.Builder registryBuilder = TypeSpec.classBuilder(className)
        .addModifiers(Modifier.PUBLIC)
        .addSuperinterface(ClassName.get("org.pipelineframework", "StepsRegistry"))
        .addAnnotation(AnnotationSpec.builder(ClassName.get("jakarta.enterprise.context", "ApplicationScoped")).build());
    
    // Add getSteps method that returns CDI-managed instances
    MethodSpec.Builder getStepsMethod = MethodSpec.methodBuilder("getSteps")
        .addAnnotation(Override.class)
        .addModifiers(Modifier.PUBLIC)
        .returns(ParameterizedTypeName.get(ClassName.get("java.util", "List"), ClassName.get(Object.class)));
    
    getStepsMethod.addStatement("$T<Object> stepsList = new $T<>()", 
        ClassName.get("java.util", "ArrayList"), 
        ClassName.get("java.util", "ArrayList"));
        
    // Use CDI to get instances of all generated client steps
    for (String stepClassName : generatedClientStepClassNames) {
        getStepsMethod.addStatement("stepsList.add($T.current().select($T.class).get())", 
            ClassName.get("jakarta.enterprise.inject", "CDI"),
            ClassName.bestGuess(stepClassName));
    }
    
    getStepsMethod.addStatement("return stepsList");
    registryBuilder.addMethod(getStepsMethod.build());
    
    // Write the generated class
    TypeSpec registryClass = registryBuilder.build();
    JavaFile javaFile = JavaFile.builder(packageName, registryClass).build();
    JavaFileObject builderFile = processingEnv.getFiler().createSourceFile(packageName + "." + className);
    
    try (var writer = builderFile.openWriter()) {
        javaFile.writeTo(writer);
    }
}
```

### PipelineRunner Simplification

```java
// Simplified PipelineRunner with direct CDI injection
@ApplicationScoped
public class PipelineRunner implements AutoCloseable {

    @Inject
    StepsRegistry stepsRegistry; // Direct injection instead of Instance<StepsRegistry>

    public Object run(Multi<?> input) {
        List<Object> steps = stepsRegistry.getSteps();
        // Rest of implementation remains the same
    }
    
    // Rest of methods unchanged
}
```

## Benefits of This Approach

1. **Cleaner Architecture**: Eliminates complex recorder pattern and reflection-based instantiation
2. **Better CDI Integration**: Generated classes are properly managed by CDI container
3. **Simpler Code**: Removes Jandex scanning and build step complexity
4. **Faster Builds**: Annotation processing runs during compilation, potentially faster than build steps
5. **Type Safety**: Compile-time generation provides better type safety than runtime reflection

## Potential Challenges

1. **Classpath Issues**: Ensuring generated classes are in the right classpath for CDI discovery
2. **Build Order**: Making sure annotation processing completes before CDI scanning
3. **Module Dependencies**: Managing dependencies between modules that contain generated classes
4. **Testing**: Verifying that the new approach works correctly in all scenarios

## Rollout Strategy

1. **Implement ClientStepProcessor enhancements** in a feature branch
2. **Test with a single module** to verify the approach works
3. **Gradually roll out** to all pipeline modules
4. **Remove old PipelineProcessor** once new approach is validated
5. **Update documentation** and examples
6. **Run full integration tests** to ensure no regressions

## Files to be Modified/Removed

### Modified:
- `pipeline-framework/deployment/src/main/java/io/github/mbarcia/pipeline/processor/ClientStepProcessor.java`

### Removed:
- `pipeline-framework/deployment/src/main/java/io/github/mbarcia/pipeline/processor/PipelineProcessor.java`
- `pipeline-framework/runtime/src/main/java/io/github/mbarcia/pipeline/recorder/StepsRegistryRecorder.java`

### Potentially Modified:
- `pipeline-framework/runtime/src/main/java/io/github/mbarcia/pipeline/PipelineRunner.java`

## Validation Criteria

1. All existing integration tests pass
2. Generated client steps are properly discovered by CDI
3. @GrpcClient injection works correctly in generated steps
4. StepsRegistry correctly returns all generated step instances
5. Pipeline execution works end-to-end
6. No class loading or reflection errors at runtime
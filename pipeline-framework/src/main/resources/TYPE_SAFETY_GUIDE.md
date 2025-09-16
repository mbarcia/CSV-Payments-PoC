# Pipeline Framework Developer Guide

## Understanding Type Safety in Pipeline Steps

The pipeline framework uses `Multi<Object>` for input and output streams to maintain flexibility in connecting different step types. While this sacrifices some compile-time type safety for runtime flexibility, there are ways to improve the developer experience.

## Best Practices for Type Safety

### 1. Use Specialized Step Interfaces

Instead of implementing the base `Step` interface directly, use specialized interfaces that provide better type safety:

```java
// Good: Using specialized interface
public class StringLengthStep implements StepOneToOne<String, Integer> {
    @Override
    public Uni<Integer> applyAsyncUni(String input) {
        return Uni.createFrom().item(input.length());
    }
    
    @Override
    public StepConfig effectiveConfig() {
        return new StepConfig();
    }
}

// Avoid: Direct implementation of base Step
public class BadStep implements Step {
    @Override
    public Multi<Object> apply(Multi<Object> input) {
        // Manual casting required, prone to ClassCastException
        return input.onItem().transformToUniAndMerge(item -> {
            String str = (String) item; // Runtime cast that can fail
            return Uni.createFrom().item(str.length());
        });
    }
    
    @Override
    public StepConfig effectiveConfig() {
        return new StepConfig();
    }
}
```

### 2. Leverage Generic Constraints

Specialized step interfaces enforce type constraints at compile time:

```java
// StepOneToOne<I, O> ensures:
// - Input items are of type I
// - Output items are of type O
// - Type safety is maintained throughout the pipeline

public class ParseJsonStep implements StepOneToOne<String, JsonObject> {
    @Override
    public Uni<JsonObject> applyAsyncUni(String jsonString) {
        // No casting needed - jsonString is guaranteed to be a String
        return Uni.createFrom().item(JsonObject.fromString(jsonString));
    }
    
    @Override
    public StepConfig effectiveConfig() {
        return new StepConfig();
    }
}
```

### 3. Use Proper Exception Handling

Always handle potential type mismatches gracefully:

```java
public class SafeCastStep implements StepOneToOne<Object, String> {
    @Override
    public Uni<String> applyAsyncUni(Object input) {
        if (input instanceof String) {
            return Uni.createFrom().item((String) input);
        } else {
            // Handle unexpected types gracefully
            return Uni.createFrom().failure(
                new IllegalArgumentException("Expected String, got: " + input.getClass())
            );
        }
    }
    
    @Override
    public StepConfig effectiveConfig() {
        // Enable recovery to handle unexpected types
        return new StepConfig().recoverOnFailure(true);
    }
}
```

## Pipeline Composition

When composing pipelines, ensure type compatibility between consecutive steps:

```java
// Pipeline with consistent types
List<Step> steps = List.of(
    new ParseJsonStep(),        // Object -> JsonObject
    new ExtractFieldStep(),      // JsonObject -> String
    new TransformStringStep(),   // String -> String
    new StringLengthStep()       // String -> Integer
);

// Avoid mixing incompatible types
List<Step> badSteps = List.of(
    new ParseJsonStep(),         // Object -> JsonObject
    new StringLengthStep()        // String -> Integer (expects String but gets JsonObject)
    // This will cause runtime errors
);
```

## Configuration for Type Safety

Use configuration to improve robustness:

```java
public class RobustStep implements StepOneToOne<String, Integer> {
    @Override
    public Uni<Integer> applyAsyncUni(String input) {
        return Uni.createFrom().item(input.length());
    }
    
    @Override
    public StepConfig effectiveConfig() {
        return new StepConfig()
            .retryLimit(3)
            .retryWait(Duration.ofMillis(100))
            .recoverOnFailure(true)  // Recover from unexpected types
            .debug(true);            // Log for debugging
    }
}
```

## Testing Type Safety

Write tests that verify type handling:

```java
@Test
void testHandlesCorrectTypes() {
    ParseJsonStep step = new ParseJsonStep();
    
    // Test with valid input
    Uni<JsonObject> result = step.applyAsyncUni("{\"name\":\"test\"}");
    JsonObject obj = result.await().indefinitely();
    assertEquals("test", obj.getString("name"));
}

@Test
void testHandlesInvalidTypes() {
    SafeCastStep step = new SafeCastStep();
    
    // Test with invalid input
    Uni<String> result = step.applyAsyncUni(123);
    
    // Should fail gracefully
    assertThrows(IllegalArgumentException.class, 
        () -> result.await().indefinitely());
}
```

## Conclusion

While the pipeline framework uses generic `Object` types for flexibility, developers can maintain type safety by:

1. Using specialized step interfaces
2. Leveraging compile-time generic constraints
3. Handling exceptions gracefully
4. Writing comprehensive tests
5. Configuring appropriate error recovery mechanisms

The framework's design balances flexibility with type safety, allowing developers to build robust pipelines while maintaining the ability to compose heterogeneous steps when needed.
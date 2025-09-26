# Getting Started

This guide will help you set up and start using the Pipeline Framework in your project.

## Prerequisites

- Java 21 or higher
- Maven 3.8+
- A Quarkus-based project (the framework is designed to work with Quarkus)

## Adding the Framework to Your Project

Add the following dependency to your `pom.xml`. Both runtime and deployment components are bundled in a single dependency:

```xml
<dependency>
  <groupId>io.github.mbarcia</groupId>
  <artifactId>pipeline-framework</artifactId>
  <version>LATEST_VERSION</version>
</dependency>
```

## Basic Configuration

1. Annotate your service class with `@PipelineStep`
2. Implement the appropriate step interface (`StepOneToOne`, `StepOneToMany`, etc.)
3. Create your mapper classes that implement the appropriate mapper interfaces

The framework will automatically generate the necessary adapters at build time.
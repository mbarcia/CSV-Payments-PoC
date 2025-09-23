# Getting Started

This guide will help you set up and start using the Pipeline Framework in your project.

## Prerequisites

- Java 21 or higher
- Maven 3.8+
- A Quarkus-based project (the framework is designed to work with Quarkus)

## Adding the Framework to Your Project

Add the following dependencies to your `pom.xml`:

```xml
<dependency>
  <groupId>io.github.mbarcia</groupId>
  <artifactId>pipeline-framework-runtime</artifactId>
  <version>LATEST_VERSION</version>
</dependency>

<dependency>
  <groupId>io.github.mbarcia</groupId>
  <artifactId>pipeline-framework-deployment</artifactId>
  <version>LATEST_VERSION</version>
</dependency>
```

## Basic Configuration

1. Annotate your service class with `@PipelineStep`
2. Implement the appropriate step interface (`StepOneToOne`, `StepOneToMany`, etc.)
3. Create your mapper classes and annotate them with `@MapperForStep`

The framework will automatically generate the necessary adapters at build time.
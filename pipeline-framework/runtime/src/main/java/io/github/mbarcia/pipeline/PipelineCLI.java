/*
 * Copyright (c) 2023-2025 Mariano Barcia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.mbarcia.pipeline;

import io.quarkus.runtime.QuarkusApplication;
import io.smallrye.mutiny.Multi;
import jakarta.enterprise.inject.Instance;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * CLI client app for the pipeline runner.
 */
public class PipelineCLI implements QuarkusApplication {

  // Use programmatic lookup instead of field injection
  // Instance<Object> allSteps and PipelineRunner pipelineRunner will be looked up programmatically

  @Override
  public int run(String... args) {
    System.out.println("PipelineCLI.run() method called with args: " + String.join(", ", args));
    
    // Programmatic lookup of CDI beans since field injection isn't working in main class
    jakarta.enterprise.inject.spi.CDI<Object> cdi = jakarta.enterprise.inject.spi.CDI.current();
    Instance<Object> allSteps = cdi.select(Object.class);
    
    // Debug injection
    System.out.println("allSteps is null: " + (allSteps == null));
    
    if (allSteps == null) {
      System.err.println("Error: CDI injection failed for allSteps");
      return 1;
    }
    
    // Debug: Print all available CDI beans for inspection
    System.out.println("=== Available CDI Beans ===");
    try {
        int beanCount = 0;
        for (Instance.Handle<Object> handle : allSteps.handles()) {
            Object bean = handle.get();
            String className = bean.getClass().getName();
            System.out.println("Bean #" + (++beanCount) + ": " + className);
            
            // Also print implemented interfaces
            Class<?>[] interfaces = bean.getClass().getInterfaces();
            if (interfaces.length > 0) {
                System.out.println("  Implements: ");
                for (Class<?> iface : interfaces) {
                    System.out.println("    - " + iface.getName());
                }
            }
            
            // Limit output to prevent overwhelming logs
            if (beanCount > 100) {
                System.out.println("... (more beans available, limiting output)");
                break;
            }
        }
        System.out.println("Total beans found: " + beanCount);
        System.out.println("============================");
    } catch (Exception e) {
        System.err.println("Error listing CDI beans: " + e.getMessage());
        e.printStackTrace();
    }
    
    // Try to get PipelineRunner through programmatic lookup, with fallback to manual creation
    PipelineRunner pipelineRunner = null;
    try {
        pipelineRunner = cdi.select(PipelineRunner.class).get();
        System.out.println("PipelineRunner obtained through CDI lookup");
    } catch (Exception e) {
        System.out.println("PipelineRunner CDI lookup failed: " + e.getMessage());
        System.out.println("Attempting to create PipelineRunner manually...");
        // Fallback: Create PipelineRunner manually
        try {
            pipelineRunner = new PipelineRunner();
            System.out.println("PipelineRunner created manually");
        } catch (Exception ex) {
            System.err.println("Error creating PipelineRunner manually: " + ex.getMessage());
            ex.printStackTrace();
            return 1;
        }
    }
    
    System.out.println("pipelineRunner is null: " + (pipelineRunner == null));
    
    if (pipelineRunner == null) {
      System.err.println("Error: PipelineRunner is null");
      return 1;
    }
    
    // In dev mode, get input from command line arguments passed to Quarkus
    String input = null;
    
    // Parse the input from args that are passed to the QuarkusApplication
    for (int i = 0; i < args.length; i++) {
      if (args[i].equals("-i") || args[i].equals("--input")) {
        if (i + 1 < args.length) {
          input = args[i + 1];
          break;
        }
      } else if (args[i].startsWith("--input=")) {
        // Handle --input=value format
        input = args[i].substring("--input=".length());
        break;
      } else if (args[i].startsWith("-i=")) {
        // Handle -i=value format
        input = args[i].substring("-i=".length());
        break;
      }
    }
    
    // If not found in args, try system property (for cases where it was set externally)
    if (input == null) {
      input = System.getProperty("pipeline.input");
    }
    
    // Additional debug info for argument parsing
    System.out.println("Parsed input value: " + input);
    if (input == null) {
      System.out.println("Available args: " + String.join(", ", args));
    }
    
    if (input == null) {
      System.err.println("Error: --input or -i argument is required");
      System.err.println("Usage: java -jar app.jar --input <value>");
      for (String arg : args) {
        System.out.println("Received arg: " + arg);
      }
      return 1; // Error exit code
    }
    
    System.out.println("Processing input: " + input);
    
    // Create input Multi from the input parameter
    Multi<String> inputMulti = Multi.createFrom().item(input);
    
    // Collect all step instances from CDI that implement step interfaces
    List<Object> stepList = new ArrayList<>();
    System.out.println("Iterating through allSteps handles...");
    try {
        for (Instance.Handle<Object> handle : allSteps.handles()) {
            Object step = handle.get();
            // Filter to include only actual pipeline steps (not all CDI beans)
            if (isPipelineStep(step)) {
                stepList.add(step);
            }
        }
    } catch (Exception e) {
        System.err.println("Error iterating through CDI beans: " + e.getMessage());
        e.printStackTrace();
        return 1;
    }
    
    System.out.println(MessageFormat.format("Executing pipeline with {0} steps", stepList.size()));
    
    if (stepList.isEmpty()) {
        System.out.println("No pipeline steps found, generating step classes...");
    }
    
    // Execute the pipeline with the processed input and collected steps using the pipeline runner
    try {
        pipelineRunner.run(inputMulti, stepList);
    } catch (Exception e) {
        System.err.println("Error executing pipeline: " + e.getMessage());
        e.printStackTrace();
        return 1;
    }
    
    System.out.println("Pipeline execution completed");
    return 0; // Success exit code
  }
  
  private boolean isPipelineStep(Object obj) {
      // Check if the object is actually a generated pipeline step
      // by checking for implementations of pipeline step interfaces
      String className = obj.getClass().getSimpleName();
      String fullClassName = obj.getClass().getName();
      String packageName = obj.getClass().getPackage() != null ? obj.getClass().getPackage().getName() : "";
      
      // Debug output to see what classes are being considered
      System.out.println("Checking class: " + className + " (" + fullClassName + ")");
      
      // Check for common pipeline step interface names
      boolean implementsStepInterface = false;
      Class<?> clazz = obj.getClass();
      Class<?> currentClass = clazz;
      
      // Check current class and all its superclasses
      while (currentClass != null && currentClass != Object.class) {
          // Check all interfaces of the current class
          for (Class<?> iface : currentClass.getInterfaces()) {
              String ifaceName = iface.getName();
              
              // Check if it's a pipeline step interface 
              if (isPipelineStepInterface(ifaceName)) {
                  System.out.println("  -> Implements step interface: " + ifaceName);
                  implementsStepInterface = true;
                  break;
              }
              
              // Also check interfaces that this interface extends
              if (checkInterfaceInheritance(iface)) {
                  System.out.println("  -> Implements step interface via inheritance: " + ifaceName);
                  implementsStepInterface = true;
                  break;
              }
          }
          
          if (implementsStepInterface) {
              break;
          }
          
          currentClass = currentClass.getSuperclass();
      }
      
      // Additional check: classes ending with "Step" are likely pipeline steps
      boolean isStepClass = className.endsWith("Step");
      
      // Check for typical package patterns for generated pipeline steps
      boolean isInPipelinePackage = fullClassName.contains("pipeline") || 
                                   packageName.contains("pipeline") ||
                                   fullClassName.contains("io.github.mbarcia");
      
      // Heuristic: if it's in the right package and ends with "Step", it's likely a pipeline step
      boolean result = implementsStepInterface || (isStepClass && fullClassName.contains("io.github.mbarcia"));
      
      if (result) {
          System.out.println("  -> Identified as pipeline step: " + className + " (implements step interface: " + implementsStepInterface + ", is step class: " + isStepClass + ", in pipeline package: " + isInPipelinePackage + ")");
      }
      
      return result;
  }
  
  private boolean isPipelineStepInterface(String ifaceName) {
      // Check for pipeline step interface names
      return ifaceName.contains(".StepOneToOne") || 
             ifaceName.contains(".StepOneToMany") || 
             ifaceName.contains(".StepManyToOne") || 
             ifaceName.contains(".StepManyToMany") ||
             ifaceName.contains(".StepSideEffect") ||
             ifaceName.contains(".OneToMany") || // functional interface
             ifaceName.contains(".OneToOne") || // functional interface
             ifaceName.contains(".ManyToOne") || // functional interface
             ifaceName.contains(".ManyToMany"); // functional interface
  }
  
  private boolean checkInterfaceInheritance(Class<?> iface) {
      // Check if this interface extends any pipeline interfaces
      for (Class<?> superIface : iface.getInterfaces()) {
          String superIfaceName = superIface.getName();
          if (isPipelineStepInterface(superIfaceName)) {
              return true;
          }
          // Recursively check further inheritance
          if (checkInterfaceInheritance(superIface)) {
              return true;
          }
      }
      return false;
  }
}
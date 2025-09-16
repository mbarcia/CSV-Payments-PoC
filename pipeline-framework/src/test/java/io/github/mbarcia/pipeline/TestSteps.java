/*
 * Copyright © 2023-2025 Mariano Barcia
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

import io.github.mbarcia.pipeline.step.*;
import io.github.mbarcia.pipeline.step.blocking.StepOneToOneBlocking;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TestSteps {

  public static class TestStepOneToOneBlocking extends ConfigurableStepBase
      implements StepOneToOneBlocking<String, String> {
    @Override
    public String apply(String input) {
      return "Processed: " + input;
    }
  }

  public static class TestStepOneToMany extends ConfigurableStepBase
      implements StepOneToMany<String, String> {
    @Override
    public Multi<String> applyMulti(String input) {
      return Multi.createFrom().items(input + "-1", input + "-2", input + "-3");
    }
  }

  public static class TestStepManyToMany extends ConfigurableStepBase implements StepManyToMany {
    @Override
    public Multi<Object> applyStreaming(Multi<Object> upstream) {
      return upstream.onItem().transform(item -> "Streamed: " + item);
    }
  }

  public static class TestStepOneToOne extends ConfigurableStepBase
      implements StepOneToOne<String, String> {
    @Override
    public Uni<String> applyAsyncUni(String input) {
      return Uni.createFrom().item("Async: " + input);
    }
  }

  public static class FailingStepBlocking extends ConfigurableStepBase
      implements StepOneToOneBlocking<String, String> {
    private final boolean shouldRecover;

    public FailingStepBlocking() {
      this(false);
    }

    public FailingStepBlocking(boolean shouldRecover) {
      this.shouldRecover = shouldRecover;
      if (shouldRecover) {
        liveConfig().overrides().recoverOnFailure(true);
      }
    }

    @Override
    public String apply(String input) {
      throw new RuntimeException("Intentional failure for testing");
    }

    @Override
    public Uni<Void> deadLetter(Object failedItem, Throwable cause) {
      System.out.println("Dead letter handled for: " + failedItem);
      return super.deadLetter(failedItem, cause);
    }
  }
}

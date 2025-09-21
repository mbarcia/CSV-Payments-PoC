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

package pipeline;

import io.github.mbarcia.pipeline.step.ConfigurableStep;
import io.github.mbarcia.pipeline.step.StepOneToOne;
import io.github.mbarcia.pipeline.step.blocking.StepOneToOneBlocking;
import io.smallrye.mutiny.Uni;
import java.util.concurrent.atomic.AtomicInteger;

public class RetryTestSteps {

  public static class FailBlockingNTimesStep extends ConfigurableStep
      implements StepOneToOneBlocking<String, String> {
    private final int failCount;
    private final AtomicInteger callCount = new AtomicInteger(0);
    private final boolean shouldRecover;

    public FailBlockingNTimesStep(int failCount) {
      this(failCount, false);
    }

    public FailBlockingNTimesStep(int failCount, boolean shouldRecover) {
      this.failCount = failCount;
      this.shouldRecover = shouldRecover;
      if (shouldRecover) {
        liveConfig().overrides().recoverOnFailure(true);
      }
    }

    @Override
    public String apply(String input) {
      int count = callCount.incrementAndGet();
      if (count <= failCount) {
        throw new RuntimeException("Intentional failure #" + count);
      }
      return "Success: " + input;
    }

    @Override
    public Uni<Void> deadLetter(Object failedItem, Throwable cause) {
      System.out.println("Dead letter handled for: " + failedItem);
      return super.deadLetter(failedItem, cause);
    }

    public int getCallCount() {
      return callCount.get();
    }
  }

  public static class AsyncFailNTimesStep extends ConfigurableStep
      implements StepOneToOne<String, String> {
    private final int failCount;
    private final AtomicInteger callCount = new AtomicInteger(0);

    public AsyncFailNTimesStep(int failCount) {
      this.failCount = failCount;
    }

    @Override
    public Uni<String> applyAsyncUni(String input) {
      int count = callCount.incrementAndGet();
      System.out.println("AsyncFailNTimesStep called " + count + " times for input: " + input);
      System.out.println("Fail count: " + failCount + ", Retry limit: " + retryLimit());
      if (count <= failCount) {
        RuntimeException exception = new RuntimeException("Intentional async failure #" + count);
        System.out.println("Throwing exception: " + exception.getMessage());
        return Uni.createFrom().failure(exception);
      }
      System.out.println("Returning success for input: " + input);
      return Uni.createFrom().item("Async Success: " + input);
    }

    public int getCallCount() {
      return callCount.get();
    }
  }
}

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

package io.github.mbarcia.csv;

import io.github.mbarcia.csv.step.PersistAndSendPaymentStep;
import io.github.mbarcia.csv.util.Sync;
import io.github.mbarcia.csv.util.SystemExiter;
import io.github.mbarcia.csv.step.*;
import io.github.mbarcia.pipeline.service.PipelineConfig;
import io.github.mbarcia.pipeline.service.StepConfig;
import io.github.mbarcia.pipeline.service.PipelineRunner;
import io.smallrye.mutiny.Multi;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import jakarta.inject.Inject;
import java.text.MessageFormat;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

@QuarkusMain
@CommandLine.Command(
    name = "csv-payments",
    mixinStandardHelpOptions = true,
    version = "1.0.0",
    description = "Process CSV payment files")
public class CsvPaymentsApplication implements Runnable, QuarkusApplication {

  private static final Logger LOG = LoggerFactory.getLogger(CsvPaymentsApplication.class);

  @Inject PipelineConfig pipelineConfig;

  @Inject
  Sync sync;

  @Inject
  ProcessFolderStep processFolderStep;
  
  @Inject
  ProcessInputFileStep processInputFileStep;
  
  @Inject
  ProcessOutputFileStep processOutputFileStep;
  
  @Inject
  PersistAndSendPaymentStep persistAndSendPaymentStep;
  
  @Inject
  ProcessAckPaymentStep processAckPaymentStep;
  
  @Inject
  ProcessPaymentStatusStep processPaymentStatusStep;
  
  @Inject CommandLine.IFactory factory;

  @Inject
  SystemExiter exiter;

  @CommandLine.Option(
      names = {"-c", "--csv-folder"},
      description = "The folder path containing CSV payment files (defaults to csv/ internal path)",
      defaultValue = "${env:CSV_FOLDER_PATH:-csv/}")
  String csvFolder;

  // Traditional main method for standard Java execution
  public static void main(String[] args) {
    Quarkus.run(CsvPaymentsApplication.class, args);
  }

  @Override
  public int run(String... args) {
    return new CommandLine(this, factory).execute(args);
  }

  @Override
  public void run() {
    LOG.info("APPLICATION BEGINS processing {}", csvFolder);

    StopWatch watch = new StopWatch();
    watch.start();

    // Configure profiles
    pipelineConfig.defaults().retryLimit(3).debug(false);
    pipelineConfig.profile("dev", new StepConfig().retryLimit(1).debug(true));
    pipelineConfig.profile("prod", new StepConfig().retryLimit(5).retryWait(Duration.ofSeconds(1)));

    try (PipelineRunner runner = new PipelineRunner()) {
      Multi<?> result = runner.run(
              Multi.createFrom().items(csvFolder),
              List.of(processFolderStep,
                      processInputFileStep,
                      persistAndSendPaymentStep,
                      processAckPaymentStep,
                      processPaymentStatusStep,
                      processOutputFileStep)
      );

      result
          .subscribe()
          .with(_ -> {
                LOG.info("Processing completed.");
                watch.stop();
                LOG.info(
                    "✅ APPLICATION FINISHED processing of {} in {} seconds",
                    csvFolder,
                    watch.getTime(TimeUnit.SECONDS));
                    sync.signal();
                exiter.exit(0);
              },
              failure -> {
                LOG.error(MessageFormat.format("Error: {0}", failure.getMessage()));
                watch.stop();
                LOG.error(
                    "❌ APPLICATION FAILED processing {} after {} seconds",
                    csvFolder,
                    watch.getTime(TimeUnit.SECONDS),
                    failure);
                sync.signal();
                exiter.exit(1);
              });

      sync.await(); // block main thread here until completion

    } catch (Exception e) {
      watch.stop();
      LOG.error(
          "❌ APPLICATION ABORTED due to invalid folder {} after {} seconds",
          csvFolder,
          watch.getTime(TimeUnit.SECONDS),
          e);
      exiter.exit(1);
    }
  }
}

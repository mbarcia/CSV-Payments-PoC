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

package com.example.poc.service;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import jakarta.inject.Inject;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.concurrent.CountDownLatch;
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

  @Inject OrchestratorService orchestratorService;

  @Inject CommandLine.IFactory factory;

  @Inject SystemExiter exiter;

  @CommandLine.Option(
      names = {"-c", "--csv-folder"},
      description = "The folder path containing CSV payment files (defaults to csv/ internal path)",
      defaultValue = "${env:CSV_FOLDER_PATH:-csv/}")
  String csvFolder;

  @Override
  public int run(String... args) {
    return new CommandLine(this, factory).execute(args);
  }

  @Override
  public void run() {
    LOG.info("APPLICATION BEGINS processing {}", csvFolder);

    StopWatch watch = new StopWatch();
    watch.start();

    CountDownLatch latch = new CountDownLatch(1);

    try {
      orchestratorService
          .process(csvFolder)
          .subscribe()
          .with(_ -> {
                LOG.info("Processing completed.");
                watch.stop();
                LOG.info(
                    "✅ APPLICATION FINISHED processing of {} in {} seconds",
                    csvFolder,
                    watch.getTime(TimeUnit.SECONDS));
                latch.countDown();
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
                latch.countDown();
                exiter.exit(1);
              });

      latch.await(); // block main thread here until completion

    } catch (URISyntaxException e) {
      watch.stop();
      LOG.error(
          "❌ APPLICATION ABORTED due to invalid folder {} after {} seconds",
          csvFolder,
          watch.getTime(TimeUnit.SECONDS),
          e);
      exiter.exit(1);
    } catch (Throwable e) {
      watch.stop();
      LOG.error(
          "❌ APPLICATION FAILED processing {} after {} seconds",
          csvFolder,
          watch.getTime(TimeUnit.SECONDS),
          e);
      exiter.exit(1);
    }
  }

  // Traditional main method for standard Java execution
  public static void main(String[] args) {
    Quarkus.run(CsvPaymentsApplication.class, args);
  }
}

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

package io.github.mbarcia.csv;

import io.github.mbarcia.csv.service.ProcessFolderService;
import io.github.mbarcia.csv.util.Sync;
import io.github.mbarcia.csv.util.SystemExiter;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

/**
 * Main application for the CSV Payments orchestrator service.
 * This processes CSV payment files via command-line arguments.
 */
@QuarkusMain
@CommandLine.Command(
    name = "csv-payments",
    mixinStandardHelpOptions = true,
    version = "1.0.0",
    description = "Process CSV payment files")
public class CsvPaymentsApplication implements Runnable, QuarkusApplication {

  private static final Logger LOG = LoggerFactory.getLogger(CsvPaymentsApplication.class);

  @Inject
  Sync sync;

  @Inject
  SystemExiter exiter;

  @Inject
  ProcessFolderService processFolderService;

  @Inject
  CommandLine.IFactory factory;

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
    LOG.info("Starting pipeline processing for folder: {}", csvFolder);
    
    try {
        // For now, just process the folder and log the results
        // In a real implementation, this would trigger the generated pipeline application
        var files = processFolderService.process(csvFolder);
        LOG.info("Found {} CSV files to process in folder: {}", files.count(), csvFolder);
        
        // If we had the generated pipeline application, we would use it like this:
        // generatedPipelineApp.processPipeline(csvFolder);
        // But for now we'll just log and exit
        
        LOG.info("Pipeline processing completed for: {}", csvFolder);
        
        // Signal completion
        sync.signal();
    } catch (Exception e) {
        LOG.error("Error during pipeline processing", e);
        exiter.exit(1);
    }
  }
}
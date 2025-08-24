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

import com.example.poc.common.domain.CsvPaymentsOutputFile;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@ApplicationScoped
public class OrchestratorService {

  public static final Executor VIRTUAL_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

  @Inject HybridResourceLoader resourceLoader;

  @Inject ProcessFileService processFileService;
  
  @Inject ProcessFolderService processFolderService;
  
  public Uni<Void> process(String csvFolderPath) throws URISyntaxException {
    List<Uni<CsvPaymentsOutputFile>> processingUnis =
        processFolderService.process(csvFolderPath).keySet().stream()
            .map(
                inputFile ->
                    Uni.createFrom()
                        .item(inputFile)
                        .runSubscriptionOn(VIRTUAL_EXECUTOR) // Shift execution to virtual thread
                        .flatMap(processFileService::process))
            .toList();

    if (!processingUnis.isEmpty()) {
      return Uni.combine().all().unis(processingUnis).discardItems();
    } else {
      return Uni.createFrom().voidItem();
    }
  }
}

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

package org.pipelineframework.csv.service;

import org.pipelineframework.csv.common.domain.CsvFolder;
import org.pipelineframework.csv.common.domain.CsvPaymentsInputFile;
import org.pipelineframework.csv.grpc.MutinyProcessFolderServiceGrpc;
import org.pipelineframework.csv.common.mapper.CsvFolderMapper;
import org.pipelineframework.csv.common.mapper.CsvPaymentsInputFileMapper;
import org.pipelineframework.csv.util.HybridResourceLoader;
import org.pipelineframework.annotation.PipelineStep;
import io.smallrye.mutiny.Multi;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
@PipelineStep(
    order = 1,
    inputType = CsvFolder.class,
    outputType = CsvPaymentsInputFile.class,
    inputGrpcType = org.pipelineframework.csv.grpc.InputCsvFileProcessingSvc.CsvFolder.class,
    outputGrpcType = org.pipelineframework.csv.grpc.InputCsvFileProcessingSvc.CsvPaymentsInputFile.class,
    stepType = org.pipelineframework.step.StepOneToMany.class,
    backendType = org.pipelineframework.grpc.GrpcReactiveServiceAdapter.class,
    grpcStub = MutinyProcessFolderServiceGrpc.MutinyProcessFolderServiceStub.class,
    grpcImpl = MutinyProcessFolderServiceGrpc.ProcessFolderServiceImplBase.class,
    inboundMapper = CsvFolderMapper.class,
    outboundMapper = CsvPaymentsInputFileMapper.class,
    grpcClient = "process-folder",
    restEnabled = true,
    autoPersist = true,
    debug = true
)
public class ProcessFolderService implements org.pipelineframework.service.ReactiveStreamingService<CsvFolder, CsvPaymentsInputFile> {

  private static final Logger LOG = LoggerFactory.getLogger(ProcessFolderService.class);

  @Inject
  HybridResourceLoader resourceLoader;

  public Multi<CsvPaymentsInputFile> process(CsvFolder csvFolder) {
    Path csvFolderPath = csvFolder.getPath();
    LOG.info("Reading CSV folder from path: {}", csvFolderPath);

    URL resource = resourceLoader.getResource(String.valueOf(csvFolderPath));
    if (resource == null) {
      LOG.warn("CSV folder not found: {}", csvFolderPath);
      resourceLoader.diagnoseResourceAccess(String.valueOf(csvFolderPath));
      return Multi.createFrom().failure(
          new IllegalArgumentException(
              MessageFormat.format("CSV folder not found: {0}", csvFolderPath)));
    }

      File directory;
      try {
          directory = new File(resource.toURI());
      } catch (URISyntaxException e) {
        return Multi.createFrom().failure(
            new IllegalArgumentException(
                MessageFormat.format("CSV folder URI is invalid: {0}", csvFolderPath)));
      }

      if (!directory.exists() || !directory.isDirectory()) {
      return Multi.createFrom().failure(
          new IllegalArgumentException(
              MessageFormat.format(
                  "CSV path is not a valid directory: {0}", directory.getAbsolutePath())));
    }

    File[] csvFiles = directory.listFiles((_, name) -> name.toLowerCase().endsWith(".csv"));
    if (csvFiles == null || csvFiles.length == 0) {
      LOG.warn("No CSV files found in {}", csvFolderPath);
      resourceLoader.diagnoseResourceAccess(String.valueOf(csvFolderPath));
      return Multi.createFrom().empty();
    }

    return Multi.createFrom().iterable(
            Arrays.stream(csvFiles)
                    .map(CsvPaymentsInputFile::new)
                    .toList()
    );
  }
}
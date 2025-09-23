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

package io.github.mbarcia.csv.step;

import io.github.mbarcia.csv.common.domain.CsvPaymentsInputFile;
import io.github.mbarcia.csv.service.ProcessFolderService;
import io.github.mbarcia.pipeline.step.ConfigurableStep;
import io.github.mbarcia.pipeline.step.StepOneToMany;
import io.smallrye.mutiny.Multi;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.net.URISyntaxException;
import java.util.stream.Stream;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Step supplier that processes a folder path and produces a stream of input files.
 * This converts a single folder path into multiple input files.
 */
@ApplicationScoped
@NoArgsConstructor // for CDI proxying
public class ProcessFolderStep extends ConfigurableStep implements StepOneToMany<String, CsvPaymentsInputFile> {

    private static final Logger LOG = LoggerFactory.getLogger(ProcessFolderStep.class);

    @Inject
    ProcessFolderService processFolderService;

    @Override
    public Multi<CsvPaymentsInputFile> applyOneToMany(String csvFolderPath) {
        LOG.debug("Processing folder: {}", csvFolderPath);
        try {
            Stream<CsvPaymentsInputFile> inputFileStream = processFolderService.process(csvFolderPath);
            Multi<CsvPaymentsInputFile> inputFiles = Multi.createFrom().items(inputFileStream);
            LOG.debug("Successfully processed folder: {}", csvFolderPath);
            return inputFiles;
        } catch (URISyntaxException e) {
            LOG.error("Failed to process folder: {}", csvFolderPath, e);
            return Multi.createFrom().failure(e);
        }
    }
}
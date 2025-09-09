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

package io.github.mbarcia.csv.service;

import io.github.mbarcia.csv.common.domain.CsvPaymentsOutputFile;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Event that represents the completion of a CSV output file processing operation.
 * This event contains information about the completion status, record count, and any errors.
 */
@Getter
@RequiredArgsConstructor
public class CsvOutputFileCompletionEvent {
    private final CsvPaymentsOutputFile outputFile;
    private final int recordCount;
    private final boolean completedSuccessfully;
    private final Exception error;
    
    /**
     * Create a successful completion event.
     * 
     * @param outputFile the output file that was created
     * @param recordCount the number of records processed
     * @return a successful completion event
     */
    public static CsvOutputFileCompletionEvent success(CsvPaymentsOutputFile outputFile, int recordCount) {
        return new CsvOutputFileCompletionEvent(outputFile, recordCount, true, null);
    }
    
    /**
     * Create a failed completion event.
     * 
     * @param recordCount the number of records processed before failure
     * @param error the error that caused the failure
     * @return a failed completion event
     */
    public static CsvOutputFileCompletionEvent failure(int recordCount, Exception error) {
        return new CsvOutputFileCompletionEvent(null, recordCount, false, error);
    }
}
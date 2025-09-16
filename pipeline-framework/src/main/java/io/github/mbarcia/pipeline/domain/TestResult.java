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

package io.github.mbarcia.pipeline.domain;

public class TestResult {
    
    private String processedName;
    private String processedDescription;
    
    public TestResult() {
    }
    
    public TestResult(String processedName, String processedDescription) {
        this.processedName = processedName;
        this.processedDescription = processedDescription;
    }
    
    // Getters and setters
    public String getProcessedName() {
        return processedName;
    }
    
    public void setProcessedName(String processedName) {
        this.processedName = processedName;
    }
    
    public String getProcessedDescription() {
        return processedDescription;
    }
    
    public void setProcessedDescription(String processedDescription) {
        this.processedDescription = processedDescription;
    }
}
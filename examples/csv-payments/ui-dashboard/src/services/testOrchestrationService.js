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

// Simple test version that doesn't require backend services
export const orchestrationService = {
  async processFile(file, onStatsUpdate) {
    // Simulate processing
    console.log('Processing file:', file.name);
    
    // Simulate updating stats with random values
    const interval = setInterval(() => {
      const stats = {
        uploadSpeed: Math.floor(Math.random() * 1000),
        inputProcessingSpeed: Math.floor(Math.random() * 500),
        paymentsProcessingSpeed: Math.floor(Math.random() * 400),
        paymentStatusSpeed: Math.floor(Math.random() * 300),
        outputProcessingSpeed: Math.floor(Math.random() * 200)
      };
      
      onStatsUpdate(stats);
    }, 500);
    
    // Simulate async processing
    return new Promise((resolve) => {
      setTimeout(() => {
        clearInterval(interval);
        resolve({ success: true, message: 'File processed successfully' });
      }, 5000);
    });
  }
};
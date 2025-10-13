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

import axios from 'axios';
import pLimit from 'p-limit';

// Check if we're in development mode
const isDevelopment = process.env.NODE_ENV === 'development';

// Create an axios instance
const httpsAxios = axios.create({
  timeout: 10000
});

// In development mode, disable certificate validation
// Note: This is handled differently in browsers vs Node.js
// For browsers, users may need to manually proceed through security warnings
// For Node.js (testing), we can configure the agent

// Service endpoints - going through Kong API Gateway
const INPUT_PROCESSING_SVC = 'https://localhost:8843/api/v1/input-processing';
const PAYMENTS_PROCESSING_SVC = 'https://localhost:8843/api/v1/payments-processing';
const PAYMENT_STATUS_SVC = 'https://localhost:8843/api/v1/payment-status';
const OUTPUT_PROCESSING_SVC = 'https://localhost:8843/api/v1/output-processing';

class OptimizedRestOrchestrationService {
  async processFile(file, onStatsUpdate) {
    console.log('OptimizedRestOrchestrationService.processFile called with file:', file.name);

    const stats = {
      uploadSpeed: 0,
      inputProcessingSpeed: 0,
      paymentsProcessingSpeed: 0,
      paymentStatusSpeed: 0,
      outputProcessingSpeed: 0
    };

    const counters = {
      inputProcessed: 0,
      paymentsProcessed: 0,
      statusProcessed: 0,
      outputProcessed: 0
    };

    try {
      // Process CSV file using the new input processing service
      console.log('Streaming CSV file processing through input service...');

      const paymentOutputs = [];
      // Set concurrency limit to avoid ERR_INSUFFICIENT_RESOURCES
      const limit = pLimit(50);
      
      // Track start time for upload speed calculation
      const startTime = Date.now();
      
      // Track timestamps for rate calculations
      const timestamps = {
        input: [],
        payments: [],
        status: []
      };

      const updateSpeed = (type) => {
        const now = Date.now();
        const timestampsArray = timestamps[type];
        
        // Add current timestamp
        timestampsArray.push(now);
        
        // Remove timestamps older than 5 seconds
        const cutoff = now - 5000;
        while (timestampsArray.length > 0 && timestampsArray[0] < cutoff) {
          timestampsArray.shift();
        }
        
        // Calculate rate per second (events in last 5 seconds divided by 5)
        return timestampsArray.length / 5; // events per second
      };

      // Track last update time to avoid too frequent updates
      let lastUpdate = 0;
      const minUpdateInterval = 25; // ms

      const updateStats = (type) => {
        const now = Date.now();
        // Only update if enough time has passed
        if (now - lastUpdate < minUpdateInterval && type !== undefined) {
          return;
        }
        lastUpdate = now;

        if (type === 'input') {
          stats.inputProcessingSpeed = updateSpeed('input');
        } else if (type === 'payments') {
          stats.paymentsProcessingSpeed = updateSpeed('payments');
        } else if (type === 'status') {
          stats.paymentStatusSpeed = updateSpeed('status');
        }
        // outputProcessingSpeed is kept at 0 as it's not feasible to measure throughput for file download
        onStatsUpdate({ ...stats });
      };

      // Create a promise to track when all processing is complete
      let resolveAllProcessing;
      const allProcessingComplete = new Promise((resolve) => {
        resolveAllProcessing = resolve;
      });

      // Track processing promises separately
      const processingPromises = [];

      // Start processing records immediately as they arrive
      this.processInputFile(file, async (record) => {
        // INPUT STAGE - Process records as they arrive
        updateStats('input');

        // Wrap the processing in the concurrency limit
        const processingPromise = limit(async () => {
          try {
            // ---- PAYMENTS PROCESSING ----
            // In browser environments, we can't configure httpsAgent directly
            // Certificate handling is done by the browser
            const sendResponse = await httpsAxios.post(
                `${PAYMENTS_PROCESSING_SVC}/send-payment`,
                record,
                { 
                  headers: { 'Content-Type': 'application/json' },
                  timeout: 10000
                }
            );
            updateStats('payments');

            const ackPayment = sendResponse.data;

            const processAckResponse = await this.processWithRetry(
                () => httpsAxios.post(
                    `${PAYMENTS_PROCESSING_SVC}/process-ack-payment`,
                    ackPayment,
                    { 
                      headers: { 'Content-Type': 'application/json' }
                    }
                ),
                3
            );
            console.log('Process ack payment response status:', processAckResponse.status);
            const paymentStatus = processAckResponse.data;

            updateStats('status');

            // Process payment status
            console.log('Processing payment status to:', `${PAYMENT_STATUS_SVC}/process`);
            const statusResponse = await httpsAxios.post(
                `${PAYMENT_STATUS_SVC}/process`,
                paymentStatus,
                { 
                  headers: { 'Content-Type': 'application/json' }
                }
            );
            console.log('Payment status response status:', statusResponse.status);
            const paymentOutput = statusResponse.data;
            
            paymentOutputs.push(paymentOutput);
            onStatsUpdate({ ...stats }); // Always update for final results

          } catch (error) {
            console.error('Error processing record:', error);
            if (error.response) {
              console.error('Error response status:', error.response.status);
              console.error('Error response data:', JSON.stringify(error.response.data, null, 2));
            }
            // Continue with next record instead of failing completely

            // still update stats to keep gauges alive
            onStatsUpdate({ ...stats }); // Always update for error cases
          }
        });

        processingPromises.push(processingPromise);
      }, (loaded, total) => {
        // Update upload speed
        const elapsedMs = Date.now() - startTime;
        if (elapsedMs > 0) {
          // Calculate upload speed in KB/s
          stats.uploadSpeed = (loaded / 1024) / (elapsedMs / 1000);
          onStatsUpdate({ ...stats }); // Always update for upload progress
        }
      }).then(() => {
        // When streaming is complete, wait for all processing to finish
        Promise.all(processingPromises).then(() => {
          resolveAllProcessing();
        }).catch((error) => {
          console.error('Error waiting for processing to complete:', error);
          resolveAllProcessing();
        });
      }).catch((error) => {
        console.error('Error in processInputFile:', error);
        resolveAllProcessing();
      });

      // Wait for all processing to complete
      await allProcessingComplete;

      // Reset upload speed after processing completes
      stats.uploadSpeed = 0;
      onStatsUpdate({ ...stats }); // Always update for final reset

      // Process and download the output file
      console.log('Processing output file with', paymentOutputs.length, 'records to:', `${OUTPUT_PROCESSING_SVC}/process`);
      
      // Instead of returning a download URL, we'll make a direct request to get the file
      // and let the browser handle the download
      try {
        const response = await httpsAxios.post(
          `${OUTPUT_PROCESSING_SVC}/process`,
          paymentOutputs,
          { 
            headers: { 'Content-Type': 'application/json' },
            responseType: 'blob' // Important for file downloads
          }
        );
        
        // Extract filename from Content-Disposition header if available
        const contentDisposition = response.headers['content-disposition'];
        let filename = 'payment-outputs.csv';
        if (contentDisposition) {
          const filenameMatch = contentDisposition.match(/filename="?(.+)"?/);
          if (filenameMatch && filenameMatch[1]) {
            filename = filenameMatch[1];
          }
        }
        
        // Create a blob URL for download
        const url = window.URL.createObjectURL(new Blob([response.data]));
        const link = document.createElement('a');
        link.href = url;
        link.setAttribute('download', filename);
        document.body.appendChild(link);
        link.click();
        
        // Clean up
        link.parentNode.removeChild(link);
        window.URL.revokeObjectURL(url);
        
        return {
          success: true,
          message: 'File downloaded successfully'
        };
      } catch (error) {
        console.error('Error downloading file:', error);
        throw error;
      }

    } catch (error) {
      console.error('Pipeline processing failed:', error);
      if (error.response) {
        console.error('Error response status:', error.response.status);
        console.error('Error response data:', JSON.stringify(error.response.data, null, 2));
      }
      throw error;
    }
  }

  async processInputFile(file, onRecord, onProgress) {
    // Use XMLHttpRequest for upload progress tracking with streaming response
    const xhr = new XMLHttpRequest();
    
    return new Promise((resolve, reject) => {
      // Set up upload progress tracking
      if (onProgress) {
        xhr.upload.onprogress = (event) => {
          if (event.lengthComputable) {
            onProgress(event.loaded, event.total);
          }
        };
      }
      
      // Set up response streaming
      let buffer = '';
      
      xhr.onreadystatechange = function() {
        if (xhr.readyState === 3) { // LOADING state - response is being received
          // Process chunks as they arrive
          const newChunk = xhr.responseText.substring(buffer.length);
          buffer = xhr.responseText;
          
          // Process the new chunk
          processChunk(newChunk, false, onRecord).catch(error => {
            console.error('Error processing chunk:', error);
          });
        } else if (xhr.readyState === 4) { // DONE state - request complete
          if (xhr.status === 200) {
            // Process any remaining data in the buffer
            if (buffer.trim()) {
              processChunk(buffer, false, onRecord).then(() => {
                resolve();
              }).catch(error => {
                reject(error);
              });
            } else {
              resolve();
            }
          } else {
            reject(new Error(`HTTP ${xhr.status}: ${xhr.statusText}`));
          }
        }
      };

      xhr.onerror = function() {
        reject(new Error('Network error occurred'));
      };

      // Send the request
      xhr.open('POST', `${INPUT_PROCESSING_SVC}/process`);
      const formData = new FormData();
      formData.append('file', file);
      formData.append('filename', file.name);
      xhr.send(formData);
    });
  }
  
  async processWithRetry(operation, maxRetries) {
    for (let attempt = 0; attempt <= maxRetries; attempt++) {
      try {
        console.log(`Attempting operation (attempt ${attempt + 1})...`);
        return await operation();
      } catch (error) {
        console.log(`Attempt ${attempt + 1} failed:`, error.message);
        if (this.isThrottlingError(error) && attempt < maxRetries) {
          // Exponential backoff
          const delay = Math.pow(2, attempt) * 1000;
          console.log(`Throttling error, retrying in ${delay}ms...`);
          await new Promise(resolve => setTimeout(resolve, delay));
          continue;
        }
        if (error.response) {
          console.error(`Operation failed with status ${error.response.status}:`, JSON.stringify(error.response.data, null, 2));
        }
        throw error;
      }
    }
  }
  
  isThrottlingError(error) {
    if (error.response) {
      const { status, data } = error.response;
      return status === 429 || // Too Many Requests
             status === 503 || // Service Unavailable
             status === 500 || // Internal Server Error (sometimes used for throttling)
             (data && (data.message || '').toLowerCase().includes('throttle')) ||
             (data && (data.message || '').toLowerCase().includes('rate limit')) ||
             (data && (data.message || '').toLowerCase().includes('resource exhausted'));
    }
    return false;
  }
}

// Helper function to process streaming chunks
async function processChunk(chunk, isFirstChunk, onRecord) {
  // Split the chunk by newlines to get individual lines
  const lines = chunk.split('\n');
  
  for (const line of lines) {
    if (line.trim() === '') continue;
    
    try {
      let jsonData = line.trim();
      
      // Handle Server-Sent Events format (data: prefix)
      if (jsonData.startsWith('data:')) {
        jsonData = jsonData.substring(5).trim(); // Remove 'data:' prefix
      }
      
      // Handle SSE end-of-stream message
      if (jsonData === '[DONE]') {
        continue;
      }
      
      // Parse the JSON object
      const record = JSON.parse(jsonData);
      await onRecord(record);
    } catch (error) {
      // Ignore parsing errors for incomplete lines
      // These will be handled when the complete line arrives
      if (!line.includes('}{')) { // Not a split JSON object
        console.warn('Could not parse line as JSON:', line);
      }
    }
  }
}

export const orchestrationService = new OptimizedRestOrchestrationService();

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

import axios from 'axios';
import pLimit from 'p-limit';

// Simple UUID generator function
function generateUUID() {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
    const r = Math.random() * 16 | 0;
    const v = c === 'x' ? r : (r & 0x3 | 0x8);
    return v.toString(16);
  });
}

// Service endpoints - direct Quarkus service ports
const INPUT_PROCESSING_SVC = 'http://localhost:8081';
const PAYMENTS_PROCESSING_SVC = 'http://localhost:8082';
const PAYMENT_STATUS_SVC = 'http://localhost:8083';
const OUTPUT_PROCESSING_SVC = 'http://localhost:8084';

class OrchestrationService {
  async processFile(file, onStatsUpdate) {
    console.log('OrchestrationService.processFile called with file:', file.name);

    const stats = {
      uploadSpeed: 0,
      inputProcessingSpeed: 0,
      paymentsProcessingSpeed: 0,
      paymentStatusSpeed: 0,
      outputProcessingSpeed: 0 // Keep this for UI compatibility, but won't be updated
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
      // Create a concurrency limit of 500 to prevent ERR_INSUFFICIENT_RESOURCES
      const limit = pLimit(500);
      
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
      const minUpdateInterval = 100; // ms

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
            const recordWithId = {
              ...record,
              id: record.id
            };

            // Only add an ID if one doesn't exist
            if (!recordWithId.id) {
              recordWithId.id = generateUUID();
            }

            // ---- PAYMENTS PROCESSING ----
            const sendResponse = await axios.post(
                `${PAYMENTS_PROCESSING_SVC}/api/v1/payments-processing/send-payment`,
                recordWithId,
                { headers: { 'Content-Type': 'application/json' }, timeout: 10000 }
            );
            updateStats('payments');

            const ackPayment = sendResponse.data;

            const processAckResponse = await this.processWithRetry(
                () => axios.post(
                    `${PAYMENTS_PROCESSING_SVC}/api/v1/payments-processing/process-ack-payment`,
                    ackPayment,
                    { headers: { 'Content-Type': 'application/json' } }
                ),
                3
            );
            console.log('Process ack payment response status:', processAckResponse.status);
            console.log('Process ack payment response data:', processAckResponse.data);
            const paymentStatus = processAckResponse.data;

            updateStats('status');

            // Ensure paymentStatus has all required fields
            if (!paymentStatus) {
              throw new Error('Payment status is null or undefined');
            }

            // Use the actual paymentStatus data without adding default fabricated values
            const fixedPaymentStatus = {
              id: paymentStatus.id,
              reference: paymentStatus.reference,
              fee: paymentStatus.fee,
              status: paymentStatus.status,
              message: paymentStatus.message,
              ackPaymentSentId: paymentStatus.ackPaymentSentId,
              paymentRecordId: paymentStatus.paymentRecordId,
              paymentRecord: paymentStatus.paymentRecord,
              ackPaymentSent: paymentStatus.ackPaymentSent
            };

            // Only fix ackPaymentSent if it exists, without adding default values
            if (fixedPaymentStatus.ackPaymentSent) {
              fixedPaymentStatus.ackPaymentSent = {
                id: fixedPaymentStatus.ackPaymentSent.id,
                conversationId: fixedPaymentStatus.ackPaymentSent.conversationId,
                status: fixedPaymentStatus.ackPaymentSent.status,
                message: fixedPaymentStatus.ackPaymentSent.message,
                paymentRecordId: fixedPaymentStatus.ackPaymentSent.paymentRecordId,
                paymentRecord: fixedPaymentStatus.ackPaymentSent.paymentRecord
              };
            }

            // Log the fixed payment status for debugging
            console.log('Fixed payment status data:', JSON.stringify(fixedPaymentStatus, null, 2));

            // Process payment status
            console.log('Processing payment status to:', `${PAYMENT_STATUS_SVC}/api/v1/payment-status/process`);
            const statusResponse = await axios.post(
                `${PAYMENT_STATUS_SVC}/api/v1/payment-status/process`,
                fixedPaymentStatus,
                { headers: { 'Content-Type': 'application/json' } }
            );
            console.log('Payment status response status:', statusResponse.status);
            console.log('Payment status response data:', statusResponse.data);
            const paymentOutput = {
              ...statusResponse.data,
              id: statusResponse.data.id || generateUUID(),
              paymentStatus: statusResponse.data.paymentStatus ? {
                ...statusResponse.data.paymentStatus,
                id: statusResponse.data.paymentStatus.id || generateUUID(),
                paymentRecordId: statusResponse.data.paymentStatus.paymentRecordId || (statusResponse.data.paymentStatus.paymentRecord ? statusResponse.data.paymentStatus.paymentRecord.id : null) || recordWithId.id,
                paymentRecord: statusResponse.data.paymentStatus.paymentRecord || recordWithId
              } : null
            };
            
            paymentOutputs.push(paymentOutput);
            onStatsUpdate({ ...stats }); // Always update for final results

          } catch (error) {
            console.error('Error processing record:', error);
            if (error.response) {
              console.error('Error response status:', error.response.status);
              console.error('Error response data:', JSON.stringify(error.response.data, null, 2));
              console.error('Error response headers:', error.response.headers);
            } else if (error.request) {
              console.error('Error request made but no response received:', error.request);
            } else {
              console.error('Error message:', error.message);
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

      // Step 3: Generate output CSV file
      console.log('Processing output file with', paymentOutputs.length, 'records to:', `${OUTPUT_PROCESSING_SVC}/api/v1/output-processing/process`);

      // Ensure all payment outputs have IDs and required fields without adding default values
      const fixedPaymentOutputs = paymentOutputs.map(output => {
        // Use actual paymentStatus data without adding default fabricated values
        let fixedPaymentStatus = output.paymentStatus;
        if (fixedPaymentStatus) {
          fixedPaymentStatus = {
            id: fixedPaymentStatus.id,
            reference: fixedPaymentStatus.reference,
            fee: fixedPaymentStatus.fee,
            status: fixedPaymentStatus.status,
            message: fixedPaymentStatus.message,
            ackPaymentSentId: fixedPaymentStatus.ackPaymentSentId,
            paymentRecordId: fixedPaymentStatus.paymentRecordId,
            paymentRecord: fixedPaymentStatus.paymentRecord,
            ackPaymentSent: fixedPaymentStatus.ackPaymentSent
          };

          // Only fix ackPaymentSent if it exists, without adding default values
          if (fixedPaymentStatus.ackPaymentSent) {
            fixedPaymentStatus.ackPaymentSent = {
              id: fixedPaymentStatus.ackPaymentSent.id,
              conversationId: fixedPaymentStatus.ackPaymentSent.conversationId,
              status: fixedPaymentStatus.ackPaymentSent.status,
              message: fixedPaymentStatus.ackPaymentSent.message,
              paymentRecordId: fixedPaymentStatus.ackPaymentSent.paymentRecordId,
              paymentRecord: fixedPaymentStatus.ackPaymentSent.paymentRecord
            };
          }
        }

        return {
          ...output,
          id: output.id,
          status: output.status,
          message: output.message,
          paymentStatus: fixedPaymentStatus
        };
      });

      // Process and download the output file
      console.log('Processing and downloading output file with', paymentOutputs.length, 'records');
      
      // Create a Blob from the data to enable client-side download
      const blob = new Blob([JSON.stringify(fixedPaymentOutputs)], { type: 'application/json' });
      const tempUrl = URL.createObjectURL(blob);
      
      // Trigger the download via the new endpoint
      const downloadUrl = `${OUTPUT_PROCESSING_SVC}/api/v1/output-processing/process`;
      
      // Return the download URL for the UI to create a download link
      return {
        downloadUrl: downloadUrl,
        data: fixedPaymentOutputs
      };

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
      xhr.open('POST', `${INPUT_PROCESSING_SVC}/api/v1/input-processing/process`);
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

export const orchestrationService = new OrchestrationService();
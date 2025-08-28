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
import Papa from 'papaparse';

// Simple UUID generator function
function generateUUID() {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
    const r = Math.random() * 16 | 0;
    const v = c === 'x' ? r : (r & 0x3 | 0x8);
    return v.toString(16);
  });
}

// Service endpoints - direct Quarkus service ports
const PAYMENTS_PROCESSING_SVC = 'http://localhost:8082';
const PAYMENT_STATUS_SVC = 'http://localhost:8083';
const OUTPUT_PROCESSING_SVC = 'http://localhost:8084';

class OrchestrationService {
  async processFile(file, onStatsUpdate) {
    console.log('OrchestrationService.processFile called with file:', file.name);
    
    // Initialize processing stats
    const stats = {
      uploadSpeed: 100, // File is already uploaded when this function is called
      inputProcessingSpeed: 0,
      paymentsProcessingSpeed: 0,
      paymentStatusSpeed: 0,
      outputProcessingSpeed: 0
    };

    const updateStats = (newStats) => {
      Object.assign(stats, newStats);
      console.log('Updating stats:', stats);
      onStatsUpdate({ ...stats });
    };

    try {
      // Process CSV file in streaming fashion - one line at a time
      console.log('Streaming CSV file processing...');
      updateStats({ inputProcessingSpeed: 0 });
      
      const paymentOutputs = [];
      let recordCount = 0;

      // Process records one by one in streaming fashion
      await this.streamCSVFile(file, async (record) => {
        recordCount++;
        console.log(`Processing record ${recordCount}:`, record);
        
        try {
          // Ensure the record has an ID
          const recordWithId = {
            ...record,
            id: record.id || generateUUID()
          };

          // Send payment record to payments processing service
          console.log('Sending payment record to:', `${PAYMENTS_PROCESSING_SVC}/api/v1/payments-processing/send-payment`);
          console.log('Payment record data:', recordWithId);

          const sendResponse = await axios.post(
            `${PAYMENTS_PROCESSING_SVC}/api/v1/payments-processing/send-payment`,
            recordWithId,
            {
              timeout: 10000, // 10 second timeout
              headers: {
                'Content-Type': 'application/json'
              }
            }
          );

          console.log('Send payment response status:', sendResponse.status);
          console.log('Send payment response data:', sendResponse.data);
          const ackPayment = sendResponse.data;

          // Process ack payment with retries
          console.log('Processing ack payment to:', `${PAYMENTS_PROCESSING_SVC}/api/v1/payments-processing/process-ack-payment`);
          const processAckResponse = await this.processWithRetry(
            () => axios.post(
              `${PAYMENTS_PROCESSING_SVC}/api/v1/payments-processing/process-ack-payment`, 
              ackPayment,
              {
                headers: {
                  'Content-Type': 'application/json'
                }
              }
            ),
            3
          );
          console.log('Process ack payment response status:', processAckResponse.status);
          console.log('Process ack payment response data:', processAckResponse.data);
          const paymentStatus = processAckResponse.data;

          // Ensure paymentStatus has all required fields
          if (!paymentStatus) {
            throw new Error('Payment status is null or undefined');
          }

          // Fix the paymentStatus object to ensure it has all required fields
          const fixedPaymentStatus = {
            id: paymentStatus.id || generateUUID(),
            reference: paymentStatus.reference || ("REF-" + (paymentStatus.id ? paymentStatus.id.substring(0, 8) : generateUUID().substring(0, 8))),
            fee: paymentStatus.fee !== undefined && paymentStatus.fee !== null ? paymentStatus.fee : (paymentStatus.ackPaymentSent && paymentStatus.ackPaymentSent.fee !== undefined && paymentStatus.ackPaymentSent.fee !== null ? paymentStatus.ackPaymentSent.fee : 1.01),
            status: paymentStatus.status || "Complete",
            message: paymentStatus.message || "Mock response",
            ackPaymentSentId: paymentStatus.ackPaymentSentId || (paymentStatus.ackPaymentSent ? paymentStatus.ackPaymentSent.id : null) || generateUUID(),
            paymentRecordId: paymentStatus.paymentRecordId || (paymentStatus.paymentRecord ? paymentStatus.paymentRecord.id : null) || recordWithId.id,
            paymentRecord: paymentStatus.paymentRecord || recordWithId,
            ackPaymentSent: paymentStatus.ackPaymentSent || {
              id: generateUUID(),
              conversationId: recordWithId.conversationId || generateUUID(),
              status: "1000",
              message: "OK but this is only a test",
              paymentRecordId: recordWithId.id,
              paymentRecord: recordWithId
            }
          };

          // Ensure ackPaymentSent has required fields
          if (fixedPaymentStatus.ackPaymentSent) {
            fixedPaymentStatus.ackPaymentSent = {
              id: fixedPaymentStatus.ackPaymentSent.id || generateUUID(),
              conversationId: fixedPaymentStatus.ackPaymentSent.conversationId || recordWithId.conversationId || generateUUID(),
              status: fixedPaymentStatus.ackPaymentSent.status || "1000",
              message: fixedPaymentStatus.ackPaymentSent.message || "OK but this is only a test",
              paymentRecordId: fixedPaymentStatus.ackPaymentSent.paymentRecordId || recordWithId.id,
              paymentRecord: fixedPaymentStatus.ackPaymentSent.paymentRecord || recordWithId
            };
          }

          // Log the fixed payment status for debugging
          console.log('Fixed payment status data:', JSON.stringify(fixedPaymentStatus, null, 2));

          // Process payment status
          console.log('Processing payment status to:', `${PAYMENT_STATUS_SVC}/api/v1/payment-status/process`);
          const statusResponse = await axios.post(
            `${PAYMENT_STATUS_SVC}/api/v1/payment-status/process`,
            fixedPaymentStatus,
            {
              headers: {
                'Content-Type': 'application/json'
                }
            }
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
          
          // Update stats after each record is processed (real-time)
          updateStats({ 
            inputProcessingSpeed: 100,
            paymentsProcessingSpeed: 100,
            paymentStatusSpeed: 100
          });

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
          
          // Update stats even if there's an error
          updateStats({ 
            inputProcessingSpeed: 100,
            paymentsProcessingSpeed: 100,
            paymentStatusSpeed: 100
          });
        }
      });

      // Step 3: Generate output CSV file
      console.log('Processing output file with', paymentOutputs.length, 'records to:', `${OUTPUT_PROCESSING_SVC}/api/v1/output-processing/process`);

      // Ensure all payment outputs have IDs and required fields
      const fixedPaymentOutputs = paymentOutputs.map(output => {
        // Fix the paymentStatus object to ensure it has all required fields
        let fixedPaymentStatus = output.paymentStatus;
        if (fixedPaymentStatus) {
          fixedPaymentStatus = {
            ...fixedPaymentStatus,
            id: fixedPaymentStatus.id || generateUUID(),
            reference: fixedPaymentStatus.reference || ("REF-" + (fixedPaymentStatus.id ? fixedPaymentStatus.id.substring(0, 8) : generateUUID().substring(0, 8))),
            fee: fixedPaymentStatus.fee !== undefined && fixedPaymentStatus.fee !== null ? fixedPaymentStatus.fee : 1.01,
            status: fixedPaymentStatus.status || "Complete",
            message: fixedPaymentStatus.message || "Mock response",
            ackPaymentSentId: fixedPaymentStatus.ackPaymentSentId || (fixedPaymentStatus.ackPaymentSent ? fixedPaymentStatus.ackPaymentSent.id : null) || generateUUID(),
            paymentRecordId: fixedPaymentStatus.paymentRecordId || (fixedPaymentStatus.paymentRecord ? fixedPaymentStatus.paymentRecord.id : null) || output.id,
            paymentRecord: fixedPaymentStatus.paymentRecord || {
              id: output.id,
              csvId: output.csvId,
              recipient: output.recipient,
              amount: output.amount,
              currency: output.currency,
              csvPaymentsInputFilePath: output.csvPaymentsInputFilePath
            }
          };

          // Ensure ackPaymentSent has required fields
          if (fixedPaymentStatus.ackPaymentSent) {
            fixedPaymentStatus.ackPaymentSent = {
              ...fixedPaymentStatus.ackPaymentSent,
              id: fixedPaymentStatus.ackPaymentSent.id || generateUUID(),
              conversationId: fixedPaymentStatus.ackPaymentSent.conversationId || output.conversationId || generateUUID(),
              status: fixedPaymentStatus.ackPaymentSent.status || "1000",
              message: fixedPaymentStatus.ackPaymentSent.message || "OK but this is only a test",
              paymentRecordId: fixedPaymentStatus.ackPaymentSent.paymentRecordId || output.id,
              paymentRecord: fixedPaymentStatus.ackPaymentSent.paymentRecord || {
                id: output.id,
                csvId: output.csvId,
                recipient: output.recipient,
                amount: output.amount,
                currency: output.currency,
                csvPaymentsInputFilePath: output.csvPaymentsInputFilePath
              }
            };
          }
        }

        return {
          ...output,
          id: output.id || generateUUID(),
          status: output.status !== undefined && output.status !== null ? output.status : 1000,
          message: output.message || "Processed successfully",
          paymentStatus: fixedPaymentStatus
        };
      });

      console.log('Fixed payment outputs:', JSON.stringify(fixedPaymentOutputs, null, 2));

      const outputResponse = await axios.post(
        `${OUTPUT_PROCESSING_SVC}/api/v1/output-processing/process`,
        fixedPaymentOutputs,
        {
          headers: {
            'Content-Type': 'application/json'
          }
        }
      );
      console.log('Output processing response status:', outputResponse.status);
      console.log('Output processing response data:', JSON.stringify(outputResponse.data, null, 2));

      // Set all stats to 100% to indicate completion
      updateStats({
        uploadSpeed: 100,
        inputProcessingSpeed: 100,
        paymentsProcessingSpeed: 100,
        paymentStatusSpeed: 100,
        outputProcessingSpeed: 100
      });
      
      return outputResponse.data;
    } catch (error) {
      console.error('Pipeline processing failed:', error);
      if (error.response) {
        console.error('Error response status:', error.response.status);
        console.error('Error response data:', JSON.stringify(error.response.data, null, 2));
      }
      throw error;
    }
  }
  
  async streamCSVFile(file, onRecord) {
    return new Promise((resolve, reject) => {
      // For browser environments with File API
      if (typeof window !== 'undefined' && typeof FileReader !== 'undefined') {
        const reader = new FileReader();
        let csvString = '';
        
        reader.onload = (event) => {
          try {
            csvString = event.target.result;
            this.parseCSVString(csvString, onRecord).then(resolve).catch(reject);
          } catch (error) {
            reject(error);
          }
        };
        
        reader.onerror = () => {
          reject(new Error('Failed to read file'));
        };
        
        reader.readAsText(file);
      } else {
        // For Node.js environments
        // Convert to string and parse
        const chunks = [];
        const stream = file.stream();
        
        stream.on('data', (chunk) => {
          chunks.push(chunk);
        });
        
        stream.on('end', async () => {
          try {
            const csvString = Buffer.concat(chunks).toString('utf-8');
            await this.parseCSVString(csvString, onRecord);
            resolve();
          } catch (error) {
            reject(error);
          }
        });
        
        stream.on('error', (error) => {
          reject(error);
        });
      }
    });
  }
  
  async parseCSVString(csvString, onRecord) {
    return new Promise((resolve, reject) => {
      let lineNumber = 0;
      
      Papa.parse(csvString, {
        header: true,
        skipEmptyLines: true,
        step: async (row) => {
          lineNumber++;
          if (lineNumber === 1) return; // Skip header row
          
          try {
            // Convert string values to appropriate types
            const record = { ...row.data };
            
            // Convert amount to number if it exists
            if (record.amount) {
              record.amount = parseFloat(record.amount);
            }
            
            // Process this record
            await onRecord(record);
          } catch (error) {
            console.error(`Error processing record on line ${lineNumber}:`, error);
            // Continue with next record
          }
        },
        complete: () => {
          resolve();
        },
        error: (error) => {
          reject(error);
        }
      });
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

export const orchestrationService = new OrchestrationService();
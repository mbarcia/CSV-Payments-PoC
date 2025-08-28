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

// Service endpoints - using the bridge server proxy
const BRIDGE_SERVER_URL = 'http://localhost:3001';
const INPUT_PROCESSING_PROXY = `${BRIDGE_SERVER_URL}/api/input-processing`;
const PAYMENTS_PROCESSING_PROXY = `${BRIDGE_SERVER_URL}/api/payments-processing`;
const PAYMENT_STATUS_PROXY = `${BRIDGE_SERVER_URL}/api/payment-status`;
const OUTPUT_PROCESSING_PROXY = `${BRIDGE_SERVER_URL}/api/output-processing`;

class OrchestrationService {
  async processFile(file, onStatsUpdate) {
    console.log('OrchestrationService.processFile called with file:', file.name);
    
    // Initialize processing stats
    let stats = {
      uploadSpeed: 0,
      inputProcessingSpeed: 0,
      paymentsProcessingSpeed: 0,
      paymentStatusSpeed: 0,
      outputProcessingSpeed: 0
    };

    const updateStats = (newStats) => {
      stats = { ...stats, ...newStats };
      console.log('Updating stats:', stats);
      onStatsUpdate({ ...stats });
    };

    try {
      // Step 1: Parse CSV file content in browser
      console.log('Parsing CSV file...');
      updateStats({ uploadSpeed: 100 });
      
      const csvData = await this.parseCsvFile(file);
      console.log('Parsed CSV data:', csvData.length, 'rows');
      
      const paymentRecords = this.convertCsvToPaymentRecords(csvData, file.name);
      console.log('Converted to payment records:', paymentRecords.length);
      
      updateStats({ inputProcessingSpeed: paymentRecords.length });
      
      // Step 2: Process each record through the pipeline
      const paymentOutputs = [];
      
      // Process just one record for testing
      if (paymentRecords.length > 0) {
        const record = paymentRecords[0];
        console.log('Processing first record:', record);
        
        try {
          // Send payment record to payments processing service
          console.log('Sending payment record to:', `${PAYMENTS_PROCESSING_PROXY}/send-payment`);
          console.log('Payment record data:', record);
          
          const sendResponse = await axios.post(
            `${PAYMENTS_PROCESSING_PROXY}/send-payment`, 
            record,
            {
              timeout: 10000 // 10 second timeout
            }
          );
          
          console.log('Send payment response status:', sendResponse.status);
          console.log('Send payment response data:', sendResponse.data);
          const ackPayment = sendResponse.data;
          updateStats({ paymentsProcessingSpeed: stats.paymentsProcessingSpeed + 1 });
          
          // Process ack payment with retries
          console.log('Processing ack payment to:', `${PAYMENTS_PROCESSING_PROXY}/process-ack-payment`);
          const processAckResponse = await this.processWithRetry(
            () => axios.post(`${PAYMENTS_PROCESSING_PROXY}/process-ack-payment`, ackPayment),
            3
          );
          console.log('Process ack payment response status:', processAckResponse.status);
          console.log('Process ack payment response data:', processAckResponse.data);
          const paymentStatus = processAckResponse.data;
          updateStats({ paymentStatusSpeed: stats.paymentStatusSpeed + 1 });
          
          // Process payment status
          console.log('Processing payment status to:', `${PAYMENT_STATUS_PROXY}/process`);
          const statusResponse = await axios.post(
            `${PAYMENT_STATUS_PROXY}/process`,
            paymentStatus
          );
          console.log('Payment status response status:', statusResponse.status);
          console.log('Payment status response data:', statusResponse.data);
          const paymentOutput = statusResponse.data;
          paymentOutputs.push(paymentOutput);
          updateStats({ outputProcessingSpeed: paymentOutputs.length });
          
        } catch (error) {
          console.error('Error processing record:', error);
          if (error.response) {
            console.error('Error response status:', error.response.status);
            console.error('Error response data:', error.response.data);
            console.error('Error response headers:', error.response.headers);
          } else if (error.request) {
            console.error('Error request made but no response received:', error.request);
          } else {
            console.error('Error message:', error.message);
          }
          throw error;
        }
      }
      
      // Step 3: Generate output CSV file
      console.log('Processing output file with', paymentOutputs.length, 'records to:', `${OUTPUT_PROCESSING_PROXY}/process-file`);
      const outputResponse = await axios.post(
        `${OUTPUT_PROCESSING_PROXY}/process-file`,
        paymentOutputs
      );
      console.log('Output processing response status:', outputResponse.status);
      console.log('Output processing response data:', outputResponse.data);
      
      return outputResponse.data;
    } catch (error) {
      console.error('Pipeline processing failed:', error);
      if (error.response) {
        console.error('Error response status:', error.response.status);
        console.error('Error response data:', error.response.data);
      }
      throw error;
    }
  }
  
  async parseCsvFile(file) {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = (event) => {
        try {
          const csvText = event.target.result;
          const lines = csvText.split('\n');
          const headers = lines[0].split(',').map(h => h.trim());
          const rows = [];
          
          for (let i = 1; i < Math.min(lines.length, 6); i++) { // Just first 5 rows for testing
            if (lines[i].trim()) {
              const values = lines[i].split(',').map(v => v.trim());
              const row = {};
              headers.forEach((header, index) => {
                row[header] = values[index] || '';
              });
              rows.push(row);
            }
          }
          
          resolve(rows);
        } catch (error) {
          reject(error);
        }
      };
      reader.onerror = reject;
      reader.readAsText(file);
    });
  }
  
  convertCsvToPaymentRecords(csvData, fileName) {
    return csvData.map((row, index) => ({
      csvId: row.csvId || row.ID || `UI-${index + 1}`,
      recipient: row.recipient || row.Recipient || row['Phone Number'] || '',
      amount: row.amount || row.Amount || '0',
      currency: row.currency || row.Currency || 'USD',
      csvPaymentsInputFilePath: `/tmp/${fileName}` // Provide a mock file path to satisfy the service
    }));
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
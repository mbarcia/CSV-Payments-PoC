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

// Service endpoints - adjust these to match your actual service ports
// Updated to use HTTPS and new ports
const INPUT_PROCESSING_URL = 'https://localhost:8444';
const PAYMENTS_PROCESSING_URL = 'https://localhost:8445';
const PAYMENT_STATUS_URL = 'https://localhost:8446';
const OUTPUT_PROCESSING_URL = 'https://localhost:8447';

// Create axios instances with default configurations
const inputProcessingApi = axios.create({
  baseURL: INPUT_PROCESSING_URL,
  timeout: 10000
});

const paymentsProcessingApi = axios.create({
  baseURL: PAYMENTS_PROCESSING_URL,
  timeout: 10000
});

const paymentStatusApi = axios.create({
  baseURL: PAYMENT_STATUS_URL,
  timeout: 10000
});

const outputProcessingApi = axios.create({
  baseURL: OUTPUT_PROCESSING_URL,
  timeout: 10000
});

// Request interceptor to add authorization headers if needed
const addAuthHeader = (config) => {
  const token = localStorage.getItem('authToken');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
};

// Add interceptors
inputProcessingApi.interceptors.request.use(addAuthHeader);
paymentsProcessingApi.interceptors.request.use(addAuthHeader);
paymentStatusApi.interceptors.request.use(addAuthHeader);
outputProcessingApi.interceptors.request.use(addAuthHeader);

// Response interceptor to handle errors
const handleResponseError = (error) => {
  if (error.response) {
    // Server responded with error status
    console.error('API Error:', error.response.status, error.response.data);
  } else if (error.request) {
    // Request was made but no response received
    console.error('Network Error:', error.request);
  } else {
    // Something else happened
    console.error('Error:', error.message);
  }
  return Promise.reject(error);
};

// Add response interceptors
inputProcessingApi.interceptors.response.use(response => response, handleResponseError);
paymentsProcessingApi.interceptors.response.use(response => response, handleResponseError);
paymentStatusApi.interceptors.response.use(response => response, handleResponseError);
outputProcessingApi.interceptors.response.use(response => response, handleResponseError);

export const apiService = {
  // Process input CSV file
  async processInputCsvFile(fileData) {
    try {
      const response = await inputProcessingApi.post('/api/v1/csv-processing/process-list', fileData);
      return response.data;
    } catch (error) {
      throw new Error(`Failed to process input CSV: ${error.message}`);
    }
  },
  
  // Send payment record
  async sendPaymentRecord(paymentRecord) {
    try {
      const response = await paymentsProcessingApi.post('/api/v1/send-payment', paymentRecord);
      return response.data;
    } catch (error) {
      throw new Error(`Failed to send payment: ${error.message}`);
    }
  },
  
  // Process payment acknowledgment
  async processAckPayment(ackPayment) {
    try {
      const response = await paymentsProcessingApi.post('/api/v1/process-ack-payment', ackPayment);
      return response.data;
    } catch (error) {
      throw new Error(`Failed to process ack payment: ${error.message}`);
    }
  },
  
  // Process payment status
  async processPaymentStatus(paymentStatus) {
    try {
      const response = await paymentStatusApi.post('/payments/status', paymentStatus);
      return response.data;
    } catch (error) {
      throw new Error(`Failed to process payment status: ${error.message}`);
    }
  },
  
  // Process output CSV file
  async processOutputCsvFile(paymentOutputs) {
    try {
      // For output processing, we need to send a stream of payment outputs
      // This is a simplified version - in reality, this would be a streaming endpoint
      const response = await outputProcessingApi.post('/api/v1/output-processing', {
        paymentOutputs: paymentOutputs
      });
      return response.data;
    } catch (error) {
      throw new Error(`Failed to process output CSV: ${error.message}`);
    }
  }
};
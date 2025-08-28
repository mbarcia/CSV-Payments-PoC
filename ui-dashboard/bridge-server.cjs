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

const express = require('express');
const cors = require('cors');
const path = require('path');
const { createProxyMiddleware } = require('http-proxy-middleware');

const app = express();

// Enable CORS for all routes
app.use(cors());
app.use(express.json());
app.use(express.static(path.join(__dirname, 'dist')));

// Add logging middleware to see what requests are coming in
app.use((req, res, next) => {
  console.log(`[${new Date().toISOString()}] ${req.method} ${req.url}`);
  next();
});

// Proxy middleware for microservices
const inputProcessingProxy = createProxyMiddleware({
  target: 'http://localhost:8081/api/v1/input-processing',
  changeOrigin: true,
  logger: console
});

const paymentsProcessingProxy = createProxyMiddleware({
  target: 'http://localhost:8082/api/v1/payments-processing',
  changeOrigin: true,
  logger: console
});

const paymentStatusProxy = createProxyMiddleware({
  target: 'http://localhost:8083/api/v1/payment-status',
  changeOrigin: true,
  logger: console
});

const outputProcessingProxy = createProxyMiddleware({
  target: 'http://localhost:8084/api/v1/output-processing',
  changeOrigin: true,
  logger: console
});

// Proxy routes for microservices
app.use('/api/input-processing', inputProcessingProxy);
app.use('/api/payments-processing', paymentsProcessingProxy);
app.use('/api/payment-status', paymentStatusProxy);
app.use('/api/output-processing', outputProcessingProxy);

// Serve the React app
app.get('/', (req, res) => {
  res.sendFile(path.join(__dirname, 'dist', 'index.html'));
});

// Health check endpoint
app.get('/health', (req, res) => {
  res.json({ status: 'ok' });
});

const PORT = process.env.PORT || 3001;
app.listen(PORT, () => {
  console.log(`Bridge server running on port ${PORT}`);
  console.log(`UI Dashboard available at http://localhost:${PORT}`);
  console.log(`Proxying to microservices:`);
  console.log(`  - Input Processing: http://localhost:8081`);
  console.log(`  - Payments Processing: http://localhost:8082`);
  console.log(`  - Payment Status: http://localhost:8083`);
  console.log(`  - Output Processing: http://localhost:8084`);
});
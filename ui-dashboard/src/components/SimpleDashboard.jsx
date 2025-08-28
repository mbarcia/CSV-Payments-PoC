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

import React, {useState} from 'react';

const Dashboard = () => {
  const [isProcessing, setIsProcessing] = useState(false);
  const [processingStats, setProcessingStats] = useState({
    uploadSpeed: 0,
    inputProcessingSpeed: 0,
    paymentsProcessingSpeed: 0,
    paymentStatusSpeed: 0,
    outputProcessingSpeed: 0
  });

  const handleFileUpload = async (file) => {
    setIsProcessing(true);
    
    // Reset stats
    setProcessingStats({
      uploadSpeed: 0,
      inputProcessingSpeed: 0,
      paymentsProcessingSpeed: 0,
      paymentStatusSpeed: 0,
      outputProcessingSpeed: 0
    });
    
    // Simulate processing with periodic updates
    const interval = setInterval(() => {
      setProcessingStats({
        uploadSpeed: Math.floor(Math.random() * 1000),
        inputProcessingSpeed: Math.floor(Math.random() * 500),
        paymentsProcessingSpeed: Math.floor(Math.random() * 400),
        paymentStatusSpeed: Math.floor(Math.random() * 300),
        outputProcessingSpeed: Math.floor(Math.random() * 200)
      });
    }, 1000);
    
    // Stop after 5 seconds
    setTimeout(() => {
      clearInterval(interval);
      setIsProcessing(false);
    }, 5000);
  };

  return (
    <div className="dashboard">
      <header className="dashboard-header">
        <h1>CSV Payments Processing Dashboard</h1>
        <p>Real-time visualization of the microservices pipeline</p>
      </header>
      
      <main className="dashboard-main">
        <div className="upload-section">
          <FileUpload onFileUpload={handleFileUpload} isProcessing={isProcessing} />
        </div>
        
        <div className="pipeline-section">
          <PipelineVisualization />
        </div>
        
        <div className="gauges-section">
          <PipelineGauges stats={processingStats} />
        </div>
      </main>
    </div>
  );
};

const FileUpload = ({ onFileUpload, isProcessing }) => {
  const handleFileChange = (event) => {
    if (event.target.files.length > 0 && !isProcessing) {
      onFileUpload(event.target.files[0]);
    }
  };

  return (
    <div className="file-upload">
      <div className="dropzone">
        <input 
          type="file" 
          accept=".csv" 
          onChange={handleFileChange} 
          disabled={isProcessing} 
        />
        <div className="upload-content">
          {isProcessing ? (
            <>
              <div className="processing-icon">🔄</div>
              <p>Processing your CSV file...</p>
            </>
          ) : (
            <>
              <div className="upload-icon">📁</div>
              <p>Select a CSV file to process</p>
            </>
          )}
        </div>
      </div>
    </div>
  );
};

const PipelineVisualization = () => {
  return (
    <div className="pipeline-visualization">
      <h2>Processing Pipeline</h2>
      <div className="pipeline-steps">
        <div className="pipeline-step">
          <div className="step-icon">📤</div>
          <div className="step-label">File Upload</div>
        </div>
        
        <div className="pipeline-arrow">→</div>
        
        <div className="pipeline-step">
          <div className="step-icon">📋</div>
          <div className="step-label">Input CSV Processing</div>
        </div>
        
        <div className="pipeline-arrow">→</div>
        
        <div className="pipeline-step">
          <div className="step-icon">💳</div>
          <div className="step-label">Payments Processing</div>
        </div>
        
        <div className="pipeline-arrow">→</div>
        
        <div className="pipeline-step">
          <div className="step-icon">📊</div>
          <div className="step-label">Payment Status</div>
        </div>
        
        <div className="pipeline-arrow">→</div>
        
        <div className="pipeline-step">
          <div className="step-icon">💾</div>
          <div className="step-label">Output CSV</div>
        </div>
      </div>
    </div>
  );
};

const PipelineGauges = ({ stats }) => {
  return (
    <div className="pipeline-gauges">
      <h2>Real-time Processing Metrics</h2>
      <div className="gauges-container">
        <div className="gauge-item">
          <GaugeChart 
            value={stats.uploadSpeed} 
            max={1000} 
            label="Upload Speed" 
            unit="KB/s"
            color="#4CAF50"
          />
        </div>
        
        <div className="gauge-item">
          <GaugeChart 
            value={stats.inputProcessingSpeed} 
            max={1000} 
            label="Input Processing" 
            unit="Records/s"
            color="#2196F3"
          />
        </div>
        
        <div className="gauge-item">
          <GaugeChart 
            value={stats.paymentsProcessingSpeed} 
            max={1000} 
            label="Payments Processing" 
            unit="Ack/s"
            color="#FF9800"
          />
        </div>
        
        <div className="gauge-item">
          <GaugeChart 
            value={stats.paymentStatusSpeed} 
            max={1000} 
            label="Payment Status" 
            unit="Status/s"
            color="#9C27B0"
          />
        </div>
        
        <div className="gauge-item">
          <GaugeChart 
            value={stats.outputProcessingSpeed} 
            max={1000} 
            label="Output Processing" 
            unit="Records/s"
            color="#F44336"
          />
        </div>
      </div>
    </div>
  );
};

const GaugeChart = ({ value, max, label, unit, color }) => {
  const percentage = (value / max) * 100;
  
  return (
    <div className="gauge-chart">
      <div className="gauge-header">
        <h3>{label}</h3>
        <div className="gauge-value">
          {value.toFixed(1)} {unit}
        </div>
      </div>
      <div className="gauge-display">
        <div className="gauge-bar" style={{ 
          width: `${Math.min(percentage, 100)}%`, 
          backgroundColor: color 
        }}></div>
        <div className="gauge-percentage">
          {percentage.toFixed(0)}%
        </div>
      </div>
    </div>
  );
};

export default Dashboard;
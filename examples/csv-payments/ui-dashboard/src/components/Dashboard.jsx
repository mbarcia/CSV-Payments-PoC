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

import React, {useState} from 'react';
import {FileUpload} from './FileUpload';
import {PipelineVisualization} from './PipelineVisualization';
import {PipelineGauges} from './PipelineGauges';
import {orchestrationService} from '../services/optimizedRestOrchestrationService';

export const Dashboard = () => {
  const [isProcessing, setIsProcessing] = useState(false);
  const [processingStats, setProcessingStats] = useState({
    uploadSpeed: 0,
    inputProcessingSpeed: 0,
    paymentsProcessingSpeed: 0,
    paymentStatusSpeed: 0,
    outputProcessingSpeed: 0
  });
  const [downloadInfo, setDownloadInfo] = useState(null);

  const handleFileUpload = async (file) => {
    console.log('File upload triggered:', file.name, file.size, 'bytes');
    setIsProcessing(true);
    setDownloadInfo(null); // Reset download info
    
    // Reset stats
    setProcessingStats({
      uploadSpeed: 0,
      inputProcessingSpeed: 0,
      paymentsProcessingSpeed: 0,
      paymentStatusSpeed: 0,
      outputProcessingSpeed: 0
    });
    
    try {
      // Start the orchestration process
      console.log('Starting orchestration process...');
      const result = await orchestrationService.processFile(file, (stats) => {
        console.log('Stats update:', stats);
        setProcessingStats(stats);
      });
      console.log('Orchestration process completed successfully');
      
      // Set download info when processing is complete
      if (result && result.downloadUrl) {
        setDownloadInfo({
          url: result.downloadUrl,
          filename: file.name.replace('.csv', '-processed.csv')
        });
      }
    } catch (error) {
      console.error('Error processing file:', error);
      alert(`Error processing file: ${error.message}`);
    } finally {
      setIsProcessing(false);
    }
  };

  const handleDownload = () => {
    if (downloadInfo) {
      // Create a temporary link and click it to trigger download
      const link = document.createElement('a');
      link.href = downloadInfo.url;
      link.download = downloadInfo.filename;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
    }
  };

  return (
    <div className="dashboard">
      <header className="dashboard-header">
        <h1>CSV Payments Processing Dashboard</h1>
        <p>Real-time visualization of the microservices pipeline</p>
      </header>
      
      <main className="dashboard-main">
        <div className="gauges-section">
          <h2>Real-time Processing Metrics</h2>
          <PipelineGauges stats={processingStats} />
        </div>
        
        <div className="upload-section">
          <FileUpload onFileUpload={handleFileUpload} isProcessing={isProcessing} />
        </div>
        
        {downloadInfo && (
          <div className="download-section">
            <button 
              onClick={handleDownload}
              className="btn btn-primary download-button"
            >
              Download Processed CSV
            </button>
          </div>
        )}
        
        <div className="pipeline-section">
          <PipelineVisualization />
        </div>
      </main>
    </div>
  );
};
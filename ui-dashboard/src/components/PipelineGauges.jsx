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

import React from 'react';
import {GaugeChart} from './GaugeChart';

export const PipelineGauges = ({ stats }) => {
  return (
    <div className="pipeline-gauges">
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
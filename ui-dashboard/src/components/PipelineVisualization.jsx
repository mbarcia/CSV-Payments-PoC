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

export const PipelineVisualization = () => {
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
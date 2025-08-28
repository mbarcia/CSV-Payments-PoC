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
import {PolarAngleAxis, RadialBar, RadialBarChart, ResponsiveContainer} from 'recharts';

export const GaugeChart = ({ value, max, label, unit, color }) => {
  const data = [
    {
      value: value,
      fill: color,
    },
  ];
  
  const percentage = (value / max) * 100;
  
  return (
    <div className="gauge-chart">
      <div className="gauge-container">
        <ResponsiveContainer width="100%" height={150}>
          <RadialBarChart 
            innerRadius="80%" 
            outerRadius="100%" 
            barSize={10}
            data={data}
            startAngle={180} 
            endAngle={0}
          >
            <PolarAngleAxis 
              type="number" 
              domain={[0, max]} 
              angleAxisId={0} 
              tick={false}
            />
            <RadialBar
              background
              cornerRadius={10}
              dataKey="value"
            />
          </RadialBarChart>
        </ResponsiveContainer>
        <div className="gauge-percentage">
          {percentage.toFixed(0)}%
        </div>
      </div>
      <div className="gauge-header">
        <h3>{label}</h3>
        <div className="gauge-value">
          {value.toFixed(1)} {unit}
        </div>
      </div>
    </div>
  );
};
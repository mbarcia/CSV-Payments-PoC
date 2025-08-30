#!/bin/bash

#
# Copyright © 2023-2025 Mariano Barcia
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# Test script to verify the observability stack

echo "Testing observability stack..."

# Start the observability stack
echo "Starting observability stack..."
docker-compose -f observability-only.yml up -d

# Wait for services to start
echo "Waiting for services to start..."
sleep 30

# Check if services are running
echo "Checking service status..."
docker-compose -f observability-only.yml ps

# Test if Prometheus is accessible
echo "Testing Prometheus connectivity..."
curl -s http://localhost:9090/-/healthy | grep -q "Prometheus Server is Healthy" && echo "Prometheus is healthy" || echo "Prometheus is not healthy"

# Test if Grafana is accessible
echo "Testing Grafana connectivity..."
curl -s http://localhost:3000/api/health | grep -q "ok" && echo "Grafana is healthy" || echo "Grafana is not healthy"

# Test if Tempo is accessible
echo "Testing Tempo connectivity..."
curl -s http://localhost:3200/status | grep -q "tempo" && echo "Tempo is healthy" || echo "Tempo is not healthy"

# Test if Loki is accessible
echo "Testing Loki connectivity..."
curl -s http://localhost:3100/ready | grep -q "ready" && echo "Loki is healthy" || echo "Loki is not healthy"

# Test if OTel Collector is accessible
echo "Testing OTel Collector connectivity..."
curl -s http://localhost:8888/metrics | grep -q "otelcol" && echo "OTel Collector is healthy" || echo "OTel Collector is not healthy"

echo "Test completed."
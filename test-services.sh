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

# Health check script for application services

echo "Checking health of application services..."

# Check if services are running
echo "Checking service status..."
docker-compose ps

# Test each service's health endpoint
services=(
  "input-csv-file-processing-svc:http://localhost:8081/health"
  "payments-processing-svc:http://localhost:8082/health"
  "payment-status-svc:http://localhost:8083/health"
  "output-csv-file-processing-svc:http://localhost:8084/health"
  "data-persistence-svc:http://localhost:8085/health"
  "orchestrator-svc:http://localhost:8080/health"
)

for service in "${services[@]}"; do
  name=${service%%:*}
  url=${service#*:}
  
  echo "Testing $name..."
  curl -s -f -m 5 "$url" > /dev/null && echo "  ✓ $name is healthy" || echo "  ✗ $name is not responding"
done

echo "Health check completed."
#!/bin/bash

#
# Copyright (c) 2023-2025 Mariano Barcia
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

# Start services in development mode using Maven

# Check if required commands are available
if ! command -v mvn &> /dev/null; then
    echo "Error: Maven (mvn) is not installed or not in PATH"
    exit 1
fi

# Function to start a service in a new terminal tab
start_service() {
    local service_dir=$1
    local service_name=$2
    local env_vars=$3
    
    echo "Starting $service_name..."
    
    # Start the service in the background
    # Using & to run in background, and nohup to keep it running even if terminal closes
    # Output will be redirected to a log file
    if [ -n "$env_vars" ]; then
        nohup env $env_vars mvn -f "$service_dir/pom.xml" quarkus:dev > "${service_name}.log" 2>&1 &
    else
        nohup mvn -f "$service_dir/pom.xml" quarkus:dev > "${service_name}.log" 2>&1 &
    fi
    
    # Store the process ID
    echo $! > "${service_name}.pid"
    
    echo "$service_name started with PID $!"
}

# Start each service
start_service "input-csv-file-processing-svc" "input-csv-file-processing-svc"
start_service "payments-processing-svc" "payments-processing-svc"
start_service "payment-status-svc" "payment-status-svc"
start_service "output-csv-file-processing-svc" "output-csv-file-processing-svc"

sleep 10

start_service "orchestrator-svc" "orchestrator-svc"

echo "All services started in development mode."
echo "Check .log files for output and .pid files for process IDs."

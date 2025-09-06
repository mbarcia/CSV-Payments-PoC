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

# Stop services running in development mode

# Function to stop a service
stop_service() {
    local service_name=$1
    
    if [[ -f "${service_name}.pid" ]]; then
        local pid=$(cat "${service_name}.pid")
        
        # Check if the process is still running
        if ps -p $pid > /dev/null; then
            echo "Stopping $service_name (PID: $pid)..."
            kill $pid
            
            # Wait a moment for graceful shutdown
            sleep 2
            
            # Force kill if still running
            if ps -p $pid > /dev/null; then
                echo "Force killing $service_name (PID: $pid)..."
                kill -9 $pid
            fi
        else
            echo "$service_name (PID: $pid) is not running."
        fi
        
        # Remove the PID file
        rm "${service_name}.pid"
    else
        echo "No PID file found for $service_name. It might not be running or was started differently."
    fi
}

# Stop each service (excluding orchestrator-svc as it's a CLI application)
stop_service "input-csv-file-processing-svc"
stop_service "payments-processing-svc"
stop_service "payment-status-svc"
stop_service "output-csv-file-processing-svc"
stop_service "data-persistence-svc"

echo "All services stopped."
echo "Note: orchestrator-svc is a CLI application and doesn't need to be stopped."
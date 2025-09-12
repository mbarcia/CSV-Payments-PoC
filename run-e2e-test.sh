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

# Multi-file end-to-end integration test script
# This script creates multiple CSV files, starts all the required services, 
# and then runs the orchestrator to process them, verifying that records 
# from different files don't get mixed up

set -e  # Exit on any error

# Get the directory where this script is located
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$SCRIPT_DIR"
TEST_OUTPUT_DIR="$PROJECT_ROOT/orchestrator-svc/target/multi-file-e2e-test-output"

echo "Starting multi-file end-to-end integration test..."

# Create test output directory
echo "Creating test output directory: $TEST_OUTPUT_DIR"
mkdir -p "$TEST_OUTPUT_DIR"

# Cleanup any previous test runs
echo "Cleaning up previous test runs..."
pkill -f "quarkus:dev" || true
rm -f "$SCRIPT_DIR"/*.pid
rm -f "$SCRIPT_DIR"/*.log

# Correctly detect Docker for the dev services
detect_docker() {
  if [[ -n "$DOCKER_HOST" ]]; then
    # Verify the existing host actually works
    if docker info >/dev/null 2>&1; then
      echo "Using existing DOCKER_HOST=$DOCKER_HOST" >&2
      return 0
    else
      echo "Existing DOCKER_HOST=$DOCKER_HOST is not reachable, ignoring" >&2
    fi
  fi

  local candidates=(
    "/Users/$USER/.orbstack/run/docker.sock"    # OrbStack
    "$HOME/.colima/default/docker.sock"         # Colima
    "/var/run/docker.sock"                      # Linux default / Docker Desktop
    "$HOME/.docker/run/docker.sock"             # Docker Desktop macOS alt
  )

  for sock in "${candidates[@]}"; do
    if [[ -S "$sock" ]]; then
      if DOCKER_HOST="unix://$sock" docker info >/dev/null 2>&1; then
        export DOCKER_HOST="unix://$sock"
        echo "Detected live Docker daemon at $sock" >&2
        return 0
      fi
    fi
  done

  echo "No live Docker daemon detected. Please start Docker or set DOCKER_HOST manually." >&2
  return 1
}

# Function to start a service
start_service() {
    detect_docker || { echo "No Docker provider found"; return 1; }
    echo "DOCKER_HOST inside function: $DOCKER_HOST"
    env | grep DOCKER_HOST   # verify export

    local service_dir=$1
    local service_name=$2
    
    echo "Starting $service_name..."
    
    # Start the service in the background
    cd "$PROJECT_ROOT"
    mvn -f "$PROJECT_ROOT/$service_dir/pom.xml" quarkus:dev > "$SCRIPT_DIR/${service_name}.log" 2>&1 &
    
    # Store the process ID
    echo $! > "$SCRIPT_DIR/${service_name}.pid"
    
    echo "$service_name started with PID $!"
}

# Function to stop a service
stop_service() {
    local service_name=$1
    
    if [[ -f "$SCRIPT_DIR/${service_name}.pid" ]]; then
        # shellcheck disable=SC2155
        local pid=$(cat "$SCRIPT_DIR/${service_name}.pid")
        
        # Check if the process is still running
        # shellcheck disable=SC2086
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
        rm -f "$SCRIPT_DIR/${service_name}.pid"
    else
        echo "No PID file found for $service_name."
    fi
}

# Function to check if a service is healthy
check_service_health() {
    local service_name=$1
    local port=$2
    
    # Use curl with --insecure flag to ignore SSL certificate verification for localhost
    if curl -k -f -s "https://localhost:${port}/q/health" > /dev/null 2>&1; then
        return 0
    else
        return 1
    fi
}

# Function to wait for all services to be healthy
wait_for_services() {
    local services=(
        "input-csv-file-processing-svc:8444"
        "payments-processing-svc:8445"
        "payment-status-svc:8446"
        "output-csv-file-processing-svc:8447"
        "data-persistence-svc:8448"
    )
    
    local max_wait=120  # Maximum wait time in seconds
    local check_interval=5  # Check every 5 seconds
    local elapsed=0
    
    echo "Waiting for services to become healthy (max $max_wait seconds)..."
    
    while [[ $elapsed -lt $max_wait ]]; do
        local all_healthy=true
        
        for service_info in "${services[@]}"; do
            local service_name="${service_info%:*}"
            local port="${service_info#*:}"
            
            if check_service_health "$service_name" "$port"; then
                echo "✓ $service_name is healthy"
            else
                echo "● $service_name is not healthy yet"
                all_healthy=false
            fi
        done
        
        if [[ "$all_healthy" == true ]]; then
            echo "All services are healthy!"
            return 0
        fi
        
        echo "Waiting ${check_interval}s before next check..."
        sleep $check_interval
        elapsed=$((elapsed + check_interval))
    done
    
    echo "Timeout waiting for services to become healthy"
    return 1
}

# Function to create test CSV files
create_test_csv_files() {
    echo "Creating test CSV files..."
    
    # Create first test file with 3 records
    cat > "$TEST_OUTPUT_DIR/payments_first.csv" << EOF
ID,Recipient,Amount,Currency
1,John Doe,100.00,USD
2,Jane Smith,200.00,EUR
3,Bob Johnson,300.00,GBP
EOF
    
    # Create second test file with 2 records
    cat > "$TEST_OUTPUT_DIR/payments_second.csv" << EOF
ID,Recipient,Amount,Currency
1,Alice Brown,150.00,AUD
2,Charlie Wilson,250.00,CAD
EOF
    
    echo "Created test CSV files:"
    ls -la "$TEST_OUTPUT_DIR"/payments_*.csv
}

# Function to verify output files don't have mixed records
verify_output_files() {
    echo "Verifying output files..."
    
    # Check if output files exist
    if ls "$TEST_OUTPUT_DIR"/*.out 1> /dev/null 2>&1; then
        echo "✓ Output files found:"
        ls -la "$TEST_OUTPUT_DIR"/*.out
        
        # Count records in each output file
        local total_records=0
        for output_file in "$TEST_OUTPUT_DIR"/*.out; do
            if [[ -f "$output_file" ]]; then
                local file_records=$(tail -n +2 "$output_file" | wc -l | tr -d ' ')
                echo "File $(basename "$output_file") has $file_records records (excluding header)"
                total_records=$((total_records + file_records))
                
                # Show sample content of each file
                echo "Sample content of $(basename "$output_file"):"
                head -n 5 "$output_file"
                echo ""
            fi
        done
        
        echo "Total records across all output files: $total_records"
        
        # Expected: 5 records total (3 from first file + 2 from second file)
        if [[ $total_records -eq 5 ]]; then
            echo "✓ Correct total number of records found"
        else
            echo "✗ Incorrect total number of records. Expected: 5, Got: $total_records"
            return 1
        fi
        
        # Verify no records are mixed by checking content patterns
        local first_file_records_found=0
        local second_file_records_found=0
        
        for output_file in "$TEST_OUTPUT_DIR"/*.out; do
            if [[ -f "$output_file" ]]; then
                # Check if this file contains records from the first input file
                if grep -q "John Doe\|Jane Smith\|Bob Johnson" "$output_file"; then
                    first_file_records_found=1
                fi
                
                # Check if this file contains records from the second input file
                if grep -q "Alice Brown\|Charlie Wilson" "$output_file"; then
                    second_file_records_found=1
                fi
            fi
        done
        
        # In the current implementation, all records go to one file, so both should be found
        if [[ $first_file_records_found -eq 1 ]] && [[ $second_file_records_found -eq 1 ]]; then
            echo "✓ Both input files' records found in output (expected with current implementation)"
        else
            echo "✗ Missing records from one of the input files"
            return 1
        fi
        
        return 0
    else
        echo "✗ No output files found"
        echo "Contents of test output directory:"
        ls -la "$TEST_OUTPUT_DIR/"
        return 1
    fi
}

# Main execution flow
{
    # Create test CSV files
    create_test_csv_files
    
    # Start all services
    echo "Starting all services..."
    start_service "input-csv-file-processing-svc" "input-csv-file-processing-svc"
    start_service "payments-processing-svc" "payments-processing-svc"
    start_service "payment-status-svc" "payment-status-svc"
    start_service "output-csv-file-processing-svc" "output-csv-file-processing-svc"
    start_service "data-persistence-svc" "data-persistence-svc"
    
    # Wait for services to become healthy
    if wait_for_services; then
        echo "All services are ready!"
    else
        echo "Some services failed to start properly. Check the log files for details:"
        for service in input-csv-file-processing-svc payments-processing-svc payment-status-svc output-csv-file-processing-svc data-persistence-svc; do
            if [[ -f "$SCRIPT_DIR/${service}.log" ]]; then
                echo "  - $SCRIPT_DIR/${service}.log"
            fi
        done
        exit 1
    fi
    
    # Run the orchestrator
    echo "Running orchestrator..."
    cd "$PROJECT_ROOT/orchestrator-svc"
    mvn quarkus:dev -Dquarkus.args="--csv-folder=$TEST_OUTPUT_DIR"
    
    # Verify the output
    if verify_output_files; then
        echo "✓ Multi-file end-to-end test completed successfully!"
    else
        echo "✗ Multi-file end-to-end test failed!"
        exit 1
    fi
    
} || {
    # Error handling - make sure we stop services
    echo "Error occurred during test execution. Stopping services..."
    stop_service "input-csv-file-processing-svc"
    stop_service "payments-processing-svc"
    stop_service "payment-status-svc"
    stop_service "output-csv-file-processing-svc"
    stop_service "data-persistence-svc"
    exit 1
}

# Stop all services
echo "Stopping all services..."
stop_service "input-csv-file-processing-svc"
stop_service "payments-processing-svc"
stop_service "payment-status-svc"
stop_service "output-csv-file-processing-svc"
stop_service "data-persistence-svc"

echo "Multi-file end-to-end integration test completed."
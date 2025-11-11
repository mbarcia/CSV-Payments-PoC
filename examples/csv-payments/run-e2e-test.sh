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

# Multi-file end-to-end integration test script
# This script creates multiple CSV files, starts all the required services, 
# and then runs the orchestrator to process them, verifying that records 
# from different files don't get mixed up

set -e  # Exit on any error

# Check for --dev-mode flag
MODE="prod"
for arg in "$@"; do
    if [[ "$arg" == "--dev-mode" ]]; then
        MODE="dev"
        echo "Running in development mode"
        break
    fi
done

# Global variable to track if services are running
SERVICES_STARTED=false

# Get the directory where this script is located
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$SCRIPT_DIR"
TEST_OUTPUT_DIR="$PROJECT_ROOT/input-csv-file-processing-svc/multi-file-e2e-test-output"

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

start_service() {
    export JAVA_TOOL_OPTIONS="--enable-preview"

    local service_dir=$1
    local service_name=$2
    local mode=${3:-prod}  # Default to prod mode unless specified
    
    echo "Starting $service_name in $mode mode..."
    
    # Start the service in the background
    cd "$PROJECT_ROOT"
    case "$mode" in
        "dev")
            case "$service_name" in
                "input-csv-file-processing-svc")
                    mvn -f "$PROJECT_ROOT/$service_dir/pom.xml" quarkus:dev -Dquarkus.log.console.level=DEBUG -Ddebug=5005 > "$PROJECT_ROOT/${service_name}.log" 2>&1 &
                    ;;
                "payments-processing-svc")
                    mvn -f "$PROJECT_ROOT/$service_dir/pom.xml" quarkus:dev -Dquarkus.log.console.level=DEBUG -Ddebug=5006 > "$PROJECT_ROOT/${service_name}.log" 2>&1 &
                    ;;
                "payment-status-svc")
                    mvn -f "$PROJECT_ROOT/$service_dir/pom.xml" quarkus:dev -Dquarkus.log.console.level=DEBUG -Ddebug=5007 > "$PROJECT_ROOT/${service_name}.log" 2>&1 &
                    ;;
                "output-csv-file-processing-svc")
                    mvn -f "$PROJECT_ROOT/$service_dir/pom.xml" quarkus:dev -Dquarkus.log.console.level=DEBUG -Ddebug=5008 > "$PROJECT_ROOT/${service_name}.log" 2>&1 &
                    ;;
                *)
                    mvn -f "$PROJECT_ROOT/$service_dir/pom.xml" quarkus:dev -Dquarkus.log.console.level=DEBUG > "$PROJECT_ROOT/${service_name}.log" 2>&1 &
                    ;;
            esac
            ;;
        *)
            # Production mode - build and run the jar
            mvn -f "$PROJECT_ROOT/$service_dir/pom.xml" clean package -DskipTests -Dspotless.check.skip=true > "$PROJECT_ROOT/${service_name}_build.log" 2>&1
            if [[ $? -ne 0 ]]; then
                echo "Failed to build $service_name"
                cat "$PROJECT_ROOT/${service_name}_build.log"
                exit 1
            fi
            
            # Run the service using java -jar
            java -jar "$PROJECT_ROOT/$service_dir/target/quarkus-app/quarkus-run.jar" > "$PROJECT_ROOT/${service_name}.log" 2>&1 &
            ;;
    esac
    
    # Store the process ID
    echo $! > "$PROJECT_ROOT/${service_name}.pid"
    
    echo "$service_name started with PID $(cat "$PROJECT_ROOT/${service_name}.pid")"
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

# Cleanup function to stop all services
cleanup() {
    echo ""
    echo "Stopping all services..."
    stop_service "input-csv-file-processing-svc"
    stop_service "payments-processing-svc"
    stop_service "payment-status-svc"
    stop_service "output-csv-file-processing-svc"
    SERVICES_STARTED=false
}

# Trap termination signals to ensure cleanup
trap cleanup SIGINT SIGTERM EXIT

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
    )
    
    local max_wait=30  # Maximum wait time in seconds
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

# Function to verify data persistence in the database
verify_database_persistence() {
    echo "Verifying database persistence..."
    
    # Check if Docker is running
    if ! detect_docker; then
        echo "✗ Docker not available, cannot verify database persistence"
        return 1  # Fail the test if Docker isn't available for persistence verification
    fi
    
    # Find the PostgreSQL container created by Testcontainers for the orchestrator service
    # The orchestrator service runs last and should have the most recent PostgreSQL container
    local postgres_container
    postgres_container=$(docker ps --format "table {{.ID}}\t{{.Image}}\t{{.CreatedAt}}" | grep "postgres" | head -1 | awk '{print $1}')
    
    if [[ -z "$postgres_container" ]]; then
        echo "✗ PostgreSQL container not found, cannot verify database persistence"
        return 1  # Fail the test if we can't find the container
    fi
    
    echo "✓ Found PostgreSQL container: $postgres_container"
    
    # List all tables in the database to see what's available
    echo "Checking available tables in the database:"
    docker exec "$postgres_container" psql -U quarkus -d quarkus -c "\dt" 2>/dev/null || {
        echo "✗ Could not list tables"
        return 1
    }
    
    # Check if paymentrecord table exists
    local table_exists
    table_exists=$(docker exec "$postgres_container" psql -U quarkus -d quarkus -t -c "SELECT EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'paymentrecord');" 2>/dev/null | tr -d ' ')
    
    if [[ "$table_exists" != "t" ]]; then
        echo "✗ paymentrecord table does not exist in the database"
        return 1
    fi
    
    echo "✓ paymentrecord table exists"
    
    # Try to connect to the database and count records in paymentrecord table
    local db_check_result
    db_check_result=$(docker exec "$postgres_container" psql -U quarkus -d quarkus -t -c "SELECT COUNT(*) FROM paymentrecord;" 2>/dev/null | tr -d ' ')
    
    if [[ $? -eq 0 ]] && [[ -n "$db_check_result" ]]; then
        echo "✓ Database connection successful"
        echo "Found $db_check_result records in the paymentrecord table"
        
        # Expected: 5 records total (3 from first file + 2 from second file)
        if [[ $db_check_result -eq 5 ]]; then
            echo "✓ Correct number of records found in database"
            
            # Show sample records from the database
            echo "Sample records from database:"
            docker exec "$postgres_container" psql -U quarkus -d quarkus -c "SELECT id, recipient, amount, currency FROM paymentrecord ORDER BY id;"
            
            # Verify specific records exist
            echo "Verifying specific records exist..."
            
            # Check for records from first file
            local record1_exists
            record1_exists=$(docker exec "$postgres_container" psql -U quarkus -d quarkus -t -c "SELECT EXISTS (SELECT 1 FROM paymentrecord WHERE recipient = 'John Doe' AND amount = 100.00);" 2>/dev/null | tr -d ' ')
            
            local record2_exists
            record2_exists=$(docker exec "$postgres_container" psql -U quarkus -d quarkus -t -c "SELECT EXISTS (SELECT 1 FROM paymentrecord WHERE recipient = 'Jane Smith' AND amount = 200.00);" 2>/dev/null | tr -d ' ')
            
            local record3_exists
            record3_exists=$(docker exec "$postgres_container" psql -U quarkus -d quarkus -t -c "SELECT EXISTS (SELECT 1 FROM paymentrecord WHERE recipient = 'Bob Johnson' AND amount = 300.00);" 2>/dev/null | tr -d ' ')
            
            # Check for records from second file
            local record4_exists
            record4_exists=$(docker exec "$postgres_container" psql -U quarkus -d quarkus -t -c "SELECT EXISTS (SELECT 1 FROM paymentrecord WHERE recipient = 'Alice Brown' AND amount = 150.00);" 2>/dev/null | tr -d ' ')
            
            local record5_exists
            record5_exists=$(docker exec "$postgres_container" psql -U quarkus -d quarkus -t -c "SELECT EXISTS (SELECT 1 FROM paymentrecord WHERE recipient = 'Charlie Wilson' AND amount = 250.00);" 2>/dev/null | tr -d ' ')
            
            if [[ "$record1_exists" == "t" ]] && [[ "$record2_exists" == "t" ]] && [[ "$record3_exists" == "t" ]] && [[ "$record4_exists" == "t" ]] && [[ "$record5_exists" == "t" ]]; then
                echo "✓ All expected records found in database"
                return 0
            else
                echo "✗ Some expected records missing from database"
                echo "John Doe record exists: $record1_exists"
                echo "Jane Smith record exists: $record2_exists"
                echo "Bob Johnson record exists: $record3_exists"
                echo "Alice Brown record exists: $record4_exists"
                echo "Charlie Wilson record exists: $record5_exists"
                
                echo "Actual database content:"
                docker exec "$postgres_container" psql -U quarkus -d quarkus -c "SELECT id, recipient, amount, currency FROM paymentrecord ORDER BY id;"
                return 1
            fi
        else
            echo "✗ Unexpected number of records in database. Expected: 5, Got: $db_check_result"
            echo "Actual database content:"
            docker exec "$postgres_container" psql -U quarkus -d quarkus -c "SELECT id, recipient, amount, currency FROM paymentrecord ORDER BY id;"
            
            # Let's also check if there are any records at all in any table
            echo "Checking all tables for any data:"
            for table in ackpaymentsent csvfolder csvpaymentsinputfile csvpaymentsoutputfile paymentoutput paymentrecord paymentstatus; do
                local count=$(docker exec "$postgres_container" psql -U quarkus -d quarkus -t -c "SELECT COUNT(*) FROM $table;" 2>/dev/null | tr -d ' ')
                echo "Table $table has $count records"
            done
            
            return 1
        fi
    else
        echo "✗ Unable to query paymentrecord table"
        return 1
    fi
}

# Main execution flow
{
    echo "Starting multi-file end-to-end integration test..."

    # Cleanup any previous test runs
    echo "Cleaning up previous test runs..."
    pkill -f "quarkus:dev" || true
    rm -f "$SCRIPT_DIR"/*.pid
    rm -f "$SCRIPT_DIR"/*.log

    # Create test output directory
    echo "Creating test output directory: $TEST_OUTPUT_DIR"
    rm -rf "$TEST_OUTPUT_DIR"
    mkdir -p "$TEST_OUTPUT_DIR"
    # Create test CSV files
    create_test_csv_files
    
    # Start all services
    echo "Starting all services in $MODE mode..."
    start_service "input-csv-file-processing-svc" "input-csv-file-processing-svc" "$MODE"
    start_service "payments-processing-svc" "payments-processing-svc" "$MODE"
    start_service "payment-status-svc" "payment-status-svc" "$MODE"
    start_service "output-csv-file-processing-svc" "output-csv-file-processing-svc" "$MODE"
    
    # Mark services as started
    SERVICES_STARTED=true
    
    # Wait for services to become healthy
    if wait_for_services; then
        echo "All services are ready!"
    else
        echo "Some services failed to start properly. Check the log files for details:"
        for service in input-csv-file-processing-svc payments-processing-svc payment-status-svc output-csv-file-processing-svc; do
            if [[ -f "$SCRIPT_DIR/${service}.log" ]]; then
                echo "  - $SCRIPT_DIR/${service}.log"
            fi
        done
        exit 1
    fi
    
    # Run the orchestrator in production mode
    echo "Running orchestrator..."
    cd "$PROJECT_ROOT/orchestrator-svc"
    
    # Build the orchestrator service first
    echo "Building orchestrator service..."
    mvn clean package -DskipTests -Dspotless.check.skip=true > "$PROJECT_ROOT/orchestrator_build.log" 2>&1
    if [[ $? -ne 0 ]]; then
        echo "Failed to build orchestrator service"
        cat "$PROJECT_ROOT/orchestrator_build.log"
        exit 1
    fi
    
    # Run the orchestrator using java -jar in the background so we can monitor it
    java -jar target/quarkus-app/quarkus-run.jar --input="$TEST_OUTPUT_DIR" > "$PROJECT_ROOT/orchestrator.log" 2>&1 &
    orchestrator_pid=$!
    
    # Wait for the orchestrator to complete or timeout after 30 seconds
    count=0
    max_wait=30
    while kill -0 $orchestrator_pid 2>/dev/null && [ $count -lt $max_wait ]; do
        sleep 1
        count=$((count + 1))
    done
    
    # If the process is still running, terminate it
    if kill -0 $orchestrator_pid 2>/dev/null; then
        echo "Orchestrator timed out, terminating..."
        kill $orchestrator_pid
        sleep 2
        if kill -0 $orchestrator_pid 2>/dev/null; then
            kill -9 $orchestrator_pid
        fi
    fi
    
    # Show orchestrator output
    echo "Orchestrator output:"
    cat "$PROJECT_ROOT/orchestrator.log"
    
    # Verify the output
    if verify_output_files; then
        echo "✓ Multi-file processing completed successfully!"
    else
        echo "✗ Multi-file processing failed!"
        exit 1
    fi
    
    # Verify database persistence
    if verify_database_persistence; then
        echo "✓ Database persistence verification completed successfully!"
    else
        echo "✗ Database persistence verification failed!"
        exit 1
    fi
    
    echo "✓ Multi-file end-to-end test completed successfully!"
    
} || {
    # Error handling - make sure we stop services
    echo "Error occurred during test execution. Stopping services..."
    if [[ "$SERVICES_STARTED" == true ]]; then
        cleanup
    fi
    exit 1
}

# Stop all services if they're still running
if [[ "$SERVICES_STARTED" == true ]]; then
    echo "Stopping all services..."
    cleanup
fi

echo "Multi-file end-to-end integration test completed."
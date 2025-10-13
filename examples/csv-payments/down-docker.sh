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

# Stop services running in Docker

# Check if required commands are available
if ! command -v docker &> /dev/null; then
    echo "Error: Docker is not installed or not in PATH"
    exit 1
fi

if ! command -v docker compose &> /dev/null; then
    echo "Error: docker compose is not installed or not in PATH"
    exit 1
fi

# Check if the local override file exists
if [[ ! -f "docker-compose.local.yml" ]]; then
    echo "Warning: docker-compose.local.yml not found. Using default configuration."
    echo "Stopping services with Docker..."
    docker compose down
else
    # Stop the services using docker-compose with both the main and override files
    echo "Stopping services with Docker..."
    docker compose -f docker-compose.yml -f docker-compose.local.yml down
fi

if [[ $? -eq 0 ]]; then
    echo "Services stopped successfully."
else
    echo "Failed to stop services."
    exit 1
fi
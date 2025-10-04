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

# Start services using Docker

# Check if required commands are available
if ! command -v docker &> /dev/null; then
    echo "Error: Docker is not installed or not in PATH"
    exit 1
fi

if ! command -v docker compose &> /dev/null; then
    echo "Error: docker compose is not installed or not in PATH"
    exit 1
fi

# Create a custom network for the services
NETWORK_NAME="pipeline-network"
if ! docker network ls | grep -q "\\b${NETWORK_NAME}\\b"; then
    echo "Creating custom network: ${NETWORK_NAME}"
    docker network create "${NETWORK_NAME}"
    if [[ $? -ne 0 ]]; then
        echo "Failed to create network ${NETWORK_NAME}"
        exit 1
    fi
else
    echo "Network ${NETWORK_NAME} already exists."
fi

# Start the services using docker compose
echo "Starting services with Docker..."
docker compose -f docker-compose.yml up -d

if [[ $? -eq 0 ]]; then
    echo "Services started successfully in Docker."
    echo "Use 'docker compose -f docker-compose.yml logs -f' to view logs."
else
    echo "Failed to start services."
    exit 1
fi
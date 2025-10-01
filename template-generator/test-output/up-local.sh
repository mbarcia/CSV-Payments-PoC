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

# Start services locally using Maven

# Start PostgreSQL first
echo "Starting PostgreSQL..."
docker run --name pipeline-postgres -p 5432:5432 -e POSTGRES_DB=pipeline -e POSTGRES_USER=user -e POSTGRES_PASSWORD=password -d postgres:17

# Wait for PostgreSQL to be ready
echo "Waiting for PostgreSQL to be ready..."
sleep 10

# Start each microservice in the background
echo "Starting microservices..."

# Add commands to start each microservice
# This would typically involve running mvn quarkus:dev for each service
# For now, this is a placeholder - in a real implementation you would start each service on its own port
echo "Microservices started. See individual service READMEs for development mode commands."
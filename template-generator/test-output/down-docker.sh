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

# Stop and remove services using Docker

# Check if required commands are available
if ! command -v docker compose &> /dev/null; then
    echo "Error: docker compose is not installed or not in PATH"
    exit 1
fi

# Stop and remove the services using docker compose
echo "Stopping and removing services with Docker..."
docker compose -f docker-compose.yml down

if [[ $? -eq 0 ]]; then
    echo "Services stopped and removed successfully."
else
    echo "Failed to stop services."
    exit 1
fi
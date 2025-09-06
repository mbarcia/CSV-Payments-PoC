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

# Start services using Docker with the local override file

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
NETWORK_NAME="csv-payments"
if ! docker network ls | grep -q "\b${NETWORK_NAME}\b"; then
    echo "Creating custom network: ${NETWORK_NAME}"
    docker network create "${NETWORK_NAME}"
    if [[ $? -ne 0 ]]; then
        echo "Failed to create network ${NETWORK_NAME}"
        exit 1
    fi
else
    echo "Network ${NETWORK_NAME} already exists."
fi

# Check if the local override file exists, if not create it from the template
if [[ ! -f "docker-compose.local.yml" ]]; then
    echo "Creating docker-compose.local.yml from template..."
    cp "docker-compose.local.template.yml" "docker-compose.local.yml"
    echo "Created docker-compose.local.yml. You can modify this file for your local configuration."
fi

# Function to generate certificates for Docker services
generate_docker_certificates() {
    echo "Generating Docker certificates..."
    
    # Create a temporary directory for certificate generation
    CERT_DIR="/tmp/csv-payments-certs"
    rm -rf "${CERT_DIR}"
    mkdir -p "${CERT_DIR}"
    
    # Create certificate configuration
    cat > "${CERT_DIR}/cert.conf" <<EOF
[req]
default_bits = 2048
prompt = no
default_md = sha256
distinguished_name = dn
req_extensions = v3_req

[dn]
C = US
ST = CA
L = San Francisco
O = CSV Payments PoC
CN = localhost

[v3_req]
basicConstraints = CA:FALSE
keyUsage = nonRepudiation, digitalSignature, keyEncipherment
subjectAltName = @alt_names

[alt_names]
DNS.1 = localhost
DNS.2 = input-csv-file-processing-svc
DNS.3 = payments-processing-svc
DNS.4 = payment-status-svc
DNS.5 = data-persistence-svc
DNS.6 = output-csv-file-processing-svc
DNS.7 = orchestrator-svc
IP.1 = 127.0.0.1
IP.2 = ::1
EOF
    
    # Generate the certificate and key
    openssl req -x509 -newkey rsa:2048 -keyout "${CERT_DIR}/quarkus-key.pem" -out "${CERT_DIR}/quarkus-cert.pem" -days 365 -nodes -config "${CERT_DIR}/cert.conf" -extensions v3_req
    
    # Convert to PKCS12 format
    openssl pkcs12 -export -in "${CERT_DIR}/quarkus-cert.pem" -inkey "${CERT_DIR}/quarkus-key.pem" -out "${CERT_DIR}/server-keystore.p12" -name server -passout pass:secret
    
    # Create truststore for the orchestrator
    keytool -import -file "${CERT_DIR}/quarkus-cert.pem" -keystore "${CERT_DIR}/client-truststore.jks" -storepass secret -noprompt -alias server
    
    # Copy certificates to service directories
    for svc in input-csv-file-processing-svc payments-processing-svc payment-status-svc data-persistence-svc output-csv-file-processing-svc; do
        cp "${CERT_DIR}/server-keystore.p12" "${svc}/src/main/resources/server-keystore.jks"
    done
    
    cp "${CERT_DIR}/client-truststore.jks" "orchestrator-svc/src/main/resources/client-truststore.jks"
    
    # Clean up temporary files
    rm -rf "${CERT_DIR}"
    
    echo "Docker certificates generated successfully."
}

# Check if certificates need to be generated
echo "Checking if Docker certificates need to be generated..."
CERTS_EXIST=true
for svc in input-csv-file-processing-svc payments-processing-svc payment-status-svc data-persistence-svc output-csv-file-processing-svc; do
    if [[ ! -f "${svc}/src/main/resources/server-keystore.jks" ]]; then
        CERTS_EXIST=false
        break
    fi
done

if [[ ! -f "orchestrator-svc/src/main/resources/client-truststore.jks" ]]; then
    CERTS_EXIST=false
fi

if [[ "${CERTS_EXIST}" == false ]]; then
    generate_docker_certificates
else
    echo "Docker certificates already exist."
fi

# Start the services using docker compose with both the main and override files
echo "Starting services with Docker..."
docker compose -f docker-compose.yml -f docker-compose.local.yml up  -d

if [[ $? -eq 0 ]]; then
    echo "Services started successfully in Docker."
    echo "Use 'docker compose -f docker-compose.yml -f docker-compose.local.yml logs -f' to view logs."
else
    echo "Failed to start services."
    exit 1
fi
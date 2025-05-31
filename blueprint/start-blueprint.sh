#!/bin/bash

# Kestra Blueprint Module Startup Script
# This script starts the Blueprint management module

echo "Starting Kestra Blueprint Module..."

# Set environment variables
export MICRONAUT_ENVIRONMENTS=dev
export DATASOURCES_DEFAULT_URL=jdbc:h2:mem:blueprint;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
export DATASOURCES_DEFAULT_DRIVER_CLASS_NAME=org.h2.Driver
export DATASOURCES_DEFAULT_USERNAME=sa
export DATASOURCES_DEFAULT_PASSWORD=

# Build the project if needed
echo "Building Blueprint module..."
../gradlew blueprint:build -x test

if [ $? -eq 0 ]; then
    echo "Build successful!"
    echo "Blueprint module is ready to use."
    echo ""
    echo "Available endpoints:"
    echo "  GET    /api/v1/blueprints          - List all blueprints"
    echo "  POST   /api/v1/blueprints          - Create a new blueprint"
    echo "  GET    /api/v1/blueprints/{id}     - Get blueprint by ID"
    echo "  PUT    /api/v1/blueprints/{id}     - Update blueprint"
    echo "  DELETE /api/v1/blueprints/{id}     - Delete blueprint"
    echo "  GET    /api/v1/blueprints/{id}/versions - Get blueprint versions"
    echo "  POST   /api/v1/blueprints/{id}/render    - Render blueprint template"
    echo ""
    echo "Health check: GET /health"
    echo "Metrics: GET /metrics"
    echo ""
    echo "To start the web server, run:"
    echo "  java -jar build/libs/blueprint-*.jar"
else
    echo "Build failed. Please check the error messages above."
    exit 1
fi
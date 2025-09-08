#!/bin/bash

# Production startup script for Secure QR Voting System

echo "🚀 Starting Secure QR Voting System in Production Mode"
echo "====================================================="

# Check required environment variables
required_vars=("DB_PASSWORD" "ADMIN_PASSWORD" "QR_SECRET_KEY" "ENCRYPTION_KEY")
missing_vars=()

for var in "${required_vars[@]}"; do
    if [ -z "${!var}" ]; then
        missing_vars+=("$var")
    fi
done

if [ ${#missing_vars[@]} -ne 0 ]; then
    echo "❌ Missing required environment variables:"
    printf '   %s\n' "${missing_vars[@]}"
    echo ""
    echo "Please set the following environment variables:"
    echo "  export DB_PASSWORD=your_secure_database_password"
    echo "  export ADMIN_PASSWORD=your_admin_password"
    echo "  export QR_SECRET_KEY=your_qr_secret_key"
    echo "  export ENCRYPTION_KEY=your_encryption_key"
    exit 1
fi

# Create necessary directories
echo "📁 Creating directories..."
mkdir -p data logs

# Check if JAR exists
JAR_FILE="target/secure-qr-voting-system-0.0.1-SNAPSHOT.jar"
if [ ! -f "$JAR_FILE" ]; then
    echo "📦 Building application..."
    mvn clean package -Pprod -DskipTests
    
    if [ $? -ne 0 ]; then
        echo "❌ Failed to build application."
        exit 1
    fi
fi

# Start application
echo "🎯 Starting application in production mode..."
echo "Access the application at: http://localhost:8080"
echo "Health check at: http://localhost:8080/actuator/health"
echo ""
echo "Logs will be written to: logs/"
echo "Database will be stored in: data/"
echo ""
echo "Press Ctrl+C to stop the application"
echo "====================================================="

java -jar "$JAR_FILE" --spring.profiles.active=prod
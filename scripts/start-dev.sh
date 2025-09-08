#!/bin/bash

# Development startup script for Secure QR Voting System

echo "🚀 Starting Secure QR Voting System in Development Mode"
echo "=================================================="

# Check Java version
echo "Checking Java version..."
java -version
if [ $? -ne 0 ]; then
    echo "❌ Java not found. Please install Java 17 or higher."
    exit 1
fi

# Check Maven
echo "Checking Maven..."
mvn -version
if [ $? -ne 0 ]; then
    echo "❌ Maven not found. Please install Maven 3.6 or higher."
    exit 1
fi

# Clean and install dependencies
echo "📦 Installing dependencies..."
mvn clean install -DskipTests

if [ $? -ne 0 ]; then
    echo "❌ Failed to install dependencies."
    exit 1
fi

# Run tests
echo "🧪 Running tests..."
mvn test

if [ $? -ne 0 ]; then
    echo "⚠️  Some tests failed. Continue anyway? (y/n)"
    read -r response
    if [[ ! "$response" =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

# Start application
echo "🎯 Starting application in development mode..."
echo "Access the application at: http://localhost:8080"
echo "H2 Console available at: http://localhost:8080/h2-console"
echo "Health check at: http://localhost:8080/actuator/health"
echo ""
echo "Default credentials:"
echo "  Admin: admin / password"
echo "  Test users: alice, bob, charlie, diana / password"
echo ""
echo "Press Ctrl+C to stop the application"
echo "=================================================="

mvn spring-boot:run -Pdev
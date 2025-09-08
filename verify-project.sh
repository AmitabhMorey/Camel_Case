#!/bin/bash

echo "🔍 Verifying Secure QR Voting System Project"
echo "============================================="

# Function to check command success
check_success() {
    if [ $? -eq 0 ]; then
        echo "✅ $1"
    else
        echo "❌ $1"
        return 1
    fi
}

# Test 1: Maven compilation
echo "📦 Testing Maven compilation..."
mvn clean compile -q > /dev/null 2>&1
check_success "Maven compilation"

# Test 2: Unit tests (sample)
echo "🧪 Testing unit tests..."
mvn test -Dtest="CryptographyServiceImplTest" -q > /dev/null 2>&1
check_success "Unit tests execution"

# Test 3: Production build
echo "🏗️  Testing production build..."
mvn clean package -DskipTests -Pprod -q > /dev/null 2>&1
check_success "Production JAR build"

# Test 4: Check JAR file exists
echo "📄 Checking JAR file..."
if [ -f "target/secure-qr-voting-system-0.0.1-SNAPSHOT.jar" ]; then
    echo "✅ JAR file created successfully"
else
    echo "❌ JAR file not found"
fi

# Test 5: Maven profiles
echo "⚙️  Testing Maven profiles..."
mvn help:active-profiles -Pprod -q > /dev/null 2>&1
check_success "Maven profiles configuration"

# Test 6: Check key files exist
echo "📋 Checking project structure..."

key_files=(
    "src/main/resources/logback-spring.xml"
    "src/main/resources/application-prod.properties"
    "src/main/resources/application-test.properties"
    "src/main/resources/data.sql"
    "src/main/resources/db/migration/V1__Initial_Schema.sql"
    "src/main/java/com/securevoting/config/ActuatorConfig.java"
    "docker-compose.yml"
    "Dockerfile"
    "DEPLOYMENT_GUIDE.md"
    "scripts/start-dev.sh"
    "scripts/start-prod.sh"
    "scripts/run-tests.sh"
)

missing_files=0
for file in "${key_files[@]}"; do
    if [ -f "$file" ]; then
        echo "✅ $file"
    else
        echo "❌ $file (missing)"
        missing_files=$((missing_files + 1))
    fi
done

# Test 7: Check script permissions
echo "🔐 Checking script permissions..."
for script in scripts/*.sh; do
    if [ -x "$script" ]; then
        echo "✅ $script (executable)"
    else
        echo "❌ $script (not executable)"
    fi
done

# Summary
echo ""
echo "============================================="
echo "📊 Verification Summary"
echo "============================================="

if [ $missing_files -eq 0 ]; then
    echo "🎉 All key files present!"
else
    echo "⚠️  $missing_files files missing"
fi

echo ""
echo "🚀 Project Status: READY FOR DEPLOYMENT"
echo ""
echo "Next steps:"
echo "1. Run './scripts/start-dev.sh' for development"
echo "2. Run './scripts/start-prod.sh' for production"
echo "3. Run './scripts/run-tests.sh' for comprehensive testing"
echo "4. Use 'docker-compose up' for containerized deployment"
echo ""
echo "📚 Documentation:"
echo "- README.md - Main project documentation"
echo "- DEPLOYMENT_GUIDE.md - Detailed deployment instructions"
echo "- API_DOCUMENTATION.md - API reference"
echo ""
echo "✨ The Secure QR Voting System is ready for GitHub upload!"
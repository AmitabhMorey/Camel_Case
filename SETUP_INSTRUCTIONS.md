# SecureQrVotingSystem Setup Instructions

## Table of Contents
1. [Prerequisites](#prerequisites)
2. [Installation](#installation)
3. [Configuration](#configuration)
4. [Running the Application](#running-the-application)
5. [Testing](#testing)
6. [Development Setup](#development-setup)
7. [Production Deployment](#production-deployment)
8. [Troubleshooting](#troubleshooting)

## Prerequisites

### System Requirements
- **Java**: JDK 17 or higher
- **Maven**: 3.8.0 or higher
- **Memory**: Minimum 2GB RAM
- **Storage**: 1GB free disk space
- **Operating System**: Windows 10+, macOS 10.14+, or Linux (Ubuntu 18.04+)

### Required Software

#### Java Development Kit (JDK) 17+
```bash
# Check if Java is installed
java -version

# If not installed, download from:
# https://adoptium.net/temurin/releases/
```

#### Apache Maven 3.8+
```bash
# Check if Maven is installed
mvn -version

# Install on macOS using Homebrew
brew install maven

# Install on Ubuntu/Debian
sudo apt update
sudo apt install maven

# Install on Windows using Chocolatey
choco install maven
```

#### Git (for cloning the repository)
```bash
# Check if Git is installed
git --version

# Install instructions: https://git-scm.com/downloads
```

### Optional Tools
- **IDE**: IntelliJ IDEA, Eclipse, or VS Code with Java extensions
- **Database Client**: H2 Console (included), DBeaver, or similar
- **API Testing**: Postman, Insomnia, or cURL

## Installation

### 1. Clone the Repository
```bash
git clone https://github.com/your-org/secure-qr-voting-system.git
cd secure-qr-voting-system
```

### 2. Verify Project Structure
```bash
# Ensure you have the following structure:
ls -la
# Should show: pom.xml, src/, README.md, etc.
```

### 3. Install Dependencies
```bash
# Download and install all Maven dependencies
mvn clean install
```

### 4. Verify Installation
```bash
# Run tests to ensure everything is working
mvn test
```

## Configuration

### Application Properties

The application uses `src/main/resources/application.properties` for configuration:

```properties
# Server Configuration
server.port=8080
server.servlet.context-path=/

# Database Configuration (H2 for development)
spring.datasource.url=jdbc:h2:mem:votingdb
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=password

# H2 Console (for development only)
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console

# JPA Configuration
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=true

# Security Configuration
app.security.jwt.secret=mySecretKey
app.security.jwt.expiration=86400000
app.security.session.timeout=1800

# OTP Configuration
app.otp.length=6
app.otp.expiry.minutes=5
app.otp.algorithm=HmacSHA256

# QR Code Configuration
app.qr.size=300
app.qr.format=PNG
app.qr.expiry.minutes=10

# Encryption Configuration
app.encryption.algorithm=AES/GCM/NoPadding
app.encryption.key.length=256

# Logging Configuration
logging.level.com.securevoting=INFO
logging.level.org.springframework.security=DEBUG
logging.pattern.console=%d{yyyy-MM-dd HH:mm:ss} - %msg%n
logging.file.name=logs/voting-system.log
```

### Environment-Specific Configuration

#### Development Profile (`application-dev.properties`)
```properties
# Development-specific settings
spring.jpa.show-sql=true
logging.level.com.securevoting=DEBUG
app.security.session.timeout=3600
```

#### Production Profile (`application-prod.properties`)
```properties
# Production-specific settings
spring.datasource.url=jdbc:postgresql://localhost:5432/votingdb
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
spring.jpa.hibernate.ddl-auto=validate
logging.level.com.securevoting=WARN
app.security.session.timeout=900
```

### Environment Variables

For production deployment, set these environment variables:

```bash
export DB_USERNAME=your_db_username
export DB_PASSWORD=your_db_password
export JWT_SECRET=your_jwt_secret_key
export ENCRYPTION_KEY=your_encryption_key
export SERVER_PORT=8080
```

## Running the Application

### Development Mode

#### Using Maven
```bash
# Run with default (dev) profile
mvn spring-boot:run

# Run with specific profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

#### Using Java
```bash
# Build the JAR file
mvn clean package

# Run the JAR
java -jar target/secure-qr-voting-system-0.0.1-SNAPSHOT.jar
```

#### Using IDE
1. Import the project into your IDE
2. Run the `SecureQrVotingSystemApplication` main class
3. The application will start on `http://localhost:8080`

### Production Mode

```bash
# Build for production
mvn clean package -Pprod

# Run with production profile
java -jar target/secure-qr-voting-system-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

### Verify Application Startup

1. **Check Console Output**: Look for "Started SecureQrVotingSystemApplication"
2. **Health Check**: Visit `http://localhost:8080/actuator/health`
3. **H2 Console**: Visit `http://localhost:8080/h2-console` (dev only)
4. **Application**: Visit `http://localhost:8080`

## Testing

### Running Tests

#### All Tests
```bash
mvn test
```

#### Specific Test Categories
```bash
# Unit tests only
mvn test -Dtest="*Test"

# Integration tests only
mvn test -Dtest="*IntegrationTest"

# Security tests only
mvn test -Dtest="*SecurityTest"

# Performance tests only
mvn test -Dtest="*PerformanceTest"
```

#### Test Coverage Report
```bash
# Generate JaCoCo coverage report
mvn clean test jacoco:report

# View report at: target/site/jacoco/index.html
```

### Manual Testing

#### 1. User Registration and Authentication
```bash
# Register a new user
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "password123"
  }'

# Use the returned QR data for authentication
curl -X POST http://localhost:8080/auth/login/qr \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "returned-user-id",
    "qrData": "returned-qr-data"
  }'
```

#### 2. Database Inspection (Development)
1. Visit `http://localhost:8080/h2-console`
2. Use JDBC URL: `jdbc:h2:mem:votingdb`
3. Username: `sa`, Password: `password`
4. Explore tables: `users`, `elections`, `votes`, `audit_logs`

## Development Setup

### IDE Configuration

#### IntelliJ IDEA
1. Import as Maven project
2. Set Project SDK to Java 17+
3. Enable annotation processing
4. Install Spring Boot plugin
5. Configure code style (Google Java Style recommended)

#### VS Code
1. Install Java Extension Pack
2. Install Spring Boot Extension Pack
3. Configure Java runtime path
4. Set up debugging configuration

### Code Quality Tools

#### Checkstyle
```bash
# Add to pom.xml and run
mvn checkstyle:check
```

#### SpotBugs
```bash
# Add to pom.xml and run
mvn spotbugs:check
```

#### SonarQube (Optional)
```bash
# Run SonarQube analysis
mvn sonar:sonar
```

### Database Setup for Development

#### Using H2 (Default)
- No additional setup required
- Data is in-memory and resets on restart
- Access console at `/h2-console`

#### Using PostgreSQL (Optional)
```bash
# Install PostgreSQL
# Create database
createdb votingdb

# Update application-dev.properties
spring.datasource.url=jdbc:postgresql://localhost:5432/votingdb
spring.datasource.username=your_username
spring.datasource.password=your_password
```

## Production Deployment

### Docker Deployment

#### 1. Create Dockerfile
```dockerfile
FROM openjdk:17-jre-slim

WORKDIR /app
COPY target/secure-qr-voting-system-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

#### 2. Build and Run
```bash
# Build Docker image
docker build -t secure-voting-system .

# Run container
docker run -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DB_USERNAME=your_db_user \
  -e DB_PASSWORD=your_db_password \
  secure-voting-system
```

### Traditional Server Deployment

#### 1. Prepare Server
```bash
# Install Java 17+
sudo apt update
sudo apt install openjdk-17-jre

# Create application user
sudo useradd -r -s /bin/false voting-app
sudo mkdir /opt/voting-system
sudo chown voting-app:voting-app /opt/voting-system
```

#### 2. Deploy Application
```bash
# Copy JAR file to server
scp target/secure-qr-voting-system-0.0.1-SNAPSHOT.jar user@server:/opt/voting-system/

# Create systemd service
sudo nano /etc/systemd/system/voting-system.service
```

#### 3. Systemd Service Configuration
```ini
[Unit]
Description=Secure QR Voting System
After=network.target

[Service]
Type=simple
User=voting-app
WorkingDirectory=/opt/voting-system
ExecStart=/usr/bin/java -jar secure-qr-voting-system-0.0.1-SNAPSHOT.jar
Restart=always
RestartSec=10

Environment=SPRING_PROFILES_ACTIVE=prod
Environment=DB_USERNAME=your_db_user
Environment=DB_PASSWORD=your_db_password

[Install]
WantedBy=multi-user.target
```

#### 4. Start Service
```bash
sudo systemctl daemon-reload
sudo systemctl enable voting-system
sudo systemctl start voting-system
sudo systemctl status voting-system
```

### Load Balancer Configuration (Nginx)

```nginx
upstream voting_backend {
    server 127.0.0.1:8080;
    # Add more servers for load balancing
}

server {
    listen 80;
    server_name your-domain.com;

    location / {
        proxy_pass http://voting_backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

## Troubleshooting

### Common Issues

#### 1. Port Already in Use
```bash
# Find process using port 8080
lsof -i :8080

# Kill the process
kill -9 <PID>

# Or change port in application.properties
server.port=8081
```

#### 2. Java Version Issues
```bash
# Check Java version
java -version

# Set JAVA_HOME if needed
export JAVA_HOME=/path/to/java17
```

#### 3. Maven Build Failures
```bash
# Clear Maven cache
mvn dependency:purge-local-repository

# Rebuild
mvn clean install -U
```

#### 4. Database Connection Issues
```bash
# Check H2 console access
# Verify JDBC URL: jdbc:h2:mem:votingdb
# Username: sa, Password: password

# For PostgreSQL, verify:
# - Database exists
# - User has permissions
# - Connection parameters are correct
```

#### 5. Memory Issues
```bash
# Increase JVM memory
java -Xmx2g -jar target/secure-qr-voting-system-0.0.1-SNAPSHOT.jar
```

### Logging and Debugging

#### Enable Debug Logging
```properties
# In application.properties
logging.level.com.securevoting=DEBUG
logging.level.org.springframework.security=DEBUG
logging.level.org.springframework.web=DEBUG
```

#### View Application Logs
```bash
# If using file logging
tail -f logs/voting-system.log

# If using systemd
sudo journalctl -u voting-system -f
```

### Performance Tuning

#### JVM Options
```bash
java -Xms1g -Xmx2g -XX:+UseG1GC \
     -XX:+UseStringDeduplication \
     -jar target/secure-qr-voting-system-0.0.1-SNAPSHOT.jar
```

#### Database Optimization
```properties
# Connection pool settings
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=20000
```

## Support and Documentation

### Additional Resources
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Spring Security Reference](https://spring.io/projects/spring-security)
- [H2 Database Documentation](http://www.h2database.com/html/main.html)
- [Maven Documentation](https://maven.apache.org/guides/)

### Getting Help
1. Check the troubleshooting section above
2. Review application logs for error messages
3. Consult the API documentation for endpoint details
4. Check GitHub issues for known problems
5. Contact the development team for support

### Contributing
1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Ensure all tests pass
6. Submit a pull request
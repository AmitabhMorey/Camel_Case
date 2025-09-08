# Deployment Guide - Secure QR Voting System

This guide provides comprehensive instructions for deploying the Secure QR Voting System in various environments.

## 🚀 Quick Deployment Options

### Option 1: Development Mode (Recommended for Testing)
```bash
# Clone and start
git clone <repository-url>
cd secure-qr-voting-system
./scripts/start-dev.sh
```

### Option 2: Production JAR
```bash
# Set environment variables
export DB_PASSWORD=your_secure_password
export ADMIN_PASSWORD=your_admin_password
export QR_SECRET_KEY=your_qr_secret_key
export ENCRYPTION_KEY=your_encryption_key

# Build and run
./scripts/start-prod.sh
```

### Option 3: Docker Compose (Recommended for Production)
```bash
# Configure environment variables in .env file
docker-compose up -d
```

## 📋 Prerequisites

### System Requirements
- **Java**: OpenJDK 17 or higher
- **Memory**: Minimum 2GB RAM, 4GB recommended
- **Storage**: 1GB free disk space
- **Network**: Port 8080 available (configurable)

### Development Tools (for building from source)
- **Maven**: 3.6 or higher
- **Git**: For version control
- **Docker**: For containerized deployment (optional)

## 🔧 Environment Configuration

### Environment Variables

#### Required for Production
```bash
# Database security
export DB_PASSWORD=your_secure_database_password

# Admin account security
export ADMIN_PASSWORD=your_strong_admin_password

# Cryptographic keys (generate secure random keys)
export QR_SECRET_KEY=your_32_character_qr_secret_key
export ENCRYPTION_KEY=your_32_character_encryption_key
```

#### Optional Configuration
```bash
# Server configuration
export SERVER_PORT=8080
export SERVER_ADDRESS=0.0.0.0

# Database configuration (if using external DB)
export DB_URL=jdbc:postgresql://localhost:5432/votingdb
export DB_USERNAME=voting_user

# Security settings
export SESSION_TIMEOUT=15m
export RATE_LIMIT_AUTH=5
export RATE_LIMIT_VOTING=3
```

### Configuration Files

#### Production Configuration (`application-prod.properties`)
```properties
# Already configured in src/main/resources/application-prod.properties
# Key settings:
# - File-based H2 database
# - Secure session configuration
# - Production logging
# - Health check endpoints
```

#### Development Configuration (`application.properties`)
```properties
# Already configured in src/main/resources/application.properties
# Key settings:
# - In-memory H2 database
# - Debug logging
# - H2 console enabled
# - Relaxed security for development
```

## 🐳 Docker Deployment

### Single Container Deployment
```bash
# Build image
docker build -t secure-voting-system .

# Run container
docker run -d \
  --name secure-voting \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DB_PASSWORD=securepassword \
  -e ADMIN_PASSWORD=admin123 \
  -e QR_SECRET_KEY=SecureVotingQRSecret2024 \
  -e ENCRYPTION_KEY=SecureVotingEncryptionKey2024 \
  -v $(pwd)/data:/app/data \
  -v $(pwd)/logs:/app/logs \
  secure-voting-system
```

### Docker Compose Deployment
```bash
# Create .env file
cat > .env << EOF
DB_PASSWORD=securepassword
ADMIN_PASSWORD=admin123
QR_SECRET_KEY=SecureVotingQRSecret2024
ENCRYPTION_KEY=SecureVotingEncryptionKey2024
EOF

# Start services
docker-compose up -d

# Check status
docker-compose ps

# View logs
docker-compose logs -f
```

## ☁️ Cloud Deployment

### AWS Deployment (EC2)
```bash
# Launch EC2 instance (t3.medium recommended)
# Install Java 17
sudo yum update -y
sudo yum install -y java-17-openjdk

# Clone and deploy
git clone <repository-url>
cd secure-qr-voting-system
./scripts/start-prod.sh
```

### Heroku Deployment
```bash
# Create Procfile
echo "web: java -jar target/secure-qr-voting-system-0.0.1-SNAPSHOT.jar --server.port=\$PORT" > Procfile

# Deploy
heroku create your-app-name
heroku config:set SPRING_PROFILES_ACTIVE=prod
heroku config:set DB_PASSWORD=your_password
heroku config:set ADMIN_PASSWORD=your_admin_password
heroku config:set QR_SECRET_KEY=your_qr_key
heroku config:set ENCRYPTION_KEY=your_encryption_key
git push heroku main
```

### Google Cloud Platform (Cloud Run)
```bash
# Build and push to Container Registry
gcloud builds submit --tag gcr.io/PROJECT_ID/secure-voting-system

# Deploy to Cloud Run
gcloud run deploy secure-voting-system \
  --image gcr.io/PROJECT_ID/secure-voting-system \
  --platform managed \
  --region us-central1 \
  --allow-unauthenticated \
  --set-env-vars SPRING_PROFILES_ACTIVE=prod,DB_PASSWORD=your_password
```

## 🔒 Security Configuration

### SSL/TLS Configuration
```properties
# Add to application-prod.properties for HTTPS
server.ssl.enabled=true
server.ssl.key-store=classpath:keystore.p12
server.ssl.key-store-password=your_keystore_password
server.ssl.key-store-type=PKCS12
server.ssl.key-alias=secure-voting
```

### Firewall Configuration
```bash
# Allow only necessary ports
sudo ufw allow 22/tcp    # SSH
sudo ufw allow 80/tcp    # HTTP (redirect to HTTPS)
sudo ufw allow 443/tcp   # HTTPS
sudo ufw enable
```

### Reverse Proxy (Nginx)
```nginx
server {
    listen 80;
    server_name your-domain.com;
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl;
    server_name your-domain.com;
    
    ssl_certificate /path/to/certificate.crt;
    ssl_certificate_key /path/to/private.key;
    
    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

## 📊 Monitoring and Health Checks

### Health Check Endpoints
```bash
# Basic health check
curl http://localhost:8080/actuator/health

# Detailed health information (requires authentication)
curl -u admin:password http://localhost:8080/actuator/health

# Application metrics
curl http://localhost:8080/actuator/metrics

# Application information
curl http://localhost:8080/actuator/info
```

### Log Monitoring
```bash
# Production logs location
tail -f logs/secure-voting.log

# Security audit logs
tail -f logs/security-audit.log

# Development logs
tail -f logs/secure-voting-dev.log
```

### System Monitoring
```bash
# Check application process
ps aux | grep java

# Check memory usage
free -h

# Check disk usage
df -h

# Check network connections
netstat -tlnp | grep 8080
```

## 🔧 Troubleshooting

### Common Issues

#### Port Already in Use
```bash
# Find process using port 8080
lsof -i :8080

# Kill process
kill -9 <PID>

# Or change port
export SERVER_PORT=8081
```

#### Memory Issues
```bash
# Increase JVM heap size
export JAVA_OPTS="-Xmx2g -Xms1g"

# Run with custom memory settings
java -Xmx2g -Xms1g -jar target/secure-qr-voting-system-0.0.1-SNAPSHOT.jar
```

#### Database Connection Issues
```bash
# Check H2 database file permissions
ls -la data/

# Reset database (development only)
rm -rf data/votingdb*
```

#### SSL Certificate Issues
```bash
# Generate self-signed certificate for testing
keytool -genkeypair -alias secure-voting -keyalg RSA -keysize 2048 \
  -storetype PKCS12 -keystore keystore.p12 -validity 3650
```

### Log Analysis
```bash
# Check for errors
grep -i error logs/secure-voting.log

# Check authentication failures
grep -i "authentication failed" logs/security-audit.log

# Check performance issues
grep -i "slow" logs/secure-voting.log
```

## 📈 Performance Tuning

### JVM Tuning
```bash
# Production JVM settings
export JAVA_OPTS="-Xmx4g -Xms2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
```

### Database Optimization
```properties
# Add to application-prod.properties
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000
spring.jpa.properties.hibernate.jdbc.batch_size=20
```

### Caching Configuration
```properties
# Enable caching for better performance
spring.cache.type=caffeine
spring.cache.caffeine.spec=maximumSize=1000,expireAfterWrite=10m
```

## 🔄 Backup and Recovery

### Database Backup
```bash
# Backup H2 database
cp data/votingdb.mv.db backup/votingdb-$(date +%Y%m%d).mv.db

# Automated backup script
#!/bin/bash
BACKUP_DIR="/backup/voting-system"
DATE=$(date +%Y%m%d_%H%M%S)
mkdir -p $BACKUP_DIR
cp data/votingdb.mv.db $BACKUP_DIR/votingdb-$DATE.mv.db
find $BACKUP_DIR -name "votingdb-*.mv.db" -mtime +7 -delete
```

### Application Backup
```bash
# Backup entire application
tar -czf backup/secure-voting-system-$(date +%Y%m%d).tar.gz \
  --exclude=target \
  --exclude=logs \
  --exclude=.git \
  .
```

## 🚀 Scaling Considerations

### Horizontal Scaling
- Use external database (PostgreSQL/MySQL)
- Implement Redis for session storage
- Use load balancer (Nginx/HAProxy)
- Consider microservices architecture for high load

### Vertical Scaling
- Increase JVM heap size
- Optimize database connections
- Enable caching
- Use connection pooling

## 📞 Support and Maintenance

### Regular Maintenance Tasks
1. **Daily**: Check application logs and health status
2. **Weekly**: Review security audit logs and performance metrics
3. **Monthly**: Update dependencies and security patches
4. **Quarterly**: Performance testing and capacity planning

### Emergency Procedures
1. **Application Down**: Check logs, restart service, verify health endpoints
2. **Security Incident**: Review audit logs, check for suspicious activity
3. **Performance Issues**: Check memory usage, database connections, logs
4. **Data Corruption**: Restore from backup, verify data integrity

For additional support, refer to the main [README.md](README.md) and [API Documentation](API_DOCUMENTATION.md).
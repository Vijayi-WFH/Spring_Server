# Deployment Guide

## Table of Contents
1. [Deployment Architecture](#deployment-architecture)
2. [Build Artifacts](#build-artifacts)
3. [Deployment Environments](#deployment-environments)
4. [Server Configuration](#server-configuration)
5. [Secrets Management](#secrets-management)
6. [CI/CD Pipeline Setup](#cicd-pipeline-setup)
7. [Branching Strategy](#branching-strategy)
8. [Deployment Steps](#deployment-steps)
9. [Health Checks & Monitoring](#health-checks--monitoring)
10. [Rollback Procedures](#rollback-procedures)

---

## Deployment Architecture

### Module Deployment Overview

```
┌──────────────────────────────────────────────────────────────────────┐
│                        PRODUCTION ENVIRONMENT                        │
├──────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐  │
│  │   TSe_Server    │    │     TSEHR       │    │    Chat-App     │  │
│  │   (Port 8080)   │    │   (Port 8081)   │    │   (Port 8082)   │  │
│  │                 │    │                 │    │                 │  │
│  │  /api context   │    │  No context     │    │  No context     │  │
│  └────────┬────────┘    └────────┬────────┘    └────────┬────────┘  │
│           │                      │                      │            │
│           └──────────────────────┼──────────────────────┘            │
│                                  │                                   │
│  ┌─────────────────────────────────────────────────────────────────┐│
│  │                    Shared PostgreSQL Database                   ││
│  │                         (db_tse + chatapp)                      ││
│  └─────────────────────────────────────────────────────────────────┘│
│                                                                      │
│  ┌─────────────────────────────────────────────────────────────────┐│
│  │                           Redis Cache                           ││
│  └─────────────────────────────────────────────────────────────────┘│
│                                                                      │
│  ┌─────────────────┐                                                 │
│  │ Notification_   │  (Scheduled Tasks - Cron Jobs)                 │
│  │ Reminders       │  Calls TSe_Server via REST                     │
│  └─────────────────┘                                                 │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

### Port Configuration

| Module | Port | Context Path | Purpose |
|--------|------|--------------|---------|
| TSe_Server | 8080 | `/api` | Core API |
| TSEHR | 8081 | - | Timesheet/HR |
| Notification_Reminders | 8081* | - | Scheduler |
| Chat-App | 8082 | - | Messaging |

*Note: Notification_Reminders and TSEHR share port 8081 - deploy only one, or change ports.

---

## Build Artifacts

### JAR Files

| Module | Artifact Name | Location |
|--------|--------------|----------|
| TSe_Server | `tse.jar` | `TSe_Server/target/tse.jar` |
| TSEHR | `tsehr-0.0.1-SNAPSHOT.jar` | `TSEHR/target/*.jar` |
| Notification_Reminders | `tse_scheduler-0.0.1-SNAPSHOT.jar` | `Notification_Reminders/target/*.jar` |
| Chat-App | `chat-app-0.0.1-SNAPSHOT.jar` | `Vijayi_WFH_Conversation/chat-app/target/*.jar` |

### Build Script

```bash
#!/bin/bash
# build-all.sh

set -e

echo "Building all modules..."

# TSe_Server
echo "Building TSe_Server..."
cd TSe_Server
mvn clean package -DskipTests
cd ..

# TSEHR
echo "Building TSEHR..."
cd TSEHR
mvn clean package -DskipTests
cd ..

# Notification_Reminders
echo "Building Notification_Reminders..."
cd Notification_Reminders
mvn clean package -DskipTests
cd ..

# Chat-App
echo "Building Chat-App..."
cd Vijayi_WFH_Conversation/chat-app
mvn clean package -DskipTests
cd ../..

echo "Build complete!"
```

---

## Deployment Environments

### Environment Configuration

Based on `app.environment` property:
- **1 = QA** - Quality Assurance testing
- **2 = Pre-Prod** - Staging/UAT environment
- **3 = Prod** - Production environment

### Environment-Specific Properties

Create separate property files for each environment:

```
src/main/resources/
├── application.properties           # Default/local
├── application-qa.properties        # QA environment
├── application-preprod.properties   # Pre-production
└── application-prod.properties      # Production
```

**Example `application-prod.properties`:**
```properties
# SERVER
server.port=8080
server.servlet.context-path=/api

# DATASOURCE - Use encrypted values
spring.datasource.url=ENC(encrypted_url)
spring.datasource.username=ENC(encrypted_username)
spring.datasource.password=ENC(encrypted_password)

# JPA - Disable ddl-auto in production
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false

# Debug disabled
app.debug=false

# Environment
app.environment=3

# Compression
server.compression.enabled=true
```

---

## Server Configuration

### Systemd Service (Linux)

Create service file `/etc/systemd/system/tse-server.service`:

```ini
[Unit]
Description=TSe Server Application
After=network.target postgresql.service redis.service

[Service]
Type=simple
User=tse
Group=tse
WorkingDirectory=/opt/tse
ExecStart=/usr/bin/java -Xms512m -Xmx2g -jar /opt/tse/tse.jar --spring.profiles.active=prod
ExecStop=/bin/kill -TERM $MAINPID
Restart=on-failure
RestartSec=10
StandardOutput=journal
StandardError=journal
Environment="JASYPT_ENCRYPTOR_PASSWORD=<secret>"

[Install]
WantedBy=multi-user.target
```

**Enable and start:**
```bash
sudo systemctl daemon-reload
sudo systemctl enable tse-server
sudo systemctl start tse-server
sudo systemctl status tse-server
```

### Similar for other modules:
- `/etc/systemd/system/tse-hr.service` (TSEHR)
- `/etc/systemd/system/tse-chat.service` (Chat-App)
- `/etc/systemd/system/tse-scheduler.service` (Notification_Reminders)

### JVM Tuning

```bash
# Production JVM options
java \
  -Xms1g \
  -Xmx4g \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=/var/log/tse/heapdump.hprof \
  -Djava.security.egd=file:/dev/./urandom \
  -jar tse.jar \
  --spring.profiles.active=prod
```

---

## Secrets Management

### Jasypt Encryption

The project uses Jasypt for encrypting sensitive properties.

#### Encrypt a Value

```bash
cd TSe_Server
mvn jasypt:encrypt-value \
  -Djasypt.encryptor.password=TSE_SECRET_KEY \
  -Djasypt.plugin.value="mySecretPassword"
```

#### Use Encrypted Values in Properties

```properties
# Wrap encrypted values with ENC()
spring.datasource.password=ENC(encryptedPasswordHere)
spring.mail.password=ENC(encryptedMailPasswordHere)
```

#### Runtime Decryption

Pass the encryption key at runtime:
```bash
# Via environment variable
export JASYPT_ENCRYPTOR_PASSWORD=TSE_SECRET_KEY
java -jar tse.jar

# Via command line
java -jar tse.jar --jasypt.encryptor.password=TSE_SECRET_KEY
```

### Sensitive Properties to Encrypt

| Property | Reason |
|----------|--------|
| `spring.datasource.password` | Database credentials |
| `spring.mail.password` | Email SMTP password |
| `encryption.key` | AES encryption key |
| `springbootwebfluxjjwt.jjwt.secret` | JWT signing key |
| `spring.security.oauth2.client.registration.google.clientSecret` | OAuth secret |

### Security Best Practices

1. **Never commit secrets to Git**
2. **Use environment variables** for production secrets
3. **Rotate encryption keys** periodically
4. **Use a secrets manager** (HashiCorp Vault, AWS Secrets Manager) for production
5. **Restrict access** to property files on servers

---

## CI/CD Pipeline Setup

### GitLab CI/CD Example

`.gitlab-ci.yml`:

```yaml
stages:
  - build
  - test
  - package
  - deploy

variables:
  MAVEN_OPTS: "-Dmaven.repo.local=$CI_PROJECT_DIR/.m2/repository"

cache:
  paths:
    - .m2/repository/

build:
  stage: build
  image: maven:3.8-openjdk-11
  script:
    - cd TSe_Server && mvn compile
    - cd ../TSEHR && mvn compile
    - cd ../Vijayi_WFH_Conversation/chat-app && mvn compile
  only:
    - main
    - develop

test:
  stage: test
  image: maven:3.8-openjdk-11
  script:
    - cd TSe_Server && mvn test
  artifacts:
    reports:
      junit: TSe_Server/target/surefire-reports/*.xml
    paths:
      - TSe_Server/target/site/jacoco/
  only:
    - main
    - develop

package:
  stage: package
  image: maven:3.8-openjdk-11
  script:
    - cd TSe_Server && mvn package -DskipTests
    - cd ../TSEHR && mvn package -DskipTests
    - cd ../Vijayi_WFH_Conversation/chat-app && mvn package -DskipTests
  artifacts:
    paths:
      - TSe_Server/target/tse.jar
      - TSEHR/target/*.jar
      - Vijayi_WFH_Conversation/chat-app/target/*.jar
  only:
    - main

deploy_qa:
  stage: deploy
  script:
    - scp TSe_Server/target/tse.jar user@qa-server:/opt/tse/
    - ssh user@qa-server 'sudo systemctl restart tse-server'
  environment:
    name: qa
  only:
    - develop

deploy_prod:
  stage: deploy
  script:
    - scp TSe_Server/target/tse.jar user@prod-server:/opt/tse/
    - ssh user@prod-server 'sudo systemctl restart tse-server'
  environment:
    name: production
  when: manual
  only:
    - main
```

### GitHub Actions Example

`.github/workflows/deploy.yml`:

```yaml
name: Build and Deploy

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
    - uses: actions/checkout@v3

    - name: Set up JDK 11
      uses: actions/setup-java@v3
      with:
        java-version: '11'
        distribution: 'temurin'
        cache: maven

    - name: Build TSe_Server
      run: |
        cd TSe_Server
        mvn clean package -DskipTests

    - name: Run Tests
      run: |
        cd TSe_Server
        mvn test

    - name: Upload artifact
      uses: actions/upload-artifact@v3
      with:
        name: tse-jar
        path: TSe_Server/target/tse.jar

  deploy-qa:
    needs: build
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/develop'

    steps:
    - name: Download artifact
      uses: actions/download-artifact@v3
      with:
        name: tse-jar

    - name: Deploy to QA
      env:
        SSH_KEY: ${{ secrets.QA_SSH_KEY }}
      run: |
        # Add deployment steps

  deploy-prod:
    needs: build
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    environment: production

    steps:
    - name: Download artifact
      uses: actions/download-artifact@v3
      with:
        name: tse-jar

    - name: Deploy to Production
      env:
        SSH_KEY: ${{ secrets.PROD_SSH_KEY }}
      run: |
        # Add deployment steps
```

---

## Branching Strategy

### Git Flow (Recommended)

```
main (production)
  │
  ├── develop (integration)
  │     │
  │     ├── feature/TSE-123-user-auth
  │     ├── feature/TSE-124-leave-module
  │     └── feature/TSE-125-attendance
  │
  ├── release/v1.2.0 (release candidate)
  │
  └── hotfix/TSE-999-critical-fix
```

### Branch Naming Convention

| Type | Pattern | Example |
|------|---------|---------|
| Feature | `feature/{ticket}-{description}` | `feature/TSE-123-user-auth` |
| Bugfix | `bugfix/{ticket}-{description}` | `bugfix/TSE-456-login-error` |
| Hotfix | `hotfix/{ticket}-{description}` | `hotfix/TSE-999-critical` |
| Release | `release/v{major}.{minor}.{patch}` | `release/v1.2.0` |

### Merge Strategy

1. **Feature → Develop:** Squash merge
2. **Develop → Release:** Merge commit
3. **Release → Main:** Merge commit + tag
4. **Hotfix → Main & Develop:** Cherry-pick

---

## Deployment Steps

### Pre-Deployment Checklist

- [ ] All tests passing on CI
- [ ] Code reviewed and approved
- [ ] Database migrations prepared (if any)
- [ ] Configuration changes documented
- [ ] Rollback plan ready
- [ ] Team notified of deployment window

### Step-by-Step Deployment

#### 1. Build Artifacts

```bash
# On CI server or locally
git checkout main
git pull origin main
./build-all.sh
```

#### 2. Backup Current State

```bash
# On production server
mkdir -p /opt/tse/backup/$(date +%Y%m%d)
cp /opt/tse/tse.jar /opt/tse/backup/$(date +%Y%m%d)/
pg_dump db_tse > /opt/tse/backup/$(date +%Y%m%d)/db_backup.sql
```

#### 3. Deploy New Version

```bash
# Copy new JAR
scp target/tse.jar user@prod-server:/opt/tse/tse.jar.new

# On server: Atomic swap
ssh user@prod-server << 'EOF'
  cd /opt/tse
  mv tse.jar tse.jar.old
  mv tse.jar.new tse.jar
EOF
```

#### 4. Restart Services

```bash
# Graceful restart
sudo systemctl restart tse-server
sudo systemctl restart tse-hr
sudo systemctl restart tse-chat
```

#### 5. Verify Deployment

```bash
# Check service status
sudo systemctl status tse-server

# Check application logs
sudo journalctl -u tse-server -f

# Health check
curl -f http://localhost:8080/api/actuator/health
```

### Zero-Downtime Deployment (Advanced)

For zero-downtime, use blue-green or rolling deployment with a load balancer.

---

## Health Checks & Monitoring

### Endpoints to Monitor

| Endpoint | Purpose |
|----------|---------|
| `/api/actuator/health` | Application health |
| `/api/swagger-ui.html` | API documentation |
| Database connection | PostgreSQL connectivity |
| Redis connection | Cache availability |

### Log Locations

```
/var/log/tse/
├── tse-server.log
├── tse-hr.log
├── tse-chat.log
└── tse-scheduler.log
```

### Monitoring Script

```bash
#!/bin/bash
# health-check.sh

SERVICES=("tse-server:8080" "tse-hr:8081" "tse-chat:8082")

for service in "${SERVICES[@]}"; do
  name=$(echo $service | cut -d: -f1)
  port=$(echo $service | cut -d: -f2)

  if curl -sf "http://localhost:$port/actuator/health" > /dev/null; then
    echo "✓ $name is healthy"
  else
    echo "✗ $name is DOWN!"
    # Send alert
  fi
done
```

---

## Rollback Procedures

### Immediate Rollback

```bash
# On production server
cd /opt/tse

# Restore previous JAR
mv tse.jar tse.jar.failed
mv tse.jar.old tse.jar

# Restart service
sudo systemctl restart tse-server

# Verify
curl http://localhost:8080/api/actuator/health
```

### Database Rollback

```bash
# If database changes need reverting
psql -U postgres -d db_tse < /opt/tse/backup/YYYYMMDD/db_backup.sql
```

### Rollback Checklist

1. [ ] Stop the application
2. [ ] Restore previous JAR file
3. [ ] Restore database if needed
4. [ ] Restart the application
5. [ ] Verify functionality
6. [ ] Notify team of rollback
7. [ ] Document the issue

---

## Docker Deployment (Optional)

### Dockerfile

```dockerfile
FROM openjdk:11-jre-slim

WORKDIR /app

COPY target/tse.jar app.jar

EXPOSE 8080

ENV JAVA_OPTS="-Xms512m -Xmx2g"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

### Docker Compose

```yaml
version: '3.8'

services:
  tse-server:
    build: ./TSe_Server
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - JASYPT_ENCRYPTOR_PASSWORD=${JASYPT_PASSWORD}
    depends_on:
      - postgres
      - redis

  postgres:
    image: postgres:13
    volumes:
      - postgres_data:/var/lib/postgresql/data
    environment:
      - POSTGRES_DB=db_tse
      - POSTGRES_PASSWORD=${DB_PASSWORD}

  redis:
    image: redis:6
    volumes:
      - redis_data:/data

volumes:
  postgres_data:
  redis_data:
```

---

## Summary

| Aspect | Recommendation |
|--------|----------------|
| **Build Tool** | Maven 3.6+ |
| **Java Version** | OpenJDK 11 |
| **Deployment** | Systemd services or Docker |
| **Secrets** | Jasypt encryption + environment variables |
| **CI/CD** | GitLab CI or GitHub Actions |
| **Branching** | Git Flow |
| **Monitoring** | Health endpoints + log aggregation |
| **Rollback** | Keep previous JAR + database backups |

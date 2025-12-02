# Environment Setup Guide

## Table of Contents
1. [Prerequisites](#prerequisites)
2. [Required Tools](#required-tools)
3. [Database Setup](#database-setup)
4. [Redis Setup](#redis-setup)
5. [Firebase Setup](#firebase-setup)
6. [Environment Variables](#environment-variables)
7. [Running Each Module](#running-each-module)
8. [Build Commands](#build-commands)
9. [IDE Setup](#ide-setup)
10. [Troubleshooting](#troubleshooting)

---

## Prerequisites

### System Requirements
- **OS:** Windows, macOS, or Linux
- **RAM:** Minimum 8GB (16GB recommended)
- **Disk Space:** At least 2GB for dependencies

### Java Version
- **Required:** Java 11 (OpenJDK or Oracle JDK)
- **Verify:** `java -version` should show `11.x.x`

```bash
# Install on Ubuntu/Debian
sudo apt install openjdk-11-jdk

# Install on macOS (Homebrew)
brew install openjdk@11

# Set JAVA_HOME
export JAVA_HOME=/path/to/java-11
export PATH=$JAVA_HOME/bin:$PATH
```

---

## Required Tools

### 1. Apache Maven
- **Version:** 3.6+ recommended
- **Install:**
  ```bash
  # Ubuntu/Debian
  sudo apt install maven

  # macOS
  brew install maven

  # Verify
  mvn -version
  ```

### 2. PostgreSQL
- **Version:** 12+ recommended (tested with PostgreSQL 13)
- **Install:**
  ```bash
  # Ubuntu/Debian
  sudo apt install postgresql postgresql-contrib

  # macOS
  brew install postgresql

  # Start service
  sudo systemctl start postgresql  # Linux
  brew services start postgresql   # macOS
  ```

### 3. Redis
- **Version:** 6+ recommended
- **Install:**
  ```bash
  # Ubuntu/Debian
  sudo apt install redis-server

  # macOS
  brew install redis

  # Start service
  sudo systemctl start redis  # Linux
  brew services start redis   # macOS
  ```

### 4. Git
- **Install:**
  ```bash
  # Ubuntu/Debian
  sudo apt install git

  # macOS
  brew install git
  ```

---

## Database Setup

### Step 1: Create Databases

The system requires **two PostgreSQL databases**:

```sql
-- Connect to PostgreSQL
psql -U postgres

-- Create main database
CREATE DATABASE db_tse;

-- Create chat database
CREATE DATABASE chatapp;

-- Create user (optional - can use postgres)
CREATE USER tse_user WITH PASSWORD 'your_secure_password';

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE db_tse TO tse_user;
GRANT ALL PRIVILEGES ON DATABASE chatapp TO tse_user;
```

### Step 2: Schema Setup

Schemas are created automatically via Hibernate `ddl-auto=update`, but you can pre-create them:

```sql
-- Connect to db_tse
\c db_tse

-- Create schemas
CREATE SCHEMA IF NOT EXISTS tse;
CREATE SCHEMA IF NOT EXISTS public;

-- Connect to chatapp
\c chatapp

-- Create schemas
CREATE SCHEMA IF NOT EXISTS chat;
CREATE SCHEMA IF NOT EXISTS public;
```

### Step 3: Verify Connection

```bash
psql -h localhost -U postgres -d db_tse
# Enter password when prompted
# Should connect successfully
```

---

## Redis Setup

### Configuration

Redis is used for:
- OTP caching
- Session token management
- Temporary data storage

Default configuration in `application.properties`:
```properties
redis.expire.time=10  # OTP expiry in minutes
```

### Verify Redis

```bash
redis-cli ping
# Should return: PONG
```

---

## Firebase Setup

### Step 1: Create Firebase Project

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Create a new project or use existing
3. Navigate to **Project Settings > Service Accounts**
4. Click **Generate new private key**
5. Download the JSON file

### Step 2: Configure Firebase

1. Rename the JSON file to `wfhtse.json`
2. Place it in the module's resources folder:
   ```
   TSe_Server/src/main/resources/firebase-config/wfhtse.json
   ```

### Step 3: Update Configuration

In `application.properties`:
```properties
app.firebase-configuration-file=firebase-config/wfhtse.json
```

---

## Environment Variables

### Core Configuration

Create a `.env` file or set these environment variables:

```bash
# Database Configuration
export DB_URL=jdbc:postgresql://localhost:5432/db_tse
export DB_USERNAME=postgres
export DB_PASSWORD=your_password

# Secondary Database (Chat)
export CHAT_DB_URL=jdbc:postgresql://localhost:5432/chatapp
export CHAT_DB_USERNAME=postgres
export CHAT_DB_PASSWORD=your_password

# Jasypt Encryption Password
export JASYPT_ENCRYPTOR_PASSWORD=TSE_SECRET_KEY

# JWT Configuration
export JWT_SECRET=ThisIsSecretForJWTHS512SignatureAlgorithmThatMUSTHave64ByteLength
export JWT_EXPIRATION=2592000

# Mail Configuration
export MAIL_USERNAME=your_email@gmail.com
export MAIL_PASSWORD=your_app_password

# Environment (1=QA, 2=Pre-Prod, 3=Prod)
export APP_ENVIRONMENT=1

# Encryption Key (must be 8, 16, or 24 bytes)
export ENCRYPTION_KEY=your_16_byte_key
```

### Application Properties Override

You can override properties via command line:
```bash
java -jar tse.jar \
  --spring.datasource.password=${DB_PASSWORD} \
  --jasypt.encryptor.password=${JASYPT_ENCRYPTOR_PASSWORD}
```

---

## Running Each Module

### Module 1: TSe_Server (Core Application)

**Port:** 8080
**Context Path:** `/api`

```bash
cd TSe_Server

# Build
mvn clean package -DskipTests

# Run
java -jar target/tse.jar

# Or run with Maven
mvn spring-boot:run
```

**Verify:**
```bash
curl http://localhost:8080/api/health
# Or access Swagger: http://localhost:8080/api/swagger-ui.html
```

### Module 2: TSEHR (Timesheet/HR)

**Port:** 8081
**No context path**

```bash
cd TSEHR

# Build
mvn clean package -DskipTests

# Run
java -jar target/*.jar

# Or run with Maven
mvn spring-boot:run
```

### Module 3: Notification_Reminders (Scheduler)

**Port:** 8081 (same as TSEHR - run one at a time, or change port)

```bash
cd Notification_Reminders

# Build
mvn clean package -DskipTests

# Run
java -jar target/*.jar

# Or run with Maven
mvn spring-boot:run
```

**Note:** This module runs scheduled tasks and calls TSe_Server via REST API at:
```
http://localhost:8080/api/schedule/*
```

### Module 4: Chat-App

**Port:** 8082
**No context path**

```bash
cd Vijayi_WFH_Conversation/chat-app

# Build
mvn clean package -DskipTests

# Run
java -jar target/*.jar

# Or run with Maven
mvn spring-boot:run
```

---

## Build Commands

### Build All Modules

```bash
# From repository root
for module in TSe_Server TSEHR Notification_Reminders Vijayi_WFH_Conversation/chat-app; do
  echo "Building $module..."
  cd $module
  mvn clean package -DskipTests
  cd -
done
```

### Individual Module Commands

| Module | Build | Package Name |
|--------|-------|--------------|
| TSe_Server | `mvn clean package` | `tse.jar` |
| TSEHR | `mvn clean package` | `tsehr-*.jar` |
| Notification_Reminders | `mvn clean package` | `tse_scheduler-*.jar` |
| chat-app | `mvn clean package` | `chat-app-*.jar` |

### Run Tests

```bash
# Run tests for a module
cd TSe_Server
mvn test

# Run with coverage report
mvn test jacoco:report
# Report at: target/site/jacoco/index.html

# Skip tests during build
mvn package -DskipTests
```

### Clean Build

```bash
# Clean compiled files
mvn clean

# Force update dependencies
mvn clean install -U
```

---

## IDE Setup

### IntelliJ IDEA (Recommended)

1. **Import Project:**
   - File → Open → Select repository root
   - Import as Maven project

2. **Configure JDK:**
   - File → Project Structure → SDKs
   - Add JDK 11

3. **Enable Lombok:**
   - Install Lombok plugin
   - Enable annotation processing:
     - Settings → Build → Compiler → Annotation Processors
     - Check "Enable annotation processing"

4. **Run Configurations:**
   - Create Spring Boot run configuration for each module
   - Set main class:
     - TSe_Server: `com.tse.core_application.CoreApplication`
     - TSEHR: (check main class)
     - chat-app: (check main class)

### VS Code

1. **Install Extensions:**
   - Extension Pack for Java
   - Spring Boot Extension Pack
   - Lombok Annotations Support

2. **Configure JDK:**
   - Add to `settings.json`:
     ```json
     {
       "java.home": "/path/to/jdk-11",
       "java.configuration.updateBuildConfiguration": "automatic"
     }
     ```

### Eclipse

1. **Import Project:**
   - File → Import → Maven → Existing Maven Projects
   - Select repository root

2. **Install Lombok:**
   - Download lombok.jar
   - Run: `java -jar lombok.jar`
   - Select Eclipse installation

---

## Troubleshooting

### Common Issues

#### 1. Database Connection Failed

**Error:** `Connection refused` or `FATAL: password authentication failed`

**Solution:**
```bash
# Check PostgreSQL is running
sudo systemctl status postgresql

# Check pg_hba.conf allows local connections
sudo nano /etc/postgresql/*/main/pg_hba.conf
# Change "peer" to "md5" for local connections

# Restart PostgreSQL
sudo systemctl restart postgresql
```

#### 2. Port Already in Use

**Error:** `Port 8080 already in use`

**Solution:**
```bash
# Find process using port
lsof -i :8080

# Kill process
kill -9 <PID>

# Or change port in application.properties
server.port=8090
```

#### 3. Jasypt Decryption Failed

**Error:** `Failed to decrypt property`

**Solution:**
- Ensure `JASYPT_ENCRYPTOR_PASSWORD` environment variable is set
- Or pass it via command line:
  ```bash
  java -jar tse.jar --jasypt.encryptor.password=TSE_SECRET_KEY
  ```

#### 4. Redis Connection Failed

**Error:** `Unable to connect to Redis`

**Solution:**
```bash
# Check Redis is running
redis-cli ping

# Start Redis
sudo systemctl start redis
```

#### 5. Firebase Initialization Failed

**Error:** `FileNotFoundException: firebase-config/wfhtse.json`

**Solution:**
- Ensure Firebase config file exists in:
  ```
  src/main/resources/firebase-config/wfhtse.json
  ```
- Or update the path in `application.properties`

#### 6. Lombok Not Working

**Error:** `Cannot resolve symbol 'log'` or getter/setter errors

**Solution:**
- Enable annotation processing in IDE
- Install Lombok plugin
- Rebuild project

---

## Quick Start Checklist

- [ ] Java 11 installed and configured
- [ ] Maven 3.6+ installed
- [ ] PostgreSQL installed and running
- [ ] Databases `db_tse` and `chatapp` created
- [ ] Redis installed and running
- [ ] Firebase config file placed
- [ ] Environment variables set
- [ ] Clone repository
- [ ] Build TSe_Server: `mvn clean package`
- [ ] Run TSe_Server: `java -jar target/tse.jar`
- [ ] Verify: Access `http://localhost:8080/api/swagger-ui.html`

---

## Development Tips

### Hot Reload

Enable Spring DevTools for hot reload during development:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-devtools</artifactId>
    <scope>runtime</scope>
    <optional>true</optional>
</dependency>
```

### Debug Mode

Run with debug logging:
```bash
java -jar tse.jar --logging.level.com.tse=DEBUG

# Or enable via properties
app.debug=true
```

### Profile-Based Configuration

Create profile-specific properties:
```
application-dev.properties
application-qa.properties
application-prod.properties
```

Run with profile:
```bash
java -jar tse.jar --spring.profiles.active=dev
```

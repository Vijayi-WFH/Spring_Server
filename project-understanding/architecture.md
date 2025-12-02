# TSE Server - Complete System Architecture Documentation

## Table of Contents
1. [System Overview](#system-overview)
2. [Architecture Pattern](#architecture-pattern)
3. [Technology Stack](#technology-stack)
4. [Module Overview](#module-overview)
5. [Project Directory Structure](#project-directory-structure)
6. [Inter-Module Communication](#inter-module-communication)
7. [Shared Objects & Tables](#shared-objects--tables)
8. [External Integrations](#external-integrations)
9. [Security Architecture](#security-architecture)
10. [Data Flow Diagrams](#data-flow-diagrams)

---

## System Overview

TSE (Team Software Excellence) is an enterprise-grade **project management and team collaboration platform** built using Spring Boot. The system is designed as a **multi-module monolith** architecture where four interconnected modules work together to provide comprehensive workforce management capabilities.

### Core Capabilities
- **Task & Project Management** - Work items, epics, sprints, dependencies
- **Team Collaboration** - Meetings, notifications, real-time chat
- **HR & Leave Management** - Leave policies, applications, approvals
- **Attendance & Geo-fencing** - Location-based attendance, punch-in/out
- **Timesheet Tracking** - Effort logging, timesheet generation
- **GitHub Integration** - Branch linking, repository management
- **Performance Management** - Notes, ratings, evaluations

---

## Architecture Pattern

### Multi-Module Monolith Architecture

```
                    ┌──────────────────────────────────────────────────────────────┐
                    │                     SPRING_SERVER REPOSITORY                  │
                    │                                                               │
                    │  ┌─────────────────────────────────────────────────────────┐ │
                    │  │                    TSe_Server (Port 8080)                │ │
                    │  │                    [CORE APPLICATION]                    │ │
                    │  │                                                         │ │
                    │  │  • Task/Epic/Sprint Management                          │ │
                    │  │  • User/Organization Management                         │ │
                    │  │  • Meeting Management                                   │ │
                    │  │  • Leave Management                                     │ │
                    │  │  • Notification Service                                 │ │
                    │  │  • Geo-fencing & Attendance                             │ │
                    │  │  • GitHub Integration                                   │ │
                    │  │  • Authentication (JWT/OAuth2)                          │ │
                    │  └─────────────────────────────────────────────────────────┘ │
                    │          │                    │                    │         │
                    │          │ REST API           │ REST API           │ REST    │
                    │          ▼                    ▼                    ▼ API     │
                    │  ┌───────────────┐  ┌───────────────┐  ┌───────────────────┐ │
                    │  │ TSEHR         │  │ Notification_ │  │ Vijayi_WFH_       │ │
                    │  │ (Port 8081)   │  │ Reminders     │  │ Conversation      │ │
                    │  │               │  │ (Port 8081)   │  │ (Port 8082)       │ │
                    │  │ • Timesheet   │  │               │  │                   │ │
                    │  │ • Leave HR    │  │ • Schedulers  │  │ • Real-time Chat  │ │
                    │  │ • Reports     │  │ • Cron Jobs   │  │ • WebSocket       │ │
                    │  │               │  │ • Reminders   │  │ • File Sharing    │ │
                    │  └───────────────┘  └───────────────┘  └───────────────────┘ │
                    │                                                               │
                    └──────────────────────────────────────────────────────────────┘
                                              │
                                              ▼
                    ┌──────────────────────────────────────────────────────────────┐
                    │                      PostgreSQL Database                      │
                    │                                                               │
                    │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐   │
                    │  │   db_tse    │  │ db_tse_     │  │      chatapp        │   │
                    │  │  (Primary)  │  │ secondary   │  │    (Chat Schema)    │   │
                    │  └─────────────┘  └─────────────┘  └─────────────────────┘   │
                    └──────────────────────────────────────────────────────────────┘
```

### Why Multi-Module Monolith?
1. **Shared Database** - All modules share PostgreSQL with schema separation
2. **Shared Authentication** - JWT tokens validated centrally by TSe_Server
3. **Coordinated Deployment** - Modules can be deployed together or separately
4. **Reduced Complexity** - No service mesh or API gateway required
5. **Efficient Development** - Shared entity models and utilities

---

## Technology Stack

### Core Frameworks & Libraries

| Layer | Technology | Version |
|-------|------------|---------|
| **Framework** | Spring Boot | 2.4.5 - 2.7.9 |
| **Language** | Java | 11 |
| **Build Tool** | Maven | 3.6+ |
| **ORM** | Spring Data JPA / Hibernate | 5.x |
| **Database** | PostgreSQL | 12+ |
| **Caching** | Redis | 6.x |
| **Security** | Spring Security + JWT | 5.x |
| **Real-time** | Spring WebSocket | 5.x |
| **Logging** | Log4j2 | 2.23.1 |

### External Services

| Service | Purpose | Integration |
|---------|---------|-------------|
| **Firebase** | Push Notifications | Admin SDK |
| **Google OAuth2** | SSO Authentication | OAuth2 Client |
| **Gmail SMTP** | Email Notifications | JavaMail |
| **OpenFire** | XMPP Chat (Disabled) | REST API |
| **Jitsi Meet** | Video Conferencing | URL Integration |
| **GitHub** | Source Control Integration | OAuth + REST API |

### Security Stack

| Component | Technology |
|-----------|------------|
| **Authentication** | JWT RS512 (RSA 2048-bit) |
| **Password Encryption** | Jasypt PBEWithMD5AndTripleDES |
| **Data Encryption** | AES-CBC-PKCS5Padding |
| **OAuth2** | Google SSO |
| **API Security** | Role-based Access Control |

---

## Module Overview

### 1. TSe_Server (Core Application)
- **Port:** 8080
- **Context Path:** `/api`
- **Files:** 1,116 Java files
- **Purpose:** Main application handling all core business logic

**Key Domains:**
- User & Organization Management
- Task, Epic & Sprint Management
- Meeting Management
- Leave Management
- Notification System
- Geo-fencing & Attendance
- GitHub Integration
- Performance Notes

### 2. TSEHR (Timesheet/HR Module)
- **Port:** 8081
- **Files:** 114 Java files
- **Purpose:** Timesheet generation and HR-specific leave operations

**Key Features:**
- Timesheet generation and reporting
- Leave policy management
- Leave balance calculations
- Pro-rata leave allocations

### 3. Notification_Reminders (Scheduler Module)
- **Port:** 8081
- **Files:** 3 Java files
- **Purpose:** Automated scheduled tasks and reminders

**Key Schedulers:**
- Meeting reminders (every 60s)
- Timesheet reminders (every 60s)
- Leave balance resets (annually)
- Old notification cleanup (daily)
- Geo-fencing auto-checkout (every minute)

### 4. Vijayi_WFH_Conversation (Chat Module)
- **Port:** 8082
- **Files:** 108 Java files
- **Purpose:** Real-time chat and messaging

**Key Features:**
- WebSocket-based real-time messaging
- Group chat management
- File attachments
- Read receipts and delivery status
- User presence (online/offline)

---

## Project Directory Structure

```
Spring_Server/
├── TSe_Server/                          # Core Application
│   ├── pom.xml
│   └── src/main/java/com/tse/core_application/
│       ├── CoreApplication.java         # Entry point
│       ├── config/                      # Security & Web config
│       ├── configuration/               # DB & App config
│       ├── constants/                   # Application constants
│       ├── controller/                  # REST controllers (50+)
│       │   └── geo_fencing/            # Geo-fencing controllers
│       ├── dto/                        # Data Transfer Objects
│       │   ├── geo_fence/
│       │   ├── github/
│       │   ├── leave/
│       │   ├── meeting/
│       │   └── ... (22+ subdirectories)
│       ├── exception/                  # Custom exceptions
│       ├── filters/                    # JWT filter
│       ├── handlers/                   # Exception handlers
│       ├── keys/                       # RSA key files
│       ├── model/                      # JPA entities (100+)
│       │   ├── geo_fencing/
│       │   ├── github/
│       │   └── performance_notes/
│       ├── repository/                 # Spring Data repositories
│       │   └── geo_fencing/
│       ├── service/                    # Service interfaces
│       │   └── Impl/                   # Service implementations
│       │       └── geo_fencing/
│       ├── utils/                      # Utility classes
│       ├── validators/                 # Custom validators
│       └── web/                        # WebSocket handlers
│
├── TSEHR/                              # Timesheet HR Module
│   ├── pom.xml
│   └── src/main/java/com/tse/core/
│       ├── TimesheetDemoApplication.java
│       ├── configuration/
│       ├── constants/
│       ├── controller/
│       ├── dto/
│       │   └── leave/
│       ├── exception/
│       ├── model/
│       │   ├── leave/
│       │   └── supplements/
│       ├── repository/
│       │   ├── leaves/
│       │   └── supplements/
│       ├── service/
│       └── utils/
│
├── Notification_Reminders/             # Scheduler Module
│   ├── pom.xml
│   └── src/main/java/com/tse/scheduling/
│       ├── WfhSchedulerApplication.java
│       ├── constants/Constants.java
│       └── scheduler/Scheduler.java    # 22 scheduled tasks
│
├── Vijayi_WFH_Conversation/           # Chat Module
│   └── chat-app/
│       ├── pom.xml
│       └── src/main/java/com/example/chat_app/
│           ├── ChatAppApplication.java
│           ├── config/                 # WebSocket config
│           ├── constants/
│           ├── controller/
│           ├── dto/
│           ├── exception/
│           ├── jwtUtils/               # JWT validation
│           ├── model/
│           ├── repository/
│           ├── service/
│           ├── specification/
│           └── utils/
│
└── project-understanding/              # Documentation
    ├── architecture.md                 # This file
    ├── module-summary.md
    ├── db-summary.md
    ├── business-flows.md
    ├── api-map.md
    └── future-context/
```

---

## Inter-Module Communication

### Communication Patterns

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         INTER-MODULE COMMUNICATION                          │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  1. REST API CALLS (Synchronous HTTP)                                       │
│  ────────────────────────────────────                                       │
│                                                                             │
│  Notification_Reminders ──POST──> TSe_Server                                │
│  • /api/schedule/meeting/reminder                                           │
│  • /api/schedule/leave/leaveRemainingReset                                  │
│  • /api/schedule/notification/deleteOldNotifications                        │
│  • /api/schedule/geo-fencing/autoCheckout                                   │
│  • ...and 18 more endpoints                                                 │
│                                                                             │
│  Chat-App ──GET/POST──> TSe_Server                                          │
│  • /api/auth/validateTokenAccount (JWT validation)                          │
│  • /api/notification/send (Push notifications)                              │
│                                                                             │
│  TSEHR ──────> TSe_Server (via shared database only)                        │
│                                                                             │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  2. SHARED DATABASE (Asynchronous via PostgreSQL)                           │
│  ─────────────────────────────────────────────────                          │
│                                                                             │
│  db_tse (Primary)                                                           │
│  ├── TSe_Server (Read/Write all tables)                                     │
│  ├── TSEHR (Read/Write tse.* tables)                                        │
│  └── Chat-App (Read-only for user validation)                               │
│                                                                             │
│  chatapp (Chat Schema)                                                      │
│  └── Chat-App (Read/Write all chat tables)                                  │
│                                                                             │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  3. WEBSOCKET (Real-time Bidirectional)                                     │
│  ────────────────────────────────────────                                   │
│                                                                             │
│  Browser/Mobile ──WebSocket──> Chat-App (/chat endpoint)                    │
│  • Message sending/receiving                                                │
│  • Typing indicators                                                        │
│  • Online/offline status                                                    │
│  • Read receipts                                                            │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Communication Matrix

| From | To | Protocol | Purpose |
|------|-----|----------|---------|
| Notification_Reminders | TSe_Server | HTTP POST | Trigger scheduled tasks |
| Chat-App | TSe_Server | HTTP GET | JWT token validation |
| Chat-App | TSe_Server | HTTP POST | Push notifications |
| TSEHR | TSe_Server | Shared DB | Leave/timesheet data |
| Browser | Chat-App | WebSocket | Real-time messaging |
| All Modules | PostgreSQL | JDBC | Data persistence |

---

## Shared Objects & Tables

### Shared Entity Models

These entities are duplicated across modules for database access:

| Entity | TSe_Server | TSEHR | Chat-App |
|--------|------------|-------|----------|
| User | Yes | Yes (supplements) | Yes |
| UserAccount | Yes | Yes (supplements) | Yes |
| Organization | Yes | Yes (supplements) | No |
| Team | Yes | Yes (supplements) | No |
| Project | Yes | Yes (supplements) | No |
| Task | Yes | Yes (supplements) | No |
| LeaveApplication | Yes | Yes (model) | No |
| LeavePolicy | Yes | Yes (model) | No |
| LeaveRemaining | Yes | Yes (model) | No |

### Shared Database Tables

```sql
-- Schema: tse (Primary)

-- User Management
tse_users           -- User profiles (encrypted fields)
user_account        -- Account-organization mapping
organization        -- Organization details
bu                  -- Business units
project             -- Projects within BUs
team                -- Teams within projects

-- Task Management
task                -- Work items (28,000+ char entity)
task_history        -- Audit trail for tasks
epic                -- Epic containers
sprint              -- Sprint management
dependency          -- Task dependencies

-- Leave Management
leave_application   -- Leave requests
leave_policy        -- Policy definitions
leave_remaining     -- Balance tracking

-- Meeting Management
meeting             -- Meeting details
attendee            -- Meeting participants
action_item         -- Action items from meetings

-- Notifications
notification        -- System notifications
notification_type   -- Notification categories

-- Geo-fencing
geofence            -- Fence definitions
attendance_day      -- Daily attendance
attendance_event    -- Punch events
punch_request       -- Manual punch requests

-- Schema: chat (Chat-App)
user                -- Chat user profiles
group               -- Chat groups
message             -- Messages (DM + group)
message_user        -- Read receipts
message_attachment  -- File attachments
pinned_chats        -- Pinned conversations
```

---

## External Integrations

### Firebase (Push Notifications)

```
TSe_Server ──> Firebase Admin SDK ──> Mobile Devices
     │
     └── Configuration: firebase-config/wfhtse.json
```

**Notification Types:**
- Task assignments/updates
- Meeting reminders
- Leave approvals
- Chat messages
- System alerts

### Google OAuth2 (SSO)

```
Client ──> Google OAuth ──> TSe_Server
                              │
                              └── Token validation & user creation
```

**Endpoints:**
- `/auth/login` - Mobile (Google ID Token)
- `/auth/googlesso` - Web (OAuth2 flow)

### GitHub Integration

```
TSe_Server ──> GitHub API ──> Repositories
     │
     ├── OAuth linking
     ├── Branch synchronization
     └── Task-to-branch mapping
```

**Features:**
- Link GitHub accounts via OAuth
- Sync repository branches
- Associate tasks with branches
- Auto-create branches for tasks

### Jitsi Meet (Video Conferencing)

```
Meeting ──> Jitsi URL Generation ──> External Meeting
```

**Integration:**
- Meeting URL generation
- Room key embedding
- Participant management

---

## Security Architecture

### Authentication Flow

```
┌─────────────────────────────────────────────────────────────────────┐
│                      AUTHENTICATION FLOW                             │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  1. OTP-Based Login                                                  │
│  ───────────────────                                                 │
│  Client ──[POST /auth/generateotp]──> TSe_Server                     │
│    │                                      │                          │
│    │                                      ▼                          │
│    │                               Send OTP via Email                │
│    │                                      │                          │
│    │                                      ▼                          │
│  Client ──[POST /auth/login + OTP]──> TSe_Server                     │
│    │                                      │                          │
│    │                                      ▼                          │
│    │                               Generate JWT (RS512)              │
│    │                                      │                          │
│    ◄──────────────JWT Token──────────────┘                          │
│                                                                      │
│  2. Google OAuth2 SSO                                                │
│  ────────────────────                                                │
│  Client ──[OAuth Flow]──> Google ──[ID Token]──> TSe_Server          │
│    │                                                │                │
│    │                                                ▼                │
│    │                                    Validate Google Token        │
│    │                                                │                │
│    │                                                ▼                │
│    │                                    Generate JWT (RS512)         │
│    │                                                │                │
│    ◄────────────────JWT Token───────────────────────┘               │
│                                                                      │
│  3. Request Authorization                                            │
│  ────────────────────────                                            │
│  Client ──[Request + JWT + Headers]──> JwtRequestFilter              │
│    │                                        │                        │
│    │                                        ▼                        │
│    │                              Validate JWT signature             │
│    │                              Check token expiration             │
│    │                              Validate accountIds                │
│    │                              Check blocked tokens               │
│    │                              Check deactivated users            │
│    │                                        │                        │
│    │                                        ▼                        │
│    │                              Set SecurityContext                │
│    │                                        │                        │
│    ◄─────────────────Response───────────────┘                       │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### JWT Token Structure

```json
{
  "sub": "user@example.com",
  "accountIds": [1, 2, 3],
  "chatPassword": "encrypted_password",
  "iat": 1701446400,
  "exp": 1704038400
}
```

**Token Properties:**
- Algorithm: RS512 (RSA with SHA-512)
- Key Size: 2048 bits
- Expiration: 30 days
- Claims: username, accountIds, chatPassword

### Required Request Headers

| Header | Required | Description |
|--------|----------|-------------|
| `Authorization` | Yes | Bearer JWT token |
| `accountIds` | Yes | Comma-separated account IDs |
| `timeZone` | Yes | Valid timezone (e.g., "Asia/Kolkata") |
| `screenName` | Yes | Alphanumeric identifier for audit |

---

## Data Flow Diagrams

### Task Creation Flow

```
User ──POST /task/addNewTask──> TasksController
                                    │
                                    ▼
                              TaskServiceImpl
                                    │
                    ┌───────────────┼───────────────┐
                    ▼               ▼               ▼
              Validation     Set Defaults    Create History
                    │               │               │
                    └───────────────┼───────────────┘
                                    ▼
                             TaskRepository
                                    │
                                    ▼
                              PostgreSQL (task table)
                                    │
                                    ▼
                          NotificationService
                                    │
                    ┌───────────────┼───────────────┐
                    ▼               ▼               ▼
               In-App          Firebase         Email
             Notification    Push Notification  (if configured)
```

### Leave Application Flow

```
Employee ──POST /leave/leaveApplication──> LeaveController
                                              │
                                              ▼
                                        LeaveService
                                              │
                    ┌─────────────────────────┼─────────────────────────┐
                    ▼                         ▼                         ▼
           Validate Dates            Check Balance             Validate Approver
                    │                         │                         │
                    └─────────────────────────┼─────────────────────────┘
                                              ▼
                                   LeaveApplicationRepository
                                              │
                                              ▼
                                        PostgreSQL
                                              │
                                              ▼
                                  Notify Approver (Manager)
                                              │
                                              ▼
Manager ──POST /leave/changeLeaveApplicationStatus──> LeaveService
                                              │
                    ┌─────────────────────────┼─────────────────────────┐
                    ▼                         ▼                         ▼
             Update Status          Update Balance           Notify Employee
```

### Chat Message Flow

```
User ──WebSocket /chat──> WebSocketController
           │
           ▼
    Parse JSON Action
           │
    ┌──────┴──────┬──────────────┬──────────────┐
    ▼             ▼              ▼              ▼
MESSAGE_SENT  DELIVERY_ACK  READ_ACK     TYPING_INDICATOR
    │             │              │              │
    ▼             ▼              ▼              ▼
MessageService MessageUser  MessageUser   Broadcast to
    │         Repository   Repository    Active Sessions
    ▼
Save Message
    │
    ▼
MessageStats (tick counts)
    │
    ▼
FCMClient ──HTTP POST──> TSe_Server ──> Firebase ──> Recipients
```

---

## Summary

The TSE Server platform is a comprehensive enterprise solution built on:

1. **Multi-Module Architecture** - 4 specialized modules sharing a common database
2. **Spring Boot Foundation** - Leveraging Spring's ecosystem for web, security, data, and WebSocket
3. **PostgreSQL Backbone** - Primary data store with schema separation
4. **JWT Security** - RSA-based token authentication with role-based access
5. **Real-time Capabilities** - WebSocket for chat, Firebase for push notifications
6. **External Integrations** - Google OAuth, GitHub, Jitsi for extended functionality

The system demonstrates a well-structured approach to enterprise application development with clear separation of concerns while maintaining efficient inter-module communication through shared database access and REST APIs.

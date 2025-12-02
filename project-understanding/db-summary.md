# Database Schema Summary

## Table of Contents
1. [Database Overview](#database-overview)
2. [Schema: tse (Primary)](#schema-tse-primary)
3. [Schema: chat (Chat Application)](#schema-chat-chat-application)
4. [Entity Relationships](#entity-relationships)
5. [Important Enums & Constants](#important-enums--constants)
6. [Triggers, Cron Jobs & Retention](#triggers-cron-jobs--retention)

---

## Database Overview

### Database Configuration

| Database | Schema | Used By | Purpose |
|----------|--------|---------|---------|
| `db_tse` | tse | TSe_Server, TSEHR | Primary application data |
| `db_tse_secondary` | tse | TSe_Server | Secondary/replica |
| `chatapp` | chat | Chat-App | Chat/messaging data |

### Connection Details

**Primary Database (db_tse):**
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/db_tse
spring.datasource.username=userName
spring.datasource.password=password
spring.jpa.hibernate.ddl-auto=update
spring.jpa.properties.hibernate.default_schema=tse
```

**Chat Database (chatapp):**
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/chatapp
spring.datasource.username=postgres
spring.jpa.properties.hibernate.default_schema=chat
```

---

## Schema: tse (Primary)

### User Management Tables

#### tse_users
Primary user profile storage with encrypted fields.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| user_id | BIGINT | PK, AUTO | Unique user identifier |
| primary_email | VARCHAR(50) | NOT NULL, ENCRYPTED | Work email |
| alternate_email | VARCHAR(50) | ENCRYPTED | Alternate work email |
| personal_email | VARCHAR(50) | ENCRYPTED | Personal email |
| current_org_email | VARCHAR(50) | ENCRYPTED | Current org email |
| given_name | VARCHAR(50) | ENCRYPTED | Display name |
| first_name | VARCHAR(50) | ENCRYPTED | First name |
| last_name | VARCHAR(50) | ENCRYPTED | Last name |
| middle_name | VARCHAR(50) | ENCRYPTED | Middle name |
| locale | VARCHAR(50) | ENCRYPTED | User locale |
| city | VARCHAR(50) | ENCRYPTED | City |
| time_zone | VARCHAR(50) | NOT NULL | Timezone (e.g., "Asia/Kolkata") |
| gender | INTEGER | FK | Gender reference |
| age_range | INTEGER | FK | Age range reference |
| highest_education | INTEGER | FK | Education level |
| chat_user_name | VARCHAR | ENCRYPTED | OpenFire username |
| chat_password | VARCHAR | ENCRYPTED | OpenFire password |
| image_data | VARCHAR(5000) | ENCRYPTED | Profile image |
| multi_association | BOOLEAN | | Multiple org membership |
| is_user_managing | BOOLEAN | | Is manager flag |
| managing_user_id | BIGINT | FK | Manager's user ID |
| fk_country_id | BIGINT | FK | Country reference |
| created_date_time | TIMESTAMP | AUTO | Creation timestamp |
| last_updated_date_time | TIMESTAMP | AUTO | Update timestamp |

#### user_account
Account-organization mapping for multi-tenancy.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| account_id | BIGINT | PK, AUTO, UNIQUE | Account identifier |
| org_id | BIGINT | NOT NULL, FK | Organization reference |
| email | VARCHAR(50) | NOT NULL, ENCRYPTED | Account email |
| is_default | BOOLEAN | DEFAULT true | Default account flag |
| is_active | BOOLEAN | DEFAULT true | Active status |
| is_verified | BOOLEAN | DEFAULT true | Email verified |
| is_registered_in_ai_service | BOOLEAN | DEFAULT true | AI service registration |
| is_disabled_by_sams | BOOLEAN | | Disabled by admin |
| deactivated_by_role | INTEGER | | Role that deactivated |
| deactivated_by_account_id | BIGINT | FK | Who deactivated |
| account_deactivated_date | TIMESTAMP | | Deactivation date |
| fk_user_id | BIGINT | NOT NULL, FK | User reference |
| created_date_time | TIMESTAMP | AUTO | Creation timestamp |
| last_updated_date_time | TIMESTAMP | AUTO | Update timestamp |

#### organization
Organization/tenant details.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| org_id | BIGINT | PK, AUTO, UNIQUE | Organization identifier |
| organization_name | VARCHAR(100) | ENCRYPTED | Full name |
| organization_display_name | VARCHAR(100) | ENCRYPTED | Display name |
| owner_account_id | BIGINT | FK | Owner's account |
| owner_email | VARCHAR | ENCRYPTED | Owner's email |
| is_disabled | BOOLEAN | DEFAULT false | Disabled status |
| max_bu_count | INTEGER | | Max business units |
| max_project_count | INTEGER | | Max projects |
| max_team_count | INTEGER | | Max teams |
| max_user_count | INTEGER | | Max users |
| max_memory_quota | BIGINT | | Max storage (bytes) |
| used_memory_quota | BIGINT | | Used storage |
| paid_subscription | BOOLEAN | | Paid status |
| on_trial | BOOLEAN | | Trial status |
| created_date_time | TIMESTAMP | AUTO | Creation timestamp |
| last_updated_date_time | TIMESTAMP | AUTO | Update timestamp |

#### bu (Business Unit)
| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| bu_id | BIGINT | PK, AUTO | BU identifier |
| bu_name | VARCHAR(255) | ENCRYPTED | BU name |
| org_id | BIGINT | NOT NULL, FK | Organization |
| owner_account_id | BIGINT | FK | Owner account |
| is_deleted | BOOLEAN | | Soft delete flag |
| is_disabled | BOOLEAN | | Disabled flag |

#### project
| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| project_id | BIGINT | PK, AUTO | Project identifier |
| project_name | VARCHAR(100) | NOT NULL, ENCRYPTED | Project name |
| project_desc | VARCHAR(1000) | ENCRYPTED | Description |
| project_type | INTEGER | | 1=default, 2=user-created |
| org_id | BIGINT | NOT NULL, FK | Organization |
| bu_id | BIGINT | NOT NULL, FK | Business unit |
| owner_account_id | BIGINT | FK | Owner account |
| is_disabled | BOOLEAN | | Disabled flag |
| is_deleted | BOOLEAN | | Soft delete flag |
| deleted_on | TIMESTAMP | | Deletion date |
| fk_deleted_by_account_id | BIGINT | FK | Who deleted |

#### team
| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| team_id | BIGINT | PK, AUTO | Team identifier |
| team_name | VARCHAR(255) | NOT NULL, ENCRYPTED | Team name |
| team_desc | VARCHAR(4000) | NOT NULL, ENCRYPTED | Description |
| team_code | VARCHAR(10) | NOT NULL | Unique team code |
| parent_team_id | BIGINT | FK | Parent team (hierarchy) |
| chat_room_name | VARCHAR | ENCRYPTED | OpenFire room name |
| is_deleted | BOOLEAN | | Soft delete flag |
| is_disabled | BOOLEAN | | Disabled flag |
| deleted_on | TIMESTAMP | | Deletion date |
| fk_project_id | BIGINT | NOT NULL, FK | Project reference |
| fk_org_id | BIGINT | NOT NULL, FK | Organization reference |
| fk_owner_account_id | BIGINT | NOT NULL, FK | Owner account |
| fk_deleted_by_account_id | BIGINT | FK | Who deleted |

---

### Task Management Tables

#### task
Core work item entity (28,000+ chars in model).

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| task_id | BIGINT | PK, AUTO | Task identifier |
| task_number | VARCHAR(40) | NOT NULL | Display number (e.g., "TEAM-123") |
| task_identifier | BIGINT | UNIQUE/TEAM | Sequential per team |
| task_title | VARCHAR(70) | NOT NULL | Title (3-70 chars) |
| task_desc | VARCHAR(5000) | NOT NULL, ENCRYPTED | Description |
| task_workflow_id | INTEGER | NOT NULL | Workflow type |
| current_activity_indicator | INTEGER | NOT NULL | Progress 0-100 |
| task_type_id | INTEGER | | 1=Task, 2=Parent, 3=Child, 4=Bug |
| parent_task_type_id | INTEGER | | Parent's task type |
| task_priority | VARCHAR | | Priority level |
| task_state | VARCHAR | | State (Todo, InProgress, etc.) |
| **Scheduling** |
| task_exp_start_date | TIMESTAMP | | Expected start |
| task_exp_end_date | TIMESTAMP | | Expected end |
| task_exp_start_time | TIME | | Expected start time |
| task_exp_end_time | TIME | | Expected end time |
| task_act_st_date | TIMESTAMP | | Actual start |
| task_act_end_date | TIMESTAMP | | Actual end |
| task_completion_date | DATE | | Completion date |
| **Effort Tracking** |
| recorded_effort | INTEGER | | Logged effort (minutes) |
| recorded_task_effort | INTEGER | | Task-specific effort |
| task_estimate | INTEGER | | Estimated hours |
| earned_time_task | INTEGER | | Earned value |
| total_effort | INTEGER | | Total effort |
| total_meeting_effort | INTEGER | | Meeting effort |
| **Content** |
| parking_lot | VARCHAR(1000) | ENCRYPTED | Parking lot notes |
| key_decisions | VARCHAR(1000) | ENCRYPTED | Key decisions |
| acceptance_criteria | VARCHAR(1000) | ENCRYPTED | Acceptance criteria |
| attachments | VARCHAR | | Attachment references |
| blocked_reason | VARCHAR | | Why blocked |
| **Relationships** |
| sprint_id | BIGINT | FK | Sprint reference |
| parent_task_id | BIGINT | FK | Parent task |
| child_task_ids | JSONB | | Array of child IDs |
| bug_task_relation | JSONB | | Related bugs |
| meeting_list | JSONB | | Related meetings |
| dependency_ids | JSONB | | Dependencies |
| **Stats** |
| task_progress_system | VARCHAR | ENUM | System-calculated status |
| task_progress_set_by_user | VARCHAR | ENUM | User-set status |
| **Accounts** |
| fk_account_id | BIGINT | NOT NULL, FK | Assigned to |
| fk_account_id_creator | BIGINT | NOT NULL, FK | Created by |
| fk_account_id_last_updated | BIGINT | NOT NULL, FK | Last updated by |
| fk_account_id_mentor_1 | BIGINT | FK | Mentor 1 |
| fk_account_id_mentor_2 | BIGINT | FK | Mentor 2 |
| fk_account_id_observer_1 | BIGINT | FK | Observer 1 |
| fk_account_id_observer_2 | BIGINT | FK | Observer 2 |
| **Entity References** |
| fk_team_id | BIGINT | NOT NULL, FK | Team |
| fk_org_id | BIGINT | NOT NULL, FK | Organization |
| fk_project_id | BIGINT | FK | Project |
| fk_epic_id | BIGINT | FK | Epic |
| fk_workflow_task_status | INTEGER | NOT NULL, FK | Workflow status |
| **Audit** |
| created_date_time | TIMESTAMP | AUTO | Creation timestamp |
| last_updated_date_time | TIMESTAMP | AUTO | Update timestamp |
| version | INTEGER | OPTIMISTIC LOCK | Version for concurrency |

#### task_history
Audit trail for all task changes.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| task_history_id | BIGINT | PK, AUTO | History identifier |
| task_id | BIGINT | FK | Original task |
| changed_by_account_id | BIGINT | FK | Who changed |
| change_type | VARCHAR | | CREATE, UPDATE, DELETE |
| changed_field | VARCHAR | | Which field changed |
| old_value | TEXT | | Previous value |
| new_value | TEXT | | New value |
| change_date_time | TIMESTAMP | | When changed |
| *All task fields mirrored* |

#### epic
| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| epic_id | BIGINT | PK, AUTO | Epic identifier |
| epic_number | VARCHAR(40) | UNIQUE | Display number |
| epic_title | VARCHAR(70) | NOT NULL, ENCRYPTED | Title |
| epic_desc | VARCHAR(5000) | NOT NULL, ENCRYPTED | Description |
| epic_priority | VARCHAR | | Priority |
| estimate | INTEGER | | Total estimate |
| original_estimate | INTEGER | | Original estimate |
| running_estimate | INTEGER | | Running estimate |
| color | VARCHAR(7) | | RGB hex color |
| linked_epic_id | JSONB | | Related epics |
| team_id_list | JSONB | | Associated teams |
| fk_workflow_epic_status | INTEGER | NOT NULL, FK | Workflow status |
| fk_org_id | BIGINT | NOT NULL, FK | Organization |
| fk_project_id | BIGINT | NOT NULL, FK | Project |
| fk_account_id_assigned | BIGINT | FK | Assigned to |
| fk_epic_owner | BIGINT | FK | Epic owner |

#### sprint
| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| sprint_id | BIGINT | PK, AUTO | Sprint identifier |
| sprint_title | VARCHAR(70) | NOT NULL, ENCRYPTED | Title |
| sprint_objective | VARCHAR(1000) | NOT NULL, ENCRYPTED | Objective |
| sprint_exp_start_date | TIMESTAMP | NOT NULL | Planned start |
| sprint_exp_end_date | TIMESTAMP | NOT NULL | Planned end |
| sprint_act_start_date | TIMESTAMP | | Actual start |
| sprint_act_end_date | TIMESTAMP | | Actual end |
| capacity_adjustment_deadline | TIMESTAMP | | Capacity lock date |
| sprint_status | INTEGER | | 1=Created, 2=Started, 3=Completed |
| entity_type_id | INTEGER | | For which entity type |
| entity_id | BIGINT | | For which entity |
| sprint_members | JSONB | | Member details |
| hours_of_sprint | INTEGER | | Total sprint hours |
| earned_efforts | INTEGER | | Earned value |
| next_sprint_id | BIGINT | FK | Next sprint |
| previous_sprint_id | BIGINT | FK | Previous sprint |
| fk_account_id_creator | BIGINT | NOT NULL, FK | Created by |
| version | INTEGER | OPTIMISTIC LOCK | Version |

---

### Leave Management Tables

#### leave_application
| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| leave_application_id | BIGINT | PK, AUTO | Application identifier |
| account_id | BIGINT | NOT NULL, FK | Applicant |
| leave_type_id | SMALLINT | NOT NULL, FK | Leave type |
| leave_application_status_id | SMALLINT | NOT NULL, FK | Status |
| from_date | DATE | NOT NULL | Start date |
| from_time | TIME | | Start time |
| to_date | DATE | NOT NULL | End date |
| to_time | TIME | | End time |
| include_lunch_time | BOOLEAN | NOT NULL | Include lunch |
| leave_reason | VARCHAR(4000) | NOT NULL, ENCRYPTED | Reason |
| approver_reason | VARCHAR(4000) | ENCRYPTED | Approver comment |
| approver_account_id | BIGINT | NOT NULL, FK | Approver |
| phone | VARCHAR | NOT NULL | Contact phone |
| address | VARCHAR(1000) | ENCRYPTED | Contact address |
| notify_to | VARCHAR | ENCRYPTED | Notification recipients |
| doctor_certificate | BYTEA | | Medical certificate |
| doctor_certificate_file_name | VARCHAR | | File name |
| doctor_certificate_file_type | VARCHAR | | MIME type |
| doctor_certificate_file_size | BIGINT | | File size |
| is_leave_for_half_day | BOOLEAN | | Half-day flag |
| number_of_leave_days | NUMERIC(4,2) | | Calculated days |
| leave_cancellation_reason | VARCHAR | ENCRYPTED | Cancellation reason |
| half_day_leave_type | INTEGER | | 1=First, 2=Second half |
| is_sprint_capacity_adjustment | BOOLEAN | | Affects sprint capacity |

#### leave_policy
| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| leave_policy_id | BIGINT | PK, AUTO | Policy identifier |
| leave_type_id | SMALLINT | NOT NULL, FK | Leave type |
| org_id | BIGINT | NOT NULL, FK | Organization |
| bu_id | BIGINT | FK | Business unit (optional) |
| project_id | BIGINT | FK | Project (optional) |
| team_id | BIGINT | FK | Team (optional) |
| leave_policy_title | VARCHAR(300) | ENCRYPTED | Policy name |
| initial_leaves | NUMERIC(4,2) | NOT NULL | Annual allocation |
| is_leave_carry_forward | BOOLEAN | NOT NULL | Allow carry-forward |
| max_leave_carry_forward | NUMERIC(4,2) | | Max carry-forward |
| is_negative_leave_allowed | BOOLEAN | | Allow negative |
| max_negative_leaves | NUMERIC(4,2) | | Max negative |
| include_non_business_days_in_leave | BOOLEAN | | Count weekends |
| created_by_account_id | BIGINT | FK | Creator |
| last_updated_by_account_id | BIGINT | FK | Last updater |

#### leave_remaining
| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| leave_remaining_id | BIGINT | PK, AUTO | Balance identifier |
| account_id | BIGINT | NOT NULL, FK | Employee |
| leave_policy_id | BIGINT | NOT NULL, FK | Policy reference |
| leave_type_id | SMALLINT | NOT NULL, FK | Leave type |
| leave_remaining | NUMERIC(4,2) | | Available balance |
| leave_taken | NUMERIC(4,2) | | Used leaves |
| calender_year | SMALLINT | NOT NULL | Year |
| currently_active | BOOLEAN | NOT NULL | Active policy flag |
| start_leave_policy_on | TIMESTAMP | | Policy start date |
| start_leave_policy_used_once | BOOLEAN | DEFAULT false | Pro-rata flag |

#### leave_type
| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| leave_type_id | SMALLINT | PK | Type identifier |
| leave_type | VARCHAR(50) | | Name (Time Off, Sick Leave) |
| leave_type_desc | VARCHAR | | Description |

#### leave_application_status
| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| leave_application_status_id | SMALLINT | PK | Status identifier |
| leave_application_status | VARCHAR(50) | | Status name |
| leave_application_status_desc | VARCHAR | | Description |

**Status Values:**
| ID | Status |
|----|--------|
| 1 | WAITING_APPROVAL |
| 2 | WAITING_CANCEL |
| 3 | APPROVED |
| 4 | REJECTED |
| 5 | CANCELLED |
| 6 | CANCELLED_AFTER_APPROVAL |
| 7 | APPLICATION_EXPIRED |
| 8 | CONSUMED |

---

### Meeting Tables

#### meeting
| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| meeting_id | BIGINT | PK, AUTO | Meeting identifier |
| meeting_number | VARCHAR(30) | UNIQUE | Display number |
| meeting_key | VARCHAR(255) | ENCRYPTED | Jitsi room key |
| title | VARCHAR(255) | NOT NULL | Meeting title |
| organizer_account_id | BIGINT | FK | Organizer |
| venue | VARCHAR(100) | ENCRYPTED | Location |
| agenda | VARCHAR(255) | ENCRYPTED | Agenda |
| start_date_time | TIMESTAMP | | Scheduled start |
| end_date_time | TIMESTAMP | | Scheduled end |
| actual_start_date_time | TIMESTAMP | | Actual start |
| actual_end_date_time | TIMESTAMP | | Actual end |
| duration | INTEGER | | Minutes |
| reminder_time | INTEGER | | Minutes before |
| minutes_of_meeting | VARCHAR(5000) | ENCRYPTED | MOM |
| meeting_type_indicator | INTEGER | | Meeting type |
| meeting_progress | VARCHAR | ENUM | Meeting status |
| is_cancelled | BOOLEAN | | Cancelled flag |
| team_id | BIGINT | FK | Team reference |
| bu_id | BIGINT | FK | BU reference |
| project_id | BIGINT | FK | Project reference |
| org_id | BIGINT | FK | Organization |
| fk_recurring_meeting_id | BIGINT | FK | Recurring parent |

#### attendee
| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| attendee_id | BIGINT | PK, AUTO | Attendee identifier |
| account_id | BIGINT | NOT NULL, FK | User account |
| rsvp_status | INTEGER | | 1=Yes, 2=No, 3=Maybe |
| actual_attendance | BOOLEAN | | Actually attended |
| fk_meeting_id | BIGINT | NOT NULL, FK | Meeting reference |

#### action_item
| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| action_item_id | BIGINT | PK, AUTO | Action item identifier |
| description | VARCHAR | ENCRYPTED | Description |
| assigned_to_account_id | BIGINT | FK | Assignee |
| due_date | DATE | | Due date |
| status | INTEGER | | Status |
| fk_meeting_id | BIGINT | NOT NULL, FK | Meeting reference |

---

### Notification Tables

#### notification
| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| notification_id | BIGINT | PK, AUTO | Notification identifier |
| notification_title | VARCHAR(1000) | ENCRYPTED | Title |
| notification_body | VARCHAR(2500) | ENCRYPTED | Body content |
| payload | VARCHAR(5000) | ENCRYPTED | JSON payload |
| category_id | INTEGER | FK | Category |
| task_number | VARCHAR(40) | | Task reference |
| meeting_id | BIGINT | FK | Meeting reference |
| leave_application_id | BIGINT | FK | Leave reference |
| is_updation | BOOLEAN | | Is update notification |
| tagged_account_ids | JSONB | | Tagged users |
| fk_notification_type_id | INTEGER | NOT NULL, FK | Type |
| fk_org_id | BIGINT | FK | Organization |
| fk_project_id | BIGINT | FK | Project |
| fk_team_id | BIGINT | FK | Team |
| fk_account_id | BIGINT | FK | Recipient |
| fk_notification_creator_account_id | BIGINT | FK | Creator |
| created_date_time | TIMESTAMP | AUTO | Creation timestamp |

---

### Geo-Fencing Tables

#### geofence
| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGINT | PK, AUTO | Fence identifier |
| org_id | BIGINT | NOT NULL | Organization |
| name | VARCHAR(120) | NOT NULL | Fence name |
| location_kind | VARCHAR | ENUM | OFFICE, REMOTE |
| site_code | VARCHAR | | Site code |
| tz | VARCHAR | | Timezone |
| center_lat | DOUBLE | NOT NULL | Latitude |
| center_lng | DOUBLE | NOT NULL | Longitude |
| radius_m | INTEGER | NOT NULL | Radius in meters |
| is_active | BOOLEAN | DEFAULT true | Active flag |
| created_by | BIGINT | FK | Creator |
| updated_by | BIGINT | FK | Updater |
| created_datetime | TIMESTAMP | AUTO | Creation timestamp |
| updated_datetime | TIMESTAMP | AUTO | Update timestamp |

#### attendance_day
| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGINT | PK, AUTO | Day identifier |
| org_id | BIGINT | NOT NULL | Organization |
| account_id | BIGINT | NOT NULL | Employee |
| date_key | DATE | NOT NULL | Attendance date |
| first_in_utc | TIMESTAMP | | First punch-in |
| last_out_utc | TIMESTAMP | | Last punch-out |
| worked_seconds | INTEGER | DEFAULT 0 | Total worked |
| break_seconds | INTEGER | DEFAULT 0 | Break time |
| status | VARCHAR | ENUM | PRESENT, ABSENT, etc. |
| anomalies | JSONB | | Anomaly details |

#### fence_assignment
| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGINT | PK, AUTO | Assignment identifier |
| fence_id | BIGINT | NOT NULL, FK | Fence reference |
| account_id | BIGINT | NOT NULL | Employee |
| effective_from | DATE | | Start date |
| effective_to | DATE | | End date |

#### punch_request
| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGINT | PK, AUTO | Request identifier |
| account_id | BIGINT | NOT NULL | Employee |
| request_type | VARCHAR | | PUNCH_IN, PUNCH_OUT |
| requested_time | TIMESTAMP | | Requested time |
| approver_account_id | BIGINT | FK | Approver |
| status | VARCHAR | | PENDING, APPROVED, REJECTED |
| reason | VARCHAR | | Request reason |

---

### GitHub Integration Tables

#### github_account
| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| github_account_id | BIGINT | PK, AUTO | Account identifier |
| org_id | BIGINT | NOT NULL | Organization |
| github_user_code | VARCHAR | NOT NULL | OAuth code |
| github_access_token | VARCHAR(1000) | NOT NULL, ENCRYPTED | Access token |
| github_user_name | VARCHAR | NOT NULL | GitHub username |
| is_linked | BOOLEAN | DEFAULT true | Link status |
| fk_user_id | BIGINT | NOT NULL, FK | User reference |

#### work_item_github_branch
| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGINT | PK, AUTO | Mapping identifier |
| task_id | BIGINT | FK | Task reference |
| branch_name | VARCHAR | | Branch name |
| repository_name | VARCHAR | | Repository |
| repository_owner | VARCHAR | | Repo owner |
| github_account_id | BIGINT | FK | GitHub account |

---

## Schema: chat (Chat Application)

### Core Chat Tables

#### chat.user
| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| account_id | BIGINT | PK | Account identifier |
| user_id | BIGINT | NOT NULL | TSe user ID |
| first_name | VARCHAR | ENCRYPTED | First name |
| last_name | VARCHAR | ENCRYPTED | Last name |
| middle_name | VARCHAR | ENCRYPTED | Middle name |
| org_id | BIGINT | NOT NULL | Organization |
| is_active | BOOLEAN | NOT NULL | Active status |
| email | VARCHAR | NOT NULL, ENCRYPTED | Email |
| is_org_admin | BOOLEAN | | Org admin flag |

#### chat.message
| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| message_id | BIGINT | PK, AUTO | Message identifier |
| sender_id | BIGINT | NOT NULL | Sender account |
| receiver_id | BIGINT | | Receiver (DM only) |
| group_id | BIGINT | FK | Group (group msg) |
| content | VARCHAR(8000) | ENCRYPTED | Message content |
| reply_id | BIGINT | FK | Parent message |
| is_delivered | BOOLEAN | DEFAULT false | Delivery status |
| is_read | BOOLEAN | DEFAULT false | Read status |
| is_edited | BOOLEAN | DEFAULT false | Edited flag |
| is_deleted | BOOLEAN | DEFAULT false | Soft delete |
| timestamp | TIMESTAMP | | Message time |
| task_attachment_id | BIGINT | | Task attachment ref |
| message_attachment_ids | VARCHAR | | Attachment IDs |

#### chat.group
| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| group_id | BIGINT | PK, AUTO | Group identifier |
| name | VARCHAR | NOT NULL, ENCRYPTED | Group name |
| description | VARCHAR | | Description |
| type | VARCHAR | NOT NULL | CUSTOM, SYSTEM_* |
| org_id | BIGINT | | Organization |
| last_message | VARCHAR | ENCRYPTED | Last message preview |
| last_message_sender_account_id | BIGINT | | Last message sender |
| last_message_id | BIGINT | | Last message ID |
| last_message_timestamp | TIMESTAMP | | Last message time |
| entity_type_id | BIGINT | | Linked entity type |
| entity_id | BIGINT | | Linked entity ID |
| group_icon_code | VARCHAR | | Icon enum |
| group_icon_color | VARCHAR | | Hex color |
| is_active | BOOLEAN | | Active flag |
| created_by_account_id | BIGINT | | Creator |
| created_date | TIMESTAMP | | Creation date |

#### chat.user_group (Composite PK)
| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| group_id | BIGINT | PK, FK | Group reference |
| account_id | BIGINT | PK, FK | Account reference |
| is_admin | BOOLEAN | DEFAULT false | Admin flag |
| is_deleted | BOOLEAN | DEFAULT false | Soft delete |

#### chat.message_user (Composite PK)
| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| message_id | BIGINT | PK, FK | Message reference |
| account_id | BIGINT | PK, FK | Account reference |
| is_delivered | BOOLEAN | DEFAULT false | Delivered flag |
| delivered_at | TIMESTAMP | | Delivery time |
| is_read | BOOLEAN | DEFAULT false | Read flag |
| read_at | TIMESTAMP | | Read time |
| reaction | VARCHAR | | Emoji reaction |

#### chat.message_attachment
| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| message_attachment_id | BIGINT | PK, AUTO | Attachment identifier |
| message_id | BIGINT | NOT NULL | Message reference |
| file_name | VARCHAR(500) | ENCRYPTED | File name |
| file_type | VARCHAR(100) | NOT NULL | MIME type |
| file_size | DOUBLE | NOT NULL | Size in bytes |
| file_content | BYTEA | NOT NULL | Binary content |
| file_status | CHAR(1) | NOT NULL | A=Active, D=Deleted |
| created_date_time | TIMESTAMP | AUTO | Upload time |
| deleted_date_time | TIMESTAMP | | Deletion time |

#### chat.message_stats
| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| message_id | BIGINT | PK | Message reference |
| group_id | BIGINT | | Group reference |
| group_size | INTEGER | | Total recipients |
| delivered_count | INTEGER | DEFAULT 1 | Delivered count |
| read_count | INTEGER | DEFAULT 1 | Read count |
| sender_id | BIGINT | | Sender account |

#### chat.user_group_event
| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| user_group_event_id | BIGINT | PK, AUTO | Event identifier |
| account_id | BIGINT | NOT NULL | User account |
| group_id | BIGINT | NOT NULL | Group reference |
| event_type | VARCHAR | NOT NULL | JOIN, LEAVE |
| occurred_at | TIMESTAMP | DEFAULT NOW | Event time |

#### chat.pinned_chats
| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| pin_or_favourite_id | BIGINT | PK, AUTO | Pin identifier |
| account_id | BIGINT | | User account |
| chat_type_id | BIGINT | | 1=USER, 2=GROUP |
| chat_id | BIGINT | | Entity ID |

#### chat.favourite_chats
| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| favourite_id | BIGINT | PK, AUTO | Favourite identifier |
| account_id | BIGINT | | User account |
| chat_type_id | BIGINT | | 1=USER, 2=GROUP |
| chat_id | BIGINT | | Entity ID |

---

## Entity Relationships

### Organization Hierarchy
```
Organization (org_id)
    │
    ├── BU (bu_id) [Many]
    │   │
    │   └── Project (project_id) [Many]
    │       │
    │       └── Team (team_id) [Many]
    │           │
    │           └── Task (task_id) [Many]
    │
    └── UserAccount (account_id) [Many]
        │
        └── User (user_id) [One]
```

### Task Relationships
```
Task
├── ParentTask (parent_task_id) [Optional]
├── ChildTasks (child_task_ids) [Many, JSON]
├── Epic (fk_epic_id) [Optional]
├── Sprint (sprint_id) [Optional]
├── Team (fk_team_id) [Required]
├── Assignee (fk_account_id) [Required]
├── Creator (fk_account_id_creator) [Required]
├── Mentors (fk_account_id_mentor_1, _2) [Optional]
├── Observers (fk_account_id_observer_1, _2) [Optional]
├── Labels [Many-to-Many via task_label]
├── Comments [One-to-Many]
├── Notes [One-to-Many]
├── Attachments [One-to-Many]
└── Dependencies (dependency_ids) [Many, JSON]
```

### Meeting Relationships
```
Meeting
├── Organizer (organizer_account_id) [One]
├── Team (team_id) [Optional]
├── Project (project_id) [Optional]
├── Attendees [One-to-Many]
├── ActionItems [One-to-Many]
├── MeetingNotes [One-to-Many]
├── Labels [Many-to-Many]
└── RecurringMeeting (fk_recurring_meeting_id) [Optional]
```

### Leave Relationships
```
LeavePolicy
├── Organization (org_id) [Required]
├── BU (bu_id) [Optional]
├── Project (project_id) [Optional]
├── Team (team_id) [Optional]
└── LeaveType (leave_type_id) [Required]

LeaveRemaining
├── Account (account_id) [Required]
├── Policy (leave_policy_id) [Required]
└── LeaveType (leave_type_id) [Required]

LeaveApplication
├── Applicant (account_id) [Required]
├── Approver (approver_account_id) [Required]
├── LeaveType (leave_type_id) [Required]
└── Status (leave_application_status_id) [Required]
```

---

## Important Enums & Constants

### StatType (Task Progress)
| Value | Code | Description |
|-------|------|-------------|
| 0 | DELAYED | Behind schedule |
| 1 | WATCHLIST | At risk |
| 2 | ONTRACK | On schedule |
| 3 | NOTSTARTED | Not begun |
| 4 | LATE_COMPLETION | Completed late |
| 5 | COMPLETED | Completed on time |

### MeetingStats
| Value | Description |
|-------|-------------|
| MEETING_SCHEDULED | Scheduled |
| MEETING_STARTED | In progress |
| MEETING_DELAYED | Started late |
| MEETING_ENDED | Ended |
| MEETING_OVER_RUN | Exceeded time |
| MEETING_COMPLETED | Finished |

### EntityTypes
| ID | Type |
|----|------|
| 1 | USER |
| 2 | ORG |
| 3 | BU |
| 4 | PROJECT |
| 5 | TEAM |
| 6 | TASK |
| 7 | MEETING |
| 8 | LEAVE |
| 9 | HOLIDAY |

### TaskTypes
| ID | Type |
|----|------|
| 1 | TASK |
| 2 | PARENT_TASK |
| 3 | CHILD_TASK |
| 4 | BUG_TASK |
| 5 | EPIC |
| 6 | INITIATIVE |
| 7 | RISK |
| 8 | PERSONAL_TASK |

### LeaveApplicationStatus
| ID | Status |
|----|--------|
| 1 | WAITING_APPROVAL |
| 2 | WAITING_CANCEL |
| 3 | APPROVED |
| 4 | REJECTED |
| 5 | CANCELLED |
| 6 | CANCELLED_AFTER_APPROVAL |
| 7 | APPLICATION_EXPIRED |
| 8 | CONSUMED |

### SprintStatus
| ID | Status |
|----|--------|
| 1 | CREATED |
| 2 | STARTED |
| 3 | COMPLETED |

### ChatTypes
| ID | Type |
|----|------|
| 1 | USER (DM) |
| 2 | CUSTOM (Group) |
| 3 | SYSTEM_ORG |
| 4 | SYSTEM_BU |
| 5 | SYSTEM_PROJ |
| 6 | SYSTEM_TEAM |

---

## Triggers, Cron Jobs & Retention

### Scheduled Jobs (via Notification_Reminders)

| Job | Schedule | Action |
|-----|----------|--------|
| `deleteOldNotificationsScheduler` | Daily midnight | Delete notifications older than threshold |
| `deleteAlerts` | Daily 23:50:59 | Delete old alerts |
| `leaveRemainingReset` | Jan 1st midnight | Reset annual leave balances |
| `leaveRemainingMonthlyUpdate` | Last day 23:58:59 | Update monthly leave accrual |
| `expireLeaveApplicationsSchedular` | Daily 23:58:59 | Expire pending applications |
| `changeLeaveStatusToConsumed` | Daily 3 AM | Mark approved leaves as consumed |

### Retention Logic

**Notifications:**
- Old notifications deleted via scheduled job
- Retention period configurable in TSe_Server

**Leave Applications:**
- Pending applications expire if not actioned
- Expiration checked daily at 23:58:59

**Task History:**
- All changes preserved indefinitely
- No automatic cleanup

**Chat Messages:**
- Messages soft-deleted (is_deleted flag)
- No automatic hard delete

### Database Maintenance

**Auto-vacuum:** PostgreSQL default
**Index Optimization:** Manual via REINDEX
**Statistics Update:** PostgreSQL auto-analyze

### Encryption at Rest

**Encrypted Columns (using DataEncryptionConverter):**
- User: email, name, personal info
- Task: title, description, notes
- Meeting: venue, agenda, MOM
- Leave: reason, address
- Chat: message content, file names

**Encryption Algorithm:** AES/CBC/PKCS5Padding
**Key Management:** Hardcoded in application.properties (security risk)

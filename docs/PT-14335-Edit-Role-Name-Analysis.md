# PT-14335: Analysis - Edit Role Name Per Organization

## Overview

This document provides a comprehensive analysis for implementing the feature that allows **Org Admins** to edit role names for their organization. The role ID and other fields will remain constant across all organizations, but the **role name can be customized per organization**.

---

## Current System Architecture

### RoleEnum (Constant Roles)

**Location:** `TSe_Server/src/main/java/com/tse/core_application/constants/RoleEnum.java`

The current system uses a `RoleEnum` that defines 30 fixed roles:

| Role ID | Enum Name | Default Role Name |
|---------|-----------|-------------------|
| 1 | TASK_BASIC_USER | Task Basic User |
| 2 | TASK_BASIC_TEAM_USER | Task Basic Team User |
| 3 | TASK_ESSENTIAL_TEAM_USER | Task Essential Team User |
| 4 | TASK_BASIC_ESSENTIAL_USER | Task Basic & Essential User |
| 5 | FORMAL_TEAM_BASIC_USER | Formal Team Basic User |
| 6 | FORMAL_TEAM_INTERMEDIATE_USER | Formal Team Intermediate User |
| 7 | FORMAL_TEAM_SENIOR_USER_LEVEL_1 | Formal Team Senior User Level 1 |
| 8 | FORMAL_TEAM_SENIOR_USER_LEVEL_2 | Formal Team Senior User Level 2 |
| 9 | FORMAL_TEAM_LEAD_LEVEL_1 | Formal Team Lead Level 1 |
| 10 | FORMAL_TEAM_LEAD_LEVEL_2 | Formal Team Lead Level 2 |
| 11 | TEAM_MANAGER_NON_SPRINT | Team Manager - non Sprint |
| 12 | TEAM_MANAGER_SPRINT | Team Manager - Sprint |
| 13 | TEAM_VIEWER | Team Viewer |
| 14 | PROJECT_MANAGER_NON_SPRINT | Project Manager - non Sprint |
| 15 | PROJECT_MANAGER_SPRINT | Project Manager - Sprint |
| 16 | PROJECT_MANAGER_NON_SPRINT_PROJECT | Project Manager - Non Sprint |
| 17 | PROJECT_MANAGER_SPRINT_PROJECT | Project Manager - Sprint |
| **91** | **PERSONAL_USER** | **Personal User** *(EXCLUDED)* |
| 101 | BACKUP_TEAM_ADMIN | Backup Team Admin |
| 102 | TEAM_ADMIN | Team Admin |
| 111 | BACKUP_PROJECT_ADMIN | Backup Project Admin |
| 112 | PROJECT_ADMIN | Project Admin |
| 113 | PROJECT_VIEWER | Project Viewer |
| 121 | BACKUP_BU_ADMIN | Backup BU Admin |
| 122 | BU_ADMIN | BU Admin |
| 123 | BU_VIEWER | BU Viewer |
| 131 | BACKUP_ORG_ADMIN | Backup Org Admin |
| 132 | ORG_ADMIN | Org Admin |
| 133 | ORG_VIEWER | Org Viewer |
| **900** | **SUPER_ADMIN** | **Super Admin** *(EXCLUDED)* |

### Roles to Exclude (Not Org-Specific)
- **Role ID 91 (PERSONAL_USER)**: Personal Team Actions - system-level role
- **Role ID 900 (SUPER_ADMIN)**: Super Admin - system-level role

---

## Current Role Entity

**Location:** `TSe_Server/src/main/java/com/tse/core_application/model/Role.java`

```java
@Entity
@Table(name = "role", schema = "tse")
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_id")
    private Integer roleId;

    @Column(name = "role_name", nullable = false, length = 50)
    private String roleName;

    @Column(name = "role_desc", length = 256)
    private String roleDesc;

    @CreationTimestamp
    @Column(name = "created_date_time", updatable = false, nullable = false)
    private Timestamp createdDateTime;

    @UpdateTimestamp
    @Column(name = "last_updated_date_time", insertable = false)
    private Timestamp lastUpdatedDateTime;
}
```

---

## Proposed Solution

### 1. New Database Table: `organization_role`

Create a new table to store organization-specific role names.

#### Table Structure

| Column Name | Data Type | Constraints | Description |
|-------------|-----------|-------------|-------------|
| `org_role_id` | BIGSERIAL | PRIMARY KEY | Auto-generated primary key |
| `org_id` | BIGINT | NOT NULL, FK to organization(org_id) | Organization ID |
| `role_id` | INTEGER | NOT NULL, FK to role(role_id) | Role ID (from RoleEnum) |
| `custom_role_name` | VARCHAR(100) | NOT NULL | Custom role name for this org |
| `created_date_time` | TIMESTAMP | NOT NULL, DEFAULT NOW() | Record creation timestamp |
| `updated_date_time` | TIMESTAMP | NULL | Last update timestamp |
| `created_by` | BIGINT | NULL | Account ID who created |
| `updated_by` | BIGINT | NULL | Account ID who last updated |

#### SQL Script to Create Table

```sql
CREATE TABLE IF NOT EXISTS tse.organization_role (
    org_role_id BIGSERIAL PRIMARY KEY,
    org_id BIGINT NOT NULL,
    role_id INTEGER NOT NULL,
    custom_role_name VARCHAR(100) NOT NULL,
    created_date_time TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_date_time TIMESTAMP WITHOUT TIME ZONE,
    created_by BIGINT,
    updated_by BIGINT,

    CONSTRAINT fk_org_role_org FOREIGN KEY (org_id)
        REFERENCES tse.organization(org_id) ON DELETE CASCADE,
    CONSTRAINT fk_org_role_role FOREIGN KEY (role_id)
        REFERENCES tse.role(role_id) ON DELETE CASCADE,
    CONSTRAINT uq_org_role UNIQUE (org_id, role_id)
);

-- Index for faster lookups
CREATE INDEX idx_org_role_org_id ON tse.organization_role(org_id);
CREATE INDEX idx_org_role_role_id ON tse.organization_role(role_id);
```

### 2. SQL Script to Populate Initial Data

This script will create entries for all organizations with default role names (excluding role IDs 91 and 900).

```sql
-- Insert organization-specific roles for all existing organizations
-- Excluding role_id 91 (PERSONAL_USER) and 900 (SUPER_ADMIN)

INSERT INTO tse.organization_role (org_id, role_id, custom_role_name, created_date_time)
SELECT
    o.org_id,
    r.role_id,
    r.role_name,  -- Use default role name initially
    NOW()
FROM tse.organization o
CROSS JOIN tse.role r
WHERE r.role_id NOT IN (91, 900)
ORDER BY o.org_id, r.role_id;
```

---

## Proposed New Entity: OrganizationRole

**Proposed Location:** `TSe_Server/src/main/java/com/tse/core_application/model/OrganizationRole.java`

```java
@Entity
@Table(name = "organization_role", schema = "tse")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "org_role_id")
    private Long orgRoleId;

    @Column(name = "org_id", nullable = false)
    private Long orgId;

    @Column(name = "role_id", nullable = false)
    private Integer roleId;

    @Column(name = "custom_role_name", nullable = false, length = 100)
    private String customRoleName;

    @Column(name = "created_date_time", updatable = false, nullable = false)
    private LocalDateTime createdDateTime;

    @Column(name = "updated_date_time")
    private LocalDateTime updatedDateTime;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", insertable = false, updatable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", insertable = false, updatable = false)
    private Role role;
}
```

---

## API Endpoints

### 1. Get All Roles for Organization

**Endpoint:** `GET /role/org/{orgId}/roles`

**Purpose:** Get list of all roles with organization-specific names for display in UI (role list page, user role assignment dropdowns, etc.)

**Response:**
```json
{
    "success": true,
    "data": [
        {
            "roleId": 1,
            "roleName": "Custom Task Basic User",
            "roleDesc": "Action is Task Basic Update",
            "isCustomName": true
        },
        {
            "roleId": 2,
            "roleName": "Task Basic Team User",
            "roleDesc": "Task Basic User role + Team Task View Action",
            "isCustomName": false
        }
    ]
}
```

**Authorization:** Any active user belonging to the organization

### 2. Edit Role Name

**Endpoint:** `PUT /role/org/{orgId}/role/{roleId}`

**Purpose:** Allow org admin to customize the role name for their organization

**Request Body:**
```json
{
    "customRoleName": "New Custom Role Name"
}
```

**Response:**
```json
{
    "success": true,
    "message": "Role name updated successfully",
    "data": {
        "roleId": 1,
        "roleName": "New Custom Role Name",
        "roleDesc": "Action is Task Basic Update"
    }
}
```

**Authorization:** Only ORG_ADMIN (132) or BACKUP_ORG_ADMIN (131)

### 3. Reset Role Name to Default (Optional)

**Endpoint:** `PUT /role/org/{orgId}/role/{roleId}/reset`

**Purpose:** Reset the role name back to default

**Response:**
```json
{
    "success": true,
    "message": "Role name reset to default",
    "data": {
        "roleId": 1,
        "roleName": "Task Basic User",
        "roleDesc": "Action is Task Basic Update"
    }
}
```

**Authorization:** Only ORG_ADMIN (132) or BACKUP_ORG_ADMIN (131)

---

## Places Where Role Name is Currently Sent in Responses

The following files currently send role names in API responses and **will need to be updated** to fetch organization-specific role names:

### 1. RoleService
**Location:** `TSe_Server/src/main/java/com/tse/core_application/service/Impl/RoleService.java`

| Method | Line | Current Behavior | Required Change |
|--------|------|------------------|-----------------|
| `getRoles()` | 54 | `roleEnum.getRoleName()` | Fetch from `organization_role` table based on orgId |
| `getTeamRoles()` | 79 | `role.getRoleName()` | Fetch from `organization_role` table based on orgId |

### 2. AccessDomainService
**Location:** `TSe_Server/src/main/java/com/tse/core_application/service/Impl/AccessDomainService.java`

| Method | Line | Current Behavior | Required Change |
|--------|------|------------------|-----------------|
| `getEmailFirstNameLastNameRoleList()` | 255-257 | `roleRepository.findRoleNameByRoleId()` | Fetch from `organization_role` using team's orgId |

### 3. NotificationService
**Location:** `TSe_Server/src/main/java/com/tse/core_application/service/Impl/NotificationService.java`

| Method | Line | Current Behavior | Required Change |
|--------|------|------------------|-----------------|
| Role notification | 2326 | `roleRepository.findRoleNameByRoleId()` | Fetch from `organization_role` using relevant orgId |

### 4. AuditService
**Location:** `TSe_Server/src/main/java/com/tse/core_application/service/Impl/AuditService.java`

| Method | Line | Current Behavior | Required Change |
|--------|------|------------------|-----------------|
| `auditForAddedTeamMember()` | 248-249 | `roleRepository.findRoleNameByRoleId()` | Fetch from `organization_role` using team's orgId |
| `auditForAddedProjectMember()` | 263-264 | `roleRepository.findRoleNameByRoleId()` | Fetch from `organization_role` using project's orgId |
| `auditForEditedTeamMembers()` | 278-279 | `roleRepository.findRoleNameByRoleId()` | Fetch from `organization_role` using team's orgId |
| `auditForEditedProjectMember()` | 292-293 | `roleRepository.findRoleNameByRoleId()` | Fetch from `organization_role` using project's orgId |

### 5. TaskServiceImpl
**Location:** `TSe_Server/src/main/java/com/tse/core_application/service/Impl/TaskServiceImpl.java`

| Method | Line | Current Behavior | Required Change |
|--------|------|------------------|-----------------|
| Task user role | 8196 | `RoleEnum.getRoleNameById()` | Fetch from `organization_role` using task's orgId |

### 6. RoleController
**Location:** `TSe_Server/src/main/java/com/tse/core_application/controller/RoleController.java`

| Method | Line | Current Behavior | Required Change |
|--------|------|------------------|-----------------|
| `getAllRoles()` | 40-66 | Returns roles from RoleEnum | Add orgId parameter and fetch org-specific names |
| `getTeamRoles()` | 70-98 | Returns team roles | Add orgId parameter and fetch org-specific names |

---

## Access Domain - No Changes Required

**Confirmed:** The `access_domain` table stores only `role_id` (Integer), NOT role name.

**Location:** `TSe_Server/src/main/java/com/tse/core_application/model/AccessDomain.java`

```java
@Column(name = "role_id")
private Integer roleId;  // Only role ID is stored
```

Since access control is based on `role_id` (which remains constant across organizations), **no changes are required in the access domain logic**.

---

## Proposed New Repository: OrganizationRoleRepository

**Proposed Location:** `TSe_Server/src/main/java/com/tse/core_application/repository/OrganizationRoleRepository.java`

```java
@Repository
public interface OrganizationRoleRepository extends JpaRepository<OrganizationRole, Long> {

    // Find custom role name for specific org and role
    Optional<OrganizationRole> findByOrgIdAndRoleId(Long orgId, Integer roleId);

    // Find all roles for an organization
    List<OrganizationRole> findByOrgIdOrderByRoleId(Long orgId);

    // Check if custom role exists
    boolean existsByOrgIdAndRoleId(Long orgId, Integer roleId);

    // Find role name projection
    @Query("SELECT o.customRoleName FROM OrganizationRole o WHERE o.orgId = :orgId AND o.roleId = :roleId")
    Optional<String> findCustomRoleNameByOrgIdAndRoleId(@Param("orgId") Long orgId, @Param("roleId") Integer roleId);
}
```

---

## Helper Method for Fetching Role Name

Create a utility method that can be used across all services:

```java
public String getRoleNameForOrg(Long orgId, Integer roleId) {
    // First try to get custom name from organization_role table
    Optional<String> customName = organizationRoleRepository
        .findCustomRoleNameByOrgIdAndRoleId(orgId, roleId);

    if (customName.isPresent()) {
        return customName.get();
    }

    // Fallback to default role name from RoleEnum
    return RoleEnum.getRoleNameById(roleId);
}
```

---

## Summary of Files to Create (During Implementation)

| File | Location | Purpose |
|------|----------|---------|
| `OrganizationRole.java` | `/model/` | Entity for organization-specific roles |
| `OrganizationRoleRepository.java` | `/repository/` | Repository for OrganizationRole |
| `OrganizationRoleService.java` | `/service/` | Service for role name management |
| `OrganizationRoleController.java` | `/controller/` | Controller for role name APIs |
| `EditRoleNameRequest.java` | `/dto/` | Request DTO for editing role name |
| `OrgRoleResponse.java` | `/dto/` | Response DTO for org role details |
| `V{version}__create_organization_role_table.sql` | `/resources/db/migration/` | Flyway migration script |

---

## Summary of Files to Modify (During Implementation)

| File | Modification |
|------|--------------|
| `RoleService.java` | Update methods to fetch org-specific role names |
| `RoleController.java` | Add orgId parameter to existing endpoints |
| `AccessDomainService.java` | Update team member role name fetching |
| `NotificationService.java` | Update notification role name fetching |
| `AuditService.java` | Update audit message role name fetching |
| `TaskServiceImpl.java` | Update task role name fetching |

---

## Authorization Rules Summary

| Action | Allowed Roles |
|--------|---------------|
| View roles for org | Any user of that organization |
| Edit role name | ORG_ADMIN (132), BACKUP_ORG_ADMIN (131) |
| Reset role name | ORG_ADMIN (132), BACKUP_ORG_ADMIN (131) |

---

## Edge Cases to Handle (During Implementation)

1. **New Organization Created**: Automatically create entries in `organization_role` table with default role names
2. **Role Name Validation**:
   - Cannot be empty
   - Maximum 100 characters
   - Consider if duplicate names within same organization should be allowed
3. **Concurrent Updates**: Handle optimistic locking for role name updates
4. **Cache Invalidation**: If caching is used, invalidate when role name is updated

---

## Testing Scenarios (During Implementation)

1. Verify default role names are shown for new organizations
2. Verify org admin can edit role name
3. Verify backup org admin can edit role name
4. Verify non-admin users cannot edit role name
5. Verify edited role names appear in team member lists
6. Verify edited role names appear in project member lists
7. Verify edited role names appear in audit logs
8. Verify edited role names appear in notifications
9. Verify role ID 91 and 900 are not editable
10. Verify reset to default works correctly

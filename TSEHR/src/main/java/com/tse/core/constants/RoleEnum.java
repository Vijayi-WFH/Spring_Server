package com.tse.core.constants;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

    @Getter
    public enum RoleEnum {
        TASK_BASIC_USER(1, "Task Basic User", "Action is Task Basic Update"),
        TASK_BASIC_TEAM_USER(2, "Task Basic Team User", "Task Basic User role + Team Task View Action"),
        TASK_ESSENTIAL_TEAM_USER(3, "Task Essential Team User", "Task Basic Team User role + Task Essential Update Action"),
        TASK_BASIC_ESSENTIAL_USER(4, "Task Basic & Essential User", "Task Basic User role + Task Essential Update Action"),
        FORMAL_TEAM_BASIC_USER(5, "Formal Team Basic User", "Task Basic Team User + All Task Basic Update Action"),
        FORMAL_TEAM_INTERMEDIATE_USER(6, "Formal Team Intermediate User", "Formal Team Basic User + Task Essential Update Action"),
        FORMAL_TEAM_SENIOR_USER_LEVEL_1(7, "Formal Team Senior User Level 1", "Formal Team Intermediate User + Task Add Without Assignment Action + Self-created Task Self-Assignment Action"),
        FORMAL_TEAM_SENIOR_USER_LEVEL_2(8, "Formal Team Senior User Level 2", "Formal Team Senior User Level 1 User + Self-created Task Assignment to Others Action"),
        FORMAL_TEAM_LEAD_LEVEL_1(9, "Formal Team Lead Level 1", "Formal Team Senior User Level 2 User + Task Assignment To Anyone Action"),
        FORMAL_TEAM_LEAD_LEVEL_2(10, "Formal Team Lead Level 2", "Formal Team Lead Level 1 User + All Task Essential Update Action"),
        TEAM_MANAGER_NON_SPRINT(11, "Team Manager - non Sprint", "Formal Team Lead Level 2 User + Task Delete Action"),
        TEAM_MANAGER_SPRINT(12, "Team Manager - Sprint", "Formal Team Lead Level 2 + Task Delete Action + Change Task Sprint Action"),
        TEAM_VIEWER(13, "Team Viewer", "Team Task View Action"),
        PROJECT_MANAGER_NON_SPRINT(14, "Project Manager - non Sprint", "Team Manager - non Sprint User + Change Task Team Action"),
        PROJECT_MANAGER_SPRINT(15, "Project Manager - Sprint", "Team Manager - Sprint User + Change Task Team Action"),
        BACKUP_TEAM_ADMIN(101, "Backup Team Admin", "Team Edit Action + Add users to the team Action + Remove users from the team Action"),
        TEAM_ADMIN(102, "Team Admin", "Backup Team Admin Role + Team Remove Action"),
        BACKUP_PROJECT_ADMIN(111, "Backup Project Admin", "Team Add Action + Team Remove Action + Project Edit Action + Project View Action"),
        PROJECT_ADMIN(112, "Project Admin", "Backup Project Admin Role + Project Remove Action"),
        PROJECT_VIEWER(113, "Project Viewer", "Project View"),
        BACKUP_BU_ADMIN(121, "Backup BU Admin", "Project Add Action + Project Remove Action + BU Edit Action + BU View Action"),
        BU_ADMIN(122, "BU Admin", "Backup BU Admin Role + BU Remove Action"),
        BU_VIEWER(123, "BU Viewer", "BU View Action"),
        BACKUP_ORG_ADMIN(131, "Backup Org Admin", "BU Add Action + BU Remove Action + Org Edit Action + Org View Action"),
        ORG_ADMIN(132, "Org Admin", "Backup Org Admin Role + Org Remove Action"),
        ORG_VIEWER(133, "Org Viewer", "Org View Action"),
        PERSONAL_USER(91, "Personal User", "Personal Team Actions");

        private final int roleId;
        private final String roleName;
        private final String roleDesc;

        RoleEnum(int roleId, String roleName, String roleDesc) {
            this.roleId = roleId;
            this.roleName = roleName;
            this.roleDesc = roleDesc;
        }

        public static RoleEnum valueOf(int roleId) {
            for (RoleEnum roleEnum : RoleEnum.values()) {
                if (roleEnum.getRoleId() == roleId) {
                    return roleEnum;
                }
            }
            throw new IllegalArgumentException("No role found for id: " + roleId);
        }

        // Method to get team roles
        public static List<RoleEnum> getTeamRoles() {
            return Arrays.stream(RoleEnum.values())
                    .filter(role -> (role.roleId >= 1 && role.roleId <= 102))
                    .collect(Collectors.toList());
        }

        // Method to get project roles
        public static List<RoleEnum> getProjectRoles() {
            return Arrays.stream(RoleEnum.values())
                    .filter(role -> role.roleId >= 111 && role.roleId <= 113)
                    .collect(Collectors.toList());
        }

        // Method to get BU roles
        public static List<RoleEnum> getBURoles() {
            return Arrays.stream(RoleEnum.values())
                    .filter(role -> role.roleId >= 121 && role.roleId <= 123)
                    .collect(Collectors.toList());
        }

        // Method to get Org roles
        public static List<RoleEnum> getOrgRoles() {
            return Arrays.stream(RoleEnum.values())
                    .filter(role -> role.roleId >= 131 && role.roleId <= 133)
                    .collect(Collectors.toList());
        }

        public static List<RoleEnum> getAllRoles() {
            return Arrays.asList(RoleEnum.values());
        }

    }

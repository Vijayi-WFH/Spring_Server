package com.tse.core_application.service.Impl;

import com.tse.core_application.constants.Constants;
import com.tse.core_application.constants.RoleEnum;
import com.tse.core_application.custom.model.RoleIdRoleNameRoleDesc;
import com.tse.core_application.exception.NoDataFoundException;
import com.tse.core_application.handlers.CustomResponseHandler;
import com.tse.core_application.handlers.StackTraceHandler;
import com.tse.core_application.model.Role;
import com.tse.core_application.repository.RoleRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RoleService {

    private static final Logger logger = LogManager.getLogger(RoleService.class.getName());

    @Autowired
    private RoleRepository roleRepository;

    public List<RoleIdRoleNameRoleDesc> getRoles(Integer entityType) {
        List<RoleIdRoleNameRoleDesc> roleIdRoleNameRoleDescList = new ArrayList<>();
        List<RoleEnum> roles = new ArrayList<>();

        if (entityType == null) {
            roles = RoleEnum.getAllRoles();
        } else {
            switch (entityType) {
                case com.tse.core_application.model.Constants.EntityTypes.TEAM:
                    roles = RoleEnum.getTeamRoles(); break;
                case com.tse.core_application.model.Constants.EntityTypes.ORG:
                    roles = RoleEnum.getOrgRoles(); break;
                case com.tse.core_application.model.Constants.EntityTypes.PROJECT:
                    roles = RoleEnum.getProjectRoles(); break;
                case com.tse.core_application.model.Constants.EntityTypes.BU:
                    roles = RoleEnum.getBURoles(); break;
                default:
                    throw new IllegalArgumentException("Invalid Entity Type");
            }
        }

        for (RoleEnum roleEnum : roles) {
            RoleIdRoleNameRoleDesc roleIdRoleNameRoleDesc = new RoleIdRoleNameRoleDesc();
            roleIdRoleNameRoleDesc.setRoleId(roleEnum.getRoleId());
            roleIdRoleNameRoleDesc.setRoleName(roleEnum.getRoleName());
            roleIdRoleNameRoleDesc.setRoleDesc(roleEnum.getRoleDesc());
            roleIdRoleNameRoleDescList.add(roleIdRoleNameRoleDesc);
        }

        return roleIdRoleNameRoleDescList;
    }

    public ResponseEntity<Object> getAllRolesFormattedResponse(List<RoleIdRoleNameRoleDesc> roles) {
        if(roles.isEmpty()) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(new NoDataFoundException());
            logger.error("No roles found. ", new Throwable(allStackTraces));
            ThreadContext.clearMap();
            throw new NoDataFoundException();
        } else {
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, roles);
        }
    }

    public List<RoleIdRoleNameRoleDesc> getTeamRoles() {
        List<RoleIdRoleNameRoleDesc> roleIdRoleNameRoleDescList = new ArrayList<>();
        List<Role> rolesDb = roleRepository.findRoleForTeam(com.tse.core_application.model.Constants.TEAM_ROLE_IDS);
        for(Role role: rolesDb) {
            RoleIdRoleNameRoleDesc roleIdRoleNameRoleDesc = new RoleIdRoleNameRoleDesc();
            roleIdRoleNameRoleDesc.setRoleId(role.getRoleId());
            roleIdRoleNameRoleDesc.setRoleName(role.getRoleName());
            roleIdRoleNameRoleDesc.setRoleDesc(role.getRoleDesc());
            roleIdRoleNameRoleDescList.add(roleIdRoleNameRoleDesc);
        }
        return roleIdRoleNameRoleDescList;
    }
}

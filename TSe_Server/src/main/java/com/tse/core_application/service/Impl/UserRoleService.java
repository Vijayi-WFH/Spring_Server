package com.tse.core_application.service.Impl;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.tse.core_application.custom.model.RoleIdInUserRoleRepository;
import com.tse.core_application.model.UserRole;
import com.tse.core_application.repository.UserRoleRepository;

@Service
public class UserRoleService {

    @Autowired
    private UserRoleRepository userRoleRepository;

    // ---> to get general roles of user by userId
    public List<RoleIdInUserRoleRepository> getGeneralRoleIdByUserId(long userid) {
        List<RoleIdInUserRoleRepository> userRole = userRoleRepository.getRoleIdByUserId(userid);
        return userRole;
    }

    // ---> to confirm user's general role by roleName and accountId
    public Boolean isInRole(String rolename, long accountid) {
        Optional<UserRole> userRole = userRoleRepository.getUserRoleByRoleNameAndAccountId(rolename, accountid);
        if (userRole.isPresent()) {
            return true;
        } else {
            return false;
        }
    }

    // ---> to confirm user's general role by its roleId and accountId
    public Boolean isInRole(int roleid, long accountid) {
        Optional<UserRole> userRole = userRoleRepository.findByRoleIdAndAccountId(roleid, accountid);
        if (userRole.isPresent()) {
            return true;
        } else {
            return false;
        }
    }

}

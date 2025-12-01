package com.tse.core_application.repository;

import com.tse.core_application.custom.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import com.tse.core_application.model.Role;
import java.util.List;

@Repository
public interface RoleRepository extends JpaRepository<Role, Integer>{

    RoleId findRoleIdByRoleName(String roleName);

    @Query("select new com.tse.core_application.custom.model.RoleName (r.roleName ) from Role r inner join r.accessDomains a where a.accountId=:accountId")
    RoleName getRoleNameByAccountId(Long accountId);

    RoleName findRoleNameByRoleId(Integer roleId);

    Role findRoleByRoleName(String roleName);

    @Query("select r from Role r where r.roleId in :teamRoleIds")
    List<Role> findRoleForTeam(List<Integer> teamRoleIds);

    Role findRoleByRoleId(Integer roleId);
}

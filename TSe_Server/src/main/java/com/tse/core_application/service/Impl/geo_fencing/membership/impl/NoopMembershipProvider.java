package com.tse.core_application.service.Impl.geo_fencing.membership.impl;

import com.tse.core_application.model.Constants;
import com.tse.core_application.repository.AccessDomainRepository;
import com.tse.core_application.repository.ProjectRepository;
import com.tse.core_application.repository.TeamRepository;
import com.tse.core_application.service.Impl.geo_fencing.membership.MembershipProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * No-operation implementation of MembershipProvider.
 * Returns empty lists for teams and projects.
 * Used as default until real directory/membership system is integrated.
 */
@Service
public class NoopMembershipProvider implements MembershipProvider {

    @Autowired
    private AccessDomainRepository accessDomainRepository;

    @Autowired
    private TeamRepository teamRepository;
    @Autowired
    private ProjectRepository projectRepository;

    @Override
    public List<Long> listTeamsForUser(long orgId, long accountId) {
        Set<Long> teamIdList = accessDomainRepository.findEntityIdByAccountIdAndEntityTypeId(Set.of(accountId), true, Constants.EntityTypes.TEAM);
        if (teamIdList != null) {
            return new ArrayList<>(teamIdList);
        }
        return Collections.emptyList();
    }

    @Override
    public List<Long> listProjectsForUser(long orgId, long accountId, List<Long> teamIdList) {
        Set<Long> projectIds = new HashSet<>();
        List<Long> projectIdListByTeam = teamRepository.findFkProjectIdProjectIdByTeamIds(teamIdList);
        Set<Long> projectIdListByAccessDomain = accessDomainRepository.findEntityIdByAccountIdAndEntityTypeId(Set.of(accountId), true, Constants.EntityTypes.PROJECT);
        if (projectIdListByTeam != null) {
            projectIds.addAll(new HashSet<>(projectIdListByTeam));
        }
        if (projectIdListByAccessDomain != null) {
            projectIds.addAll(projectIdListByAccessDomain);
        }

        return new ArrayList<>(projectIds);
    }

    @Override
    public boolean orgExists(long orgId) {
        return true;
    }
}

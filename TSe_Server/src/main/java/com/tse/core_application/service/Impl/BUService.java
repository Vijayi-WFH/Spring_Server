package com.tse.core_application.service.Impl;

import com.tse.core_application.constants.Constants;
import com.tse.core_application.custom.model.AccountId;
import com.tse.core_application.custom.model.OrgId;
import com.tse.core_application.model.Organization;
import com.tse.core_application.model.BU;
import com.tse.core_application.repository.AccessDomainRepository;
import com.tse.core_application.repository.BURepository;
import com.tse.core_application.repository.ProjectRepository;
import com.tse.core_application.repository.TeamRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BUService {

    @Autowired
    private BURepository buRepository;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private AccessDomainRepository accessDomainRepository;
    @Autowired
    private TeamRepository teamRepository;

    public List<BU> getBUByOrgId(Long orgId) {
        List<BU> allBu = buRepository.findByOrgId(orgId);
        return allBu;
    }

    public BU getBuByBuId(Long buId){
        return buRepository.findByBuId(buId);
    }

    public BU addBUByOrganization(Organization organization) {
        BU addedBU = null;
        if (organization != null) {
            BU buToAdd = new BU();
            buToAdd.setOrgId(organization.getOrgId());
            String buName = Constants.BU_NAME;
            buToAdd.setBuName(buName);
            addedBU = buRepository.save(buToAdd);
        }
        return addedBU;
    }

    public List<BU> getAllBUsByBUIds(List<Long> buIds) {
        List<BU> allBus = new ArrayList<>();
        if (!buIds.isEmpty()) {
            allBus = buRepository.findByBuIdIn(buIds);
        }
        return allBus;
    }

    /**
     * This method will find the orgId for the given buId.
     *
     * @param buId The buId for which the orgId has to be found.
     * @return Long value (i.e. orgId)
     */
    public Long getOrgIdByBUId(Long buId) {
        OrgId foundOrgId = buRepository.findOrgIdBybuId(buId);
        return foundOrgId.getOrgId();
    }

    /**
     * This method will find all the BU for the given list of orgIds.
     *
     * @param orgIds the list of orgIds.
     * @return List<BU>.
     */
    public List<BU> getAllBUByOrgIds(List<Long> orgIds) {
        return buRepository.findByOrgIdIn(orgIds);
    }

    /** returns a list of accountIds of BU members */
    public List<AccountId> getBuMembersAccountIdList(Long buId) {
        List<AccountId> accountIdListBu = accessDomainRepository.findDistinctAccountIdByEntityTypeIdAndEntityIdAndIsActive(com.tse.core_application.model.Constants.EntityTypes.BU, buId, true);
        List<Long> projectIdList = projectRepository.findProjectIdsByBuId(buId);
        List<Long> teamIdList = teamRepository.findTeamIdsByFkProjectIdProjectIdIn(projectIdList);
        List<AccountId> teamAccountIds = accessDomainRepository.findDistinctAccountIdByEntityTypeIdAndEntityIdInAndIsActive(com.tse.core_application.model.Constants.EntityTypes.TEAM, teamIdList, true);
        List<AccountId> projectAccountIds = accessDomainRepository.findDistinctAccountIdByEntityTypeIdAndEntityIdInAndIsActive(com.tse.core_application.model.Constants.EntityTypes.PROJECT, projectIdList, true);
        // Combine the lists
        List<AccountId> combinedList = new ArrayList<>(accountIdListBu);
        combinedList.addAll(teamAccountIds);
        combinedList.addAll(projectAccountIds);

        Set<AccountId> distinctAccountIds = new HashSet<>(combinedList);

        return new ArrayList<>(distinctAccountIds);
    }

    public List<AccountId> getAllBuMembersAccountIdList(Long buId) {
        List<AccountId> accountIdListBu = accessDomainRepository.findDistinctAccountIdByEntityTypeIdAndEntityId(com.tse.core_application.model.Constants.EntityTypes.BU, buId);
        List<Long> projectIdList = projectRepository.findProjectIdsByBuId(buId);
        List<Long> teamIdList = teamRepository.findTeamIdsByFkProjectIdProjectIdIn(projectIdList);
        List<AccountId> teamAccountIds = accessDomainRepository.findDistinctAccountIdByEntityTypeIdAndEntityIdIn(com.tse.core_application.model.Constants.EntityTypes.TEAM, teamIdList);
        List<AccountId> projectAccountIds = accessDomainRepository.findDistinctAccountIdByEntityTypeIdAndEntityIdIn(com.tse.core_application.model.Constants.EntityTypes.PROJECT, projectIdList);
        // Combine the lists
        List<AccountId> combinedList = new ArrayList<>(accountIdListBu);
        combinedList.addAll(teamAccountIds);
        combinedList.addAll(projectAccountIds);

        Set<AccountId> distinctAccountIds = new HashSet<>(combinedList);

        return new ArrayList<>(distinctAccountIds);
    }
}

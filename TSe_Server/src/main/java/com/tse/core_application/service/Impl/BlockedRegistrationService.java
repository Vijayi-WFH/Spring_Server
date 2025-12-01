package com.tse.core_application.service.Impl;

import com.tse.core_application.dto.super_admin.BlockedRegistrationRequest;
import com.tse.core_application.exception.ValidationFailedException;
import com.tse.core_application.model.BlockedRegistration;
import com.tse.core_application.model.Constants;
import com.tse.core_application.model.Organization;
import com.tse.core_application.model.UserAccount;
import com.tse.core_application.repository.AccessDomainRepository;
import com.tse.core_application.repository.BlockedRegistrationRepository;
import com.tse.core_application.repository.OrganizationRepository;
import com.tse.core_application.repository.UserAccountRepository;
import com.tse.core_application.utils.CommonUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class BlockedRegistrationService {

    private static final Logger logger = LogManager.getLogger(BlockedRegistrationService.class.getName());

    @Autowired
    private BlockedRegistrationRepository blockedRegistrationRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private AccessDomainRepository accessDomainRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private AuditService auditService;

    public BlockedRegistration addBlockedUser(BlockedRegistrationRequest blockedRegistrationRequest, String accountIds) {
        BlockedRegistration blockedRegistration = new BlockedRegistration();
        List<Long> headerAccountIds = CommonUtils.convertToLongList(accountIds);
        UserAccount userAccount = userAccountRepository.findByAccountIdInAndOrgIdAndIsActive(headerAccountIds, Constants.OrgIds.PERSONAL.longValue(), true);
        validateManageRegistrationAction(userAccount.getAccountId());
        normalizeBlockedUserRequest(blockedRegistrationRequest);
        if (blockedRegistrationRepository.existsByEmailAndOrganizationNameAndIsDeleted(blockedRegistrationRequest.getEmail(), blockedRegistration.getOrganizationName(), false)) {
            throw new IllegalStateException("User already registered as exceptional user");
        }
        Optional<Organization> organizationOptional = organizationRepository.findByOrganizationName(blockedRegistrationRequest.getOrganizationName());
        if (organizationOptional.isEmpty()) {
            throw new IllegalStateException("No organization with name " + blockedRegistrationRequest.getOrganizationName() + " exist.");
        }
        if (userAccountRepository.existsByEmailAndOrgIdAndIsActive(blockedRegistrationRequest.getEmail(), organizationOptional.get().getOrgId(), true)) {
            throw new IllegalStateException("User already registered in organization");
        }
        BlockedRegistration alreadyExist = blockedRegistrationRepository.findByEmailAndOrganizationNameAndIsDeleted (blockedRegistrationRequest.getEmail(), blockedRegistrationRequest.getOrganizationName(), true);
        if (alreadyExist != null) {
            blockedRegistration = alreadyExist;
            blockedRegistration.setIsDeleted(false);
            blockedRegistration.setModifiedByAccountId(userAccount);
        }
        CommonUtils.copyNonNullProperties(blockedRegistrationRequest, blockedRegistration);
        blockedRegistration.setCreatedByAccountId(userAccount);
        BlockedRegistration savedRegistration = blockedRegistrationRepository.save(blockedRegistration);
        auditService.auditForBlockedRegistration(userAccount, savedRegistration, Constants.AuditStatusEnum.ADD);
        return savedRegistration;
    }

    public BlockedRegistration removeBlockedUser (Long blockedRegistrationId, String accountIds) {
        List<Long> headerAccountIds = CommonUtils.convertToLongList(accountIds);
        UserAccount userAccount = userAccountRepository.findByAccountIdInAndOrgIdAndIsActive(headerAccountIds, Constants.OrgIds.PERSONAL.longValue(), true);
        validateManageRegistrationAction(userAccount.getAccountId());
        BlockedRegistration blockedRegistration = blockedRegistrationRepository.findByBlockedRegistrationId(blockedRegistrationId);
        blockedRegistration.setIsDeleted(true);
        blockedRegistration.setModifiedByAccountId(userAccount);
        auditService.auditForBlockedRegistration(userAccount, blockedRegistration, Constants.AuditStatusEnum.REMOVE);
        return blockedRegistrationRepository.save(blockedRegistration);
    }

    private void normalizeBlockedUserRequest (BlockedRegistrationRequest blockedRegistrationRequest) {
        blockedRegistrationRequest.setEmail(blockedRegistrationRequest.getEmail().toLowerCase());
    }

    public List<BlockedRegistration> getAllBlockedRegistrationList (String accountIds) {
        List<Long> headerAccountIds = CommonUtils.convertToLongList(accountIds);
        Long accountId = userAccountRepository.findByAccountIdInAndOrgIdAndIsActive(headerAccountIds, Constants.OrgIds.PERSONAL.longValue(), true).getAccountId();
        validateManageRegistrationAction(accountId);
        return blockedRegistrationRepository.findAll();
    }

    public List<BlockedRegistration> getAllActiveBlockedRegistrationList (String accountIds) {
        List<Long> headerAccountIds = CommonUtils.convertToLongList(accountIds);
        Long accountId = userAccountRepository.findByAccountIdInAndOrgIdAndIsActive(headerAccountIds, Constants.OrgIds.PERSONAL.longValue(), true).getAccountId();
        validateManageRegistrationAction(accountId);
        return blockedRegistrationRepository.findByIsDeleted(false);
    }

    private void validateManageRegistrationAction (Long accountId) {
        if (!accessDomainRepository.findUserRoleInEntity(Constants.EntityTypes.ORG, Constants.OrgIds.PERSONAL.longValue(), accountId, true, Constants.ActionId.MANAGE_REGISTRATION)) {
            throw new ValidationFailedException("User do not have action to manage registration for blocked users");
        }
    }
}

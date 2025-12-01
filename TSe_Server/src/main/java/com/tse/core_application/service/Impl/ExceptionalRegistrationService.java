package com.tse.core_application.service.Impl;

import com.tse.core_application.dto.super_admin.ExceptionalRegistrationRequest;
import com.tse.core_application.exception.ValidationFailedException;
import com.tse.core_application.model.Constants;
import com.tse.core_application.model.ExceptionalRegistration;
import com.tse.core_application.model.UserAccount;
import com.tse.core_application.repository.AccessDomainRepository;
import com.tse.core_application.repository.ExceptionalRegistrationRepository;
import com.tse.core_application.repository.UserAccountRepository;
import com.tse.core_application.utils.CommonUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class ExceptionalRegistrationService {

    private static final Logger logger = LogManager.getLogger(ExceptionalRegistrationService.class.getName());

    @Autowired
    private ExceptionalRegistrationRepository exceptionalRegistrationRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private AccessDomainRepository accessDomainRepository;

    @Autowired
    private AuditService auditService;

    public ExceptionalRegistration addExceptionalUser (ExceptionalRegistrationRequest exceptionalRegistrationRequest, String accountIds) {
        ExceptionalRegistration exceptionalRegistration = new ExceptionalRegistration();
        List<Long> headerAccountIds = CommonUtils.convertToLongList(accountIds);
        UserAccount userAccount = userAccountRepository.findByAccountIdInAndOrgIdAndIsActive(headerAccountIds, Constants.OrgIds.PERSONAL.longValue(), true);
        validateManageRegistrationAction(userAccount.getAccountId());
        normalizeExceptionalUserRequest(exceptionalRegistrationRequest);
        if (exceptionalRegistrationRepository.existsByEmailAndIsDeleted(exceptionalRegistrationRequest.getEmail(), false)) {
            throw new IllegalStateException("User already registered as exceptional user");
        }
        CommonUtils.copyNonNullProperties(exceptionalRegistrationRequest, exceptionalRegistration);
        exceptionalRegistration.setCreatedByAccountId(userAccount);
        ExceptionalRegistration savedRegistration = exceptionalRegistrationRepository.save(exceptionalRegistration);
        auditService.auditForExceptionalRegistration(userAccount, savedRegistration, Constants.AuditStatusEnum.ADD);
        return savedRegistration;
    }

    public ExceptionalRegistration removeExceptionalUser (Long exceptionalRegistrationId, String accountIds) {
        List<Long> headerAccountIds = CommonUtils.convertToLongList(accountIds);
        UserAccount userAccount = userAccountRepository.findByAccountIdInAndOrgIdAndIsActive(headerAccountIds, Constants.OrgIds.PERSONAL.longValue(), true);
        validateManageRegistrationAction(userAccount.getAccountId());
        ExceptionalRegistration exceptionalRegistration = exceptionalRegistrationRepository.findByExceptionalRegistrationId(exceptionalRegistrationId);
        exceptionalRegistration.setIsDeleted(true);
        exceptionalRegistration.setModifiedByAccountId(userAccount);
        auditService.auditForExceptionalRegistration(userAccount, exceptionalRegistration, Constants.AuditStatusEnum.REMOVE);
        return exceptionalRegistrationRepository.save(exceptionalRegistration);
    }

    private void normalizeExceptionalUserRequest (ExceptionalRegistrationRequest exceptionalRegistrationRequest) {
        exceptionalRegistrationRequest.setEmail(exceptionalRegistrationRequest.getEmail().toLowerCase());
    }

    public List<ExceptionalRegistration> getAllExceptionalRegistrationList (String accountIds) {
        List<Long> headerAccountIds = CommonUtils.convertToLongList(accountIds);
        Long accountId = userAccountRepository.findByAccountIdInAndOrgIdAndIsActive(headerAccountIds, Constants.OrgIds.PERSONAL.longValue(), true).getAccountId();
        validateManageRegistrationAction(accountId);
        return exceptionalRegistrationRepository.findAll();
    }

    public List<ExceptionalRegistration> getAllActiveExceptionalRegistrationList (String accountIds) {
        List<Long> headerAccountIds = CommonUtils.convertToLongList(accountIds);
        Long accountId = userAccountRepository.findByAccountIdInAndOrgIdAndIsActive(headerAccountIds, Constants.OrgIds.PERSONAL.longValue(), true).getAccountId();
        validateManageRegistrationAction(accountId);
        return exceptionalRegistrationRepository.findByIsDeleted(false);
    }

    private void validateManageRegistrationAction (Long accountId) {
        if (!accessDomainRepository.findUserRoleInEntity(Constants.EntityTypes.ORG, Constants.OrgIds.PERSONAL.longValue(), accountId, true, Constants.ActionId.MANAGE_REGISTRATION)) {
            throw new ValidationFailedException("User do not have action to manage registration for exceptional users");
        }
    }

    public ExceptionalRegistration updateExceptionalUser (Long exceptionalRegistrationId, ExceptionalRegistrationRequest exceptionalRegistrationRequest, String accountIds) {
        ExceptionalRegistration exceptionalRegistration = exceptionalRegistrationRepository.findByExceptionalRegistrationId(exceptionalRegistrationId);
        List<Long> headerAccountIds = CommonUtils.convertToLongList(accountIds);
        UserAccount userAccount = userAccountRepository.findByAccountIdInAndOrgIdAndIsActive(headerAccountIds, Constants.OrgIds.PERSONAL.longValue(), true);
        validateManageRegistrationAction(userAccount.getAccountId());
        if (exceptionalRegistrationRequest.getEmail() != null && !Objects.equals(exceptionalRegistrationRequest.getEmail(), exceptionalRegistration.getEmail())) {
            throw new ValidationFailedException("User not allowed to update email of exceptional registration");
        }
        if (!exceptionalRegistrationRepository.existsByEmailAndIsDeleted(exceptionalRegistrationRequest.getEmail(), false)) {
            throw new IllegalStateException("User not registered as exceptional user");
        }
        CommonUtils.copyNonNullProperties(exceptionalRegistrationRequest, exceptionalRegistration);
        exceptionalRegistration.setModifiedByAccountId(userAccount);
        ExceptionalRegistration savedRegistration = exceptionalRegistrationRepository.save(exceptionalRegistration);
        auditService.auditForExceptionalRegistration(userAccount, savedRegistration, Constants.AuditStatusEnum.UPDATE);
        return savedRegistration;
    }

    public ExceptionalRegistration reAddExceptionalUser (Long exceptionalRegistrationId, String accountIds) {
        List<Long> headerAccountIds = CommonUtils.convertToLongList(accountIds);
        UserAccount userAccount = userAccountRepository.findByAccountIdInAndOrgIdAndIsActive(headerAccountIds, Constants.OrgIds.PERSONAL.longValue(), true);
        validateManageRegistrationAction(userAccount.getAccountId());
        ExceptionalRegistration exceptionalRegistration = exceptionalRegistrationRepository.findByExceptionalRegistrationId(exceptionalRegistrationId);
        exceptionalRegistration.setIsDeleted(false);
        exceptionalRegistration.setModifiedByAccountId(userAccount);
        auditService.auditForExceptionalRegistration(userAccount, exceptionalRegistration, Constants.AuditStatusEnum.RE_ADD);
        return exceptionalRegistrationRepository.save(exceptionalRegistration);
    }
}

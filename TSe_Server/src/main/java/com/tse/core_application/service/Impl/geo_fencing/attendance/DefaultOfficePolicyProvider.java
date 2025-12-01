package com.tse.core_application.service.Impl.geo_fencing.attendance;

import com.tse.core_application.constants.RoleEnum;
import com.tse.core_application.custom.model.AccountId;
import com.tse.core_application.model.Constants;
import com.tse.core_application.model.EntityPreference;
import com.tse.core_application.model.UserAccount;
import com.tse.core_application.repository.AccessDomainRepository;
import com.tse.core_application.repository.EntityPreferenceRepository;
import com.tse.core_application.repository.UserAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * Phase 6b: Stub implementation with default office hours (9 AM - 5 PM UTC).
 */
@Component
public class DefaultOfficePolicyProvider implements OfficePolicyProvider {

    @Autowired
    private EntityPreferenceRepository entityPreferenceRepository;
    @Autowired
    private AccessDomainRepository accessDomainRepository;
    @Autowired
    UserAccountRepository userAccountRepository;

    @Override
    public LocalTime getOfficeStartTime(long orgId) {
        Optional<EntityPreference> entityPreference = entityPreferenceRepository.findByEntityTypeIdAndEntityId(Constants.EntityTypes.ORG, orgId);
        if (entityPreference.isPresent()) {
            return entityPreference.get().getOfficeHrsStartTime() != null ? entityPreference.get().getOfficeHrsStartTime() : Constants.OFFICE_START_TIME.toLocalTime();
        }
        return Constants.OFFICE_START_TIME.toLocalTime();
    }

    @Override
    public LocalTime getOfficeEndTime(long orgId) {
        Optional<EntityPreference> entityPreference = entityPreferenceRepository.findByEntityTypeIdAndEntityId(Constants.EntityTypes.ORG, orgId);
        if (entityPreference.isPresent()) {
            return entityPreference.get().getOfficeHrsEndTime() != null ? entityPreference.get().getOfficeHrsEndTime() : Constants.OFFICE_END_TIME.toLocalTime();
        }
        return Constants.OFFICE_END_TIME.toLocalTime();
    }

    @Override
    public String getOperationalTimezone(long orgId) {
        String timezone = Constants.DEFAULT_TIME_ZONE;
        List<AccountId> orgAdminAccountId = accessDomainRepository.findDistinctAccountIdByEntityTypeIdAndEntityIdAndRoleIdInAndIsActive(Constants.EntityTypes.ORG, orgId, List.of(RoleEnum.ORG_ADMIN.getRoleId()), true);
        if (orgAdminAccountId != null && !orgAdminAccountId.isEmpty()) {
            Long accountId = orgAdminAccountId.get(0).getAccountId();
            UserAccount userAccount = userAccountRepository.findByAccountIdAndIsActive(accountId, true);
            if (userAccount != null && userAccount.getFkUserId().getTimeZone() != null) {
                timezone = userAccount.getFkUserId().getTimeZone();
            }
        }
        return timezone;
    }
}

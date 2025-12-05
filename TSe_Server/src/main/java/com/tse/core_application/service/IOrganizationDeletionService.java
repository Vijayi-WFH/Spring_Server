package com.tse.core_application.service;

import com.tse.core_application.dto.org_deletion.OrgDeletionResponse;
import com.tse.core_application.dto.org_deletion.RequestOrgDeletionRequest;
import com.tse.core_application.dto.org_deletion.ReverseOrgDeletionRequest;

import java.util.List;

public interface IOrganizationDeletionService {

    OrgDeletionResponse requestOrganizationDeletion(RequestOrgDeletionRequest request, Long requesterAccountId);

    OrgDeletionResponse reverseOrganizationDeletion(ReverseOrgDeletionRequest request, Long requesterAccountId);

    List<OrgDeletionResponse> processScheduledDeletions();

    OrgDeletionResponse hardDeleteOrganization(Long orgId);
}

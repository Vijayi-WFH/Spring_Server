package com.tse.core_application.service;

import com.tse.core_application.dto.org_deletion.OrgDeletionResponse;

/**
 * Service interface for Organization Deletion operations.
 * Handles soft deletion requests, reversal, and hard deletion via scheduler.
 */
public interface IOrgDeletionService {

    /**
     * Request organization deletion (initiated by Org Admin).
     * - Marks organization as pending deletion
     * - Deactivates all user accounts
     * - Updates Redis caches
     * - Marks chat users as inactive
     * - Sends confirmation email
     *
     * @param orgId The organization ID to delete
     * @param requestingAccountId The account ID of the org admin requesting deletion
     * @param reason Optional reason for deletion
     * @return OrgDeletionResponse with deletion details
     */
    OrgDeletionResponse requestOrgDeletion(Long orgId, Long requestingAccountId, String reason);

    /**
     * Reverse organization deletion (initiated by Super Admin).
     * - Clears deletion request flag
     * - Reactivates user accounts that were deactivated due to deletion
     * - Updates Redis caches
     * - Reactivates chat users
     * - Sends notification to org admin
     *
     * @param orgId The organization ID to restore
     * @param superAdminAccountId The account ID of the super admin performing reversal
     * @return OrgDeletionResponse with reversal details
     */
    OrgDeletionResponse reverseOrgDeletion(Long orgId, Long superAdminAccountId);

    /**
     * Process scheduled deletions (called by scheduler).
     * Finds all organizations past the 30-day grace period and hard deletes them.
     */
    void processScheduledDeletions();

    /**
     * Hard delete a single organization and all its data.
     * - Collects and stores org statistics before deletion
     * - Deletes all dependent data in FK-safe order
     * - Deletes chat database data
     * - Sends completion email to org admin
     *
     * @param orgId The organization ID to hard delete
     */
    void hardDeleteOrganization(Long orgId);
}

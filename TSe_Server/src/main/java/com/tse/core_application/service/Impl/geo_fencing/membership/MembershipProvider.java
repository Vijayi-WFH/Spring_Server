package com.tse.core_application.service.Impl.geo_fencing.membership;

import java.util.List;

/**
 * Interface for resolving user memberships across teams and projects.
 * Used for determining effective fence assignments.
 */
public interface MembershipProvider {

    /**
     * Returns the list of team IDs that the given user belongs to in the specified org.
     *
     * @param orgId the organization ID
     * @param accountId the user account ID
     * @return list of team IDs (empty if none)
     */
    List<Long> listTeamsForUser(long orgId, long accountId);

    /**
     * Returns the list of project IDs that the given user belongs to in the specified org.
     *
     * @param orgId the organization ID
     * @param accountId the user account ID
     * @return list of project IDs (empty if none)
     */
    List<Long> listProjectsForUser(long orgId, long accountId, List<Long> teamIdList);

    /**
     * Checks if the given organization exists.
     * Default implementation returns true (for demo/skip-org-validation).
     *
     * @param orgId the organization ID
     * @return true if org exists, false otherwise
     */
    default boolean orgExists(long orgId) {
        return true;
    }
}

package com.tse.core_application.specification;

import com.tse.core_application.model.Constants;
import com.tse.core_application.model.Task;
import com.tse.core_application.model.WorkFlowTaskStatus;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;

public class OpenTaskSpecification {

    public static Specification<Task> notCompletedOrDeleted() {
        return (root, query, criteriaBuilder) -> {
            Join<Task, WorkFlowTaskStatus> workflowStatusJoin = root.join("fkWorkflowTaskStatus");
            return criteriaBuilder.not(workflowStatusJoin.get("workflowTaskStatus").in("Completed", "Deleted"));
        };
    }

    public static Specification<Task> isMentorOrObserver(Long accountId) {
        return (root, query, criteriaBuilder) -> {
            Predicate mentor1 = criteriaBuilder.equal(root.get("fkAccountIdMentor1").get("accountId"), accountId);
            Predicate mentor2 = criteriaBuilder.equal(root.get("fkAccountIdMentor2").get("accountId"), accountId);
            Predicate observer1 = criteriaBuilder.equal(root.get("fkAccountIdObserver1").get("accountId"), accountId);
            Predicate observer2 = criteriaBuilder.equal(root.get("fkAccountIdObserver2").get("accountId"), accountId);
            return criteriaBuilder.or(mentor1, mentor2, observer1, observer2);
        };
    }

    public static Specification<Task> matchesEntity(Integer entityTypeId, Long entityId) {
        return (root, query, criteriaBuilder) -> {
            if (entityTypeId.equals(Constants.EntityTypes.ORG)) {
                return criteriaBuilder.equal(root.get("fkOrgId").get("orgId"), entityId);
            } else if (entityTypeId.equals(Constants.EntityTypes.TEAM)) {
                return criteriaBuilder.equal(root.get("fkTeamId").get("teamId"), entityId);
            } else {
                throw new IllegalArgumentException("Invalid entity type ID");
            }
        };
    }

    public static Specification<Task> assignedTo(Long accountId) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("fkAccountIdAssigned").get("accountId"), accountId);
    }

    public static Specification<Task> hasImmediateAttention(String removedUserEmail) {
        return (root, query, criteriaBuilder) -> {
            Predicate immediateAttention = criteriaBuilder.equal(root.get("immediateAttention"), 1);
            Predicate attentionFromEmail = criteriaBuilder.equal(root.get("immediateAttentionFrom"), removedUserEmail);
            return criteriaBuilder.and(immediateAttention, attentionFromEmail);
        };
    }


}

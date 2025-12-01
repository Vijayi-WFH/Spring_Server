package com.tse.core_application.specification;

import com.tse.core_application.dto.SearchCriteria;
import com.tse.core_application.model.*;
import lombok.*;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.*;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class TaskSpecification implements Specification<Task> {
    private SearchCriteria criteria;

    @Override
    public Predicate toPredicate(Root<Task> root, CriteriaQuery<?> query, CriteriaBuilder builder) {

        if (criteria.getOperation().equalsIgnoreCase(">")) {
            if (root.get(criteria.getKey()).getJavaType() == java.time.LocalDateTime.class) {
                return builder.greaterThanOrEqualTo(root.<java.time.LocalDateTime>get(criteria.getKey()), (java.time.LocalDateTime) criteria.getValue());
            }
            return builder.greaterThanOrEqualTo(root.<String>get(criteria.getKey()), criteria.getValue().toString());
        } else if (criteria.getOperation().equalsIgnoreCase("<")) {
            if (root.get(criteria.getKey()).getJavaType() == java.time.LocalDateTime.class) {
                return builder.lessThanOrEqualTo(root.<java.time.LocalDateTime>get(criteria.getKey()), (java.time.LocalDateTime) criteria.getValue());
            }
            return builder.lessThanOrEqualTo(root.<String>get(criteria.getKey()), criteria.getValue().toString());
        } else if (criteria.getOperation().equalsIgnoreCase(":")) {
            if (root.get(criteria.getKey()).getJavaType() == String.class) {
                return builder.like(root.<String>get(criteria.getKey()), "%" + criteria.getValue() + "%");
            } else {
                return builder.equal(root.get(criteria.getKey()), criteria.getValue());
            }
        } else if ("isNull".equals(criteria.getOperation())) {
            return builder.isNull(root.get(criteria.getKey()));
        }
        return null;
    }

    public static Specification<Task> joinSpecification(SearchCriteria criteria) {
        return (root, query, criteriaBuilder) -> {
            if ("labels".equals(criteria.getKey()) && "in".equals(criteria.getOperation())) {
                Join<Task, Label> labelsJoin = root.join("labels");
                return criteriaBuilder.in(labelsJoin.get("labelId")).value(criteria.getValue());
            }

            return null;
        };
    }

    public static Specification<Task> byTeamId(Long teamId) {
        return (root, query, cb) -> cb.equal(root.get("fkTeamId"), teamId);
    }

    public static Specification<Task> byTeamIdAndAccountId(Long teamId, Long accountId) {
        return new Specification<Task>() {
            @Override
            public Predicate toPredicate(Root<Task> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
                Predicate teamPredicate = builder.equal(root.get("fkTeamId"), teamId);
                Predicate accountPredicate = builder.equal(root.get("fkAccountIdAssigned"), accountId);
                return builder.and(teamPredicate, accountPredicate);
            }
        };
    }

    public static Specification<Task> byTeamIdAndAccountIdsIn(Long teamId, List<Long> accountIds) {
        return (Root<Task> root, CriteriaQuery<?> query, CriteriaBuilder builder) -> {
            Predicate teamPredicate = builder.equal(root.get("fkTeamId"), teamId);
            Predicate accountPredicate = root.get("fkAccountIdAssigned").in(accountIds);
            return builder.and(teamPredicate, accountPredicate);
        };
    }

    public static Specification<Task> isBugReportedByList(List<Long> reporterAccountIds) {
        return (Root<Task> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            return cb.and(
                    cb.equal(root.get("taskTypeId"), Constants.TaskTypes.BUG_TASK),
                    root.get("fkAccountIdBugReportedBy").get("accountId").in(reporterAccountIds)
            );
        };
    }

    public static Specification<Task> byEntityAndNotAssigned(Long teamId, Long projectId, List<Long> orgIds) {
        return (Root<Task> root, CriteriaQuery<?> query, CriteriaBuilder builder) -> {
            Predicate unassignedPredicate = builder.isNull(root.get("fkAccountIdAssigned"));
            if (teamId == null && projectId == null && (orgIds == null || orgIds.isEmpty())) {
                return unassignedPredicate;
            }
            Predicate entityPredicate = null;
            if (teamId != null) {
                entityPredicate = builder.equal(root.get("fkTeamId"), teamId);
            } else if (projectId != null) {
                entityPredicate = builder.equal(root.get("fkProjectId"), projectId);
            } else if (orgIds != null && !orgIds.isEmpty()) {
                entityPredicate = root.get("fkOrgId").in(orgIds);
            }
            return (entityPredicate != null) ? builder.and(entityPredicate, unassignedPredicate) : unassignedPredicate;
        };
    }

}


package com.tse.core_application.specification;

import com.tse.core_application.dto.board_view.ViewBoardRequest;
import com.tse.core_application.model.Constants;
import com.tse.core_application.model.Label;
import com.tse.core_application.model.Task;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class BoardViewSpecification {

    /**
     * This method returns a list of tasks that have been completed since the previous working day and the tasks that are currently scheduled and all other tasks assigned to the user/ member in team (that are not in backlog/ completed/ deleted)
     * it's sorted based on priority.
     */
    public static Specification<Task> build(ViewBoardRequest request, LocalDate previousDate, List<String> invalidWorkFlowStatuses) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (request.getOrgId() != null) { // orgId is mandatory but still we've put a check
                predicates.add(criteriaBuilder.equal(root.get("fkOrgId"), request.getOrgId()));
            }
            if(request.getProjectId() != null) { // projectId is mandatory but still we've put a check
                predicates.add(criteriaBuilder.equal(root.get("fkProjectId"), request.getProjectId()));
            }
            if (request.getTeamId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("fkTeamId"), request.getTeamId()));
            }

//            // Filter tasks based on teamIds with task view action
//            if (!teamIdsWithTaskViewAction.isEmpty()) {
//                predicates.add(root.get("teamId").in(teamIdsWithTaskViewAction));
//            }
//
//            // Add criteria to handle tasks assigned to the user in teams without task view action
//            Predicate assignedToUserPredicate = criteriaBuilder.equal(root.get("accountIdAssigned"), accountIdRequestor);
//            Predicate noTeamTaskViewActionPredicate = criteriaBuilder.not(root.get("teamId").in(teamIdsWithTaskViewAction));
//            if (!request.getAccountIds().contains(accountIdRequestor)) {
//                predicates.add(criteriaBuilder.and(assignedToUserPredicate, noTeamTaskViewActionPredicate));
//            } else {
//                predicates.add(noTeamTaskViewActionPredicate);
//            }

            if (request.getSprintId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("sprintId"), request.getSprintId()));
            }
            if (Boolean.TRUE.equals(request.getIsStarred())) {
                predicates.add(criteriaBuilder.equal(root.get("isStarred"), true));
                if (request.getStarredBy() != null && !request.getStarredBy().isEmpty()) {
                    predicates.add(root.get("fkAccountIdStarredBy").in(request.getStarredBy()));
                }
            }
            if (request.getLabelIds() != null && !request.getLabelIds().isEmpty()) {
                Join<Task, Label> labelsJoin = root.join("labels", JoinType.LEFT);
                predicates.add(labelsJoin.get("labelId").in(request.getLabelIds()));
                query.distinct(true);
            }

            // Handling accountIds and currentTaskTimeSheetIndicator
            if (request.getAccountIds() != null && !request.getAccountIds().isEmpty()) {
                predicates.add(root.get("fkAccountIdAssigned").in(request.getAccountIds()));
            }

            // Handling workflow status
            Predicate completedStatusPredicate = criteriaBuilder.and(
                    criteriaBuilder.equal(root.join("fkWorkflowTaskStatus").get("workflowTaskStatus"), Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED_TITLE_CASE),
                    criteriaBuilder.greaterThan(root.get("taskActEndDate"), previousDate.atStartOfDay())
            );

            Predicate otherStatusPredicate = criteriaBuilder.and(
                    criteriaBuilder.not(root.join("fkWorkflowTaskStatus").get("workflowTaskStatus").in(invalidWorkFlowStatuses))
            );

            if (request.getCurrentTaskTimeSheetIndicator()) {
                predicates.add(criteriaBuilder.equal(root.get("currentlyScheduledTaskIndicator"), true));
                predicates.add(otherStatusPredicate);
            } else {
                predicates.add(criteriaBuilder.or(completedStatusPredicate, otherStatusPredicate));
            }

            query.orderBy(criteriaBuilder.asc(root.get("taskPriority")));

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}



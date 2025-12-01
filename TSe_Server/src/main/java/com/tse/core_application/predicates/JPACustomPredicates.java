package com.tse.core_application.predicates;

import com.tse.core_application.dto.StatsRequest;
import com.tse.core_application.model.*;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;
import java.util.Arrays;
import java.util.List;

import static com.tse.core_application.constants.PredicateConstant.*;

/**
 * This class is used for creating custom predicates.
 *
 * @author Karan
 */
public class JPACustomPredicates {

    /**
     * This predicate is used for creating custom queries with specific condition.
     * where ((workflow task status is Completed) and task act end date >= From Date and task act end date <= To Date)
     *
     * @param statsRequest                field.
     * @param workflowCompletedStatusList field.
     * @return specification reference.
     */
    public static Specification<Task> workflowCompleteAndReqPredicate(StatsRequest statsRequest, List<Integer> workflowCompletedStatusList) {

        return (root, query, criteriaBuilder) -> {
            Join<Task, WorkFlowTaskStatus> taskStatusRoot = root.join(WORKFLOW_TASK_STATUS_INNER_REF);

            Predicate completeStatusPredicate = criteriaBuilder.in(taskStatusRoot.get(WORKFLOW_TASK_STATUS_ID_INNER_REF)).value(workflowCompletedStatusList);
            Predicate taskActFromDatePredicate = criteriaBuilder.greaterThanOrEqualTo(root.get(TASK_ACT_END_DATE), statsRequest.getFromDate());
            Predicate taskActToDatePredicate = criteriaBuilder.lessThanOrEqualTo(root.get(TASK_ACT_END_DATE), statsRequest.getToDate());

            return criteriaBuilder.and(completeStatusPredicate, taskActFromDatePredicate, taskActToDatePredicate);
        };
    }

    /**
     * This predicate is used for creating custom queries with specific condition.
     * where (workflow task status is Started or On Hold or Blocked or Not Started) and task act start date is not null and task act start date <= To Date)
     *
     * @param statsRequest field.
     * @return specification reference.
     */
    public static Specification<Task> workflowOtherStatusAndReqPredicate(StatsRequest statsRequest, List<Integer> list) {
        return (root, query, criteriaBuilder) -> {
            Join<Task, WorkFlowTaskStatus> taskStatusRoot = root.join(WORKFLOW_TASK_STATUS_INNER_REF);

            Predicate otherStatusPredicate = criteriaBuilder.in(taskStatusRoot.get(WORKFLOW_TASK_STATUS_ID_INNER_REF)).value(list);
            Predicate ActStartDateNotNullPredicate = criteriaBuilder.isNotNull(root.get(TASK_ACT_START_DATE));
            Predicate ActStartDateAndToDatePredicate = criteriaBuilder.lessThanOrEqualTo(root.get(TASK_ACT_START_DATE), statsRequest.getToDate());

            return criteriaBuilder.and(otherStatusPredicate, ActStartDateNotNullPredicate, ActStartDateAndToDatePredicate);
        };
    }

    /**
     * This predicate is used for creating custom queries with specific condition.
     * where (Backlog and (task priority = P0 or task priority = P1) and not Sprint)
     *
     * @return specification reference. change.
     */
    public static Specification<Task> workflowBacklogCriticalStatusPredicate(List<Integer> list) {
        return (root, query, criteriaBuilder) -> {
            Join<Task, WorkFlowTaskStatus> taskStatusRoot = root.join(WORKFLOW_TASK_STATUS_INNER_REF);

            Predicate blockedStatusPredicate = criteriaBuilder.in(taskStatusRoot.get(WORKFLOW_TASK_STATUS_ID_INNER_REF)).value(list);
            Predicate priorityPredicate = criteriaBuilder.
                    in(root.get(TASK_PRIORITY)).value(Arrays.asList(TaskPriority.P1.name(), TaskPriority.P0.name()));
            Predicate notSprintTaskPredicate = criteriaBuilder.notEqual(root.get(TASK_WORKFLOW_ID), Constants.TaskWorkFlowIds.SPRINT);
            return criteriaBuilder.and(blockedStatusPredicate, priorityPredicate, notSprintTaskPredicate);
        };
    }

    /**
     * This predicate is used for creating custom queries with specific condition.
     * where (workflow task status is (Not Started Or On Hold Or Blocked) and Task Exp Start Date <= To Date).
     *
     * @param statsRequest field.
     * @return specification reference.
     */
    public static Specification<Task> workflowStatusAndTaskExpReqPredicate(StatsRequest statsRequest, List<Integer> list) {
        return (root, query, criteriaBuilder) -> {
            Join<Task, WorkFlowTaskStatus> taskStatusRoot = root.join(WORKFLOW_TASK_STATUS_INNER_REF);

            Predicate workflowTaskStatusPredicate = criteriaBuilder.in(taskStatusRoot.get(WORKFLOW_TASK_STATUS_ID_INNER_REF)).value(list);
            Predicate taskExpectedDatePredicate = criteriaBuilder.lessThanOrEqualTo(root.get(TASK_EXP_START_DATE), statsRequest.getToDate());

            return criteriaBuilder.and(workflowTaskStatusPredicate, taskExpectedDatePredicate);
        };
    }

    /**
     * This predicate is used for creating custom queries with specific condition.
     * where (workflow task status is Backlog and Critical and not Sprint).
     *
     * @return specification reference.
     */
    public static Specification<Task> workflowStatusIDFilterPredicate(List<Integer> list) {
        return (root, query, criteriaBuilder) -> {
            Join<Task, WorkFlowTaskStatus> taskStatusRoot = root.join(WORKFLOW_TASK_STATUS_INNER_REF);

            return criteriaBuilder.in(taskStatusRoot.get(WORKFLOW_TASK_STATUS_ID_INNER_REF)).value(list);
        };
    }

    public static Specification<Task> addSimilarityFunctionPredicate(List<String> list, String attr, float searchMultiplier) {
        return (root, query, criteriaBuilder) -> {
            Predicate finalPredicate = criteriaBuilder.disjunction();

            for (String word : list) {
                if (word.length() < 3) {
                    // Skip search if word is too short (avoids matching all tasks for "f", "a", etc.)
                    continue;
                }

                // Similarity function for fuzzy matching
                Expression<Float> similarityValue = criteriaBuilder.function("SIMILARITY", Float.class, root.get(attr), criteriaBuilder.literal(word));
                Predicate similarityPredicate = criteriaBuilder.greaterThan(similarityValue, searchMultiplier);

                // ILIKE filter with wildcard for partial word matches
                Predicate ilikePredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get(attr)), "% " + word.toLowerCase() + "%");

                // Combine predicates using OR
                finalPredicate = criteriaBuilder.or(finalPredicate, similarityPredicate, ilikePredicate);
            }

            // Order results by similarity score
            query.orderBy(criteriaBuilder.desc(criteriaBuilder.function("SIMILARITY", Float.class, root.get(attr), criteriaBuilder.literal(String.join(" ", list)))));

            return finalPredicate;
        };
    }

}

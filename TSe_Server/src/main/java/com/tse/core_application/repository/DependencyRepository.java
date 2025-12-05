package com.tse.core_application.repository;

import com.tse.core_application.model.Dependency;
import com.tse.core_application.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface DependencyRepository extends JpaRepository<Dependency, Long> {

    long countByPredecessorTaskIdAndIsRemoved(Long taskId, Boolean isRemoved);
    long countBySuccessorTaskIdAndIsRemoved(Long taskId, Boolean isRemoved);

    List<Dependency> findByDependencyIdInAndIsRemoved(List<Long> dependencyIds, Boolean isRemoved);

    Dependency findByDependencyIdAndIsRemoved(Long dependencyId, Boolean isRemoved);

    @Modifying
    @Transactional
    @Query(value = "update Dependency d set d.isRemoved = true, d.lastUpdatedDateTime = current_timestamp() where d.dependencyId = :dependencyId")
    int removeDependency(Long dependencyId);

    /**
     * Retrieves all predecessors and successors of a specified task, traversing through the
     * entire dependency graph to identify all tasks that are indirectly related to the
     * given task. This method helps in identifying cyclic dependencies in the task dependency graph.
     * @param taskId
     * @return
     */
//    @Query(value =
//            "WITH RECURSIVE all_dependencies AS (" +
//                    "SELECT predecessor_task_id, successor_task_id " +
//                    "FROM tse.dependency " +
//                    "WHERE predecessor_task_id = :taskId OR successor_task_id = :taskId " +
//                    "UNION " +
//                    "SELECT d.predecessor_task_id, d.successor_task_id " +
//                    "FROM tse.dependency d " +
//                    "JOIN all_dependencies ad ON d.successor_task_id = ad.predecessor_task_id OR d.predecessor_task_id = ad.successor_task_id " +
//                    ") " +
//                    "SELECT t.* FROM tse.dependency t INNER JOIN all_dependencies ad ON t.predecessor_task_id = ad.predecessor_task_id AND t.successor_task_id = ad.successor_task_id",
//            nativeQuery = true)
//    List<Dependency> findAllDependenciesForTask(@Param("taskId") Long taskId);

    @Query(value =
            "WITH RECURSIVE all_dependencies AS (" +
                    "SELECT predecessor_task_id, successor_task_id " +
                    "FROM tse.dependency " +
                    "WHERE (predecessor_task_id = :taskId OR successor_task_id = :taskId) " +
                    "AND is_removed = false " +
                    "UNION " +
                    "SELECT d.predecessor_task_id, d.successor_task_id " +
                    "FROM tse.dependency d " +
                    "JOIN all_dependencies ad ON d.successor_task_id = ad.predecessor_task_id OR d.predecessor_task_id = ad.successor_task_id " +
                    "WHERE d.is_removed = false " +
                    ") " +
                    "SELECT t.* FROM tse.dependency t INNER JOIN all_dependencies ad ON t.predecessor_task_id = ad.predecessor_task_id AND t.successor_task_id = ad.successor_task_id " +
                    "WHERE t.is_removed = false",
            nativeQuery = true)
    List<Dependency> findAllDependenciesForTask(@Param("taskId") Long taskId);

    @Query(value =
            "WITH RECURSIVE upstream_tasks AS ( " +
                    "    SELECT d.predecessor_task_id " +
                    "    FROM tse.dependency d " +
                    "    JOIN tse.task t ON d.successor_task_id = :taskId " +
                    "    WHERE d.is_removed = FALSE " +
                    "      AND t.workflow_task_status_id NOT IN :workflowStatusIds " +
                    "      AND d.predecessor_task_id NOT IN ( " +
                    "          SELECT t2.task_id " +
                    "          FROM tse.task t2 " +
                    "          WHERE t2.workflow_task_status_id IN :workflowStatusIds " +
                    "      ) " +
                    "    UNION ALL " +
                    "    SELECT d.predecessor_task_id " +
                    "    FROM tse.dependency d " +
                    "    JOIN upstream_tasks ut ON d.successor_task_id = ut.predecessor_task_id " +
                    "    WHERE d.is_removed = FALSE " +
                    "      AND d.predecessor_task_id NOT IN ( " +
                    "          SELECT t2.task_id " +
                    "          FROM tse.task t2 " +
                    "          WHERE t2.workflow_task_status_id IN :workflowStatusIds " +
                    "      ) " +
                    ") " +
                    "SELECT DISTINCT d.* " +
                    "FROM upstream_tasks ut " +
                    "JOIN tse.dependency d ON ut.predecessor_task_id = d.predecessor_task_id",
            nativeQuery = true)
    List<Dependency> findUpstreamDependencies(@Param("taskId") Long taskId, @Param("workflowStatusIds") List<Integer> workflowStatusIds);


    @Query(value =
            "WITH RECURSIVE downstream_tasks AS ( " +
                    "    SELECT d.successor_task_id " +
                    "    FROM tse.dependency d " +
                    "    WHERE d.predecessor_task_id = :taskId " +
                    "      AND d.is_removed = FALSE " +
                    "    UNION ALL " +
                    "    SELECT d.successor_task_id " +
                    "    FROM tse.dependency d " +
                    "    JOIN downstream_tasks dt ON d.predecessor_task_id = dt.successor_task_id " +
                    "    WHERE d.is_removed = FALSE " +
                    ") " +
                    "SELECT DISTINCT t.task_id " +
                    "FROM downstream_tasks dt " +
                    "JOIN tse.task t ON dt.successor_task_id = t.task_id",
            nativeQuery = true)
    List<Long> findDownstreamTasks(@Param("taskId") Long taskId);



    @Query("SELECT d FROM Dependency d WHERE d.dependencyId IN :dependencyIds AND " +
            "((d.predecessorTaskId = :predecessorId AND d.successorTaskId = :successorId) OR " +
            "(d.predecessorTaskId = :successorId AND d.successorTaskId = :predecessorId))")
    List<Dependency> findByDependencyIdsAndTaskIds(
            @Param("dependencyIds") List<Long> dependencyIds,
            @Param("predecessorId") Long predecessorId,
            @Param("successorId") Long successorId
    );

    @Query("SELECT d.predecessorTaskId FROM Dependency d WHERE d.successorTaskId = :successorTaskId AND d.isRemoved = :isRemoved")
    List<Long> findPredecessorTaskIdBySuccessorTaskIdAndIsRemoved(Long successorTaskId, Boolean isRemoved);

    @Query("SELECT d.successorTaskId FROM Dependency d WHERE d.predecessorTaskId = :predecessorTaskId AND d.isRemoved = :isRemoved")
    List<Long> findSuccessorTaskIdByPredecessorTaskIdAndIsRemoved(Long predecessorTaskId, Boolean isRemoved);

    @Query(value =
            "WITH RECURSIVE all_dependencies AS ( " +
                    "SELECT predecessor_task_id, successor_task_id " +
                    "FROM tse.dependency " +
                    "WHERE (predecessor_task_id = :taskId OR successor_task_id = :taskId) " +
                    "AND is_removed = false " +
                    "UNION " +
                    "SELECT d.predecessor_task_id, d.successor_task_id " +
                    "FROM tse.dependency d " +
                    "JOIN all_dependencies ad ON d.successor_task_id = ad.predecessor_task_id OR d.predecessor_task_id = ad.successor_task_id " +
                    "WHERE d.is_removed = false " +
                    ") " +
                    "SELECT t.* " +
                    "FROM tse.dependency t " +
                    "INNER JOIN all_dependencies ad ON t.predecessor_task_id = ad.predecessor_task_id AND t.successor_task_id = ad.successor_task_id " +
                    "INNER JOIN tse.task task_predecessor ON t.predecessor_task_id = task_predecessor.task_id " +
                    "INNER JOIN tse.task task_successor ON t.successor_task_id = task_successor.task_id " +
                    "WHERE t.is_removed = false " +
                    "AND (task_predecessor.sprint_id IN :sprintIds AND task_successor.sprint_id IN :sprintIds)",
            nativeQuery = true)
    List<Dependency> findAllDependenciesForTaskAndSprints(@Param("taskId") Long taskId, @Param("sprintIds") List<Long> sprintIds);

    @Query(value =
            "WITH RECURSIVE all_dependencies AS ( " +
                    "SELECT predecessor_task_id, successor_task_id " +
                    "FROM tse.dependency " +
                    "WHERE (predecessor_task_id = :taskId OR successor_task_id = :taskId) " +
                    "AND is_removed = false " +
                    "UNION " +
                    "SELECT d.predecessor_task_id, d.successor_task_id " +
                    "FROM tse.dependency d " +
                    "JOIN all_dependencies ad ON d.successor_task_id = ad.predecessor_task_id OR d.predecessor_task_id = ad.successor_task_id " +
                    "WHERE d.is_removed = false " +
                    ") " +
                    "SELECT t.* " +
                    "FROM tse.dependency t " +
                    "INNER JOIN all_dependencies ad ON t.predecessor_task_id = ad.predecessor_task_id AND t.successor_task_id = ad.successor_task_id " +
                    "INNER JOIN tse.task task_predecessor ON t.predecessor_task_id = task_predecessor.task_id " +
                    "INNER JOIN tse.task task_successor ON t.successor_task_id = task_successor.task_id " +
                    "WHERE t.is_removed = false " +
                    "AND (task_predecessor.epic_id IN :epicIds AND task_successor.epic_id IN :epicIds)",
            nativeQuery = true)
    List<Dependency> findAllDependenciesForTaskAndEpics(@Param("taskId") Long taskId, @Param("epicIds") List<Long> epicIds);

    @Query(value = "SELECT d FROM Dependency d where d.isRemoved = :isRemoved AND d.predecessorTaskId IN (:taskIds) OR d.successorTaskId IN (:taskIds)")
    List<Dependency> findAllDependenciesForTasks(List<Long> taskIds, Boolean isRemoved);

    @Modifying
    @Transactional
    @Query("DELETE FROM Dependency d WHERE d.predecessorTaskId IN :taskIds OR d.successorTaskId IN :taskIds")
    void deleteByTaskIdIn(List<Long> taskIds);
}

package com.tse.core_application.service.Impl;

import com.tse.core_application.custom.model.Edge;
import com.tse.core_application.custom.model.Node;
import com.tse.core_application.dto.DependencyGraph;
import com.tse.core_application.dto.DependencyGraphRequest;
import com.tse.core_application.exception.InvalidRequestParamater;
import com.tse.core_application.exception.ValidationFailedException;
import com.tse.core_application.model.Constants;
import com.tse.core_application.model.Dependency;
import com.tse.core_application.model.Task;
import com.tse.core_application.repository.AccessDomainRepository;
import com.tse.core_application.repository.DependencyRepository;
import com.tse.core_application.repository.TaskRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class DependencyService {

    @Autowired
    private DependencyRepository dependencyRepository;
    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private TaskService taskService;
    @Autowired
    private TaskHistoryService taskHistoryService;
    @Autowired
    private TaskHistoryMetadataService taskHistoryMetadataService;
    @Autowired
    private ActionService actionService;
    @Autowired
    private TaskServiceImpl taskServiceImpl;
    @Autowired
    private AccessDomainRepository accessDomainRepository;

    /**
     * method to perform cyclic check when a new dependency is added for a task
     */
    public boolean checkForCycles(Task task, Dependency newDependency) {
        // Fetch all dependencies for the task using the recursive CTE
        List<Dependency> allDependencies = dependencyRepository.findAllDependenciesForTask(task.getTaskId());

        // If no dependencies and newDependency doesn't create a self-loop
        if (allDependencies.isEmpty() && !newDependency.getPredecessorTaskId().equals(newDependency.getSuccessorTaskId())) {
            return false; // No cycle
        }

        allDependencies.add(newDependency);

        // Convert the dependencies into an adjacency list
        Map<Long, List<Long>> graph = new HashMap<>();
        for (Dependency dep : allDependencies) {
            graph.putIfAbsent(dep.getPredecessorTaskId(), new ArrayList<>());
            graph.get(dep.getPredecessorTaskId()).add(dep.getSuccessorTaskId());
        }

        // Perform DFS to check for cycles
        Set<Long> visited = new HashSet<>();
        Set<Long> recStack = new HashSet<>();
        for (Long taskId : graph.keySet()) {
            if (isCyclic(taskId, visited, recStack, graph)) {
                throw new IllegalArgumentException("A cyclic dependency detected for Work Item number: " + task.getTaskNumber());
            }
        }
        return false;
    }

    private boolean isCyclic(Long taskId, Set<Long> visited, Set<Long> recStack, Map<Long, List<Long>> graph) {
        if (recStack.contains(taskId))
            return true;

        if (visited.contains(taskId))
            return false;

        visited.add(taskId);
        recStack.add(taskId);

        if (graph.containsKey(taskId)) {
            for (Long child : graph.get(taskId)) {
                if (isCyclic(child, visited, recStack, graph))
                    return true;
            }
        }

        recStack.remove(taskId);
        return false;
    }

    /**
     * creates a dependency graph object for all the direct and indirect dependencies of the task
     */
    public DependencyGraph getDependencyGraphForTask(Long taskId) {
        List<Node> nodes = new ArrayList<>();
        List<Edge> edges = new ArrayList<>();

        List<Dependency> allDependencies = dependencyRepository.findAllDependenciesForTask(taskId);

        if (allDependencies != null) {
            // Retrieve all related tasks
            Set<Long> allTaskIds = new HashSet<>();
            for (Dependency dep : allDependencies) {
                allTaskIds.add(dep.getPredecessorTaskId());
                allTaskIds.add(dep.getSuccessorTaskId());
            }
            List<Task> allTasks = taskRepository.findByTaskIdIn(new ArrayList<>(allTaskIds));

            // Constructing nodes
            for (Task task : allTasks) {
                Node node = new Node();
                BeanUtils.copyProperties(task, node);
                node.setTeamId(task.getFkTeamId().getTeamId());
                if(task.getFkAccountIdAssigned() != null) {
                    node.setAccountIdAssigned(task.getFkAccountIdAssigned().getAccountId());
                }
                node.setWorkflowStatus(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus());
                nodes.add(node);
            }

            // Constructing edges
            for (Dependency dep : allDependencies) {
                Edge edge = new Edge();
                BeanUtils.copyProperties(dep, edge);
                edges.add(edge);
            }
        }
        return new DependencyGraph(nodes, edges);
    }

    public boolean getAllUpstreamAndDownstreamDependencies(Long taskId1, Long taskId2) {
        List<Integer> workFlowIdsToStopAt = Stream.concat(Constants.WorkFlowStatusIds.COMPLETED.stream(),
                Constants.WorkFlowStatusIds.DELETED.stream()).collect(Collectors.toList());
        List<Dependency> upstream = dependencyRepository.findUpstreamDependencies(taskId1, workFlowIdsToStopAt);
        upstream = upstream.stream().filter(dependency -> !dependency.getIsRemoved()).collect(Collectors.toList());
        List<Long> downstream = dependencyRepository.findDownstreamTasks(taskId2);
        HashSet<Long> downstreamSet = new HashSet<>();
        for(Long taskId : downstream)
            downstreamSet.add(taskId);
        downstreamSet.add(taskId2);

        for(Dependency dependency : upstream)
            if(downstreamSet.contains(dependency.getSuccessorTaskId()))
                return true;
        downstream = dependencyRepository.findDownstreamTasks(taskId1);
        for(Long taskId:downstream)
            if(Objects.equals(taskId,taskId2))
                return true;
        return false;
    }

    /**
     * method to remove dependency from a task. It will mark the dependency as removed in the dependency table and remove the dependencyId from the dependencyIds list of both the related tasks
     */
    public void removeDependency(Long dependencyId, Long taskId, Long accountId) {
        Dependency dependency = dependencyRepository.findByDependencyIdAndIsRemoved(dependencyId, false);
        Task taskDb = taskRepository.findByTaskId(taskId);
        Task otherRelatedTask;
        if ((taskDb == null || dependency == null) && !(Objects.equals(dependency.getPredecessorTaskId(), taskId) || Objects.equals(dependency.getSuccessorTaskId(), taskId))) {
            throw new IllegalArgumentException("parameter dependencyId or taskId is invalid");
        }

        boolean isUpdateAllowed = false;
        ArrayList<String> userActionList = actionService.getUserActionList(accountId, taskDb.getFkTeamId().getTeamId());

        if (userActionList.contains(Constants.UpdateTeam.All_Task_Essential_Update)) {
            isUpdateAllowed = true;
        }

        if (isUpdateAllowed) {
            dependencyRepository.removeDependency(dependencyId);
            if (Objects.equals(dependency.getPredecessorTaskId(), taskId)) {
                otherRelatedTask = taskRepository.findByTaskId(dependency.getSuccessorTaskId());
            } else {
                otherRelatedTask = taskRepository.findByTaskId(dependency.getPredecessorTaskId());
            }
            if (taskDb.getTaskTypeId() == Constants.TaskTypes.CHILD_TASK && otherRelatedTask.getTaskTypeId() == Constants.TaskTypes.CHILD_TASK && Objects.equals(taskDb.getParentTaskId(), otherRelatedTask.getParentTaskId())) {
                taskServiceImpl.updateInternalAndExternalDependencyCount(taskDb, true, false);
            }
            else {
                if (taskDb.getTaskTypeId() == Constants.TaskTypes.CHILD_TASK) {
                    taskServiceImpl.updateInternalAndExternalDependencyCount(taskDb, false, false);
                }
                if (otherRelatedTask.getTaskTypeId() == Constants.TaskTypes.CHILD_TASK) {
                    taskServiceImpl.updateInternalAndExternalDependencyCount(otherRelatedTask, false, false);
                }
            }
            deleteDependencyIdFromTask(taskDb, dependencyId);
            deleteDependencyIdFromTask(otherRelatedTask, dependencyId);
        }
    }

    /** It will modify the dependencyIds list in a task and remove a particular dependencyId and create history for the same */
    public void deleteDependencyIdFromTask(Task taskDb, Long dependencyIdToRemove) {
        List<Long> dependencyIds = new ArrayList<>(taskDb.getDependencyIds());
        if(dependencyIds != null) {
            taskHistoryService.addTaskHistoryOnSystemUpdate(taskDb);
            dependencyIds.remove(dependencyIdToRemove);
            taskDb.setDependencyIds(dependencyIds);
            taskRepository.save(taskDb);
            taskHistoryMetadataService.addTaskHistoryMetadata(List.of(Constants.TaskFields.DEPENDENCY_IDS), taskDb);
        }
    }

    /** When a task is deleted, this method will remove the dependencyIds from the tasks related to the deleted task*/
    public List<String> removeDependenciesOnTaskDeletion(Task task) {
        List<Long> dependencyIds = task.getDependencyIds();
        List<Long> relatedTaskIds = new ArrayList<>();
        List<String> taskNumbers = new ArrayList<>();
        if(dependencyIds != null && !dependencyIds.isEmpty()) {
            List<Dependency> dependencies = dependencyRepository.findByDependencyIdInAndIsRemoved(dependencyIds, false);
            for(Dependency dependency: dependencies) {
                if(Objects.equals(dependency.getPredecessorTaskId(), task.getTaskId())) {
                    relatedTaskIds.add(dependency.getSuccessorTaskId());
                } else {
                    relatedTaskIds.add(dependency.getPredecessorTaskId());
                }
            }
            List<Task> relatedTasks = taskRepository.findByTaskIdIn(relatedTaskIds);
            Map<Long, Task> taskMap = relatedTasks.stream().collect(Collectors.toMap(Task::getTaskId, t -> t));
            for(Dependency dependency: dependencies) {
                if(Objects.equals(dependency.getPredecessorTaskId(), task.getTaskId())) {
                    deleteDependencyIdFromTask(taskMap.get(dependency.getSuccessorTaskId()), dependency.getDependencyId());
                    taskNumbers.add(taskMap.get(dependency.getSuccessorTaskId()).getTaskNumber());
                } else {
                    deleteDependencyIdFromTask(taskMap.get(dependency.getPredecessorTaskId()), dependency.getDependencyId());
                    taskNumbers.add(taskMap.get(dependency.getPredecessorTaskId()).getTaskNumber());
                }
                dependencyRepository.removeDependency(dependency.getDependencyId());
            }
        }
        return  taskNumbers;
    }

    public DependencyGraph getDependencyGraphByFilter(DependencyGraphRequest dependencyGraphRequest, String accountIds) {
        LocalDateTime startDate=dependencyGraphRequest.getStartDate();
        LocalDateTime endDate=dependencyGraphRequest.getEndDate();
        Boolean isInternal=dependencyGraphRequest.getIsInternal();
        if(isInternal==null)
            isInternal=false;
        List<Long> sprintIds=dependencyGraphRequest.getSprintIds();
        List<Long> epicIds=dependencyGraphRequest.getEpicIds();

        List<Long> taskIds = new ArrayList<>();

        List<Long> accountIdsList = Arrays.stream(accountIds.split(",")).map(Long::valueOf).collect(Collectors.toList());

        if(startDate!=null && endDate!=null && (sprintIds==null || sprintIds.isEmpty()) && (epicIds ==null || epicIds.isEmpty())) {
            if(endDate.isBefore(startDate))
                throw new ValidationFailedException("End Date cannot be before Start Date.");
            List<Long> teamIds = dependencyGraphRequest.getTeamId()==null ?
                    accessDomainRepository.findTeamIdsByAccountIdsAndIsActiveTrue(accountIdsList) : List.of(dependencyGraphRequest.getTeamId());
            if(dependencyGraphRequest.getGetTasksWithOnlyDependencies()!=null && dependencyGraphRequest.getGetTasksWithOnlyDependencies())
                taskIds = taskRepository.findTaskIdsWithExpDatesInRangeAndOnlyWithDependencies(startDate, endDate, teamIds);
            else
                taskIds = taskRepository.findTaskIdsWithExpDatesInRange(startDate, endDate, teamIds);
        }
        else if(sprintIds!=null && startDate==null && endDate==null && (epicIds==null || epicIds.isEmpty())) {
            if(dependencyGraphRequest.getGetTasksWithOnlyDependencies()!=null && dependencyGraphRequest.getGetTasksWithOnlyDependencies())
                taskIds = taskRepository.findTaskIdsBySprintIdInAndOnlyWithDependencies(sprintIds);
            else
                taskIds = taskRepository.findTaskIdsBySprintIdIn(sprintIds);
        }
        else if(epicIds!=null && startDate==null && endDate==null && (sprintIds==null || sprintIds.isEmpty())) {
            if(dependencyGraphRequest.getGetTasksWithOnlyDependencies()!=null && dependencyGraphRequest.getGetTasksWithOnlyDependencies())
                taskIds = taskRepository.findTaskIdsByEpicIdsInAndOnlyWithDependencies(epicIds);
            else
                taskIds = taskRepository.findTaskIdsByEpicIdsIn(epicIds);
        }
        else
            throw new InvalidRequestParamater("Incorrect filter applied to get dependency graph.");

        HashSet<Long> processedTasks = new HashSet<>();
        HashSet<Node> nodes = new HashSet<>();
        HashSet<Edge> edges = new HashSet<>();

        for(Long taskId:taskIds) {

            if(processedTasks.contains(taskId))
                continue;

            List<Dependency> allDependencies = new ArrayList<>();

            if (sprintIds != null && isInternal)
                allDependencies = dependencyRepository.findAllDependenciesForTaskAndSprints(taskId, sprintIds);
            else if (epicIds != null && isInternal)
                allDependencies = dependencyRepository.findAllDependenciesForTaskAndEpics(taskId, epicIds);
            else
                allDependencies = dependencyRepository.findAllDependenciesForTask(taskId);

            if (allDependencies != null) {
                // Retrieve all related tasks
                Set<Long> allTaskIds = new HashSet<>();
                for (Dependency dep : allDependencies) {
                    allTaskIds.add(dep.getPredecessorTaskId());
                    allTaskIds.add(dep.getSuccessorTaskId());
                    processedTasks.add(dep.getPredecessorTaskId());
                    processedTasks.add(dep.getSuccessorTaskId());
                }
                if(!processedTasks.contains(taskId)) {
                    allTaskIds.add(taskId);
                    processedTasks.add(taskId);
                }
                List<Task> allTasks = taskRepository.findByTaskIdIn(new ArrayList<>(allTaskIds));

                // Constructing nodes
                for (Task task : allTasks) {
                    Node node = new Node();
                    BeanUtils.copyProperties(task, node);
                    node.setTeamId(task.getFkTeamId().getTeamId());
                    if (task.getFkAccountIdAssigned() != null) {
                        node.setAccountIdAssigned(task.getFkAccountIdAssigned().getAccountId());
                    }
                    node.setWorkflowStatus(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus());
                    nodes.add(node);
                }

                // Constructing edges
                for (Dependency dep : allDependencies) {
                    Edge edge = new Edge();
                    BeanUtils.copyProperties(dep, edge);
                    edges.add(edge);
                }
            }
        }
        return new DependencyGraph(new ArrayList<>(nodes), new ArrayList<>(edges));
    }

    public void reCalculateLagTimeOnTaskUpdates(List<Long> taskList){

        List<Dependency> dependencyList = dependencyRepository.findAllDependenciesForTasks(taskList, false);
        if(dependencyList != null && !dependencyList.isEmpty()) {
            List<Long> dependentTaskList = dependencyList.parallelStream().map(Dependency::getPredecessorTaskId).collect(Collectors.toList());
            dependentTaskList.addAll(dependencyList.parallelStream().map(Dependency::getSuccessorTaskId).collect(Collectors.toList()));
            //ToDo: We have to change the result of script to DTo as we only need dates (no need to fetch whole TaskObject).
            List<Task> dependentTasks = taskRepository.findByTaskIdIn(dependentTaskList);
            if(dependentTasks != null && !dependentTasks.isEmpty()) {
                Map<Long, Task> dependentTaskMap = dependentTasks.parallelStream().collect(Collectors.toMap(Task::getTaskId, task -> task));
                for (Dependency dependency : dependencyList) {
                    if(dependentTaskMap.containsKey(dependency.getSuccessorTaskId()) && dependentTaskMap.containsKey(dependency.getPredecessorTaskId())) {
                        dependency.setLagTime(taskServiceImpl.calculateLagTimeInMinutes(dependentTaskMap.get(dependency.getSuccessorTaskId()).getTaskExpStartDate(),
                                dependentTaskMap.get(dependency.getPredecessorTaskId()).getTaskExpEndDate(), dependentTaskMap.get(dependency.getPredecessorTaskId())).getLagTime());
                    }
                }
                dependencyRepository.saveAll(dependencyList);
            }
        }
    }
}

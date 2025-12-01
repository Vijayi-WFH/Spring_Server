package com.tse.core_application.service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tse.core_application.dto.TestRequestDTO;
import com.tse.core_application.dto.TestResponseDTO;
import com.tse.core_application.model.*;
import com.tse.core_application.repository.TaskRepository;
import com.tse.core_application.util.BasicTestUtil;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

import static com.tse.core_application.util.FileNameConstant.*;

@DataJpaTest
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.jpa.properties.hibernate.default_schema=tse")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@TestPropertySource("classpath:application-test.properties")
public class TaskServiceDBTest {


    @Autowired
    private TaskRepository taskRepository;

    @Value("${test_request_file.path}")
    private String commonFileRequest;

    @Value("${test_response_file.path}")
    private String commonFileResponse;

    ObjectMapper objectMapper = BasicTestUtil.staticObjectMapper();

    @Test
    //@Sql("classpath:test-data.sql")
    public void testCreateTask() throws IOException, InvalidFormatException {                // ->


        Map<Long, TestRequestDTO> requestDTOMap = new HashMap<>();
//        Map<Long, TestRequestDTO> requestMap = BasicTestUtil.readXlsxFileData(requestDTOMap, FileNameConstant.commonFileName, FileNameConstant.createTaskSheetName);
        Map<Long, TestRequestDTO> requestMap = BasicTestUtil.readXlsxFileData(requestDTOMap, commonFileRequest, createTaskSheetName);

        Map<Long, TestResponseDTO> responseMap = new HashMap<>();
        requestMap.forEach((k, v) -> {
            boolean expected = true;
            try {
                Task task = new Task();
                ObjectNode objectNode = objectMapper.readValue(v.getRequest(), ObjectNode.class);
                objectNode = setDateFields(objectNode, task);

                Task willSaveTask = objectMapper.readValue(objectMapper.writeValueAsString(objectNode), Task.class);
                willSaveTask.setTaskExpStartDate(task.getTaskExpStartDate());
                willSaveTask.setTaskExpStartTime(task.getTaskExpStartTime());
                willSaveTask.setTaskExpEndDate(task.getTaskExpEndDate());
                willSaveTask.setTaskExpEndTime(task.getTaskExpEndTime());

                saveTaskAndCommitTransaction(willSaveTask);

                if(expected!=v.isActualStatus()) {
                    BasicTestUtil.markFailure(responseMap, k, v, "Test failed without exception!", "Test failed without exception!", "NA", expected);
                }
            } catch (Exception e) {
                expected = false;

                if(expected != v.isActualStatus()) {
                    BasicTestUtil.markFailure(responseMap, k, v, e.getLocalizedMessage(), e.getMessage(), Arrays.toString(e.getStackTrace()), expected);
                }
            }
        });
//        BasicTestUtil.createFailedTestCaseReport(responseMap, createTaskResponseFileName,"create_task");
        BasicTestUtil.createFailedTestCaseReport(responseMap, commonFileResponse, "create_task");
    }

    private ObjectNode setDateFields(ObjectNode objectNode, Task task) {
        JsonNode taskExpStartDate = objectNode.remove("taskExpStartDate");
        task.setTaskExpStartDate(LocalDateTime.parse(taskExpStartDate.asText()));
        JsonNode taskExpStartTime = objectNode.remove("taskExpStartTime");
        task.setTaskExpStartTime(LocalTime.parse(taskExpStartTime.asText()));

        JsonNode taskExpEndDate = objectNode.remove("taskExpEndDate");
        task.setTaskExpEndDate(LocalDateTime.parse(taskExpEndDate.asText()));

        JsonNode taskExpEndTime = objectNode.remove("taskExpEndTime");
        task.setTaskExpEndTime(LocalTime.parse(taskExpEndTime.asText()));

        return objectNode;
    }

    public void saveTaskAndCommitTransaction(Task task) {
        taskRepository.saveAndFlush(task);
    }

    /*@Test
    public void testUpdateTeamInTeamTable() throws IOException, InvalidFormatException {

        Map<Long, TestRequestDTO> requestDTOMap = new HashMap<>();
        Map<Long, TestRequestDTO> requestMap = BasicTestUtil.readXlsxFileData(requestDTOMap, updateTeamFileName);
        Map<Long, TestResponseDTO> responseMap = new HashMap<>();
        requestMap.forEach((k,v) -> {
            Team willCreateTeam = createDBTeamObj(k);
            teamRepository.save(willCreateTeam);
        });

        requestMap.forEach((k,v) ->{
            boolean expected = true;
            try {
                Team requestedTeam = objectMapper.readValue(v.getRequest(), Team.class);
                saveTeamAndCommitTransaction(requestedTeam);
                if(expected!=v.isActualStatus()) {
                    BasicTestUtil.markFailure(responseMap, k, v, "Test failed without exception!", "Test failed without exception!", "NA", expected);
                }
            } catch (Exception e) {
                expected = false;

                if(expected != v.isActualStatus()) {
                    BasicTestUtil.markFailure(responseMap, k, v, e.getLocalizedMessage(), e.getMessage(), Arrays.toString(e.getStackTrace()), expected);
                }
            }

        });
        BasicTestUtil.createFailedTestCaseReport(responseMap, updateTeamResponseFileName);
    }

    private Team createDBTeamObj(Long teamId) {
        Team team = new Team();
        team.setTeamId(teamId);
        String testDataUUID = UUID.randomUUID().toString();
        team.setTeamName("Test Team  - " + testDataUUID);
        team.setTeamDesc("Test Desc - " + testDataUUID);
        Instant currentDate = new Date().toInstant();
        team.setLastUpdatedDateTime(Timestamp.from(currentDate));
        team.setCreatedDateTime(Timestamp.from(currentDate));
        Organization organization = new Organization();
        organization.setOrgId(0L);
        team.setFkOrgId(organization);
        UserAccount userAccount = new UserAccount();
        userAccount.setAccountId(1L);
        team.setFkOwnerAccountId(userAccount);
        Project project = new Project();
        project.setProjectId(1L);
        team.setFkProjectId(project);
        team.setParentTeamId(1L);
        return team;
    }

*//*
    private Team updateDBTeamObj() {
        Team team = new Team();
        String testDataUUID = UUID.randomUUID().toString();
        team.setTeamName("Test Team  - " + testDataUUID);
        return team;
    }
*//*
*/

    @Test
    //@Sql("classpath:test-data.sql")
    public void testUpdateTask() throws IOException, InvalidFormatException {                // ->
        Map<Long, TestRequestDTO> requestDTOMap = new HashMap<>();
//        Map<Long, TestRequestDTO> requestMap = BasicTestUtil.readXlsxFileData(requestDTOMap, FileNameConstant.commonFileName, FileNameConstant.updateTaskSheetName);
        Map<Long, TestRequestDTO> requestMap = BasicTestUtil.readXlsxFileData(requestDTOMap, commonFileRequest, updateTaskSheetName);



        Map<Long, TestResponseDTO> responseMap = new HashMap<>();
        requestMap.forEach((k, v) -> {
            boolean expected = true;
            try {
                Task task = new Task();
                ObjectNode objectNode = objectMapper.readValue(v.getRequest(), ObjectNode.class);
                objectNode = setDateFields(objectNode, task);

                Task willSaveTask = objectMapper.readValue(objectMapper.writeValueAsString(objectNode), Task.class);

                willSaveTask.setTaskExpStartDate(task.getTaskExpStartDate());
                willSaveTask.setTaskExpStartTime(task.getTaskExpStartTime());
                willSaveTask.setTaskExpEndDate(task.getTaskExpEndDate());
                willSaveTask.setTaskExpEndTime(task.getTaskExpEndTime());

                createDummyTaskForTesting(willSaveTask);

                saveTaskAndCommitTransaction(willSaveTask);

                if(expected!=v.isActualStatus()) {
                    BasicTestUtil.markFailure(responseMap, k, v, "Test failed without exception!", "Test failed without exception!", "NA", expected);
                }
            } catch (Exception e) {
                expected = false;

                if(expected != v.isActualStatus()) {
                    BasicTestUtil.markFailure(responseMap, k, v, e.getLocalizedMessage(), e.getMessage(), Arrays.toString(e.getStackTrace()), expected);
                }
            }
        });
//        BasicTestUtil.createFailedTestCaseReport(responseMap, updateTaskResponseFileName, "update_task");
        BasicTestUtil.createFailedTestCaseReport(responseMap, commonFileResponse, "update_task");
    }

    private void createDummyTaskForTesting(Task willSaveTask) {
        Task task = new Task();
        task.setTaskId(willSaveTask.getTaskId());
        task.setTaskTitle("NA");
        task.setTaskDesc("THIS IS DESC");
        task.setTaskWorkflowId(1);
        task.setCurrentActivityIndicator(1);
        task.setTaskPriority("NA");
        task.setTaskExpStartDate(willSaveTask.getTaskExpStartDate());
        task.setTaskExpStartTime(willSaveTask.getTaskExpStartTime());
        task.setTaskExpEndDate(willSaveTask.getTaskExpEndDate());
        task.setTaskExpEndTime(willSaveTask.getTaskExpEndTime());
        task.setRecordedEffort(1);

        WorkFlowTaskStatus wf = new WorkFlowTaskStatus();
        wf.setWorkflowTaskStatusId(21);
        task.setFkWorkflowTaskStatus(wf);
        Organization organization = new Organization();
        organization.setOrgId(1L);
        task.setFkOrgId(organization);

        Project project = new Project();
        project.setProjectId(1L);
        task.setFkProjectId(project);

        Team team = new Team();
        team.setTeamId(1L);
        task.setFkTeamId(team);

        UserAccount userAccount = new UserAccount();
        userAccount.setAccountId(1L);

        task.setFkAccountId(userAccount);

        taskRepository.saveAndFlush(task);
    }


}
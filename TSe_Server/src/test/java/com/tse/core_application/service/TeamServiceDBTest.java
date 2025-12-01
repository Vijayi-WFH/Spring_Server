package com.tse.core_application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tse.core_application.model.*;
import com.tse.core_application.repository.TeamRepository;
import com.tse.core_application.dto.TestRequestDTO;
import com.tse.core_application.dto.TestResponseDTO;
import com.tse.core_application.util.BasicTestUtil;
import com.tse.core_application.util.FileNameConstant;
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

import java.io.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

@DataJpaTest
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.jpa.properties.hibernate.default_schema=tse")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@TestPropertySource("classpath:application-test.properties")
public class TeamServiceDBTest {

    @Autowired
    private TeamRepository teamRepository;

    ObjectMapper objectMapper = new ObjectMapper();

    @Value("${test_request_file.path}")
    private String commonFileRequest;

    @Value("${test_response_file.path}")
    private String commonFileResponse;



    @Test
    //@Sql("classpath:test-data.sql")
    public void testCreateTeam() throws IOException, InvalidFormatException {                // ->
        Map<Long, TestRequestDTO> requestDTOMap = new HashMap<>();
//        Map<Long, TestRequestDTO> requestMap = BasicTestUtil.readXlsxFileData(requestDTOMap, FileNameConstant.commonFileName, FileNameConstant.createTeamSheetName);
          Map<Long, TestRequestDTO> requestMap = BasicTestUtil.readXlsxFileData(requestDTOMap, commonFileRequest, FileNameConstant.createTeamSheetName);


        Map<Long, TestResponseDTO> responseMap = new HashMap<>();
        requestMap.forEach((k, v) -> {
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
//        BasicTestUtil.createFailedTestCaseReport(responseMap, FileNameConstant.createTeamResponseFileName, "create_team");
          BasicTestUtil.createFailedTestCaseReport(responseMap, commonFileResponse, "create_team");
    }

    public void saveTeamAndCommitTransaction(Team requestedTeam) {
        teamRepository.saveAndFlush(requestedTeam);
    }

    @Test
    public void testUpdateTeamInTeamTable() throws IOException, InvalidFormatException {

        Map<Long, TestRequestDTO> requestDTOMap = new HashMap<>();
//        Map<Long, TestRequestDTO> requestMap = BasicTestUtil.readXlsxFileData(requestDTOMap, FileNameConstant.commonFileName, FileNameConstant.updateTeamSheetName);
          Map<Long, TestRequestDTO> requestMap = BasicTestUtil.readXlsxFileData(requestDTOMap, commonFileRequest, FileNameConstant.updateTeamSheetName);



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
//        BasicTestUtil.createFailedTestCaseReport(responseMap, FileNameConstant.updateTeamResponseFileName);
          BasicTestUtil.createFailedTestCaseReport(responseMap, commonFileResponse, "update-team");
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

/*
    private Team updateDBTeamObj() {
        Team team = new Team();
        String testDataUUID = UUID.randomUUID().toString();
        team.setTeamName("Test Team  - " + testDataUUID);
        return team;
    }
*/

}

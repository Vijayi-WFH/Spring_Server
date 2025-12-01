package com.tse.core_application.service.Impl;

import com.tse.core_application.handlers.StackTraceHandler;
import com.tse.core_application.model.AccessDomain;
import com.tse.core_application.model.User;
import com.tse.core_application.model.UserAccount;
import com.tse.core_application.utils.CommonUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.sql.Timestamp;
import java.time.LocalDateTime;

@Service
public class SecondaryDatabaseService {

//    @Autowired
//    private JdbcTemplate jdbcTemplate;

    @Autowired
    @Qualifier("secondaryDataSource")
    private DataSource secondaryDataSource;

    @Autowired
    private TeamService teamService;

    @Autowired
    private UserAccountService userAccountService;

    private static final Logger logger = LogManager.getLogger(SecondaryDatabaseService.class.getName());

    //    private JdbcTemplate secondaryJdbcTemplate = new JdbcTemplate(secondaryDataSource);
    private JdbcTemplate secondaryJdbcTemplate;

    @PostConstruct
    public void init() {
        this.secondaryJdbcTemplate = new JdbcTemplate(secondaryDataSource);
    }


    public void insertData(Long teamId, String teamName, String primaryEmail, String firstName, String lastName, String givenName, String middleName, String chatRoomName, Timestamp created_date_time) {
        String sql = "INSERT INTO tse.user_demographics (team_id, team_name, primary_email, first_name, last_name, given_name, middle_name, chat_room_name, created_date_time, modified_date_time) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try{
            secondaryJdbcTemplate.update(sql, teamId, teamName, primaryEmail, firstName, lastName, givenName, middleName, chatRoomName, created_date_time, null);
        } catch (Exception e){
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("Something went wrong: could not insert data in secondary database" + e, new Throwable(allStackTraces));
        }
    }

    public void deleteDataFromSecondaryDatabase(String primaryEmail) {
        String sql = "DELETE FROM tse.user_demographics WHERE primary_email = ?";
        try{
            secondaryJdbcTemplate.update(sql, primaryEmail);
        } catch (Exception e){
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("Something went wrong: could not delete data from secondary database" + e, new Throwable(allStackTraces));
        }
    }

    public void insertDataInSecondaryDatabase(Long teamId, AccessDomain accessDomain){
        try{
            String teamName = teamService.getTeamByTeamId(teamId).getTeamName();
            UserAccount userAccount = userAccountService.getActiveUserAccountByAccountId(accessDomain.getAccountId());
            User user = userAccount.getFkUserId();
            String chatRoomName = CommonUtils.createJIDForTeamName(teamName, teamId);
            String primaryEmail = user.getPrimaryEmail();
            insertData(teamId, teamName, primaryEmail, user.getFirstName(), user.getLastName(), user.getGivenName(), user.getMiddleName(), chatRoomName, accessDomain.getCreatedDateTime());
        } catch (Exception e){
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("Something went wrong in call to insertData method of secondaryDatabaseService" + e, new Throwable(allStackTraces));
        }
    }

    public void updateTeamNameInSecondaryDatabase(Long teamId, String teamName){
            String sql = "UPDATE tse.user_demographics SET team_name = ?, modified_date_time = ? WHERE team_id = ?";
            try{
                LocalDateTime currentTime = LocalDateTime.now();
                secondaryJdbcTemplate.update(sql, teamName, currentTime, teamId);
            } catch (Exception e){
                e.printStackTrace();
                String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                logger.error("Something went wrong: could not update data in secondary database" + e, new Throwable(allStackTraces));
            }
        }

}

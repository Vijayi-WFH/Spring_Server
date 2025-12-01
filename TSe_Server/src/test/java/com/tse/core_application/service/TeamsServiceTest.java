package com.tse.core_application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tse.core_application.model.AccessDomain;
import com.tse.core_application.model.Audit;
import com.tse.core_application.model.Team;
import com.tse.core_application.model.User;
import com.tse.core_application.repository.TeamRepository;
import com.tse.core_application.service.Impl.AccessDomainService;
import com.tse.core_application.service.Impl.AuditService;
import com.tse.core_application.service.Impl.SecondaryDatabaseService;
import com.tse.core_application.service.Impl.TeamService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class TeamsServiceTest {

    @Spy
    @InjectMocks
    private TeamService teamService;

    @Mock
    private AuditService auditService;

    @Mock
    private AccessDomainService accessDomainService;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private SecondaryDatabaseService secondaryDatabaseService;

    @Before
    public void init() {

    }

    @Test
    public void testCreateAndAuditTeam() {
        // todo declaration
        Team team = mock(Team.class);
        Audit audit = mock(Audit.class);
        int len = 60;


        AccessDomain accessDomain = mock(AccessDomain.class);

        // stubbing
        lenient().when(team.getTeamId()).thenReturn(1000L);

        lenient().when(team.getTeamName()).thenReturn("karanhfjhduhrhdsdhgsfahahhffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffhfffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff");


        lenient().when(teamRepository.save(any(Team.class))).thenReturn(team);

        lenient().when(auditService.auditForCreateTeam(any(Team.class))).thenReturn(audit);
        lenient().when(accessDomainService.addAccessDomainAfterTeamAdd(any(Team.class), anyLong())).thenReturn(accessDomain);
        Team savedTeamResponse = null;
        String errorMsg = null;
        try {
            savedTeamResponse = teamService.createAndAuditTeam(team, anyLong(), anyLong(), anyLong());
        }
        catch(Exception e){
            errorMsg = e.getLocalizedMessage();
        }

        //assertThat(savedTeamResponse.getTeamId()).isEqualTo(1000L);
        assertThat(savedTeamResponse).isNotNull();
//        assertThat(savedTeamResponse).isLessThan(50);
    }

    @Test
    public void testUpdateTeamInTeamTable() {

        Team team = mock(Team.class);
        team.setTeamName("team1");

        Audit audit = mock(Audit.class);
        HashMap<String, Object> mapTeamData = new HashMap<>();
        mapTeamData.put("TEST", "TEST_VAL");
        HashMap<String, Object> mapTeam = mock(HashMap.class);
        Set<Map.Entry<String, Object>> data = new HashSet<>();
        data.add(new Map.Entry<String, Object>() {
            @Override
            public String getKey() {
                return "teamName";
            }

            @Override
            public Object getValue() {
                return "teamValue";
            }

            @Override
            public Object setValue(Object value) {
                return new Object();
            }
        });


        lenient().when(team.getTeamId()).thenReturn(1000L);
        lenient().when(teamRepository.findByTeamId(anyLong())).thenReturn(team);
        lenient().when(objectMapper.convertValue(any(Team.class), eq(HashMap.class))).thenReturn(mapTeam);
        lenient().when(mapTeam.entrySet()).thenReturn(data);
        lenient().when(mapTeam.get(any())).thenReturn("TEST");
        lenient().when(teamRepository.save(any(Team.class))).thenReturn(team);
        lenient().when(auditService.auditForUpdateTeam(any(Team.class))).thenReturn(audit);
        lenient().when(teamService.getFieldsToUpdate(team)).thenReturn(new ArrayList<>(Collections.singletonList("teamName")));
        lenient().doNothing().when(secondaryDatabaseService).updateTeamNameInSecondaryDatabase(anyLong(), anyString());


/*
        Iterator mockIterator = mock(Iterator.class);
        doCallRealMethod().when(data).forEach(any(Consumer.class));
        when(data.iterator()).thenReturn(mockIterator);
        when(mockIterator.hasNext()).thenReturn(true, false);
        when(mockIterator.next()).thenReturn(data);
*/

        Team savedTeamResponse = teamService.updateTeamInTeamTable(team, anyLong(), new User());
        assertThat(savedTeamResponse.getTeamId()).isEqualTo(1000L);
        assertThat(savedTeamResponse).isNotNull();
    }

}

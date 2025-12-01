package com.tse.core_application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tse.core_application.custom.model.RestResponseWithData;
import com.tse.core_application.exception.NoDataFoundException;
import com.tse.core_application.model.AgeRange;
import com.tse.core_application.model.Country;
import com.tse.core_application.model.Education;
import com.tse.core_application.model.Gender;
import com.tse.core_application.repository.AgeRangeRepository;
import com.tse.core_application.repository.CountryRepository;
import com.tse.core_application.repository.EducationRepository;
import com.tse.core_application.repository.GenderRepository;
import com.tse.core_application.service.Impl.RegisterService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class RegisterServiceTest {

    @Mock
    private GenderRepository genderRepository;

    @Mock
    private EducationRepository educationRepository;

    @Mock
    private AgeRangeRepository ageRangeRepository;

    @Mock
    private CountryRepository countryRepository;

    @Mock
    private ObjectMapper objectMapper;



    @InjectMocks
    private RegisterService registerService = new RegisterService();

    /** Test GetAllRecordsFromThreeTables method */
    @Test
    public void testGetAllRecordsFromThreeTables() {

        Gender gender = mock(Gender.class);
        Education education = mock(Education.class);
        AgeRange age = mock(AgeRange.class);
        Country country = mock(Country.class);

        HashMap<String, Object> genderMap = new HashMap<>();
        genderMap.put("genderDisplayName", "Male");
        genderMap.put("genderDescription", "Male");

        HashMap<String, Object> educationMap = new HashMap<>();
        educationMap.put("educationDisplayName", "Higher");
        educationMap.put("educationDescription", "Higher Education");

        HashMap<String, Object> ageMap = new HashMap<>();
        ageMap.put("ageRangeDisplayName", "19-25");
        ageMap.put("ageRangeDescription", "19-25 Bracket");

        when(genderRepository.findAll()).thenReturn(List.of(gender));
        when(objectMapper.convertValue(gender, HashMap.class)).thenReturn(genderMap);
        when(educationRepository.findAll()).thenReturn(List.of(education));
        when(objectMapper.convertValue(education, HashMap.class)).thenReturn(educationMap);
        when(ageRangeRepository.findAll()).thenReturn(List.of(age));
        when(objectMapper.convertValue(age, HashMap.class)).thenReturn(ageMap);
        when(countryRepository.findAll()).thenReturn(List.of(country));


        HashMap<String, Object> records = registerService.getAllRecordsFromThreeTables();

        assertNotNull(records);
        assertTrue(records.containsKey("gender"));
        assertNotNull(records.get("gender"));
        assertTrue(records.containsKey("education"));
        assertNotNull(records.get("education"));
        assertTrue(records.containsKey("ageRange"));
        assertNotNull(records.get("ageRange"));
        assertTrue(records.containsKey("country"));
        assertNotNull(records.get("country"));
    }

    /** Test testGetFormattedRegistrationOptionsResponse method with null records*/
    @Test(expected = NoDataFoundException.class)
    public void testGetFormattedRegistrationOptionsResponse_NoDataFoundException() {
        HashMap<String, Object> records = new HashMap<>();
        ResponseEntity<Object> response = registerService.getFormattedRegistrationOptionsResponse(records);
    }

    /** Test testGetFormattedRegistrationOptionsResponse method with valid records*/
    @Test
    public void testGetFormattedRegistrationOptionsResponse() {
        HashMap<String, Object> records = registerService.getAllRecordsFromThreeTables();
        ResponseEntity<Object> response = registerService.getFormattedRegistrationOptionsResponse(records);
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        RestResponseWithData restResponseWithData = (RestResponseWithData) response.getBody();
        assertEquals(records, restResponseWithData.getData());
    }

}

package com.tse.core_application.service.Impl;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;

import com.tse.core_application.constants.Constants;
import com.tse.core_application.dto.DefaultImageResponse;
import com.tse.core_application.model.Education;
import com.tse.core_application.model.Gender;
import com.tse.core_application.exception.NoDataFoundException;
import com.tse.core_application.handlers.CustomResponseHandler;
import com.tse.core_application.model.Country;
import com.tse.core_application.repository.CountryRepository;
import com.tse.core_application.utils.CommonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.tse.core_application.model.AgeRange;
import com.tse.core_application.repository.AgeRangeRepository;
import com.tse.core_application.repository.EducationRepository;
import com.tse.core_application.repository.GenderRepository;

import javax.imageio.ImageIO;

@Service
public class RegisterService {


	@Autowired
	private GenderRepository genderRepository;

	@Autowired
	private EducationRepository educationRepository;

	@Autowired
	private AgeRangeRepository ageRangeRepository;

	@Autowired
	private CountryRepository countryRepository;

	@Autowired
	private RegistrationService registrationService;

	ObjectMapper objectMapper = new ObjectMapper();

	public HashMap<String, Object> getAllRecordsFromThreeTables() {

		HashMap<String, Object> finalHashMap = new HashMap<String, Object>();
		
		ArrayList<HashMap<String, Object>> genderArrayList = new ArrayList<HashMap<String, Object>>();

		ArrayList<HashMap<String, Object>> educationArrayList = new ArrayList<HashMap<String, Object>>();
		
		ArrayList<HashMap<String, Object>> ageRangeArrayList = new ArrayList<HashMap<String, Object>>();
		
		// loop for gender table
			for (Gender gender : genderRepository.findAll()) {
				HashMap<String, Object> map1 = objectMapper.convertValue(gender, HashMap.class);
				String gender2 = (String) map1.remove(Constants.Descriptions.Gender_Description);
				genderArrayList.add(map1);
			}
		finalHashMap.put("gender", genderArrayList);

		// loop for education table
			for (Education education : educationRepository.findAll()) {
				HashMap<String, Object> map2 = objectMapper.convertValue(education, HashMap.class);
				String education2 = (String) map2.remove(Constants.Descriptions.Education_Description);
				educationArrayList.add(map2);

			}
		finalHashMap.put("education", educationArrayList);

		// loop for ageRange table
			for (AgeRange age : ageRangeRepository.findAll()) {
				HashMap<String, Object> map3 = objectMapper.convertValue(age, HashMap.class);
				String ageRange = (String) map3.remove(Constants.Descriptions.AgeRange_Description);
				ageRangeArrayList.add(map3);
			}
		finalHashMap.put("ageRange", ageRangeArrayList);

			//  loop for country table
		List<Country> countries = countryRepository.findAllByOrderByCountryNameAsc();
		finalHashMap.put("country", countries);

		return finalHashMap;

	}

	public ResponseEntity<Object> getFormattedRegistrationOptionsResponse(HashMap<String, Object> allRecords) {
		ResponseEntity<Object> formattedResponse = null;
		if (allRecords.isEmpty()) {
			throw new NoDataFoundException();
		}
		return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, "success", allRecords);
	}

	public DefaultImageResponse getUserDefaultImage (String firstName, String lastName) throws IOException {
		DefaultImageResponse response = new DefaultImageResponse();
		String normalizedFirstName = CommonUtils.convertToTitleCase(firstName);
		String normalizedLastName = CommonUtils.convertToTitleCase(lastName);
		BufferedImage defaultImage = registrationService.generateDefaultImage(normalizedFirstName, normalizedLastName);
		String base64Image = registrationService.convertToBase64(defaultImage, "jpeg");
		response.setImageData(base64Image);
		return response;
	}

	public byte[] getUserImageInByte(String imageData) throws IOException {
		return Base64.getDecoder().decode(imageData);
	}

	public byte[] getDefaultImageOfUserInByte(String firstName, String lastName) throws IOException {
		String normalizedFirstName = CommonUtils.convertToTitleCase(firstName);
		String normalizedLastName = CommonUtils.convertToTitleCase(lastName);

		BufferedImage defaultImage = registrationService.generateDefaultImage(normalizedFirstName, normalizedLastName);

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ImageIO.write(defaultImage, "jpeg", baos);

		return baos.toByteArray();
	}
}

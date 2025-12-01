package com.tse.core_application.utils;

import com.tse.core_application.dto.ScanResponse;
import com.tse.core_application.dto.ScanResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

@Component
public class ComponentUtils {

    private static String scanApiUrl;

    @Value("${scanfile.endpoint}")
    public void setScanApiUrl(String url) {
        ComponentUtils.scanApiUrl = url;
    }

    /** scan attachment files for viruses */
    public static ScanResult scanFile(MultipartFile file) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
//        HttpEntity<MultipartFile> requestEntity = new HttpEntity<>(file, headers);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", file.getResource());

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        ResponseEntity<ScanResponse> response = restTemplate.postForEntity(scanApiUrl, requestEntity, ScanResponse.class);
        ScanResponse scanResponse = response.getBody();
        return new ScanResult(scanResponse.getStatus(), scanResponse.getResult());
    }
}

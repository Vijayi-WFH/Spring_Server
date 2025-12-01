package com.example.chat_app.jwtUtils;

import com.example.chat_app.exception.InternalServerErrorException;
import com.example.chat_app.exception.UnauthorizedLoginException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class JWTUtil {

    @Value("${tseserver.application.root.path}")
    private String tseServerBaseUrl;

    public void validateTokenAndAccountIds(String jwtToken, String screenName, List<Long> accountIds){

        if (jwtToken.startsWith("Bearer ")) {
            jwtToken = jwtToken.substring(7); // Remove the "Bearer " prefix
        } else {
            throw new UnauthorizedLoginException("Invalid token");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + jwtToken);

        headers.set("screenName", screenName);
        headers.set("accountIds", String.join(",", accountIds.stream().map(String::valueOf).collect(Collectors.toList())));
        headers.set("token", jwtToken);

        HttpEntity<Object> requestEntity = new HttpEntity<>(headers);

        RestTemplate restTemplate = new RestTemplate();

        ResponseEntity<Object> serverResponse = null;
        Object response = null;
        try {
            serverResponse = restTemplate.exchange(tseServerBaseUrl + "/api/auth/validateTokenAccount", HttpMethod.GET, requestEntity, Object.class
            );
            response = serverResponse.getBody();
        } catch (RestClientException e) {
            throw new com.example.chat_app.exception.RestClientException(e.getMessage());
        }
        if (serverResponse.getStatusCode() == HttpStatus.OK) {
            System.out.println("Token and accountIds are valid: " + response);
        } else if (serverResponse.getStatusCode() == HttpStatus.UNAUTHORIZED) {
            throw new UnauthorizedLoginException("Unauthorised accountIds");
        } else {
            throw new InternalServerErrorException("Unexpected response: " + response);
        }
    }
}

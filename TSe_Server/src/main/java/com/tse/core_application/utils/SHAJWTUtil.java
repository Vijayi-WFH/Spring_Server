package com.tse.core_application.utils;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestParam;

import javax.crypto.SecretKey;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class SHAJWTUtil {

    private static final Logger log = LogManager.getLogger(SHAJWTUtil.class);
    @Value("${springbootwebfluxjjwt.jjwt.secret}")
    private String secret;

    private final String appId = "Vijayi-WFH-JitsiMeet";

    private final Long expirationTime = 3600L * 1000; // one hour in milliseconds

    public String doGenerateToken(Map<String, String> claims){

        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationTime);
        Map<String , Object> context = new HashMap<>();
        Map<String, String> claimsMap = new HashMap<>();
        claimsMap.put("name", claims.get("username"));
        claimsMap.put("email", claims.get("email"));
        claimsMap.put("orgId", claims.get("orgId"));
        claimsMap.put("userId", claims.get("userId"));
        claimsMap.put("accountId", claims.get("accountId"));
        claimsMap.put("userType", claims.get("userType"));
        claimsMap.put("meetingId", claims.get("meetingId"));
        claimsMap.put("isOrganiser", claims.get("isOrganiser"));
        claimsMap.put("timezone", claims.get("timezone"));
        claimsMap.put("teamId", claims.get("teamId"));
        claimsMap.put("projectId", claims.get("projectId"));

        context.put("user", claimsMap);
        return Jwts.builder()
                .setHeaderParam("typ", "JWT")
                .setHeaderParam("alg", "HS256")
                .setSubject(claims.get("username"))
                .setIssuer(appId)
                .setAudience(appId)
                .claim("room", claims.get("roomName"))
                .claim("context", context)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    private SecretKey getSigningKey() {
        try {
            String combinedInput = appId + secret;

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = digest.digest(combinedInput.getBytes());

            return Keys.hmacShaKeyFor(hashedBytes);

        } catch (NoSuchAlgorithmException e) {
            log.error("Error: SHA-256 algorithm not found. This should not happen in a standard JRE.");
            throw new com.tse.core_application.exception.NoSuchAlgorithmException("Could not generate signing key, Algorithm not Found");
        }
    }

    public String doGenerateTokenForGuests(Map<String, String> claims){

        Date now = new Date();
        Date expiry = new Date(now.getTime() + Long.parseLong(claims.get("expirationTime")));
        Map<String , Object> context = new HashMap<>();
        context.put("user", Map.of(
                "name", claims.get("username"),
                "email", claims.get("email"),
                "orgId", claims.get("orgId"),
                "senderUserId", claims.get("senderUserId"),
                "userType", claims.get("userType")
        ));

        return Jwts.builder()
                .setHeaderParam("typ", "JWT")
                .setHeaderParam("alg", "HS256")
                .setSubject(claims.get("username"))
                .setIssuer(appId)
                .setAudience(appId)
                .claim("room", claims.get("roomName"))
                .claim("context", context)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }
}

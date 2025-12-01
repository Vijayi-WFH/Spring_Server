package com.tse.core_application.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tse.core_application.config.DebugConfig;
import com.tse.core_application.dto.User;
import com.tse.core_application.repository.UserAccountRepository;
import com.tse.core_application.service.Impl.UserService;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.security.*;
import java.util.*;

@Component
public class JWTUtil {

    @Autowired
    private UserService userService;
    @Autowired
    private UserAccountRepository userAccountRepository;

    private static final String KEY_PATH_PRIVATE_KEY = "src/main/java/com/tse/core_application/keys/private_key.der";
    private static final String KEY_PATH_PUBLIC_KEY = "src/main/java/com/tse/core_application/keys/public_key.der";
    ObjectMapper objectMapper = new ObjectMapper();
    @Value("${springbootwebfluxjjwt.jjwt.secret}")
    private String secret;
    @Value("${springbootwebfluxjjwt.jjwt.expiration}")
    private String expirationTime;
    private Key key;

    private static Map<String, Object> getRSAKeys() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        PrivateKey privateKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();
        Map<String, Object> keys = new HashMap<String, Object>();
        keys.put("private", privateKey);
        keys.put("public", publicKey);
        return keys;
    }

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        Map<String, Object> rsaKeys = null;
        try {
            rsaKeys = getRSAKeys();
        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    public Claims getAllClaimsFromToken(String token) {
        Claims claims = null;
        PublicKey publicKey = null;
        try {
            publicKey = CommonUtils.getPublicKey(KEY_PATH_PUBLIC_KEY);
        } catch (Exception e) {
            if (DebugConfig.getInstance().isDebug()) {
                System.out.println("Exception while generating public Key in getAllClaimsFromToken -------> " + e);
            }
        }
        JwtParser parser = Jwts.parserBuilder().setSigningKey(publicKey).build();
        claims = parser.parseClaimsJws(token).getBody();
        return claims;
    }

    public String getUsernameFromToken(String token) {
        String sub = null;
        try {
            Claims claims = getAllClaimsFromToken(token);
            sub = claims.getSubject();
        } catch (Exception e) {
            if (e instanceof ExpiredJwtException) {
                String username = ((ExpiredJwtException) e).getClaims().getSubject();
                return username;
            } else {
                e.printStackTrace();
            }
        }
        return sub;
    }

    public List<Long> getAllAccountIdsFromToken(String token) {
        try {
//            Claims claims = getAllClaimsFromToken(token);
//            List<Long> accountIds = (List<Long>) claims.get("acountIds");
//            return accountIds;
            Claims claims = getAllClaimsFromToken(token);
            List<?> accountIds = (List<?>) claims.get("acountIds");

            if (accountIds == null) {
                return null;
            }

            List<Long> longAccountIds = new ArrayList<>();
            for (Object accountIdObj : accountIds) {
                if (accountIdObj instanceof Number) {
                    longAccountIds.add(((Number) accountIdObj).longValue());
                } else {
                    throw new IllegalArgumentException("Invalid account ID: " + accountIdObj);
                }
            }
            return longAccountIds;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public Date getExpirationDateFromToken(String token) {
        try {
            Claims claims = getAllClaimsFromToken(token);
            return claims.getExpiration();
        } catch (Exception e) {
            if (e instanceof ExpiredJwtException) {
                Claims claims = ((ExpiredJwtException) e).getClaims();
                return claims.getExpiration();
            } else {
                e.printStackTrace();
            }
        }
        return null;
    }

    public Boolean isTokenExpired(String token) {
        final Date expiration = getExpirationDateFromToken(token);
        if (expiration != null) {
            return expiration.before(new Date());
        }
        return true;
    }

    public String generateToken(User user, List<Long> accountIds) {
        Boolean isZeroAccountIdPresent = false;
        if (accountIds != null) {
            if (accountIds.contains(0L)) {
                isZeroAccountIdPresent = true;
            }
            if (!accountIds.isEmpty() && !(accountIds.size() == 1 && isZeroAccountIdPresent)) {
                accountIds = userAccountRepository.findAllAccountIdsByAccountIdInAndIsActiveAndIsVerifiedTrue(accountIds, true);
            }
        }
        if (isZeroAccountIdPresent && !accountIds.contains(0L)) {
            accountIds.add(0L);
        }
        if (user != null) {
            Map<String, Object> claims = new HashMap<>();
            Map<Long, Integer> accountRoleMap = new HashMap<>();
            claims.put("acountIds", accountIds);
            claims.put("chatPassword", userService.getUser(user.getUsername()).getChatPassword());
            return doGenerateToken(claims, user.getUsername());
        }
        return null;
    }

    private String doGenerateToken(Map<String, Object> claims, String username) {
        Long expirationTimeLong = Long.parseLong(expirationTime); //in seconds for 30 days
        final Date createdDate = new Date();
        final Date expirationDate = new Date(createdDate.getTime() + expirationTimeLong * 1000);

        PrivateKey privateKey = null;
        try {
            privateKey = CommonUtils.getPrivateKey(KEY_PATH_PRIVATE_KEY);
        } catch (Exception e) {
            if (DebugConfig.getInstance().isDebug()) {
                System.out.println("Exception while reading the private key in doGenerateToken -------> " + e);
            }
        }
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(createdDate)
                .setExpiration(expirationDate)
                .signWith(privateKey, SignatureAlgorithm.RS512)
                .compact();
    }

    private Boolean verifyToken(String token, PublicKey publicKey) {

        Claims claims;
        try {
            JwtParser parser = Jwts.parserBuilder().setSigningKey(publicKey).build();
            claims = parser.parseClaimsJws(token).getBody();
            if (DebugConfig.getInstance().isDebug()) {
                System.out.println("claims in verifyToken ------>" + claims);
                System.out.println("Id from claims in verifyToken ------> " + claims.get("id"));
                System.out.println("Role from claims in verifyToken ------> " + claims.get("role"));
                System.out.println("Subject from claims in verifyToken ------> " + claims.get("subject"));
            }

        } catch (Exception e) {
            claims = null;
        }
        return !isTokenExpired(token);
    }

    public Boolean validateToken(String token) {
        PublicKey publicKey = null;
        try {
            publicKey = CommonUtils.getPublicKey(KEY_PATH_PUBLIC_KEY);
        } catch (Exception e) {
            if (DebugConfig.getInstance().isDebug()) {
                System.out.println("Exception while generating public Key in validateToken ------> " + e);
            }
        }
        boolean result = verifyToken(token, publicKey);
        if (DebugConfig.getInstance().isDebug()) {
            System.out.println("Is Token Validated in validateToken ---------->" + result);
        }
        return result;

    }

}


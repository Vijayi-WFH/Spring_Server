package com.example.chat_app.config;

import com.example.chat_app.dto.WebSocketUrlHeaders;
import com.example.chat_app.jwtUtils.JWTUtil;
import com.example.chat_app.model.User;
import com.example.chat_app.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class WebSocketHandshakeInterceptor implements HandshakeInterceptor {

    private final JWTUtil jwtUtil;
    private final UserRepository userRepository;

    @Autowired
    public WebSocketHandshakeInterceptor(JWTUtil jwtUtil, UserRepository userRepository) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    @Value("${tseserver.application.root.path}")
    private String tseServerBaseUrl;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        String token = request.getHeaders().getFirst("Authorization");
        String path = request.getURI().getQuery();
        WebSocketUrlHeaders requestHeaders = new WebSocketUrlHeaders();
        if (path != null) {
            String[] queryParams = path.split("&");
            for (String param : queryParams) {
                if (token == null && param.startsWith("Authorization=")) {
                    token = param.split("=")[1];
                }
                if (param.startsWith("timeZone=")) {
                    String timezone = param.split("=")[1];
                    requestHeaders.setTimeZone(timezone);
                }
            }
        }
        requestHeaders.setAuthorization(token);
        List<Long> accountIds = List.of();
        if (request.getHeaders().getFirst("accountIds") == null) {
            if (path != null) {
                String[] queryParams = path.split("&");
                for (String param : queryParams) {
                    if (param.startsWith("accountIds=")) {
                        // Parsing accountIds
                        String accountIdsStr = param.split("=")[1];
                        requestHeaders.setAccountIds(accountIdsStr);
                        accountIds = Arrays.stream(accountIdsStr.split(","))
                                .map(Long::parseLong)
                                .collect(Collectors.toList());
                        break;
                    }
                }
            }
        } else {
            accountIds = Arrays.stream(request.getHeaders().getFirst("accountIds").split(",")).map(Long::valueOf).collect(Collectors.toList());
            requestHeaders.setAccountIds(request.getHeaders().getFirst("accountIds"));
        }
        String screenName = request.getHeaders().getFirst("screenName");
        requestHeaders.setScreenName(screenName);
        jwtUtil.validateTokenAndAccountIds(token, screenName, accountIds);
        if (!accountIds.isEmpty()) {
            long userId = 0L;
            if (path != null) {
                String[] queryParams = path.split("&");
                for (String param : queryParams) {
                    if (param.startsWith("userId=")) {
                        userId = Long.parseLong(param.split("=")[1]);
                        requestHeaders.setUserId(userId);
                        break;
                    }
                }
            }
            User user = userRepository.findFirstByAccountIdInAndIsActive(accountIds, true);
            attributes.put("HEADERS_ATTRIBUTE", requestHeaders);
            return Objects.equals(userId, user.getUserId());
        }
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {

    }
}
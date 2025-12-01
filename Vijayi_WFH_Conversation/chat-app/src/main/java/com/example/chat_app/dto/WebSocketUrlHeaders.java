package com.example.chat_app.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
public class WebSocketUrlHeaders {

    private Long userId;
    private String screenName;
    private String accountIds;
    private String timeZone;
    private String authorization;

    public WebSocketUrlHeaders(Long userId, String screenName, List<Long> accountIds, String timeZone, String authorization) {
        this.userId = userId;
        this.screenName = screenName;
        this.accountIds = accountIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        this.timeZone = timeZone;
        this.authorization = authorization;
    }
}

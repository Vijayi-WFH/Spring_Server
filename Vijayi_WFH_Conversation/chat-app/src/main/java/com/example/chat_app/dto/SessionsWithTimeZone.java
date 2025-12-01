package com.example.chat_app.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.socket.WebSocketSession;

@Getter
@Setter
public class SessionsWithTimeZone {

    private final WebSocketSession session;
    private final String timeZone;

    public SessionsWithTimeZone(WebSocketSession session, String timeZone) {
        this.session = session;
        this.timeZone = timeZone != null ? timeZone : "UTC"; // Default to UTC if null
    }
}

package com.example.chat_app.constants;

public class DtoValidationErrors {

    public static class MessageUsersDto {
        public static final String NON_EMPTY_MESSAGE_ID = "Message Id can't be null or empty for this API. Please provide one.";
        public static final String NON_EMPTY_SENDER_ID = "Sender Id can't be null or empty for this API. Please provide one.";
    }
}

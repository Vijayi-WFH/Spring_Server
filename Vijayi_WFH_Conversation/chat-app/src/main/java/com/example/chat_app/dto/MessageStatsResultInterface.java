package com.example.chat_app.dto;

public interface MessageStatsResultInterface {
    Long getMessageId();
    Long getSenderId();
    Integer getDeliveredCount();
    Integer getReadCount();
    Integer getGroupSize();
}

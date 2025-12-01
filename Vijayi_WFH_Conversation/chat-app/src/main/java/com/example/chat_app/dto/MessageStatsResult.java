package com.example.chat_app.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MessageStatsResult {

        private Long messageId;
        private Long senderId;
        private Long readCount;
        private Long deliveredCount;
        private String tickStatus;
}

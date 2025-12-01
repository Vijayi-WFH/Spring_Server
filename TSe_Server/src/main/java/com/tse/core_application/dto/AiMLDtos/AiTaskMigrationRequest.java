package com.tse.core_application.dto.AiMLDtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class AiTaskMigrationRequest {

    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private String encryptedKey;

}

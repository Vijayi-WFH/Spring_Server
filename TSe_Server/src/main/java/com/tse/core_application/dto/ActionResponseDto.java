package com.tse.core_application.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ActionResponseDto {
        private Integer actionId;
        private String actionName;
        private String actionDesc;
    }



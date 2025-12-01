package com.tse.core_application.dto.org_response;

import lombok.Data;

import java.util.List;

@Data
public class BuDetail {
    private Long buId;
    private String buName;
    private List<ProjectDetail> project;
}

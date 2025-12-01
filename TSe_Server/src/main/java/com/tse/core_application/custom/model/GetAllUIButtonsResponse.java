package com.tse.core_application.custom.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class GetAllUIButtonsResponse {

    private List<DashboardButtonIdDisplayName> dashboard;
}

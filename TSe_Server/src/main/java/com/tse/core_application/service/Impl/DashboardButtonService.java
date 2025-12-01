package com.tse.core_application.service.Impl;

import com.tse.core_application.constants.Constants;
import com.tse.core_application.custom.model.DashboardButtonIdDisplayName;
import com.tse.core_application.custom.model.GetAllUIButtonsResponse;
import com.tse.core_application.exception.NoDataFoundException;
import com.tse.core_application.handlers.CustomResponseHandler;
import com.tse.core_application.handlers.StackTraceHandler;
import com.tse.core_application.model.DashboardButtons;
import com.tse.core_application.repository.DashboardButtonRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class DashboardButtonService {

    private static final Logger logger = LogManager.getLogger(DashboardButtonService.class.getName());

    @Autowired
    private DashboardButtonRepository dashboardButtonRepository;

    public GetAllUIButtonsResponse getAllButtons() {
        List<DashboardButtons> allButtonsFoundDb = dashboardButtonRepository.findAll();
        List<DashboardButtonIdDisplayName> dashboardButtonIdDisplayNameList = new ArrayList<>();

        for(DashboardButtons button: allButtonsFoundDb) {
            DashboardButtonIdDisplayName dashboardButtonIdDisplayNameToAdd = new DashboardButtonIdDisplayName();
            dashboardButtonIdDisplayNameToAdd.setDashboardButtonName(button.getDashboardButtonName());
            dashboardButtonIdDisplayNameToAdd.setDashboardButtonId(button.getDashboardButtonId());
            dashboardButtonIdDisplayNameToAdd.setDashboardButtonDisplayName(button.getDashboardButtonDisplayName());
            dashboardButtonIdDisplayNameList.add(dashboardButtonIdDisplayNameToAdd);
        }
        GetAllUIButtonsResponse getAllUIButtonsResponse = new GetAllUIButtonsResponse();
        getAllUIButtonsResponse.setDashboard(dashboardButtonIdDisplayNameList);

        return getAllUIButtonsResponse;
    }

    public ResponseEntity<Object> getFormattedResponseOfGetAllUIButtons(GetAllUIButtonsResponse getAllUIButtonsResponse) {
        if(getAllUIButtonsResponse.getDashboard().isEmpty()) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(new NoDataFoundException());
            logger.error("No data found. ", new Throwable(allStackTraces));
            ThreadContext.clearMap();
            throw new NoDataFoundException();
        } else {
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, getAllUIButtonsResponse);
        }
    }
}

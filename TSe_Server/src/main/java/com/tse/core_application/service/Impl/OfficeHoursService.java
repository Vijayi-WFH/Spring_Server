package com.tse.core_application.service.Impl;

import com.tse.core_application.model.OfficeHours;
import com.tse.core_application.repository.OfficeHoursRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OfficeHoursService {

    @Autowired
    private OfficeHoursRepository officeHoursRepository;

    public OfficeHours getOfficeHoursByKeyAndWorkflowTypeId(String key, Integer workflowTypeId) {
        OfficeHours foundOfficeHours = officeHoursRepository.findByKeyAndWorkflowTypeId(key, workflowTypeId);
        return foundOfficeHours;
    }
}

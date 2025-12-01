package com.tse.core_application.service.Impl;

import com.tse.core_application.custom.model.PriorityIdDescDisplayAs;
import com.tse.core_application.model.Priority;
import com.tse.core_application.repository.PriorityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PriorityService {

    @Autowired
    private PriorityRepository priorityRepository;

    public List<PriorityIdDescDisplayAs> getAllPriority() {
        List<PriorityIdDescDisplayAs> priorityIdDescDisplayAsList = priorityRepository.getPriorityIdDescDisplayAs();
        return priorityIdDescDisplayAsList;
    }
}

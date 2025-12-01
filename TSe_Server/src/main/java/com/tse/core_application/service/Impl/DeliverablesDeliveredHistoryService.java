package com.tse.core_application.service.Impl;

import com.tse.core_application.model.DeliverablesDelivered;
import com.tse.core_application.model.DeliverablesDeliveredHistory;
import com.tse.core_application.repository.DeliverablesDeliveredHistoryRepository;
import com.tse.core_application.repository.DeliverablesDeliveredRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DeliverablesDeliveredHistoryService {

    @Autowired
    private DeliverablesDeliveredService  deliverablesDeliveredService;

    @Autowired
    private DeliverablesDeliveredHistoryRepository deliverablesDeliveredHistoryRepository;

    @Autowired
    private DeliverablesDeliveredRepository deliverablesDeliveredRepository;

    /***
     * This method is used to add the previous version of a deliverablesDelivered to deliverablesDeliveredHistory table
     */
    public DeliverablesDeliveredHistory addDeliverablesDeliveredHistory(DeliverablesDelivered deliverablesDelivered) {
        DeliverablesDelivered deliverablesDeliveredFoundDb = deliverablesDeliveredRepository.findByDeliverablesDeliveredLogId(deliverablesDelivered.getDeliverablesDeliveredLogId());
        DeliverablesDeliveredHistory deliverablesDeliveredHistory = new DeliverablesDeliveredHistory();
        BeanUtils.copyProperties(deliverablesDeliveredFoundDb, deliverablesDeliveredHistory);
        return deliverablesDeliveredHistoryRepository.save(deliverablesDeliveredHistory);
    }
}

package com.tse.core_application.service.Impl;

import com.tse.core_application.constants.Constants;
import com.tse.core_application.custom.model.ListOfDeliverablesDeliveredId;
import com.tse.core_application.model.DeliverablesDelivered;
import com.tse.core_application.model.Task;
import com.tse.core_application.repository.DeliverablesDeliveredRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Service
public class DeliverablesDeliveredService {


    @Autowired
    DeliverablesDeliveredHistoryService deliverablesDeliveredHistoryService;
    @Autowired
    DeliverablesDeliveredRepository deliverablesDeliveredRepository;

    /**
     * This function updates deliverablesDelivered and deliverablesDeliveredHistory table with all the deliverablesDelivered (newly added , updated or deleted) received in the task request
     */
    public List<DeliverablesDelivered> updateAllDeliverablesDelivered(List<DeliverablesDelivered> listOfDeliverablesDelivered, Task task) {
        List<DeliverablesDelivered>  deliverablesDeliveredToDelete = new ArrayList<>();
        List<DeliverablesDelivered>  deliverablesDeliveredUpdated = new ArrayList<>();
        List<DeliverablesDelivered> deliverablesDeliveredToAdd = new ArrayList<>();
        if (listOfDeliverablesDelivered != null && !listOfDeliverablesDelivered.isEmpty()) {
            Long listOfDeliverablesDeliveredId = task.getListOfDeliverablesDeliveredId() != null ? task.getListOfDeliverablesDeliveredId() : getMaxListOfDeliverablesDeliveredId();
            for (DeliverablesDelivered n : listOfDeliverablesDelivered) {
                if (isDeliverablesDeliveredDeleted(n)) {
                    deliverablesDeliveredToDelete.add(n);
                } else {
                    if (isDeliverablesDeliveredUpdated(n)) {
                        deliverablesDeliveredUpdated.add(n);
                        deliverablesDeliveredHistoryService.addDeliverablesDeliveredHistory(n);
                        deliverablesDeliveredRepository.save(n);
                    } else {
                        if (isNewDeliverablesDelivered(n)) {
                            deliverablesDeliveredToAdd.add(n);
                        }
                    }
                }
            }
            deliverablesDeliveredUpdated.addAll(addNewDeliverablesDeliveredOnUpdateTask(deliverablesDeliveredToAdd, listOfDeliverablesDeliveredId, task));
            if (!deliverablesDeliveredToDelete.isEmpty())
                deleteAllListOfDeliverablesDelivered(deliverablesDeliveredToDelete);
        }
        return deliverablesDeliveredUpdated;
    }

    /**
     * This function adds all the newly added deliverables to deliverablesDelivered table
     */
    public List<DeliverablesDelivered> addNewDeliverablesDeliveredOnUpdateTask(List<DeliverablesDelivered> listOfDeliverablesDelivered, Long listOfDeliverablesDeliveredId, Task taskAdded) {
        List<DeliverablesDelivered> listOfDeliverablesDeliveredToAdd = new ArrayList<>();

        for (DeliverablesDelivered n : listOfDeliverablesDelivered) {
            DeliverablesDelivered addDeliverablesDelivered = new DeliverablesDelivered();
            addDeliverablesDelivered.setDeliverablesDelivered(n.getDeliverablesDelivered());
            addDeliverablesDelivered.setIsDeleted(Constants.Task_DeliverablesDelivered_Status.DELIVERABLES_DELIVERED_NOT_DELETED);
            addDeliverablesDelivered.setCreatedByAccountId(n.getCreatedByAccountId());
            addDeliverablesDelivered.setListOfDeliverablesDeliveredId(listOfDeliverablesDeliveredId);
            addDeliverablesDelivered.setTask(taskAdded);
            listOfDeliverablesDeliveredToAdd.add(addDeliverablesDelivered);
        }
        return deliverablesDeliveredRepository.saveAll(listOfDeliverablesDeliveredToAdd);
    }


    /**
     * This function checks whether the deliverablesDelivered is a newly added one
     */

    public boolean isNewDeliverablesDelivered(DeliverablesDelivered deliverablesDelivered) {
        boolean isNewDeliverablesDelivered = false;
        if (deliverablesDelivered.getVersion() == null) {
            isNewDeliverablesDelivered = true;
        }
        return isNewDeliverablesDelivered;
    }

    /**
    * This function fetches the next ListofdeliverablesDeliveredId from the database
     */

    public Long getMaxListOfDeliverablesDeliveredId() {
        ListOfDeliverablesDeliveredId deliverablesDeliveredIdDb = deliverablesDeliveredRepository.getMaxListOfDeliverablesDeliveredId();
        if (deliverablesDeliveredIdDb.getListOfDeliverablesDeliveredId() == null){
            int intDeliverablesDeliveredId = 1;
            return (long) intDeliverablesDeliveredId;
        } else {
            return deliverablesDeliveredIdDb.getListOfDeliverablesDeliveredId() + 1;
        }
    }

    /***
     * This method checks whether the deliverablesDelivered is an updated one
     */
    public boolean isDeliverablesDeliveredUpdated(DeliverablesDelivered deliverablesDelivered) {
        boolean isUpdated = false;
        if (deliverablesDelivered.getIsUpdated() != null && deliverablesDelivered.getIsUpdated() == 1) {
            isUpdated = true;
        }
        return isUpdated;
    }

    /***
     * This method checks whether the deliverablesDelivered is a deleted one
     */
    public boolean isDeliverablesDeliveredDeleted(DeliverablesDelivered deliverablesDelivered) {
        boolean isDeleted = false;
        if (deliverablesDelivered.getIsDeleted() != null && deliverablesDelivered.getIsDeleted() == 1) {
            isDeleted = true;
        }
        return isDeleted;
    }

    /***
     * This method soft delete the deliverablesDelivered in the deliverablesDelivered table by setting the isDeleted status as 1 for deleted deliverablesDelivered
     */
    public Integer deleteAllListOfDeliverablesDelivered(List<DeliverablesDelivered> listOfDeliverablesDelivered) {
        List<Long> deliverablesDeliveredLogIds = new ArrayList<>();
        for (DeliverablesDelivered deliverablesDelivered : listOfDeliverablesDelivered) {
            deliverablesDeliveredLogIds.add(deliverablesDelivered.getDeliverablesDeliveredLogId());
        }
        return deliverablesDeliveredRepository.setIsDeletedByDeliverablesDeliveredLogIdIn(deliverablesDeliveredLogIds, Constants.Task_DeliverablesDelivered_Status.DELIVERABLES_DELIVERED_DELETED);
    }

    /***
     * This method is used in getTask request to remove the deleted deliverablesDeliverables from the list in getTaskByTaskNumber
     */
    public List<DeliverablesDelivered> removeDeletedlistOfDeliverablesDelivered(List<DeliverablesDelivered> listOfDeliverablesDelivered) {
        Iterator<DeliverablesDelivered> itr = listOfDeliverablesDelivered.iterator();
        while (itr.hasNext()) {
            DeliverablesDelivered deliverablesDelivered = itr.next();
            if (isDeliverablesDeliveredDeleted(deliverablesDelivered)) {
                itr.remove();
            }
        }
        return listOfDeliverablesDelivered;
    }
}

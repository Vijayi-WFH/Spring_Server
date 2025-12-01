package com.tse.core_application.service.Impl;

import com.tse.core_application.constants.Constants;
import com.tse.core_application.model.ActionItem;
import com.tse.core_application.model.Meeting;
import com.tse.core_application.model.Note;
import com.tse.core_application.repository.ActionItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ActionItemService {

    @Autowired
    private ActionItemRepository actionItemRepository;

    /** This method is used to update action items in request - two cases either an action item in the request can be updated or there can be a new action item in the request
     * Updated action item is identified by isUpdated field being true in the request and new action item by version being null.
     * */
    public List<ActionItem> updateActionItems(List<ActionItem> actionItems, Meeting meetingUpdated) {

        List<ActionItem> actionItemsUpdated = new ArrayList<>();
        List<ActionItem> actionItemsToAdd = new ArrayList<>();

        if (actionItems != null && !actionItems.isEmpty()) {

            for (ActionItem a : actionItems) {

                    if (isActionItemUpdatedOrDeleted(a)) {
                        if(a.getModifiedByAccountId() == null){
                            a.setModifiedByAccountId(meetingUpdated.getOrganizerAccountId());
                        }
                        a.setMeeting(meetingUpdated);
                        actionItemsUpdated.add(a);
                        actionItemRepository.save(a);
                    } else {
                        if (isNewActionItem(a)) {
                            actionItemsToAdd.add(a);
                        }
                    }
            }
            actionItemsUpdated.addAll(addNewActionItem(actionItemsToAdd, meetingUpdated));

        }
        return actionItemsUpdated;
    }

    /** This method is used to add a new action item to action item table on update meeting and returns the list of new action items added*/
    public List<ActionItem> addNewActionItem(List<ActionItem> actionItems, Meeting meeting) {
        List<ActionItem> actionItemsToAdd = new ArrayList<>();

        for (ActionItem a : actionItems) {
            ActionItem actionItem = new ActionItem();
            actionItem.setActionItem(a.getActionItem());
            actionItem.setIsImportant(a.getIsImportant());
            actionItem.setIsDeleted(false);
            if(a.getPostedByAccountId() != null) {
                actionItem.setPostedByAccountId(a.getPostedByAccountId());
            }
            else{
                actionItem.setPostedByAccountId(meeting.getOrganizerAccountId());
            }
            actionItem.setMeeting(meeting);
            actionItemsToAdd.add(actionItem);
        }
        return actionItemRepository.saveAll(actionItemsToAdd);
    }

    /** This method identifies whether the action item is new or not*/
    public boolean isNewActionItem(ActionItem actionItem) {
        return actionItem.getVersion() == null;
    }


    /** This method identifies whether the action item is updated or not*/
    public boolean isActionItemUpdatedOrDeleted(ActionItem actionItem) {
        return (actionItem.getIsUpdated() != null && actionItem.getIsUpdated())||(actionItem.getIsDeleted() != null && actionItem.getIsDeleted());
    }


    /** This method soft deletes the list of action items which are deleted (not present in request but present in database with isDeleted as false) and also returns the deleted action items list*/
    public List<ActionItem> getDeletedActionItems(List<ActionItem> actionItemsRequest, List<ActionItem> actionItemsDb){

        List<ActionItem> deletedActionItems = new ArrayList<>();

        HashSet<Long> actionItemIdsRequestSet = actionItemsRequest.stream()
                .map(ActionItem::getActionItemId)
                .filter(Objects::nonNull)  // exclude null values
                .collect(Collectors.toCollection(HashSet::new));

        for(ActionItem actionItem :  actionItemsDb){

            if(!actionItemIdsRequestSet.contains(actionItem.getActionItemId())){

                actionItemRepository.setIsDeletedByActionItemId(actionItem.getActionItemId());
                deletedActionItems.add(actionItem);
            }
        }

        return deletedActionItems;
    }


//    public Integer deleteAllNotes(List<Note> notes) {
//        List<Long> noteLogIds = new ArrayList<>();
//        for (Note note : notes) {
//            noteLogIds.add(note.getNoteLogId());
//        }
//        return noteRepository.setIsDeletedByNoteLogIdIn(noteLogIds, Constants.Task_Note_Status.NOTE_DELETED);
//    }
//
    /** This method removes the action items in the request list , which are having isDeleted field a true*/
    public List<ActionItem> removeDeletedActionItems(List<ActionItem> actionItems) {
        actionItems.removeIf(ActionItem::getIsDeleted);
        return actionItems;
    }


}

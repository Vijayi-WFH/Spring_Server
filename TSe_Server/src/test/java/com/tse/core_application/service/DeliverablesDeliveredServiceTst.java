package com.tse.core_application.service;

import com.tse.core_application.model.DeliverablesDelivered;
import com.tse.core_application.model.DeliverablesDeliveredHistory;
import com.tse.core_application.model.Task;
import com.tse.core_application.repository.DeliverablesDeliveredHistoryRepository;
import com.tse.core_application.repository.DeliverablesDeliveredRepository;
import com.tse.core_application.service.Impl.DeliverablesDeliveredHistoryService;
import com.tse.core_application.service.Impl.DeliverablesDeliveredService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class DeliverablesDeliveredServiceTst {

    @Mock
    private DeliverablesDeliveredRepository ddRepo;

    @Mock
    private DeliverablesDeliveredHistoryService ddHistoryService;

    @Mock
    private DeliverablesDeliveredHistoryRepository ddHistoryRepo;

    @Spy
    @InjectMocks
    private DeliverablesDeliveredService deliverablesDeliveredService;

    @Test
    public void updateAllDeliverablesDelivered_whenNewDeliverableAdded() {

        Task task = mock(Task.class);
        DeliverablesDelivered dd = mock(DeliverablesDelivered.class);
        DeliverablesDeliveredHistory ddHistory = mock(DeliverablesDeliveredHistory.class);

        List<DeliverablesDelivered> listOfDeliverablesDelivered = new ArrayList<>();
        DeliverablesDelivered d1 = new DeliverablesDelivered();
        d1.setListOfDeliverablesDeliveredId(1L);
        d1.setDeliverablesDelivered("Hey this is 1st deliverable");
        d1.setVersion(null);
        listOfDeliverablesDelivered.add(d1);
        DeliverablesDelivered d2 = new DeliverablesDelivered();
        d2.setListOfDeliverablesDeliveredId(1L);
        d2.setDeliverablesDelivered("Hey this is 2nd deliverable");
        d2.setVersion(null);
        listOfDeliverablesDelivered.add(d2);

        // Set up mock behavior
        lenient().when(task.getListOfDeliverablesDeliveredId()).thenReturn(1L);
        lenient().when(task.toString()).thenReturn(null);
        lenient().when(ddHistoryRepo.save(any(DeliverablesDeliveredHistory.class))).thenReturn(ddHistory);
        lenient().when(ddHistoryService.addDeliverablesDeliveredHistory(any(DeliverablesDelivered.class))).thenReturn(ddHistory);
        lenient().when(ddRepo.save(any(DeliverablesDelivered.class))).thenReturn(dd);
//        lenient().doReturn(false).when(deliverablesDeliveredService).isDeliverablesDeliveredDeleted(any(DeliverablesDelivered.class));
//        lenient().doReturn(false).when(deliverablesDeliveredService).isDeliverablesDeliveredUpdated(any(DeliverablesDelivered.class));
//        lenient().doReturn(true).when(deliverablesDeliveredService).isNewDeliverablesDelivered(any(DeliverablesDelivered.class));
        lenient().when(deliverablesDeliveredService.isDeliverablesDeliveredDeleted(d1)).thenReturn(false);
        lenient().when(deliverablesDeliveredService.isDeliverablesDeliveredUpdated(d1)).thenReturn(false);
        lenient().when(deliverablesDeliveredService.isNewDeliverablesDelivered(d1)).thenReturn(true);
        lenient().when(deliverablesDeliveredService.isDeliverablesDeliveredDeleted(d2)).thenReturn(false);
        lenient().when(deliverablesDeliveredService.isDeliverablesDeliveredUpdated(d2)).thenReturn(false);
        lenient().when(deliverablesDeliveredService.isNewDeliverablesDelivered(d2)).thenReturn(true);
        lenient().when(deliverablesDeliveredService.addNewDeliverablesDeliveredOnUpdateTask(listOfDeliverablesDelivered, 1L, task)).thenReturn(List.of(d1, d2));
//        lenient().when(deliverablesDeliveredService.isDeliverablesDeliveredDeleted(any(DeliverablesDelivered.class))).thenReturn(false);
//        lenient().when(deliverablesDeliveredService.isDeliverablesDeliveredUpdated(any(DeliverablesDelivered.class))).thenReturn(false);
//        lenient().when(deliverablesDeliveredService.isNewDeliverablesDelivered(any(DeliverablesDelivered.class))).thenReturn(true);


        // Call the function
        List<DeliverablesDelivered> result = deliverablesDeliveredService.updateAllDeliverablesDelivered(listOfDeliverablesDelivered, task);
        assertEquals(2, result.size());

    }
}










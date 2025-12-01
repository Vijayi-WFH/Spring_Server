package com.tse.core_application.service;

import com.tse.core_application.service.Impl.TaskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.*;

import static org.junit.Assert.*;

@SpringBootTest
public class TaskServiceConcurrencyTest {

    @Autowired
    private TaskService taskService;

    @Test
    public void testConcurrentTaskIdentifierGeneration() throws InterruptedException {
        int numberOfThreads = 20;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CyclicBarrier barrier = new CyclicBarrier(numberOfThreads);
        ConcurrentHashMap<Long, Boolean> identifiers = new ConcurrentHashMap<>();
        Long teamId = 337L;

        // Make sure the sequence for the team is initialized
//        taskService.initializeTaskSequence(teamId);

        Runnable task = () -> {
            try {
                barrier.await(); // Ensure all threads start at the same time
                Long id = taskService.getNextTaskIdentifier(teamId);
                identifiers.put(id, true);
            } catch (Exception e) {
                fail("Exception in task execution: " + e.getMessage());
            }
        };

        for (int i = 0; i < numberOfThreads; i++) {
            executorService.submit(task);
        }

        executorService.shutdown();
        assertTrue(executorService.awaitTermination(1, TimeUnit.MINUTES));

        // Check all identifiers are unique
        assertEquals(numberOfThreads, identifiers.size());
    }

}




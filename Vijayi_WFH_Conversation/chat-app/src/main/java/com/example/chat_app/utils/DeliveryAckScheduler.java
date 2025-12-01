package com.example.chat_app.utils;

import com.example.chat_app.constants.Constants;
import com.example.chat_app.controller.WebSocketController;
import com.example.chat_app.dto.MessageStatsResultInterface;
import com.example.chat_app.dto.ReadReceiptsResponse;
import com.example.chat_app.repository.MessageStatsRepository;
import com.example.chat_app.repository.MessageUserRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class DeliveryAckScheduler {

    private static final Logger log = LogManager.getLogger(DeliveryAckScheduler.class);

    // The thread-safe buffer for accumulating incoming ACKs
    // Key: messageId (long), Value: Set of accountIds (Set<Long>)
    private volatile ConcurrentHashMap<String, Long> deliveryReceiptBuffer = new ConcurrentHashMap<>();

    // The time interval for batch processing (1.5 seconds)
    private static final long BATCH_INITIAL_STARTUP = 5000;
    private static final int BATCH_INTERVAL_MILLIS = 2000; // Let's use 500ms for a tighter batch interval

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private WebSocketController webSocketController;

    @Autowired
    private MessageUserRepository messageUserRepository;

    @Autowired
    private MessageStatsRepository messageStatsRepository;

    public DeliveryAckScheduler() {
        // Start the scheduled job to run every 1500ms
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(
                this::processBatch,
                BATCH_INITIAL_STARTUP,
                BATCH_INTERVAL_MILLIS,
                TimeUnit.MILLISECONDS
        );
    }

    /**
     * Public method used by the Chat Server's message-handling threads
     * to submit a new delivery acknowledgment.
     */
    public void addMessageAckValue(Long receiverId ,String type, Long messageId, String contextType ,Long contextId) {
        //If key does not exist, merge behaves like put(key, newValue).
        //If key already exists, it computes Long.max(oldValue, newValue) and stores the result.
        String key = receiverId + "-" + contextType + "-" + contextId + "-" + type;
        deliveryReceiptBuffer.merge(key, messageId, Long::max);
        // System.out.printf("ACK added: M%d by A%d. Buffer size: %d%n", messageId, accountId, deliveryReceiptBuffer.size());
    }

    /**
     * The core scheduled method that swaps the buffer and executes the bulk update.
     */
//    @Scheduled(fixedDelay = 1500)
    private void processBatch() {
        // --- A. SWAP/COPY THE BUFFER (Atomic Operation) ---
        // Create a new empty buffer.
        ConcurrentHashMap<String, Long> newBuffer = new ConcurrentHashMap<>();

        // Atomically replace the current buffer with the new empty one.
        // This is done by assigning the reference, which is extremely fast and doesn't block
        // incoming ACKs for long.
        ConcurrentHashMap<String, Long> bufferToProcess = this.deliveryReceiptBuffer;
        this.deliveryReceiptBuffer = newBuffer;

        if (bufferToProcess.isEmpty()) {
            // System.out.println("Scheduler run: Buffer empty. Skipping DB update.");
            return;
        }
        System.out.println("\n--- Scheduler Triggered: Processing Batch (" + bufferToProcess.size() + " messages) ---" + LocalDateTime.now());


        // --- D. GROUP STATUS UPDATE & E. PUSH STATUS ---
        // In a real application, you would iterate the bufferToProcess map
        // and send real-time status updates (via WebSocket/PubSub) to the sender(s).
        List<Long> deliveredMessageIds = new ArrayList<>();
        List<Long> readMessageIds = new ArrayList<>();
        bufferToProcess.forEach((key, maxMessageId) -> {
            String[] keyArray = key.split("-");
            Long receiverId = Long.valueOf(keyArray[0]);
            String contextType = keyArray[1];
            Long contextId = Long.valueOf(keyArray[2]);
            String type = keyArray[3];


            if(type.equals(Constants.MessageStatusType.READ_ACK)) {
                if(contextType.equals(Constants.MessageStatusType.GROUP)) {
                    readMessageIds.addAll(messageUserRepository.markAsReadForGroupMessagesInBulk(contextId, maxMessageId, receiverId));
                } else if(contextType.equals(Constants.MessageStatusType.DIRECT)) {
                    readMessageIds.addAll(messageUserRepository.markAsReadForDirectMessagesInBulk(contextId, maxMessageId, receiverId));
                }
            }
            else if(type.equals(Constants.MessageStatusType.DELIVERY_ACK)) {
                if (contextType.equals(Constants.MessageStatusType.GROUP)) {
                    deliveredMessageIds.addAll(messageUserRepository.markAsDeliveredForGroupMessagesInBulk(contextId, maxMessageId, receiverId));
                } else if (contextType.equals(Constants.MessageStatusType.DIRECT)) {
                    deliveredMessageIds.addAll(messageUserRepository.markAsDeliveredForDirectMessagesInBulk(contextId, maxMessageId, receiverId));
                }
            }

        });
        // to update GroupMessageStatus table (deliveredCount = deliveredCount + accountIds.size())
        // to send WebSocket notification to the original sender.
        if(!deliveredMessageIds.isEmpty()) {
            updateStatsAndNotifySender(deliveredMessageIds, Constants.MessageStatusType.DELIVERED);
        }
        if(!readMessageIds.isEmpty()) {
            updateStatsAndNotifySender(readMessageIds, Constants.MessageStatusType.READ);
        }
        log.info("Delivered MessageIds been updated and sent to Receiver is: {} and receiverMessageIds are: {}", deliveredMessageIds, readMessageIds);
    }

    private void updateStatsAndNotifySender(List<Long> messageIds, String updateType) {

        List<MessageStatsResultInterface> statsList;

        if (Constants.MessageStatusType.DELIVERED.equals(updateType)) {
            statsList = messageStatsRepository.incrementAndFetchDelivered(messageIds);
        } else {
            statsList = messageStatsRepository.incrementAndFetchRead(messageIds);
        }
        for (MessageStatsResultInterface stat : statsList) {
            String tickStatus = calculateTickStatus(stat.getDeliveredCount(), stat.getReadCount(), stat.getGroupSize());

            ReadReceiptsResponse payload = new ReadReceiptsResponse(
                    stat.getMessageId(),
                    tickStatus,
                    stat.getDeliveredCount(),
                    stat.getReadCount(),
                    stat.getGroupSize(),
                    updateType
            );
            webSocketController.sendAcknowledgementReceivedStatus(updateType, stat.getSenderId(), payload);
        }
    }

    public static String calculateTickStatus(Integer deliveryCount, Integer readCount, Integer groupSize) {
        // Priority 1: All Read (Double Blue)
        if (readCount >= groupSize) {
            return Constants.ReadReceiptsStatus.DOUBLE_BLUE_TICK;
        }
        // Priority 2: All Delivered (Double Gray)
        if (deliveryCount >= groupSize) {
            return Constants.ReadReceiptsStatus.DOUBLE_TICK;
        }

//        // Priority 3: Partial Read (Optional - usually just shown as Double Tick)
//        if (stat.getReadCount() > 0) {
//            return "DOUBLE_BLUE_PARTIAL";
//        }

        // Priority 4: Partial Delivery (Standard Double Tick)
        // Most apps show Double Tick as soon as 1 person gets it,
        // but strict logic waits for all. Adjust based on your preference.
//        if (stat.getDeliveredCount() > 0) {
//            return "DOUBLE_TICK_PARTIAL";
//        }
        return Constants.ReadReceiptsStatus.SINGLE_TICK; // Single Tick
    }
}
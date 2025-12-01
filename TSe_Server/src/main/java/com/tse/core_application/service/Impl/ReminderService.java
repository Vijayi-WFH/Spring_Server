package com.tse.core_application.service.Impl;

import com.tse.core_application.custom.model.EmailFirstLastAccountId;
import com.tse.core_application.dto.AllReminderResponse;
import com.tse.core_application.dto.ReminderRequest;
import com.tse.core_application.dto.DateRequest;
import com.tse.core_application.dto.ReminderResponse;
import com.tse.core_application.exception.ValidationFailedException;
import com.tse.core_application.model.Constants;
import com.tse.core_application.model.Reminder;
import com.tse.core_application.model.UserAccount;
import com.tse.core_application.repository.ReminderRepository;
import com.tse.core_application.repository.UserAccountRepository;
import com.tse.core_application.utils.CommonUtils;
import com.tse.core_application.utils.DateTimeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
public class ReminderService {

    private static final Logger logger = LogManager.getLogger(ReminderService.class.getName());

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private ReminderRepository reminderRepository;

    @Autowired
    private AuditService auditService;

    /**
     * This api adds reminder to the reminder table
     */
    public ReminderResponse addReminder (ReminderRequest request, String timeZone) throws IllegalAccessException, InvocationTargetException {
        Reminder reminder = new Reminder();
        ReminderResponse reminderResponse = new ReminderResponse();
        normaliseDate(request, timeZone);
        LocalDateTime localDateTime = LocalDateTime.of(request.getReminderDate(), request.getReminderTime());
        if(localDateTime.isBefore(LocalDateTime.now()))
            throw new IllegalStateException("User not allowed to create reminder in past");
        BeanUtils.copyProperties(request, reminder);
        UserAccount userAccount = userAccountRepository.findByAccountIdAndIsActive(request.getAccountIdCreator(), true);
        if (userAccount == null) {
            throw new IllegalAccessException("User not authorized to create reminder");
        }
        boolean isExists = reminderRepository.existsReminder(request.getAccountIdCreator(), request.getReminderStatus(), request.getReminderDate(), request.getReminderTime(), request.getReminderTitle());
        if (isExists) {
            throw new ValidationFailedException("A reminder with this title and time already exists");
        }
        reminder.setFkAccountIdCreator(userAccount);
        reminder.setReminderStatus(Constants.ReminderStatusEnum.PENDING.getStatus());
        reminderRepository.save(reminder);
        auditService.auditForReminder(userAccount, reminder, false);
        reminderResponse = getReminderResponse(reminder, timeZone);
        return reminderResponse;
    }

    /**
     * This api updates the reminder
     */
    public ReminderResponse updateReminder (Long reminderId, ReminderRequest request, String timeZone, String accountIds) throws IllegalAccessException, InvocationTargetException {
        normaliseDate(request,timeZone);
        ReminderResponse reminderResponse = new ReminderResponse();
        Reminder reminder = reminderRepository.findByReminderId(reminderId);
        if(reminder == null)
            throw new IllegalStateException("Reminder not found");
        validateUser(reminder.getFkAccountIdCreator().getAccountId(), accountIds);
        if(Objects.equals(reminder.getReminderStatus(),Constants.ReminderStatusEnum.DELETED.getStatus()) && !Objects.equals(request.getReminderStatus(),Constants.ReminderStatusEnum.DELETED.getStatus()))
            throw new IllegalAccessException("Reminder is deleted");
        if(Objects.equals(reminder.getReminderStatus(),Constants.ReminderStatusEnum.DELETED.getStatus()))
            throw new IllegalAccessException("Reminder is already deleted");
        if(Objects.equals(reminder.getReminderStatus(),Constants.ReminderStatusEnum.COMPLETED.getStatus()) && !Objects.equals(request.getReminderStatus(),Constants.ReminderStatusEnum.COMPLETED.getStatus()))
            throw new IllegalAccessException("Reminder has been marked Completed");
        if(Objects.equals(reminder.getReminderStatus(),Constants.ReminderStatusEnum.COMPLETED.getStatus()))
            throw new IllegalAccessException("Reminder has already been marked Completed");
        if(!Objects.equals(request.getReminderStatus(),Constants.ReminderStatusEnum.PENDING.getStatus()) && !Objects.equals(request.getReminderStatus(),Constants.ReminderStatusEnum.COMPLETED.getStatus()) && !Objects.equals(request.getReminderStatus(),Constants.ReminderStatusEnum.DELETED.getStatus()))
            throw new IllegalStateException("Please provide valid state");
        if(!Objects.equals(request.getReminderDate(),reminder.getReminderDate()) || !Objects.equals(request.getReminderTime(),reminder.getReminderTime())) {
            LocalDateTime localDateTime = LocalDateTime.of(request.getReminderDate(), request.getReminderTime());
            if (localDateTime.isBefore(LocalDateTime.now()))
                throw new IllegalStateException("User cannot schedule reminders for past dates");
        }
        if (request.getIsEarlyReminderSet() != null && request.getIsEarlyReminderSet() && request.getEarlyReminderTime() != null
                && request.getEarlyReminderTime().isAfter(LocalDateTime.of(reminder.getReminderDate(), reminder.getReminderTime()))) {
            throw new ValidationFailedException("Early reminder time could not be after the actual reminder set time.");
        }
        BeanUtils.copyProperties(request, reminder);
        reminderRepository.save(reminder);
        auditService.auditForReminder(reminder.getFkAccountIdCreator(), reminder, true);
        reminderResponse = getReminderResponse(reminder, timeZone);
        return reminderResponse;
    }

    /**
     * This api deletes reminder by setting reminder status deleted
     */
//    public String deleteReminder(Long reminderId, String accountIds) throws IllegalAccessException {
//        Optional<Reminder> optionalReminder = reminderRepository.findById(reminderId);
//        if (optionalReminder.isEmpty()) {
//            throw new IllegalStateException("Reminder not found");
//        }
//        Reminder reminder = optionalReminder.get();
//        validateUser(reminder.getFkAccountIdCreator().getAccountId(), accountIds);
//        if (Objects.equals(reminder.getReminderStatus(), Constants.ReminderStatusEnum.DELETED.getStatus())) {
//            throw new ValidationFailedException("Reminder already deleted");
//        }
//        reminder.setReminderStatus(Constants.ReminderStatusEnum.DELETED.getStatus());
//        reminderRepository.save(reminder);
//        return "Reminder successfully deleted";
//    }

    /**
     * This api gets reminder by reminder id
     */
    public ReminderResponse getReminder(Long reminderId, String accountIds, String timeZone) throws IllegalAccessException, InvocationTargetException {
        ReminderResponse reminderResponse = new ReminderResponse();
        Optional<Reminder> optionalReminder = reminderRepository.findById(reminderId);
        if (optionalReminder.isEmpty()) {
            throw new IllegalStateException("Reminder not found");
        }
        Reminder reminder = optionalReminder.get();
        validateUser(reminder.getFkAccountIdCreator().getAccountId(), accountIds);
        reminderResponse = getReminderResponse(reminder, timeZone);
        return reminderResponse;
    }

    /**
     * This function reutrns all the reminders for provided date
     */
    public List<ReminderResponse> getRemindersForDate(DateRequest dateRequest, String accountIds, String timeZone) throws IllegalAccessException, InvocationTargetException {
        List<ReminderResponse> reminderResponseList = new ArrayList<>();
        List<Long> accountIdList = CommonUtils.convertToLongList(accountIds);
        List<Reminder> reminderList = reminderRepository.findAllByFkAccountIdCreatorAccountIdInAndReminderDateAndReminderStatus(accountIdList, dateRequest.getTodaysDate().toLocalDate(), Constants.ReminderStatusEnum.PENDING.getStatus());
        if (!reminderList.isEmpty()) {
            for (Reminder reminder : reminderList) {
                ReminderResponse reminderResponse = new ReminderResponse();
                reminderResponse = getReminderResponse(reminder, timeZone);
                reminderResponseList.add(reminderResponse);
            }
        }
        reminderResponseList.sort(Comparator.comparing(r -> r.getReminderTime()));
        return reminderResponseList;
    }

    /**
     * This method returns all the user made reminders that are pending
     */
    public AllReminderResponse getUserAllReminders(String accountIds, String timeZone) throws IllegalAccessException, InvocationTargetException {
        AllReminderResponse reminderResponseList = new AllReminderResponse();
        List<Long> accountIdList = CommonUtils.convertToLongList(accountIds);
        List<Reminder> reminderList = reminderRepository.findAllByFkAccountIdCreatorAccountIdIn(accountIdList);
        List<ReminderResponse> todayReminders = new ArrayList<>();
        List<ReminderResponse> futureReminders = new ArrayList<>();
        List<ReminderResponse> pastReminders = new ArrayList<>();
        if (!reminderList.isEmpty()) {

            LocalDate today = LocalDate.now(ZoneId.of(timeZone));

            for (Reminder reminder : reminderList) {
                ReminderResponse reminderResponse = getReminderResponse(reminder, timeZone);

                if(!Objects.equals(reminderResponse.getReminderStatus(), Constants.ReminderStatusEnum.DELETED.getStatus())) {
                    if (Objects.equals(reminderResponse.getReminderDate(), today)) {
                        todayReminders.add(reminderResponse);
                    } else if (reminderResponse.getReminderDate().isAfter(today)) {
                        futureReminders.add(reminderResponse);
                    } else {
                        pastReminders.add(reminderResponse);
                    }
                }
            }
        }
        Comparator<ReminderResponse> customComparator = Comparator
                .comparing((ReminderResponse r) -> {
                    // Define the order for statuses
                    switch (r.getReminderStatus()) {
                        case "Pending":
                            return 1;
                        case "Completed":
                            return 2;
                        default:
                            return 3;
                    }
                })
                .thenComparing(ReminderResponse::getReminderTime);

        todayReminders.sort(customComparator);
        futureReminders.sort(customComparator);
        pastReminders.sort(customComparator);

        reminderResponseList.setTodayReminders(todayReminders);
        reminderResponseList.setFutureReminders(futureReminders);
        reminderResponseList.setPastReminders(pastReminders);

//        reminderResponseList.sort(Comparator.comparing(r -> r.getReminderTime()));
        return reminderResponseList;
    }

    /**
     * Validate dates and set seconds as 0
     */
    private void normaliseDate (ReminderRequest request, String timeZone) {
        LocalDateTime serverDateTime = DateTimeUtils.convertUserDateToServerTimezone(LocalDateTime.of(request.getReminderDate(), request.getReminderTime()), timeZone);
        request.setReminderDate(serverDateTime.toLocalDate());
        request.setReminderTime(serverDateTime.toLocalTime().withSecond(0).withNano(0));
        request.setEarlyReminderTime(DateTimeUtils.convertUserDateToServerTimezone(request.getEarlyReminderTime(), timeZone));
    }

    /**
     * This method generates the response for frontend
     */
    private ReminderResponse getReminderResponse (Reminder reminder, String timeZone) throws InvocationTargetException, IllegalAccessException {
        ReminderResponse reminderResponse = new ReminderResponse();
        BeanUtils.copyProperties(reminder, reminderResponse);
        LocalDateTime localDateTime = DateTimeUtils.convertServerDateToUserTimezone(LocalDateTime.of(reminder.getReminderDate(), reminder.getReminderTime()), timeZone);
        reminderResponse.setReminderDate(localDateTime.toLocalDate());
        reminderResponse.setReminderTime(localDateTime.toLocalTime());
        reminderResponse.setEarlyReminderTime(DateTimeUtils.convertServerDateToUserTimezone(reminder.getEarlyReminderTime(), timeZone));
        EmailFirstLastAccountId userDetails = new EmailFirstLastAccountId();
        userDetails.setEmail(reminder.getFkAccountIdCreator().getEmail());
        userDetails.setFirstName(reminder.getFkAccountIdCreator().getFkUserId().getFirstName());
        userDetails.setLastName(reminder.getFkAccountIdCreator().getFkUserId().getLastName());
        userDetails.setAccountId(reminder.getFkAccountIdCreator().getAccountId());
        reminderResponse.setUserDetails(userDetails);
        return reminderResponse;
    }

    /**
     * This method validates user who s accessing reminder
     */
    private void validateUser (Long accountIdCreator, String accountIds) throws IllegalAccessException {
        List<Long> accountIdList = CommonUtils.convertToLongList(accountIds);
        if (!accountIdList.contains(accountIdCreator)) {
            throw new IllegalAccessException("Unauthorized Access: User trying to access someone else's reminders");
        }
    }
}

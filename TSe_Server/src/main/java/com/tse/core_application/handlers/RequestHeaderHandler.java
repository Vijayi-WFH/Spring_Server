package com.tse.core_application.handlers;

import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class RequestHeaderHandler {

    //  get accountId from the header
    public Long getAccountIdFromRequestHeader(String accountIds) {
        String[] stringAccountIds = accountIds.split(",");
        List<Long> accountIdsList = new ArrayList<>();

        if(stringAccountIds!=null){
            for(String accountId : stringAccountIds){
                accountIdsList.add(Long.parseLong(accountId));
            }
        }

       if (accountIdsList.size() < 2 ) {
           return accountIdsList.get(0);
       } else {
           return Long.valueOf(0);
       }
    }

    public Long getUserIdFromRequestHeader(String userId) {
        Long userID = Long.parseLong(userId);

        return userID;
    }

    // converts the comma separated accountIds in String format to List of Long
    public List<Long> convertToLongList(String accountIds) {
        List<Long> result = new ArrayList<>();
        if (accountIds != null && !accountIds.trim().isEmpty()) {
            String[] splitIds = accountIds.split(",");
            for (String accountId : splitIds) {
                try {
                    result.add(Long.parseLong(accountId.trim()));
                } catch (NumberFormatException e) {
                    System.err.println("Cannot convert to Long: " + accountId);
                    throw new NumberFormatException();
                }
            }
        }
        return result;
    }

}

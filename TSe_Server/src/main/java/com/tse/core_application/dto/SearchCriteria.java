package com.tse.core_application.dto;

import lombok.*;

@Getter
@Setter
@RequiredArgsConstructor
/*INFO: Concept of @RequiredArgsConstructor says that one constructor will be created for All @NonNull properties*/
@AllArgsConstructor
public class SearchCriteria {
    @NonNull
    private String key;
    @NonNull
    private String operation;
//    @NonNull
    private Object value;
    private Boolean orPredicate;
    private Boolean isJoin;

    public SearchCriteria(String key, String operation, Object value) {
        this.setKey(key);
        this.setOperation(operation);
        this.setValue(value);
    }
}
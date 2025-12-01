package com.tse.core_application.custom.model;

import lombok.*;

@Getter
@Setter
@RequiredArgsConstructor
public class SearchCriteria {

    @NonNull
    private String key;

    @NonNull
    private Object value;

    @NonNull
    private SearchOperation operation;

}

package com.tse.core_application.custom.model;

import lombok.*;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class PageInfoPagination {

    private Integer pageNumber;
    private Integer pageSize;
    private Long TotalElements;
    private Integer TotalPages;
    private boolean isLastPage;
}

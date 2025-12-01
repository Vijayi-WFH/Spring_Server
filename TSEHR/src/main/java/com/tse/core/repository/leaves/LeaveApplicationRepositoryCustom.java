package com.tse.core.repository.leaves;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.util.List;

public interface LeaveApplicationRepositoryCustom {
    Page<Object[]> findLeaveApplicationsExpanded(
            Long orgId,
            LocalDate fromDate,
            LocalDate toDate,
            List<Long> accountIdList,
            List<Long> approverAccountIdList,
            List<Short> leaveStatusIdList,
            Sort sort,
            Pageable pageable
    );
}


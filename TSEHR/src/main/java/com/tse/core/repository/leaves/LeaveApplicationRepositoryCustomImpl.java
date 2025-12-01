package com.tse.core.repository.leaves;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.time.LocalDate;
import java.util.List;

@Repository
public class LeaveApplicationRepositoryCustomImpl implements LeaveApplicationRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<Object[]> findLeaveApplicationsExpanded(
            Long orgId,
            LocalDate fromDate,
            LocalDate toDate,
            List<Long> accountIdList,
            List<Long> approverAccountIdList,
            List<Short> leaveStatusIdList,
            Sort sort,
            Pageable pageable
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT la, uaApplicant, uApplicant, uaApprover, uApprover ")
                .append("FROM LeaveApplication la ")
                .append("JOIN UserAccount uaApplicant ON la.accountId = uaApplicant.accountId ")
                .append("JOIN User uApplicant ON uaApplicant.fkUserId.userId = uApplicant.userId ")
                .append("JOIN UserAccount uaApprover ON la.approverAccountId = uaApprover.accountId ")
                .append("JOIN User uApprover ON uaApprover.fkUserId.userId = uApprover.userId ")
                .append("WHERE uaApplicant.orgId = :orgId ")
                .append("AND la.fromDate <= :toDate AND la.toDate >= :fromDate ");
        if (accountIdList != null && !accountIdList.isEmpty()) {
            sb.append("AND la.accountId IN :accountIdList ");
        }
        if (approverAccountIdList != null && !approverAccountIdList.isEmpty()) {
            sb.append("AND la.approverAccountId IN :approverAccountIdList ");
        }
        if (leaveStatusIdList != null && !leaveStatusIdList.isEmpty()) {
            sb.append("AND la.leaveApplicationStatusId IN :leaveStatusIdList ");
        }
        // Optional: Add ORDER BY if you want to sort in DB

        Query query = entityManager.createQuery(sb.toString());
        query.setParameter("orgId", orgId);
        query.setParameter("fromDate", fromDate);
        query.setParameter("toDate", toDate);
        if (accountIdList != null && !accountIdList.isEmpty()) {
            query.setParameter("accountIdList", accountIdList);
        }
        if (approverAccountIdList != null && !approverAccountIdList.isEmpty()) {
            query.setParameter("approverAccountIdList", approverAccountIdList);
        }
        if (leaveStatusIdList != null && !leaveStatusIdList.isEmpty()) {
            query.setParameter("leaveStatusIdList", leaveStatusIdList);
        }

        // ==== Construct count query (matches same WHERE/params) ====
        StringBuilder countSb = new StringBuilder();
        countSb.append("SELECT COUNT(la) ")
                .append("FROM LeaveApplication la ")
                .append("JOIN UserAccount uaApplicant ON la.accountId = uaApplicant.accountId ")
                .append("WHERE uaApplicant.orgId = :orgId ")
                .append("AND la.fromDate <= :toDate AND la.toDate >= :fromDate ");
        if (accountIdList != null && !accountIdList.isEmpty()) {
            countSb.append("AND la.accountId IN :accountIdList ");
        }
        if (approverAccountIdList != null && !approverAccountIdList.isEmpty()) {
            countSb.append("AND la.approverAccountId IN :approverAccountIdList ");
        }
        if (leaveStatusIdList != null && !leaveStatusIdList.isEmpty()) {
            countSb.append("AND la.leaveApplicationStatusId IN :leaveStatusIdList ");
        }

        Query countQuery = entityManager.createQuery(countSb.toString());
        countQuery.setParameter("orgId", orgId);
        countQuery.setParameter("fromDate", fromDate);
        countQuery.setParameter("toDate", toDate);
        if (accountIdList != null && !accountIdList.isEmpty()) {
            countQuery.setParameter("accountIdList", accountIdList);
        }
        if (approverAccountIdList != null && !approverAccountIdList.isEmpty()) {
            countQuery.setParameter("approverAccountIdList", approverAccountIdList);
        }
        if (leaveStatusIdList != null && !leaveStatusIdList.isEmpty()) {
            countQuery.setParameter("leaveStatusIdList", leaveStatusIdList);
        }

        // ==== Pagination (only if paged) ====
        if (pageable.isPaged()) {
            query.setFirstResult((int) pageable.getOffset());
            query.setMaxResults(pageable.getPageSize());
        }

        // ==== Execute queries ====
        @SuppressWarnings("unchecked")
        List<Object[]> partialResult = query.getResultList();
        long total = (long) countQuery.getSingleResult();

        return new PageImpl<>(partialResult, pageable, total);
    }
}


package com.tse.core_application.repository;

import com.tse.core_application.model.DashboardPinnedStickyNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface DashboardPinnedStickyNoteRepository extends JpaRepository<DashboardPinnedStickyNote, Long> {

    DashboardPinnedStickyNote findByUserIdAndNoteId(Long userId, Long noteId);

    @Query("SELECT s FROM DashboardPinnedStickyNote s WHERE s.userId in :userIdList ORDER BY s.createdDateTime DESC")
    List<DashboardPinnedStickyNote> findByUserIdInAndMaxCreatedDateTime (List<Long> userIdList);

    boolean existsByUserIdAndNoteId(Long userId, Long noteId);

    @Modifying
    @Transactional
    @Query("DELETE FROM DashboardPinnedStickyNote dpsn WHERE dpsn.userId IN :accountIds")
    void deleteByAccountIdIn(List<Long> accountIds);
}

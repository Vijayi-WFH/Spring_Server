package com.example.chat_app.repository;

import com.example.chat_app.model.PinnedChats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface PinnedChatsRepository extends JpaRepository<PinnedChats, Long> {
    List<PinnedChats> findByAccountIdAndChatTypeIdAndChatId(Long accountId, Long chatTypeId, Long chatId);

    PinnedChats findByAccountIdInAndChatTypeIdAndChatId(List<Long> accountIds, Long chatTypeId, Long entityId);

    @Transactional
    @Modifying
    @Query("DELETE FROM PinnedChats pc WHERE pc.chatId IN :groupIds AND pc.chatTypeId = (SELECT ct.chatTypeId FROM ChatType ct WHERE ct.chatTypeName = 'GROUP')")
    void deleteByGroupIdIn(@Param("groupIds") List<Long> groupIds);

    @Transactional
    @Modifying
    @Query("DELETE FROM PinnedChats pc WHERE pc.accountId IN :userIds")
    void deleteByUserIdIn(@Param("userIds") List<Long> userIds);
}

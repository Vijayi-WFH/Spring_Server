package com.example.chat_app.repository;

import com.example.chat_app.model.FavouriteChats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface FavouriteChatsRepository extends JpaRepository<FavouriteChats, Long> {
    FavouriteChats findByAccountIdInAndChatTypeIdAndChatId(List<Long> accountIds, Long chatTypeId, Long chatId);

    List<FavouriteChats> findByAccountIdAndChatTypeIdAndChatId(Long requesterAccountId, Long chatTypeId, Long chatId);

    @Transactional
    @Modifying
    @Query("DELETE FROM FavouriteChats fc WHERE fc.chatId IN :groupIds AND fc.chatTypeId = (SELECT ct.chatTypeId FROM ChatType ct WHERE ct.chatTypeName = 'GROUP')")
    void deleteByGroupIdIn(@Param("groupIds") List<Long> groupIds);

    @Transactional
    @Modifying
    @Query("DELETE FROM FavouriteChats fc WHERE fc.accountId IN :userIds")
    void deleteByUserIdIn(@Param("userIds") List<Long> userIds);
}

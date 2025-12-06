package com.example.chat_app.repository;

import com.example.chat_app.model.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {

    @Transactional
    @Modifying
    @Query("DELETE FROM Tag t WHERE t.groupId IN :groupIds")
    void deleteByGroupIdIn(@Param("groupIds") List<Long> groupIds);
}

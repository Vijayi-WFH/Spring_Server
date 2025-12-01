package com.tse.core_application.repository;

import com.tse.core_application.dto.personal_task.PersonalFileMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import com.tse.core_application.model.personal_task.PersonalAttachment;

import java.util.List;
import java.util.Optional;


@Repository
public interface PersonalAttachmentRepository extends JpaRepository<PersonalAttachment, Long> {

    @Query("SELECT new com.tse.core_application.dto.personal_task.PersonalFileMetadata(pa.fileName, pa.fileSize) " +
            "FROM PersonalAttachment pa " +
            "WHERE pa.personalTaskId = :personalTaskId AND pa.fileStatus = :fileStatus")
    List<PersonalFileMetadata> findFileMetadataByPersonalTaskIdAndFileStatus(Long personalTaskId, Character fileStatus);

    Optional<PersonalAttachment> findByPersonalTaskIdAndFileNameAndFileStatus(Long personalTaskId, String fileName, Character fileStatus);

    @Modifying
    @Query("update PersonalAttachment t set t.fileStatus = :fileStatus, t.deletedDateTime = CURRENT_TIMESTAMP where t.personalTaskId = :taskId AND t.accountId = :removerAccountId")
    void updateAllPersonalTaskAttachmentsStatusByTaskId(Long taskId, Long removerAccountId, Character fileStatus);

    @Modifying
    @Query("update PersonalAttachment t set t.fileStatus = :fileStatus, t.deletedDateTime = CURRENT_TIMESTAMP where t.personalTaskId = :taskId AND t.fileName = :fileName AND t.accountId = :removerAccountId")
    void updatePersonalTaskAttachmentStatusByTaskIAndFileName(Long taskId, String fileName, Long removerAccountId, Character fileStatus);
}

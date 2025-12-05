package com.tse.core_application.repository;

import com.tse.core_application.model.StickyNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface StickyNoteRepository extends JpaRepository<StickyNote, Long> {

    /**
     * This method will find all the sticky note of the given user which is either private or public
     * in that entity and sharedAccountIds is null.
     *
     * @param postedByAccountId the accountId of user.
     * @param accessType        private/public
     * @param orgIds            list of Ids of all organization of the user
     * @return List<StickyNote>
     */
//    List<StickyNote> findByPostedByAccountIdInAndAccessTypeInAndOrgIdInAndSharedAccountIdsIsNullAndIsDeleted(List<Long> postedByAccountId, List<Integer> accessType, List<Long> orgIds, Integer isDeleted);

    /**
     * This method will find all the sticky note of the given user which is either private or public
     * in that entity and sharedAccountIds is null.
     *
     * @param postedByAccountId the accountId of user.
     * @param accessType        private/public
     * @param buIds             list of Ids of all BU of the user
     * @return List<StickyNote>
     */
//    List<StickyNote> findByPostedByAccountIdInAndAccessTypeInAndBuIdInAndSharedAccountIdsIsNullAndIsDeleted(List<Long> postedByAccountId, List<Integer> accessType, List<Long> buIds, Integer isDeleted);

    /**
     * This method will find all the sticky note of the given user which is either private or public
     * in that entity and sharedAccountIds is null.
     *
     * @param postedByAccountId the accountId of user.
     * @param accessType        private/public
     * @param projectIds        list of Ids of all project of the user
     * @return List<StickyNote>
     */
//    List<StickyNote> findByPostedByAccountIdInAndAccessTypeInAndProjectIdInAndSharedAccountIdsIsNullAndIsDeleted(List<Long> postedByAccountId, List<Integer> accessType, List<Long> projectIds, Integer isDeleted);

    /**
     * This method will find all the sticky note of the given user which is either private or public
     * in that entity and sharedAccountIds is null.
     *
     * @param postedByAccountId the accountId of user.
     * @param accessType        private/public
     * @param teamIds           list of Ids of all team of the user
     * @return List<StickyNote>
     */
//    List<StickyNote> findByPostedByAccountIdInAndAccessTypeInAndTeamIdInAndSharedAccountIdsIsNullAndIsDeleted(List<Long> postedByAccountId, List<Integer> accessType, List<Long> teamIds, Integer isDeleted);

    /**
     * This method will find the sticky note where sharedAccountIds matches with the given accountId
     *
     * @param accountId  the accountId which has to be matched
     * @param accessType public
     * @return List<StickyNote>
     */
//    List<StickyNote> findByAccessTypeAndSharedAccountIdsContainingAndIsDeleted(Integer accessType, String accountId, Integer isDeleted);
//    List<StickyNote> findByAccessTypeAndSharedAccountIdsNotNullAndSharedAccountIdsAndIsDeleted(Integer accessType, List<String> accountIds, Integer isDeleted);


// --------------------------------------------------------------------------------------------------------------------------//
    /**
     * This method will soft delete the given noteId.
     *
     * @param isDeleted isDeleted type.
     * @param noteId    the noteId which has to be deleted.
     * @return resultSet.
     */
    @Transactional
    @Modifying
    @Query("update StickyNote s set s.isDeleted = :isDeleted where s.noteId = :noteId")
    Integer updateStickyNoteStatusByNoteId(Integer isDeleted, Long noteId);

    /**
     * This method will find the sticky note by its noteId.
     *
     * @param noteId the noteId which has to be searched.
     * @return the sticky note object.
     */
    StickyNote findByNoteId(Long noteId);

    List<StickyNote> findByAccessTypeAndOrgIdInAndIsDeleted(Integer accessType, List<Long> orgIds, Integer isDeleted);

    List<StickyNote> findByAccessTypeAndBuIdInAndIsDeleted(Integer accessType, List<Long> buIds, Integer isDeleted);

    List<StickyNote> findByAccessTypeAndProjectIdInAndIsDeleted(Integer accessType, List<Long> projectIds, Integer isDeleted);

    List<StickyNote> findByAccessTypeAndTeamIdInAndIsDeleted(Integer accessType, List<Long> teamIds, Integer isDeleted);

    List<StickyNote> findByCreatedByUserIdAndAccessTypeAndIsDeleted(Long userId, Integer accessType, Integer isDeleted);

    List<StickyNote> findByAccessTypeAndSharedAccountIdsNotNullAndOrgIdInAndIsDeletedAndCreatedByUserIdNot(Integer accessType, List<Long> orgIds, Integer isDeleted, Long createdByUserId);

    List<StickyNote> findByAccessTypeAndSharedAccountIdsNotNullAndBuIdInAndIsDeletedAndCreatedByUserIdNot(Integer accessType, List<Long> buIds, Integer isDeleted, Long createdByUserId);

    List<StickyNote> findByAccessTypeAndSharedAccountIdsNotNullAndProjectIdInAndIsDeletedAndCreatedByUserIdNot(Integer accessType, List<Long> projectIds, Integer isDeleted, Long createdByUserId);

    List<StickyNote> findByAccessTypeAndSharedAccountIdsNotNullAndTeamIdInAndIsDeletedAndCreatedByUserIdNot(Integer accessType, List<Long> teamIds, Integer isDeleted, Long createdByUserId);

    List<StickyNote> findByTeamId(Long teamId);

    List<StickyNote> findByAccessTypeAndTeamIdInAndIsDeletedAndSharedAccountIdsNotNull(Integer accessType, List<Long> teamIds, Integer isDeleted);

    List<StickyNote> findByAccessTypeAndOrgIdInAndIsDeletedAndSharedAccountIdsNotNull(Integer accessType, List<Long> teamIds, Integer isDeleted);

    @Query("SELECT count(s) FROM StickyNote s WHERE s.orgId = :orgId")
    Integer findStickyNotesCountByOrgId(Long orgId);

    @Transactional
    @Modifying
    @Query("DELETE FROM StickyNote s WHERE s.createdByAccountId IN :accountIds")
    void deleteAllByAccountIdIn(List<Long> accountIds);
}


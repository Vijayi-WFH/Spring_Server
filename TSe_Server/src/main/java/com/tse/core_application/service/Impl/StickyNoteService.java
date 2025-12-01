package com.tse.core_application.service.Impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tse.core_application.constants.Constants;
import com.tse.core_application.custom.model.AccountId;
import com.tse.core_application.custom.model.EmailFirstLastAccountId;
import com.tse.core_application.custom.model.OrgIdOrgName;
import com.tse.core_application.dto.StickyNoteAddRequest;
import com.tse.core_application.exception.StickyNoteFailedException;
import com.tse.core_application.exception.ValidationFailedException;
import com.tse.core_application.model.*;
import com.tse.core_application.repository.*;
import com.tse.core_application.utils.CommonUtils;
import com.tse.core_application.utils.DateTimeUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.tse.core_application.utils.DateTimeUtils.convertServerDateToUserTimezoneWithSeconds;

@Service
public class StickyNoteService {

    @Autowired
    private StickyNoteRepository stickyNoteRepository;

    @Autowired
    private UserAccountService userAccountService;

    @Autowired
    private BUService buService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private TeamService teamService;

    @Autowired
    private UserService userService;

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private PinnedStickyNoteRepository pinnedStickyNoteRepository;

    @Autowired
    private ImportantStickyNoteRepository importantStickyNoteRepository;

    @Autowired
    private AccessDomainRepository accessDomainRepository;
    @Autowired
    private TeamRepository teamRepository;
    @Autowired
    private DashboardPinnedStickyNoteRepository dashboardPinnedStickyNoteRepository;
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private AuditService auditService;

    private ObjectMapper objectMapper = new ObjectMapper();

    /**
     * This method will validate the length of the sticky note. The maximum length of the sticky note
     * is 10000 characters in database but initially restricted to 5000 characters.
     *
     * @param note the note which has to be validated.
     * @return boolean value.
     */
    @Deprecated
    private boolean validateNoteLength(String note) {
        boolean isNoteLengthValidated = false;
        if (note.length() <= 5000) {
            isNoteLengthValidated = true;
        }
        return isNoteLengthValidated;
    }

    /**
     * This method will validate the postedByAccountId for that note. This is optional. This will
     * be null if no org is chosen for that note. If an org has been selected for that note then
     * that user accountId will be put here in that selected orgId. For now, Since the note can be shared
     * with one entity and therefore if orgId is selected then other entity has to be nulls. This does not
     * include "shared_accountIds" i.e. the shared accountIds can be present. This shared accountIds may or
     * may not be part of that selected organization.
     *
     * @param stickyNoteAddRequest the stickyNoteAddRequest which has to be validated.
     * @return boolean value.
     */
    private boolean validatePostedByAccountIdForOrgId(StickyNoteAddRequest stickyNoteAddRequest) {
        boolean isPostedByAccountIdValidated = false;
        if (stickyNoteAddRequest.getOrgId() != null) {
            if (stickyNoteAddRequest.getPostedByAccountId() != null) {
                boolean isUserExists = userAccountService.isUserAccountExistsByAccountIdAndOrgId(stickyNoteAddRequest.getPostedByAccountId(), stickyNoteAddRequest.getOrgId());
                if (isUserExists) {
                    isPostedByAccountIdValidated = true;
                }
            }
        } else {
            if (stickyNoteAddRequest.getPostedByAccountId() != null) {
                if (stickyNoteAddRequest.getBuId() != null || stickyNoteAddRequest.getProjectId() != null || stickyNoteAddRequest.getTeamId() != null) {
                    isPostedByAccountIdValidated = true;
                }
            }
        }
        return isPostedByAccountIdValidated;
    }

    /**
     * This method will validate the accessType of the sticky note whenever a note is getting shared.
     * 1. The accessType should be turned to 1 as soon as we choose to share the sticky note within the
     * org, BU, project or team. However, at the same time, if we have shared accountIds then this will
     * not be turned to 1.
     * 2. If org, BU, project and team are all nulls then the note can be shared with people of any organization.
     * 3. If both entity(i.e. anyone of org, BU, project or team) and sharedAccountIds are present then it can only
     * be shared in the same organization or organization "0" (i.e. personal). Hence, all the sharedAccountIds has
     * to be part of the same organization(i.e. the organization in which the creator of the sticky note is present)
     * or in the personal organization.
     *
     * @param stickyNoteAddRequest The stickyNoteAddRequest which has to be validated.
     * @return boolean value.
     */
    private boolean validateStickyNoteAccessType(StickyNoteAddRequest stickyNoteAddRequest) {
        boolean isAccessTypeValidated = false;
        if (stickyNoteAddRequest.getAccessType().equals(Constants.Sticky_Note_AccessType.PUBLIC_ACCESS)) {
            if (Objects.equals(stickyNoteAddRequest.getTeamId(), com.tse.core_application.model.Constants.PERSONAL_TEAM_ID)) {
                throw new ValidationFailedException("User not allowed to create public note in personal Work Items");
            }
            if (stickyNoteAddRequest.getSharedAccountIds() == null || stickyNoteAddRequest.getSharedAccountIds().isEmpty()) {
                if (stickyNoteAddRequest.getOrgId() != null || stickyNoteAddRequest.getBuId() != null || stickyNoteAddRequest.getProjectId() != null ||
                        stickyNoteAddRequest.getTeamId() != null) {
                    isAccessTypeValidated = true;
                } else {
                    throw new StickyNoteFailedException("Sticky note can not be shared.");
                }
            } else {
                if (stickyNoteAddRequest.getOrgId() == null && stickyNoteAddRequest.getBuId() == null && stickyNoteAddRequest.getProjectId() == null &&
                        stickyNoteAddRequest.getTeamId() == null) {
                    isAccessTypeValidated = true;
                } else {
                    if (stickyNoteAddRequest.getOrgId() != null || stickyNoteAddRequest.getBuId() != null ||
                            stickyNoteAddRequest.getProjectId() != null || stickyNoteAddRequest.getTeamId() != null) {
                        isAccessTypeValidated = true;
                    }
                }
            }
        } else {
            if (stickyNoteAddRequest.getAccessType().equals(Constants.Sticky_Note_AccessType.PRIVATE_ACCESS)) {
                if (stickyNoteAddRequest.getSharedAccountIds() == null || stickyNoteAddRequest.getSharedAccountIds().isEmpty()) {
                    if (stickyNoteAddRequest.getOrgId() != null || stickyNoteAddRequest.getBuId() != null || stickyNoteAddRequest.getProjectId() != null ||
                            stickyNoteAddRequest.getTeamId() != null) {
                        isAccessTypeValidated = true;
                    }
                } else {
                    if (Objects.equals(stickyNoteAddRequest.getTeamId(), com.tse.core_application.model.Constants.PERSONAL_TEAM_ID)) {
                        throw new ValidationFailedException("User not allowed to create shared note in personal Work Items");
                    }
                    if (stickyNoteAddRequest.getOrgId() != null || stickyNoteAddRequest.getBuId() != null || stickyNoteAddRequest.getProjectId() != null ||
                            stickyNoteAddRequest.getTeamId() != null) {
                        isAccessTypeValidated = true;
                    }
                }
            }
        }
        return isAccessTypeValidated;
    }

    /**
     * This method will validate the sharedAccountIds. Since the note can only be made public/shared in the
     * same organization or personal organization and therefore all the sharedAccountIds has to be part of the
     * same organization in which the user (i.e. the creator of the sticky note) is present or in the personal
     * organization (i.e. organization "0"). This method will also validate the following validations:
     * 1. If sharedAccountIds are present and anyone of the (orgId, buId, projectId and teamId) is present
     * then it means all sharedAccountIds should be part of that orgId and hence above validation will be applied.
     * 2. If sharedAccountIds are present and no entityId is present (orgId, buId, projectId and teamId) then it
     * means it can be shared outside of organization and hence no validation on sharedAccountIds.
     *
     * @param stickyNoteAddRequest the stickyNoteAddRequest which has to be validated.
     * @return boolean value.
     */
    private boolean validateSharedAccountIdsForOrg(StickyNoteAddRequest stickyNoteAddRequest) {
        boolean isSharedAccountIdValidated = false;
        Long stickyNoteOrgId = null;
        List<Long> allSharedAccountIds = new ArrayList<>();
        if (stickyNoteAddRequest.getSharedAccountIds() != null && !stickyNoteAddRequest.getSharedAccountIds().isEmpty()) {
            for (String str : stickyNoteAddRequest.getSharedAccountIds()) {
                allSharedAccountIds.add(Long.valueOf(str));
            }
        }
        if (stickyNoteAddRequest.getOrgId() != null) {
            stickyNoteOrgId = stickyNoteAddRequest.getOrgId();
        } else {
            if (stickyNoteAddRequest.getBuId() != null) {
                stickyNoteOrgId = buService.getOrgIdByBUId(stickyNoteAddRequest.getBuId());
            } else {
                if (stickyNoteAddRequest.getProjectId() != null) {
                    stickyNoteOrgId = projectService.getOrgIdByProjectId(stickyNoteAddRequest.getProjectId());
                } else {
                    if (stickyNoteAddRequest.getTeamId() != null) {
                        stickyNoteOrgId = teamService.getOrgIdByTeamId(stickyNoteAddRequest.getTeamId());
                    } else {
                        isSharedAccountIdValidated = true;
                        return isSharedAccountIdValidated;
                    }
                }
            }
        }
        if (stickyNoteOrgId != null && !allSharedAccountIds.isEmpty()) {
            for (Long accountId : allSharedAccountIds) {
                boolean isUserExistsForOrgId = userAccountService.isUserAccountExistsByAccountIdAndOrgId(accountId, stickyNoteOrgId);
                if (!isUserExistsForOrgId) {
                    throw new StickyNoteFailedException("Sticky note can not be shared outside of the organization");
                }
                isSharedAccountIdValidated = true;
            }
        } else {
            if (stickyNoteAddRequest.getAccessType().equals(Constants.Sticky_Note_AccessType.PRIVATE_ACCESS)) {
                isSharedAccountIdValidated = true;
            }
        }
        return isSharedAccountIdValidated;
    }

    /**
     * This method will check whether the given note has been deleted or not.
     *
     * @param stickyNoteAddRequest the stickyNoteUpdateRequest.
     * @return boolean value.
     */
    private boolean isNoteDeleted(StickyNoteAddRequest stickyNoteAddRequest) {
        boolean isNoteDeleted = false;
        if (stickyNoteAddRequest.getIsDeleted() != null) {
            if (stickyNoteAddRequest.getIsDeleted().equals(Constants.Sticky_Note_DeleteType.STICKY_NOTE_DELETED)) {
                isNoteDeleted = true;
            }
        }
        return isNoteDeleted;
    }

    /**
     * This method will check whether the given note has been updated or not.
     *
     * @param stickyNoteAddRequest the stickyNoteUpdateRequest.
     * @return boolean value.
     */
    private boolean isNoteModified(StickyNoteAddRequest stickyNoteAddRequest) {
        boolean isNoteModified = false;
        if (stickyNoteAddRequest.getIsModified() != null) {
            if (stickyNoteAddRequest.getIsModified().equals(Constants.Sticky_Note_ModifiedType.STICKY_NOTE_MODIFIED)) {
                isNoteModified = true;
            }
        }
        return isNoteModified;
    }


/*
    private ArrayList<String> getFieldsToUpdate(StickyNoteAddRequest stickyNoteAddRequest) {
        StickyNote stickyNoteFound = stickyNoteRepository.findByNoteId(stickyNoteAddRequest.getNoteId());
        ArrayList<String> arrayListFields = new ArrayList<String>();
        ArrayList<HashMap<String, Object>> arrayList = new ArrayList<HashMap<String, Object>>();
        HashMap<String, Object> mapStickyNoteAddRequest = objectMapper.convertValue(stickyNoteAddRequest, HashMap.class);
        HashMap<String, Object> mapStickyNoteFound = objectMapper.convertValue(stickyNoteFound, HashMap.class);
        arrayList.add(mapStickyNoteFound);
        arrayList.add(mapStickyNoteAddRequest);
        for (int i = 0; i < (arrayList.size() - 1); i++) {
            for (Map.Entry<String, Object> entry : arrayList.get(i).entrySet()) {
                String key = entry.getKey();
                Object value1 = entry.getValue();
                Object value2 = arrayList.get(i + 1).get(key);
                if (value2 != null) {
                    if (!value2.equals(value1)) {
                        arrayListFields.add(key);
                    }
                }
            }
        }
        return arrayListFields;
    }
*/


    /**
     * This method will add the given sticky note in the table "sticky_note"
     * after validating the stickyNoteAddRequest.
     *
     * @param stickyNoteAddRequest The stickyNoteAddRequest which has to be added.
     * @return The StickyNote object.
     */
    public StickyNote addStickyNote(StickyNoteAddRequest stickyNoteAddRequest, Long userId) {
        StickyNote addedStickyNote = null;
//        boolean isNoteLengthValidated = validateNoteLength(stickyNoteAddRequest.getNote());
        stickyNoteAddRequest.setNote(stickyNoteAddRequest.getNote().trim().replaceAll("\\s+", " "));
        if (stickyNoteAddRequest.getNote().isEmpty()) {
            throw new ValidationFailedException("Sticky note cannot be empty. Please enter some text before saving.");
        }
        boolean isAccessTypeValidated = validateStickyNoteAccessType(stickyNoteAddRequest);
        boolean isPostedByAccountIdValidated = validatePostedByAccountIdForOrgId(stickyNoteAddRequest);
        boolean isSharedAccountIdsValidated = true;
        if (stickyNoteAddRequest.getSharedAccountIds() != null && !stickyNoteAddRequest.getSharedAccountIds().isEmpty()) {
            isSharedAccountIdsValidated = validateSharedAccountIdsForOrg(stickyNoteAddRequest);
        }
        if (isAccessTypeValidated && isPostedByAccountIdValidated && isSharedAccountIdsValidated) {
            StickyNote stickyNoteToAdd = new StickyNote();

            BeanUtils.copyProperties(stickyNoteAddRequest, stickyNoteToAdd);
            stickyNoteToAdd.setCreatedByUserId(userId);
            stickyNoteToAdd.setIsDeleted(Constants.Sticky_Note_DeleteType.STICKY_NOTE_NOT_DELETED);
            addedStickyNote = stickyNoteRepository.save(stickyNoteToAdd);
            addedStickyNote.setLastUpdatedDateTime(null);
        } else {
            throw new StickyNoteFailedException("Sticky note can not be created.");
        }
        auditService.auditForStickyNote(userAccountRepository.findByAccountIdAndIsActive(stickyNoteAddRequest.getPostedByAccountId(), true), addedStickyNote, false);
        return addedStickyNote;
    }

//    public List<HashMap<String, Object>> getNotesByFilter(SprintFilterRequest request, String timeZone) {
//        List<HashMap<String, Object>> finalMapStickyNote = new ArrayList<>();
//
//    }


    /**
     * This method will get all the sticky notes of the user i.e. all sticky notes which
     * are private and public to this user.
     *
     * @param userId the userId for which all sticky notes are to be found.
     * @return List<StickyNoteGetResponse>
     */
    public List<HashMap<String, Object>> getAllStickyNoteByUserId(Long userId, String timeZone) {
        List<HashMap<String, Object>> finalMapStickyNote = new ArrayList<>();
        HashSet<StickyNote> allFoundStickyNoteSet = new HashSet<>();
        HashSet<Long> allAccountIds = new HashSet<>();
        HashSet<Long> allOrgIds = new HashSet<>();
        List<Long> managedUserList = userService.getManagedUserList(userId);
        List<Long> allImportantStickyNotesIds = importantStickyNoteRepository.findAllImportantNoteIdsByUserIn(managedUserList);
        List<Long> allPinnedStickyNotesIds = pinnedStickyNoteRepository.findAllPinnedNoteIdsByUserIn(managedUserList);
        //Todo: add isActive condition at userAccount level and add the same condition at accessDomain level
        List<UserAccount> allUserAccount = userAccountService.getAllUserAccountByUserIdAndIsActive(userId);
        for (UserAccount userAccount : allUserAccount) {
            allAccountIds.add(userAccount.getAccountId());
            allOrgIds.add(userAccount.getOrgId());
        }
        Set<Long> allTeamIds = accessDomainRepository.findEntityIdByAccountIdAndEntityTypeId(allAccountIds, true, com.tse.core_application.model.Constants.EntityTypes.TEAM);
        Set<Long> allProjectIds = accessDomainRepository.findEntityIdByAccountIdAndEntityTypeId(allAccountIds, true, com.tse.core_application.model.Constants.EntityTypes.PROJECT);
        allProjectIds.addAll(teamRepository.findProjectIdsByTeamIds(allTeamIds));
        Set<Long> allBuIds = accessDomainRepository.findEntityIdByAccountIdAndEntityTypeId(allAccountIds, true, com.tse.core_application.model.Constants.EntityTypes.BU);
        List<Long> allTeamIdList = new ArrayList<>(allTeamIds);
        List<Long> allProjectIdList = new ArrayList<>(allProjectIds);
        List<Long> allBuIdList = new ArrayList<>(allBuIds);
        List<Long> allOrgIdList = new ArrayList<>(allOrgIds);
        List<StickyNote> allPublicStickyNotesByOrgId = stickyNoteRepository.findByAccessTypeAndOrgIdInAndIsDeleted(Constants.Sticky_Note_AccessType.PUBLIC_ACCESS, allOrgIdList, Constants.Sticky_Note_DeleteType.STICKY_NOTE_NOT_DELETED);
        List<StickyNote> allPublicStickyNotesByBuId = stickyNoteRepository.findByAccessTypeAndBuIdInAndIsDeleted(Constants.Sticky_Note_AccessType.PUBLIC_ACCESS, allBuIdList, Constants.Sticky_Note_DeleteType.STICKY_NOTE_NOT_DELETED);
        List<StickyNote> allPublicStickyNotesByProjectId = stickyNoteRepository.findByAccessTypeAndProjectIdInAndIsDeleted(Constants.Sticky_Note_AccessType.PUBLIC_ACCESS, allProjectIdList, Constants.Sticky_Note_DeleteType.STICKY_NOTE_NOT_DELETED);
        List<StickyNote> allPublicStickyNotesByTeamId = stickyNoteRepository.findByAccessTypeAndTeamIdInAndIsDeleted(Constants.Sticky_Note_AccessType.PUBLIC_ACCESS, allTeamIdList, Constants.Sticky_Note_DeleteType.STICKY_NOTE_NOT_DELETED);
        List<StickyNote> userPrivateStickyNotes = stickyNoteRepository.findByCreatedByUserIdAndAccessTypeAndIsDeleted(userId, Constants.Sticky_Note_AccessType.PRIVATE_ACCESS, Constants.Sticky_Note_DeleteType.STICKY_NOTE_NOT_DELETED);
        allFoundStickyNoteSet.addAll(allPublicStickyNotesByTeamId);
        allFoundStickyNoteSet.addAll(allPublicStickyNotesByProjectId);
        allFoundStickyNoteSet.addAll(allPublicStickyNotesByBuId);
        allFoundStickyNoteSet.addAll(allPublicStickyNotesByOrgId);
        allFoundStickyNoteSet.addAll(userPrivateStickyNotes);
        List<String> stringAccountIds = allAccountIds.stream().map(Object::toString).collect(Collectors.toList());
        List<StickyNote> privateStickyNotesSharedWithUser = getPrivateSharedNotesOfUser(Constants.Sticky_Note_AccessType.PRIVATE_ACCESS, stringAccountIds, Constants.Sticky_Note_DeleteType.STICKY_NOTE_NOT_DELETED, allOrgIdList, allBuIdList, allProjectIdList, allTeamIdList, userId);
        allFoundStickyNoteSet.addAll(privateStickyNotesSharedWithUser);

        for (StickyNote stickyNote : allFoundStickyNoteSet) {
            HashMap<String, Object> map = objectMapper.convertValue(stickyNote, HashMap.class);
            EmailFirstLastAccountId emailFirstLastAccountId = userAccountService.getEmailFirstLastNameByAccountId(stickyNote.getPostedByAccountId());
            map.put("fullName", emailFirstLastAccountId.getFirstName() + " " + emailFirstLastAccountId.getLastName());
            map.put("createdDateTime", convertServerDateToUserTimezoneWithSeconds(stickyNote.getCreatedDateTime(), timeZone));
            if (stickyNote.getLastUpdatedDateTime() != null) {
                map.put("lastUpdatedDateTime", convertServerDateToUserTimezoneWithSeconds(stickyNote.getLastUpdatedDateTime(), timeZone));
            }
            if (stickyNote.getOrgId() != null) {
                OrgIdOrgName orgIdOrgName = organizationService.getOrganizationByOrgId(stickyNote.getOrgId());
                if (orgIdOrgName != null) {
                    map.put("entityName", orgIdOrgName.getOrganizationName());
                    map.put("entityType", com.tse.core_application.model.Constants.EntityTypes.ORG);
                    map.put("entityId", stickyNote.getOrgId());
                } else {
                    continue;
                }
            } else if (stickyNote.getBuId() != null) {
                BU bu = buService.getBuByBuId(stickyNote.getBuId());
                if (bu != null) {
                    map.put("entityName", bu.getBuName());
                    map.put("entityType", com.tse.core_application.model.Constants.EntityTypes.BU);
                    map.put("entityId", stickyNote.getBuId());
                } else {
                    continue;
                }
            } else if (stickyNote.getProjectId() != null) {
                Project project = projectService.getProjectByProjectId(stickyNote.getProjectId());
                if (project != null) {
                    map.put("entityName", project.getProjectName());
                    map.put("entityType", com.tse.core_application.model.Constants.EntityTypes.PROJECT);
                    map.put("entityId", stickyNote.getProjectId());
                } else {
                    continue;
                }
            } else if (stickyNote.getTeamId() != null) {
                Team team = teamService.getTeamByTeamId(stickyNote.getTeamId());
                if (team != null) {
                    map.put("entityName", team.getTeamName());
                    map.put("entityType", com.tse.core_application.model.Constants.EntityTypes.TEAM);
                    map.put("entityId", stickyNote.getTeamId());
                } else {
                    continue;
                }
            }

            boolean isEditable = Objects.equals(userId, stickyNote.getCreatedByUserId()) || stickyNote.getShareEditAllowed();
            map.put("isEditable", isEditable);
            map.put("accessType", stickyNote.getAccessType());
            map.put("isSelfCreated", stickyNote.getCreatedByUserId().equals(userId));
            map.put("isImportant", allImportantStickyNotesIds.contains(stickyNote.getNoteId()));
            map.put("isPinned", allPinnedStickyNotesIds.contains(stickyNote.getNoteId()));
            finalMapStickyNote.add(map);
        }

        // Custom comparator to sort by lastUpdatedDateTime, createdDateTime, and isPinned
        Comparator<HashMap<String, Object>> stickyNoteComparator = Comparator.comparing((HashMap<String, Object> stickyNote) -> {
            Object lastUpdatedDateTime = stickyNote.get("lastUpdatedDateTime");
            if (lastUpdatedDateTime != null) {
                return (LocalDateTime) lastUpdatedDateTime;
            } else {
                return (LocalDateTime) stickyNote.get("createdDateTime");
            }
        }).reversed().thenComparing(stickyNote -> (Boolean) stickyNote.get("isPinned"));

        // Sort finalMapStickyNote using the custom comparator
        finalMapStickyNote.sort(stickyNoteComparator);

        return finalMapStickyNote;
    }


    List<StickyNote> getPrivateSharedNotesOfUser(
            Integer accessType, List<String> accountIds, Integer isDeleted, List<Long> allOrgIds, List<Long> allBuIds, List<Long> allProjectIds, List<Long> allTeamIds, Long userId) {

        List<StickyNote> privateNoteInOrg = stickyNoteRepository.findByAccessTypeAndSharedAccountIdsNotNullAndOrgIdInAndIsDeletedAndCreatedByUserIdNot(accessType, allOrgIds, isDeleted, userId);
        List<StickyNote> privateNoteInBu = stickyNoteRepository.findByAccessTypeAndSharedAccountIdsNotNullAndBuIdInAndIsDeletedAndCreatedByUserIdNot(accessType, allBuIds, isDeleted, userId);
        List<StickyNote> privateNoteInProject = stickyNoteRepository.findByAccessTypeAndSharedAccountIdsNotNullAndProjectIdInAndIsDeletedAndCreatedByUserIdNot(accessType, allProjectIds, isDeleted, userId);
        List<StickyNote> privateNoteInTeam = stickyNoteRepository.findByAccessTypeAndSharedAccountIdsNotNullAndTeamIdInAndIsDeletedAndCreatedByUserIdNot(accessType, allTeamIds, isDeleted, userId);
        List<StickyNote> notes = new ArrayList<>();
        notes.addAll(privateNoteInBu);
        notes.addAll(privateNoteInTeam);
        notes.addAll(privateNoteInOrg);
        notes.addAll(privateNoteInProject);
        List<StickyNote> notesToAdd = new ArrayList<>();
        for (StickyNote stickyNote : notes) {
            for (String accountId : accountIds) {
                if (stickyNote.getSharedAccountIds().contains(accountId)) {
                    notesToAdd.add(stickyNote);
                    break;
                }
            }
        }
        return notesToAdd;
    }


    /**
     * This method will update the given sticky note.
     *
     * @param stickyNoteAddRequest the stickyNoteAddRequest.
     * @return A String.
     */
    public String updateStickyNote(StickyNoteAddRequest stickyNoteAddRequest, Long updatedByUserId) {
        String message = "Sticky note successfully updated.";
        stickyNoteAddRequest.setNote(stickyNoteAddRequest.getNote().trim().replaceAll("\\s+", " "));
        if (stickyNoteAddRequest.getNote().isEmpty()) {
            throw new ValidationFailedException("Sticky note cannot be empty. Please enter some text before saving.");
        }
        StickyNote stickyNoteFound = stickyNoteRepository.findByNoteId(stickyNoteAddRequest.getNoteId());
        boolean isUpdateByCreator = Objects.equals(stickyNoteFound.getCreatedByUserId(), updatedByUserId);

        boolean isNoteDeleted = isNoteDeleted(stickyNoteAddRequest);
        if (isNoteDeleted) {
            if (!isUpdateByCreator) throw new ValidationFailedException("You're not authorized to delete the sticky note");
            boolean pinnedStickyNote = dashboardPinnedStickyNoteRepository.existsByUserIdAndNoteId(updatedByUserId,stickyNoteAddRequest.getNoteId());
            if (pinnedStickyNote) {
                unpinNoteFromDashboard(updatedByUserId,stickyNoteAddRequest.getNoteId());
            }
            Integer resultSet = stickyNoteRepository.updateStickyNoteStatusByNoteId(Constants.Sticky_Note_DeleteType.STICKY_NOTE_DELETED, stickyNoteAddRequest.getNoteId());
            return message;
        }

        boolean isNoteModified = isNoteModified(stickyNoteAddRequest);
        if (isNoteModified) {
            isUpdateAllowed(stickyNoteAddRequest, stickyNoteFound, updatedByUserId);

            boolean isAccessTypeValidated = validateStickyNoteAccessType(stickyNoteAddRequest);
            boolean isPostedByAccountIdValidated = validatePostedByAccountIdForOrgId(stickyNoteAddRequest);
            boolean isSharedAccountIdsValidated = true;
            if (stickyNoteAddRequest.getSharedAccountIds() != null && !stickyNoteAddRequest.getSharedAccountIds().isEmpty()) {
                isSharedAccountIdsValidated = validateSharedAccountIdsForOrg(stickyNoteAddRequest);
            }

            if (isAccessTypeValidated && isPostedByAccountIdValidated && isSharedAccountIdsValidated) {
                stickyNoteFound.setNote(stickyNoteAddRequest.getNote());
                stickyNoteFound.setAccessType(stickyNoteAddRequest.getAccessType());
                stickyNoteFound.setOrgId(stickyNoteAddRequest.getOrgId());
                stickyNoteFound.setBuId(stickyNoteAddRequest.getBuId());
                stickyNoteFound.setProjectId(stickyNoteAddRequest.getProjectId());
                stickyNoteFound.setTeamId(stickyNoteAddRequest.getTeamId());
                stickyNoteFound.setPostedByAccountId(stickyNoteAddRequest.getPostedByAccountId());
                stickyNoteFound.setShareEditAllowed(stickyNoteAddRequest.getShareEditAllowed());
                stickyNoteFound.setSharedAccountIds(stickyNoteAddRequest.getSharedAccountIds());
                StickyNote updatedStickyNote = stickyNoteRepository.save(stickyNoteFound);
                auditService.auditForStickyNote(userAccountRepository.findByAccountIdAndIsActive(stickyNoteAddRequest.getPostedByAccountId(), true), updatedStickyNote,true);
                return message;
            } else {
                throw new StickyNoteFailedException("Sticky note can not be updated.");
            }
        }
        return null;
    }

    private void isUpdateAllowed(StickyNoteAddRequest request, StickyNote stickyNote, Long updatedByUserId) {
        boolean isUpdateByCreator = Objects.equals(stickyNote.getCreatedByUserId(), updatedByUserId);
        if (isUpdateByCreator) return;

        String allAccountIdsOfUser = userAccountService.getActiveAccountIdsForUserId(updatedByUserId, null).stream()
                .map(Object::toString).collect(Collectors.joining(", "));

        // validation: only creator is allowed to edit share edit access
        if (!Objects.equals(request.getShareEditAllowed(), stickyNote.getShareEditAllowed())) {
            throw new ValidationFailedException("You're not authorized to update share access of sticky note");
        }

        // validation: only creator is allowed to share the sticky note
        if (!Objects.equals(request.getSharedAccountIds(), stickyNote.getSharedAccountIds())) {
            throw new ValidationFailedException("You're not authorized to share the sticky note");
        }

        boolean isUpdateAllowed = false;
        if (stickyNote.getShareEditAllowed()) {
            // validate if the note is shared the user is part of that entity o.w in case of private, it is shared with the user
            validateAccess(updatedByUserId, stickyNote.getNoteId(), allAccountIdsOfUser);
            isUpdateAllowed = true;
        }

        if (!isUpdateAllowed) throw new ValidationFailedException("You're not authorized to update the sticky note");
    }

    /**
     * This method converts all date time fields in the StickNote object to user's local timezone
     */
    public void convertAllDateTimeToLocalTimeZone(StickyNote stickyNote, String timeZone) {
        stickyNote.setCreatedDateTime(DateTimeUtils.convertServerDateToUserTimezone(stickyNote.getCreatedDateTime(), timeZone));
        if (stickyNote.getLastUpdatedDateTime() != null) {
            stickyNote.setLastUpdatedDateTime(DateTimeUtils.convertServerDateToUserTimezone(stickyNote.getLastUpdatedDateTime(), timeZone));
        }
    }

    /**
     * method is used to pin a note. If the note is public, then the user pinning it should be part of that entity.
     * If the note is private then the user pinning it should be
     */
    public void pinNote(Long userId, Long noteId) {
        // Check if the note is already pinned by the user
        boolean isAlreadyPinned = pinnedStickyNoteRepository.existsByUserIdAndNoteId(userId, noteId);
        if (isAlreadyPinned) {
            throw new IllegalStateException("Note is already pinned.");
        }

        PinnedStickyNote pinnedStickyNote = new PinnedStickyNote();
        pinnedStickyNote.setNoteId(noteId);
        pinnedStickyNote.setUserId(userId);
        pinnedStickyNoteRepository.save(pinnedStickyNote);
    }

    /**
     * method is used to unpin a pinned note
     */
    public void unpinNote(Long userId, Long noteId) {
        // Find the pinned note
        PinnedStickyNote pinnedStickyNote = pinnedStickyNoteRepository.findByUserIdAndNoteId(userId, noteId);
        if (pinnedStickyNote == null) {
            throw new IllegalStateException("Note is not pinned.");
        }

        // Unpin the note
        pinnedStickyNoteRepository.delete(pinnedStickyNote);
    }

    /**
     * method is used to mark a note as important based on his access
     */
    public void markNoteAsImportant(Long userId, Long noteId) {
        // Check if the note is already marked as important by the user
        boolean isAlreadyImportant = importantStickyNoteRepository.existsByUserIdAndNoteId(userId, noteId);
        if (isAlreadyImportant) {
            throw new IllegalStateException("Note is already marked as important.");
        }

        ImportantStickyNote importantStickyNote = new ImportantStickyNote();
        importantStickyNote.setNoteId(noteId);
        importantStickyNote.setUserId(userId);
        importantStickyNoteRepository.save(importantStickyNote);
    }

    /**
     * method is used to un-mark the note as important
     */
    public void unmarkNoteAsImportant(Long userId, Long noteId) {
        // Find the important note
        ImportantStickyNote importantStickyNote = importantStickyNoteRepository.findByUserIdAndNoteId(userId, noteId);
        if (importantStickyNote == null) {
            throw new IllegalStateException("Note is not marked as important.");
        }

        importantStickyNoteRepository.delete(importantStickyNote);
    }

    /**
     * validates access of the user updating a sticky note. If it's public note, then we check whether the user is part of that entity.
     * If it is private note, then we check whether the note is shared with the user or not
     */
    public void validateAccess(Long userId, Long noteId, String accountIds) {
        List<Long> userAccountIds = CommonUtils.convertToLongList(accountIds);
        StickyNote stickyNote = stickyNoteRepository.findById(noteId).orElseThrow(() -> new EntityNotFoundException("Sticky note not found"));

        if (Objects.equals(stickyNote.getCreatedByUserId(), userId)) return;

        // If the note is public, check if the user is part of the entity
        if (stickyNote.getAccessType().equals(Constants.Sticky_Note_AccessType.PUBLIC_ACCESS)) {

            // Check if the user is part of the note's orgId, buId, projectId, or teamId
            boolean isPartOfEntity = false;
            if (stickyNote.getOrgId() != null) {
                if (userAccountService.isActiveUserAccountExistsByUserIdAndOrgId(userId, stickyNote.getOrgId()))
                    isPartOfEntity = true;
            } else if (stickyNote.getBuId() != null) {
                List<Long> buMembers = buService.getBuMembersAccountIdList(stickyNote.getBuId()).stream().map(AccountId::getAccountId).collect(Collectors.toList());
                if (!buMembers.isEmpty()) {
                    isPartOfEntity = true;
                }
            } else if (stickyNote.getProjectId() != null) {
                List<Long> projectMembers = projectService.getprojectMembersAccountIdList(List.of(stickyNote.getProjectId())).stream().map(AccountId::getAccountId).collect(Collectors.toList());
                if (!projectMembers.isEmpty()) {
                    isPartOfEntity = true;
                }
            } else if (stickyNote.getTeamId() != null) {
                List<AccessDomain> accessDomains = accessDomainRepository.findByEntityTypeIdAndEntityIdAndAccountIdInAndIsActive(com.tse.core_application.model.Constants.EntityTypes.TEAM,
                        stickyNote.getTeamId(), userAccountIds, true);
                if (accessDomains != null && !accessDomains.isEmpty()) isPartOfEntity = true;
            }

            if (!isPartOfEntity) {
                throw new ValidationFailedException("User does not have access to perform this action on note.");
            }
        } else if (stickyNote.getAccessType().equals(Constants.Sticky_Note_AccessType.PRIVATE_ACCESS)) {

            List<Long> sharedAccountIds = stickyNote.getSharedAccountIds().stream().map(Long::parseLong).collect(Collectors.toList());
            // If the note is private, check if the user is in the sharedAccountIds list
            if (stickyNote.getSharedAccountIds() == null || !CommonUtils.containsAny(sharedAccountIds, userAccountIds)) {
                throw new ValidationFailedException("User does not have access to perform this action on note.");
            }
        }
    }

    /** this method removes the deleted user's accountId from the sharedAccountIds list */
    public void removeDeletedAccountFromStickyNotesSharedList(Integer entityTypeId, Long entityId, Long removedUserAccountId) {
        List<StickyNote> stickyNotes = new ArrayList<>();
        List<StickyNote> modifiedStickyNotes = new ArrayList<>();
        if (Objects.equals(entityTypeId, com.tse.core_application.model.Constants.EntityTypes.TEAM)) {
            stickyNotes = stickyNoteRepository.findByAccessTypeAndTeamIdInAndIsDeletedAndSharedAccountIdsNotNull(Constants.Sticky_Note_AccessType.PRIVATE_ACCESS, List.of(entityId), Constants.Sticky_Note_DeleteType.STICKY_NOTE_NOT_DELETED);
        } else if (Objects.equals(entityTypeId, com.tse.core_application.model.Constants.EntityTypes.ORG)) {
            stickyNotes = stickyNoteRepository.findByAccessTypeAndOrgIdInAndIsDeletedAndSharedAccountIdsNotNull(Constants.Sticky_Note_AccessType.PRIVATE_ACCESS, List.of(entityId), Constants.Sticky_Note_DeleteType.STICKY_NOTE_NOT_DELETED);
        }

        for (StickyNote stickyNote : stickyNotes) {
            List<String> sharedAccountIds = new ArrayList<>(stickyNote.getSharedAccountIds());
            if (sharedAccountIds.contains(removedUserAccountId.toString())) {
                sharedAccountIds.remove(removedUserAccountId.toString());
                stickyNote.setSharedAccountIds(sharedAccountIds);
                modifiedStickyNotes.add(stickyNote);
            }
        }

        if (!modifiedStickyNotes.isEmpty()) {
            stickyNoteRepository.saveAll(modifiedStickyNotes);
        }
    }

    /**
     * method is used to pin a note to dashboard. If the note is public, then the user pinning it should be part of that entity.
     */
    public void pinNoteToDashboard(Long userId, Long noteId) {
        List<Long> userIdList = userService.getManagedUserList(userId);
        List<DashboardPinnedStickyNote> pinnedStickyNoteList = dashboardPinnedStickyNoteRepository.findByUserIdInAndMaxCreatedDateTime(userIdList);
        DashboardPinnedStickyNote pinnedStickyNote = !pinnedStickyNoteList.isEmpty() ? pinnedStickyNoteList.get(0) : null;

        if (pinnedStickyNote == null) {
            pinnedStickyNote = new DashboardPinnedStickyNote();
        }
        pinnedStickyNote.setNoteId(noteId);
        pinnedStickyNote.setUserId(userId);
        dashboardPinnedStickyNoteRepository.save(pinnedStickyNote);
    }

    /**
     * method is used to unpin a pinned note from dashboard
     */
    public void unpinNoteFromDashboard(Long userId, Long noteId) {
        // Find the pinned note
        DashboardPinnedStickyNote pinnedStickyNote = dashboardPinnedStickyNoteRepository.findByUserIdAndNoteId(userId, noteId);
        if (pinnedStickyNote == null) {
            throw new IllegalStateException("Note is not pinned.");
        }
        // Unpin the note
        dashboardPinnedStickyNoteRepository.delete(pinnedStickyNote);
    }

    public StickyNote getUserStickyNoteForDashboard (Long userId) {
        StickyNote stickyNote = new StickyNote();
        List<Long> userIdList = userService.getManagedUserList(userId);
        List<DashboardPinnedStickyNote> dashboardPinnedStickyNote = dashboardPinnedStickyNoteRepository.findByUserIdInAndMaxCreatedDateTime(userIdList);
        if (dashboardPinnedStickyNote != null && !dashboardPinnedStickyNote.isEmpty()) {
            stickyNote = stickyNoteRepository.findByNoteId(dashboardPinnedStickyNote.get(0).getNoteId());
        }
        return stickyNote;
    }
}

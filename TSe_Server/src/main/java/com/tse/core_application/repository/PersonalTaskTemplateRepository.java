package com.tse.core_application.repository;

import com.tse.core_application.model.TaskTemplate;
import com.tse.core_application.model.personal_task.PersonalTaskTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface PersonalTaskTemplateRepository extends JpaRepository<PersonalTaskTemplate, Long> {

    PersonalTaskTemplate findByTemplateId (Long templateId);

    @Query(value = "select (max (t.templateNumber)) from TaskTemplate t")
    Long getMaxTemplateNumber();

    List<PersonalTaskTemplate> findAllTemplatesByFkAccountIdAccountId (Long accountId);

    List<PersonalTaskTemplate> findAllTemplatesByFkAccountIdAccountIdIn (List<Long> accountId);

    @Modifying
    @Transactional
    @Query("DELETE FROM PersonalTaskTemplate ptt WHERE ptt.fkAccountId.accountId IN :accountIds")
    void deleteByAccountIdIn(List<Long> accountIds);

}


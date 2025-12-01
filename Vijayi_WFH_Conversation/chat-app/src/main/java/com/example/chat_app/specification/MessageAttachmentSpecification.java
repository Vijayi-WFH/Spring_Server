package com.example.chat_app.specification;

import com.example.chat_app.custom.model.SearchCriteria;
import com.example.chat_app.custom.model.SearchOperation;
import com.example.chat_app.model.MessageAttachment;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.Nullable;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
public class MessageAttachmentSpecification implements Specification<MessageAttachment> {

    private List<SearchCriteria> searchCriteriaList = new ArrayList<>();

    public void addSearchCriteria(SearchCriteria searchCriteria) {
        searchCriteriaList.add(searchCriteria);
    }

    @Nullable
    @Override
    public Predicate toPredicate(Root<MessageAttachment> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) {

        List<Predicate> predicates = new ArrayList<>();

        for(SearchCriteria searchCriteria : searchCriteriaList) {
            if(searchCriteria.getOperation().equals(SearchOperation.EQUAL)) predicates.add(criteriaBuilder.equal(root.get(searchCriteria.getKey()), searchCriteria.getValue()));
        }

        return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
    }

}

package com.tse.core_application.dto.label;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class EntityTypeLabelResponse {

        private Long labelId;
        private String labelName;

        public EntityTypeLabelResponse(Long labelId, Object labelName) {
            this.labelId = labelId;
            this.labelName = (String) labelName;
        }

    }


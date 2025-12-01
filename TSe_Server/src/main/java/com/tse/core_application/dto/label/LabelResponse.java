package com.tse.core_application.dto.label;

import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
public class LabelResponse {
    private Long labelId;
    private String labelName;
    private String teamCode;

    public LabelResponse(Long labelId, Object labelName, String teamCode) {
        this.labelId = labelId;
        this.labelName = (String) labelName;
        this.teamCode = teamCode;
    }

}

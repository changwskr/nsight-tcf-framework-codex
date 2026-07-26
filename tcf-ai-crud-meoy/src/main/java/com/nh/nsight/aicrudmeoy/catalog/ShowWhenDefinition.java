package com.nh.nsight.aicrudmeoy.catalog;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ShowWhenDefinition {
    /** 같은 단계(또는 이전) 답변의 questionId */
    private String questionId;
    /** 일치해야 보이는 값 (single 옵션 value) */
    private String equals;

    public String getQuestionId() {
        return questionId;
    }

    public void setQuestionId(String questionId) {
        this.questionId = questionId;
    }

    public String getEquals() {
        return equals;
    }

    public void setEquals(String equals) {
        this.equals = equals;
    }
}

package com.nh.nsight.aicrudmeoy.catalog;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class QuestionDefinition {
    private String id;
    private String text;
    private String type;
    private String ledgerKey;
    private String recommended;
    private String placeholder;
    private List<OptionDefinition> options = new ArrayList<>();
    /** 지정 시 조건이 맞을 때만 질문 표시·완료 필수 */
    private ShowWhenDefinition showWhen;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getLedgerKey() { return ledgerKey; }
    public void setLedgerKey(String ledgerKey) { this.ledgerKey = ledgerKey; }
    public String getRecommended() { return recommended; }
    public void setRecommended(String recommended) { this.recommended = recommended; }
    public String getPlaceholder() { return placeholder; }
    public void setPlaceholder(String placeholder) { this.placeholder = placeholder; }
    public List<OptionDefinition> getOptions() { return options; }
    public void setOptions(List<OptionDefinition> options) { this.options = options; }
    public ShowWhenDefinition getShowWhen() { return showWhen; }
    public void setShowWhen(ShowWhenDefinition showWhen) { this.showWhen = showWhen; }
}

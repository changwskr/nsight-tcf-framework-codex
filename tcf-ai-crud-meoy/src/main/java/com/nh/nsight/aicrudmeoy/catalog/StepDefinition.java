package com.nh.nsight.aicrudmeoy.catalog;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class StepDefinition {
    private String id;
    private String title;
    private int order;
    private String promptFile;
    private String resultFile;
    private String nextId;
    private String outcome;
    private boolean requiresGate;
    @JsonProperty("isGate")
    private boolean gate;
    private List<String> gateChecks = new ArrayList<>();
    private List<QuestionDefinition> questions = new ArrayList<>();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public int getOrder() { return order; }
    public void setOrder(int order) { this.order = order; }
    public String getPromptFile() { return promptFile; }
    public void setPromptFile(String promptFile) { this.promptFile = promptFile; }
    public String getResultFile() { return resultFile; }
    public void setResultFile(String resultFile) { this.resultFile = resultFile; }
    public String getNextId() { return nextId; }
    public void setNextId(String nextId) { this.nextId = nextId; }
    public String getOutcome() { return outcome; }
    public void setOutcome(String outcome) { this.outcome = outcome; }
    public boolean isRequiresGate() { return requiresGate; }
    public void setRequiresGate(boolean requiresGate) { this.requiresGate = requiresGate; }
    public boolean isGate() { return gate; }
    public void setGate(boolean gate) { this.gate = gate; }
    public List<String> getGateChecks() { return gateChecks; }
    public void setGateChecks(List<String> gateChecks) { this.gateChecks = gateChecks; }
    public List<QuestionDefinition> getQuestions() { return questions; }
    public void setQuestions(List<QuestionDefinition> questions) { this.questions = questions; }
}

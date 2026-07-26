package com.nh.nsight.marketing.oc.capnew.application.dto;

import java.util.ArrayList;
import java.util.List;

public class CapNewStepValidationCDTO {

    private boolean valid;
    private List<String> errors = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }

    public void addError(String message) {
        errors.add(message);
        valid = false;
    }

    public void addWarning(String message) {
        warnings.add(message);
    }
}

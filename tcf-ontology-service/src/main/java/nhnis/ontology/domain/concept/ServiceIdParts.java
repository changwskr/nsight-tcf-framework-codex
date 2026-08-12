package nhnis.ontology.domain.concept;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Parsed PDMG ServiceId (11 chars).
 * example: mgcoa8888S0
 */
public final class ServiceIdParts {

    private final String groupCode;
    private final String businessCode;
    private final String functionCode;
    private final String programNo;
    private final String operationType;
    private final String sequence;
    private final String fullServiceId;

    public ServiceIdParts(
            String groupCode,
            String businessCode,
            String functionCode,
            String programNo,
            String operationType,
            String sequence,
            String fullServiceId) {
        this.groupCode = groupCode;
        this.businessCode = businessCode;
        this.functionCode = functionCode;
        this.programNo = programNo;
        this.operationType = operationType;
        this.sequence = sequence;
        this.fullServiceId = fullServiceId;
    }

    public String getGroupCode() {
        return groupCode;
    }

    public String getBusinessCode() {
        return businessCode;
    }

    public String getFunctionCode() {
        return functionCode;
    }

    public String getProgramNo() {
        return programNo;
    }

    public String getOperationType() {
        return operationType;
    }

    public String getSequence() {
        return sequence;
    }

    public String getFullServiceId() {
        return fullServiceId;
    }

    public String programId() {
        return groupCode + businessCode + functionCode + programNo;
    }

    public Map<String, Object> toAttributeMap() {
        Map<String, Object> attrs = new LinkedHashMap<>();
        attrs.put("groupCode", groupCode);
        attrs.put("businessCode", businessCode);
        attrs.put("functionCode", functionCode);
        attrs.put("programNo", programNo);
        attrs.put("operationType", operationType);
        attrs.put("sequence", sequence);
        attrs.put("fullServiceId", fullServiceId);
        attrs.put("programId", programId());
        return attrs;
    }

    public OntologyConcept toConcept() {
        return OntologyConcept.builder()
                .id(ConceptIds.service(fullServiceId))
                .type(ConceptType.SERVICE_ID)
                .name(fullServiceId)
                .attributes(toAttributeMap())
                .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ServiceIdParts that)) {
            return false;
        }
        return Objects.equals(fullServiceId, that.fullServiceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fullServiceId);
    }

    @Override
    public String toString() {
        return fullServiceId + "[" + groupCode + "/" + businessCode + "/" + functionCode
                + "/" + programNo + "/" + operationType + "/" + sequence + "]";
    }
}

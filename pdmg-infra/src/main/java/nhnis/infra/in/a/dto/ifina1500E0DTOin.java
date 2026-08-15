package nhnis.infra.in.a.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ims.superspring.dto.DataObject;
import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;

/** OPEN-02 IdP 역할 동기화 요청. */
public class ifina1500E0DTOin extends DataObject {
    private static final long serialVersionUID = 1L;

    @JsonProperty("entries")
    private List<Map<String, Object>> entries = new ArrayList<>();
    private String dryRunYn;
    private String createMissingYn;

    public List<Map<String, Object>> getEntries() {
        return entries;
    }

    public void setEntries(List<Map<String, Object>> v) {
        entries = v != null ? v : new ArrayList<>();
    }

    public String getDryRunYn() {
        return dryRunYn;
    }

    public void setDryRunYn(String dryRunYn) {
        this.dryRunYn = dryRunYn;
    }

    public String getCreateMissingYn() {
        return createMissingYn;
    }

    public void setCreateMissingYn(String createMissingYn) {
        this.createMissingYn = createMissingYn;
    }

    @Override
    public Object clone() {
        ifina1500E0DTOin c = new ifina1500E0DTOin();
        c.clone(this);
        return c;
    }

    public void clone(DataObject src) {
        if (this == src) {
            return;
        }
        ifina1500E0DTOin in = (ifina1500E0DTOin) src;
        entries = in.entries == null ? new ArrayList<>() : new ArrayList<>(in.entries);
        dryRunYn = in.dryRunYn;
        createMissingYn = in.createMissingYn;
    }

    private static final Map<String, FieldProperty> fieldPropertyMap = new LinkedHashMap<>();

    static {
        fieldPropertyMap.put("entries", FieldProperty.builder().setPhysicalName("entries")
                .setLogicalName("entries").setType(FieldProperty.TYPE_OBJECT_STRING).setDecimal(-1)
                .setIsNullable(true).setIsEncrypt(false).build());
    }

    public Map<String, FieldProperty> getFieldPropertyMap() {
        return Collections.unmodifiableMap(fieldPropertyMap);
    }
}

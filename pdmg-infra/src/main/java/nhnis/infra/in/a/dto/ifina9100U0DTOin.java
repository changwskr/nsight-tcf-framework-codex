package nhnis.infra.in.a.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.ims.superspring.dto.DataObject;
import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;

public class ifina9100U0DTOin extends DataObject {
    private static final long serialVersionUID = 1L;
    private String targetType;
    private String targetId;
    private List<Map<String, Object>> items = new ArrayList<>();

    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }
    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }
    public List<Map<String, Object>> getItems() { return items; }
    public void setItems(List<Map<String, Object>> items) {
        this.items = items != null ? items : new ArrayList<>();
    }

    @Override public Object clone() { ifina9100U0DTOin c = new ifina9100U0DTOin(); c.clone(this); return c; }
    public void clone(DataObject src) {
        if (this == src) return;
        ifina9100U0DTOin in = (ifina9100U0DTOin) src;
        targetType = in.targetType; targetId = in.targetId;
        items = in.items == null ? new ArrayList<>() : new ArrayList<>(in.items);
    }
    private static final Map<String, FieldProperty> fieldPropertyMap = new LinkedHashMap<>();
    static {
        for (String n : new String[]{"targetType","targetId"}) {
            fieldPropertyMap.put(n, FieldProperty.builder().setPhysicalName(n).setLogicalName(n)
                    .setType(FieldProperty.TYPE_OBJECT_STRING).setDecimal(-1).setIsNullable(true).setIsEncrypt(false).build());
        }
    }
    public Map<String, FieldProperty> getFieldPropertyMap() { return Collections.unmodifiableMap(fieldPropertyMap); }
}

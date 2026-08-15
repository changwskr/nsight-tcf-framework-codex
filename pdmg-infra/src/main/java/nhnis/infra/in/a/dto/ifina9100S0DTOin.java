package nhnis.infra.in.a.dto;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.ims.superspring.dto.DataObject;
import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;

public class ifina9100S0DTOin extends DataObject {
    private static final long serialVersionUID = 1L;
    private String targetType;
    private String targetId;
    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }
    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }
    @Override public Object clone() { ifina9100S0DTOin c = new ifina9100S0DTOin(); c.clone(this); return c; }
    public void clone(DataObject src) {
        if (this == src) return;
        ifina9100S0DTOin in = (ifina9100S0DTOin) src;
        targetType = in.targetType; targetId = in.targetId;
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

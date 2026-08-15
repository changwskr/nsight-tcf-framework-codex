package nhnis.infra.in.a.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.ims.superspring.dto.DataObject;
import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;

public class ifina9100S0DTOout extends DataObject {
    private static final long serialVersionUID = 1L;
    private List<ifina9100S0DTOSub0> ifina9100S0DTOSub0;
    private int size;
    private int checkedCount;
    private int totalItems;
    private int progressPct;
    private String targetType;
    private String targetId;

    public int sizeifina9100S0DTOSub0() { return ifina9100S0DTOSub0 == null ? 0 : ifina9100S0DTOSub0.size(); }
    public List<ifina9100S0DTOSub0> getifina9100S0DTOSub0() { return ifina9100S0DTOSub0; }
    public void setifina9100S0DTOSub0(List<ifina9100S0DTOSub0> list) { this.ifina9100S0DTOSub0 = list; }
    public void addifina9100S0DTOSub0(ifina9100S0DTOSub0 item) {
        if (ifina9100S0DTOSub0 == null) ifina9100S0DTOSub0 = new ArrayList<>();
        ifina9100S0DTOSub0.add(item);
    }
    public int getSize() { return size; } public void setSize(int size) { this.size = size; }
    public int getCheckedCount() { return checkedCount; } public void setCheckedCount(int checkedCount) { this.checkedCount = checkedCount; }
    public int getTotalItems() { return totalItems; } public void setTotalItems(int totalItems) { this.totalItems = totalItems; }
    public int getProgressPct() { return progressPct; } public void setProgressPct(int progressPct) { this.progressPct = progressPct; }
    public String getTargetType() { return targetType; } public void setTargetType(String targetType) { this.targetType = targetType; }
    public String getTargetId() { return targetId; } public void setTargetId(String targetId) { this.targetId = targetId; }

    @Override public Object clone() { ifina9100S0DTOout c = new ifina9100S0DTOout(); c.clone(this); return c; }
    public void clone(DataObject src) {
        if (this == src) return;
        ifina9100S0DTOout in = (ifina9100S0DTOout) src;
        ifina9100S0DTOSub0 = null;
        if (in.ifina9100S0DTOSub0 != null) {
            for (ifina9100S0DTOSub0 x : in.ifina9100S0DTOSub0) {
                addifina9100S0DTOSub0(x == null ? null : (ifina9100S0DTOSub0) x.clone());
            }
        }
        size = in.size; checkedCount = in.checkedCount; totalItems = in.totalItems;
        progressPct = in.progressPct; targetType = in.targetType; targetId = in.targetId;
    }
    private static final Map<String, FieldProperty> fieldPropertyMap = new LinkedHashMap<>();
    static {
        fieldPropertyMap.put("ifina9100S0DTOSub0", FieldProperty.builder().setPhysicalName("ifina9100S0DTOSub0")
                .setLogicalName("ifina9100S0DTOSub0").setType(FieldProperty.TYPE_ABSTRACT_INCLUDE).setDecimal(-1)
                .setArray("size").setReference("nhnis.infra.in.a.dto.ifina9100S0DTOSub0")
                .setIsNullable(true).setIsEncrypt(false).build());
        fieldPropertyMap.put("size", FieldProperty.builder().setPhysicalName("size").setLogicalName("size")
                .setType(FieldProperty.TYPE_PRIMITIVE_INT).setDecimal(-1).setIsNullable(true).setIsEncrypt(false).build());
    }
    public Map<String, FieldProperty> getFieldPropertyMap() { return Collections.unmodifiableMap(fieldPropertyMap); }
}

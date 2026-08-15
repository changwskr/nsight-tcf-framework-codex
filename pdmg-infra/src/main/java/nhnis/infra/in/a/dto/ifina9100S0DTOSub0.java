package nhnis.infra.in.a.dto;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.ims.superspring.dto.DataObject;
import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;

public class ifina9100S0DTOSub0 extends DataObject {
    private static final long serialVersionUID = 1L;
    private String checklistId, itemName, severityCd, checkedYn, remark;
    private Integer sortNo;
    public String getChecklistId() { return checklistId; } public void setChecklistId(String v) { checklistId = v; }
    public String getItemName() { return itemName; } public void setItemName(String v) { itemName = v; }
    public String getSeverityCd() { return severityCd; } public void setSeverityCd(String v) { severityCd = v; }
    public Integer getSortNo() { return sortNo; } public void setSortNo(Integer v) { sortNo = v; }
    public String getCheckedYn() { return checkedYn; } public void setCheckedYn(String v) { checkedYn = v; }
    public String getRemark() { return remark; } public void setRemark(String v) { remark = v; }
    @Override public Object clone() { ifina9100S0DTOSub0 c = new ifina9100S0DTOSub0(); c.clone(this); return c; }
    public void clone(DataObject src) {
        if (this == src) return;
        ifina9100S0DTOSub0 in = (ifina9100S0DTOSub0) src;
        checklistId=in.checklistId; itemName=in.itemName; severityCd=in.severityCd; sortNo=in.sortNo;
        checkedYn=in.checkedYn; remark=in.remark;
    }
    private static final Map<String, FieldProperty> fieldPropertyMap = new LinkedHashMap<>();
    static {
        for (String n : new String[]{"checklistId","itemName","severityCd","checkedYn","remark"}) {
            fieldPropertyMap.put(n, FieldProperty.builder().setPhysicalName(n).setLogicalName(n)
                    .setType(FieldProperty.TYPE_OBJECT_STRING).setDecimal(-1).setIsNullable(true).setIsEncrypt(false).build());
        }
        fieldPropertyMap.put("sortNo", FieldProperty.builder().setPhysicalName("sortNo").setLogicalName("sortNo")
                .setType(FieldProperty.TYPE_OBJECT_INT).setDecimal(-1).setIsNullable(true).setIsEncrypt(false).build());
    }
    public Map<String, FieldProperty> getFieldPropertyMap() { return Collections.unmodifiableMap(fieldPropertyMap); }
}

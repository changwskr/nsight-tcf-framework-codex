package nhnis.mg.co.a.dto;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.ims.superspring.dto.DataObject;
import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;

/**
 * 마케팅희망고객 조회 입력 (mgcoa5530S0).
 */
public class mgcoa5530S0DTOin extends DataObject {

    private static final long serialVersionUID = 1L;

    private String trtBrc;
    private String basDt;
    private Integer pageNo;
    private Integer pageSize;

    public String getTrtBrc() {
        return trtBrc;
    }

    public void setTrtBrc(String trtBrc) {
        this.trtBrc = trtBrc;
    }

    public String getBasDt() {
        return basDt;
    }

    public void setBasDt(String basDt) {
        this.basDt = basDt;
    }

    public Integer getPageNo() {
        return pageNo;
    }

    public void setPageNo(Integer pageNo) {
        this.pageNo = pageNo;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    @Override
    public Object clone() {
        mgcoa5530S0DTOin copy = new mgcoa5530S0DTOin();
        copy.clone(this);
        return copy;
    }

    public void clone(DataObject src) {
        if (this == src) {
            return;
        }
        mgcoa5530S0DTOin in = (mgcoa5530S0DTOin) src;
        this.trtBrc = in.trtBrc;
        this.basDt = in.basDt;
        this.pageNo = in.pageNo;
        this.pageSize = in.pageSize;
    }

    @Override
    public String toString() {
        return "trtBrc : " + trtBrc + " basDt : " + basDt
                + " pageNo : " + pageNo + " pageSize : " + pageSize;
    }

    private static final Map<String, FieldProperty> fieldPropertyMap;

    static {
        fieldPropertyMap = new LinkedHashMap<>();
        putString("trtBrc");
        putString("basDt");
        fieldPropertyMap.put("pageNo", FieldProperty.builder()
                .setPhysicalName("pageNo").setLogicalName("pageNo")
                .setType(FieldProperty.TYPE_OBJECT_INT).setDecimal(-1)
                .setIsNullable(true).setIsEncrypt(false).build());
        fieldPropertyMap.put("pageSize", FieldProperty.builder()
                .setPhysicalName("pageSize").setLogicalName("pageSize")
                .setType(FieldProperty.TYPE_OBJECT_INT).setDecimal(-1)
                .setIsNullable(true).setIsEncrypt(false).build());
    }

    private static void putString(String name) {
        fieldPropertyMap.put(name, FieldProperty.builder()
                .setPhysicalName(name).setLogicalName(name)
                .setType(FieldProperty.TYPE_OBJECT_STRING).setDecimal(-1)
                .setIsNullable(true).setIsEncrypt(false).build());
    }

    public Map<String, FieldProperty> getFieldPropertyMap() {
        return Collections.unmodifiableMap(fieldPropertyMap);
    }
}

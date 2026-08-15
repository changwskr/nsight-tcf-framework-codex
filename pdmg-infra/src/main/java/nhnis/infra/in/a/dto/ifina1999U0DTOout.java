package nhnis.infra.in.a.dto;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.ims.superspring.dto.DataObject;
import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;

public class ifina1999U0DTOout extends DataObject {

    private static final long serialVersionUID = 1L;

    private Integer PROC_CNT;
    private String RSLT_CD;
    private String RSLT_MSG;

    public Integer getPROC_CNT() { return PROC_CNT; }
    public void setPROC_CNT(Integer PROC_CNT) { this.PROC_CNT = PROC_CNT; }
    public String getRSLT_CD() { return RSLT_CD; }
    public void setRSLT_CD(String RSLT_CD) { this.RSLT_CD = RSLT_CD; }
    public String getRSLT_MSG() { return RSLT_MSG; }
    public void setRSLT_MSG(String RSLT_MSG) { this.RSLT_MSG = RSLT_MSG; }

    @Override
    public Object clone() {
        ifina1999U0DTOout copy = new ifina1999U0DTOout();
        copy.clone(this);
        return copy;
    }

    public void clone(DataObject src) {
        if (this == src) {
            return;
        }
        ifina1999U0DTOout in = (ifina1999U0DTOout) src;
        this.PROC_CNT = in.PROC_CNT;
        this.RSLT_CD = in.RSLT_CD;
        this.RSLT_MSG = in.RSLT_MSG;
    }

    private static final Map<String, FieldProperty> fieldPropertyMap = new LinkedHashMap<>();

    static {
        fieldPropertyMap.put("PROC_CNT", FieldProperty.builder()
                .setPhysicalName("PROC_CNT").setLogicalName("PROC_CNT")
                .setType(FieldProperty.TYPE_OBJECT_INT).setDecimal(-1)
                .setIsNullable(true).setIsEncrypt(false).build());
        fieldPropertyMap.put("RSLT_CD", FieldProperty.builder()
                .setPhysicalName("RSLT_CD").setLogicalName("RSLT_CD")
                .setType(FieldProperty.TYPE_OBJECT_STRING).setDecimal(-1)
                .setIsNullable(true).setIsEncrypt(false).build());
        fieldPropertyMap.put("RSLT_MSG", FieldProperty.builder()
                .setPhysicalName("RSLT_MSG").setLogicalName("RSLT_MSG")
                .setType(FieldProperty.TYPE_OBJECT_STRING).setDecimal(-1)
                .setIsNullable(true).setIsEncrypt(false).build());
    }

    public Map<String, FieldProperty> getFieldPropertyMap() {
        return Collections.unmodifiableMap(fieldPropertyMap);
    }
}

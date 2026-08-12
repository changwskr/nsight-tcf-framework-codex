package nhnis.mg.co.a.dto;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.ims.superspring.dto.DataObject;
import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;

/**
 * 이미지로그 삭제 결과 (mgcoa8888D0).
 */
public class mgcoa8888D0DTOout extends DataObject {

    private static final long serialVersionUID = 1L;

    private Integer PROC_CNT;
    private String RSLT_CD;
    private String RSLT_MSG;

    public Integer getPROC_CNT() {
        return PROC_CNT;
    }

    public void setPROC_CNT(Integer PROC_CNT) {
        this.PROC_CNT = PROC_CNT;
    }

    public String getRSLT_CD() {
        return RSLT_CD;
    }

    public void setRSLT_CD(String RSLT_CD) {
        this.RSLT_CD = RSLT_CD;
    }

    public String getRSLT_MSG() {
        return RSLT_MSG;
    }

    public void setRSLT_MSG(String RSLT_MSG) {
        this.RSLT_MSG = RSLT_MSG;
    }

    @Override
    public Object clone() {
        mgcoa8888D0DTOout copy = new mgcoa8888D0DTOout();
        copy.clone(this);
        return copy;
    }

    public void clone(DataObject src) {
        if (this == src) {
            return;
        }
        mgcoa8888D0DTOout in = (mgcoa8888D0DTOout) src;
        this.PROC_CNT = in.PROC_CNT;
        this.RSLT_CD = in.RSLT_CD;
        this.RSLT_MSG = in.RSLT_MSG;
    }

    @Override
    public String toString() {
        return "PROC_CNT : " + PROC_CNT + " RSLT_CD : " + RSLT_CD + " RSLT_MSG : " + RSLT_MSG;
    }

    private static final Map<String, FieldProperty> fieldPropertyMap;

    static {
        fieldPropertyMap = new LinkedHashMap<>();
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

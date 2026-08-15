package nhnis.infra.in.a.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.ims.superspring.dto.DataObject;
import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;

public class ifina3400V0DTOout extends DataObject {
    private static final long serialVersionUID = 1L;
    private Integer okCount;
    private Integer errorCount;
    private Integer PROC_CNT;
    private String RSLT_CD;
    private String RSLT_MSG;
    private List<Map<String, Object>> okRows = new ArrayList<>();
    private List<Map<String, Object>> errors = new ArrayList<>();

    public Integer getOkCount() { return okCount; }
    public void setOkCount(Integer okCount) { this.okCount = okCount; }
    public Integer getErrorCount() { return errorCount; }
    public void setErrorCount(Integer errorCount) { this.errorCount = errorCount; }
    public Integer getPROC_CNT() { return PROC_CNT; }
    public void setPROC_CNT(Integer PROC_CNT) { this.PROC_CNT = PROC_CNT; }
    public String getRSLT_CD() { return RSLT_CD; }
    public void setRSLT_CD(String RSLT_CD) { this.RSLT_CD = RSLT_CD; }
    public String getRSLT_MSG() { return RSLT_MSG; }
    public void setRSLT_MSG(String RSLT_MSG) { this.RSLT_MSG = RSLT_MSG; }
    public List<Map<String, Object>> getOkRows() { return okRows; }
    public void setOkRows(List<Map<String, Object>> okRows) {
        this.okRows = okRows != null ? okRows : new ArrayList<>();
    }
    public List<Map<String, Object>> getErrors() { return errors; }
    public void setErrors(List<Map<String, Object>> errors) {
        this.errors = errors != null ? errors : new ArrayList<>();
    }

    @Override public Object clone() {
        ifina3400V0DTOout c = new ifina3400V0DTOout();
        c.clone(this);
        return c;
    }
    public void clone(DataObject src) {
        if (this == src) return;
        ifina3400V0DTOout in = (ifina3400V0DTOout) src;
        okCount = in.okCount; errorCount = in.errorCount; PROC_CNT = in.PROC_CNT;
        RSLT_CD = in.RSLT_CD; RSLT_MSG = in.RSLT_MSG;
        okRows = in.okRows == null ? new ArrayList<>() : new ArrayList<>(in.okRows);
        errors = in.errors == null ? new ArrayList<>() : new ArrayList<>(in.errors);
    }
    private static final Map<String, FieldProperty> fieldPropertyMap = new LinkedHashMap<>();
    static {
        for (String n : new String[]{"okCount","errorCount","PROC_CNT"}) {
            fieldPropertyMap.put(n, FieldProperty.builder().setPhysicalName(n).setLogicalName(n)
                    .setType(FieldProperty.TYPE_OBJECT_INT).setDecimal(-1).setIsNullable(true).setIsEncrypt(false).build());
        }
        for (String n : new String[]{"RSLT_CD","RSLT_MSG"}) {
            fieldPropertyMap.put(n, FieldProperty.builder().setPhysicalName(n).setLogicalName(n)
                    .setType(FieldProperty.TYPE_OBJECT_STRING).setDecimal(-1).setIsNullable(true).setIsEncrypt(false).build());
        }
    }
    public Map<String, FieldProperty> getFieldPropertyMap() { return Collections.unmodifiableMap(fieldPropertyMap); }
}

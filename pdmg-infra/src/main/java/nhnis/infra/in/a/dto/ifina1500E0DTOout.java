package nhnis.infra.in.a.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.ims.superspring.dto.DataObject;
import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;

/** OPEN-02 IdP 역할 동기화 결과. */
public class ifina1500E0DTOout extends DataObject {
    private static final long serialVersionUID = 1L;
    private Integer PROC_CNT;
    private String RSLT_CD, RSLT_MSG;
    private int syncedCount;
    private int createdCount;
    private int skippedCount;
    private int errorCount;
    private List<Map<String, Object>> details = new ArrayList<>();

    public Integer getPROC_CNT() {
        return PROC_CNT;
    }

    public void setPROC_CNT(Integer v) {
        PROC_CNT = v;
    }

    public String getRSLT_CD() {
        return RSLT_CD;
    }

    public void setRSLT_CD(String v) {
        RSLT_CD = v;
    }

    public String getRSLT_MSG() {
        return RSLT_MSG;
    }

    public void setRSLT_MSG(String v) {
        RSLT_MSG = v;
    }

    public int getSyncedCount() {
        return syncedCount;
    }

    public void setSyncedCount(int v) {
        syncedCount = v;
    }

    public int getCreatedCount() {
        return createdCount;
    }

    public void setCreatedCount(int v) {
        createdCount = v;
    }

    public int getSkippedCount() {
        return skippedCount;
    }

    public void setSkippedCount(int v) {
        skippedCount = v;
    }

    public int getErrorCount() {
        return errorCount;
    }

    public void setErrorCount(int v) {
        errorCount = v;
    }

    public List<Map<String, Object>> getDetails() {
        return details;
    }

    public void setDetails(List<Map<String, Object>> v) {
        details = v != null ? v : new ArrayList<>();
    }

    @Override
    public Object clone() {
        ifina1500E0DTOout c = new ifina1500E0DTOout();
        c.clone(this);
        return c;
    }

    public void clone(DataObject src) {
        if (this == src) {
            return;
        }
        ifina1500E0DTOout in = (ifina1500E0DTOout) src;
        PROC_CNT = in.PROC_CNT;
        RSLT_CD = in.RSLT_CD;
        RSLT_MSG = in.RSLT_MSG;
        syncedCount = in.syncedCount;
        createdCount = in.createdCount;
        skippedCount = in.skippedCount;
        errorCount = in.errorCount;
        details = in.details == null ? new ArrayList<>() : new ArrayList<>(in.details);
    }

    private static final Map<String, FieldProperty> fieldPropertyMap = new LinkedHashMap<>();

    static {
        fieldPropertyMap.put("RSLT_CD", FieldProperty.builder().setPhysicalName("RSLT_CD")
                .setLogicalName("RSLT_CD").setType(FieldProperty.TYPE_OBJECT_STRING).setDecimal(-1)
                .setIsNullable(true).setIsEncrypt(false).build());
    }

    public Map<String, FieldProperty> getFieldPropertyMap() {
        return Collections.unmodifiableMap(fieldPropertyMap);
    }
}

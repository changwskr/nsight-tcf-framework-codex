package nhnis.infra.in.a.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.ims.superspring.dto.DataObject;
import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;

/** INF-340 업로드 행 + 검증/반영 요청 */
public class ifina3400V0DTOin extends DataObject {
    private static final long serialVersionUID = 1L;
    private List<Map<String, Object>> rows = new ArrayList<>();
    private String applyMode; // okOnly | allOrNothing (C0에서 사용)

    public List<Map<String, Object>> getRows() { return rows; }
    public void setRows(List<Map<String, Object>> rows) {
        this.rows = rows != null ? rows : new ArrayList<>();
    }
    public String getApplyMode() { return applyMode; }
    public void setApplyMode(String applyMode) { this.applyMode = applyMode; }

    @Override public Object clone() {
        ifina3400V0DTOin c = new ifina3400V0DTOin();
        c.clone(this);
        return c;
    }
    public void clone(DataObject src) {
        if (this == src) return;
        ifina3400V0DTOin in = (ifina3400V0DTOin) src;
        this.rows = in.rows == null ? new ArrayList<>() : new ArrayList<>(in.rows);
        this.applyMode = in.applyMode;
    }
    private static final Map<String, FieldProperty> fieldPropertyMap = new LinkedHashMap<>();
    static {
        fieldPropertyMap.put("rows", FieldProperty.builder().setPhysicalName("rows").setLogicalName("rows")
                .setType(FieldProperty.TYPE_OBJECT_STRING).setDecimal(-1).setIsNullable(true).setIsEncrypt(false).build());
        fieldPropertyMap.put("applyMode", FieldProperty.builder().setPhysicalName("applyMode").setLogicalName("applyMode")
                .setType(FieldProperty.TYPE_OBJECT_STRING).setDecimal(-1).setIsNullable(true).setIsEncrypt(false).build());
    }
    public Map<String, FieldProperty> getFieldPropertyMap() { return Collections.unmodifiableMap(fieldPropertyMap); }
}

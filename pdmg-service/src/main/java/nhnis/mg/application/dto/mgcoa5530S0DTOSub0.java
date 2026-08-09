package nhnis.mg.application.dto;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.ims.superspring.dto.DataObject;
import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;

/**
 * 마케팅희망고객 목록 Sub DTO (mgcoa5530S0).
 */
public class mgcoa5530S0DTOSub0 extends DataObject {

    private static final long serialVersionUID = 1L;

    private String l5101;
    private String l5102;
    private String l5103;
    private String l5104;

    public String getL5101() {
        return l5101;
    }

    public void setL5101(String l5101) {
        this.l5101 = l5101;
    }

    public String getL5102() {
        return l5102;
    }

    public void setL5102(String l5102) {
        this.l5102 = l5102;
    }

    public String getL5103() {
        return l5103;
    }

    public void setL5103(String l5103) {
        this.l5103 = l5103;
    }

    public String getL5104() {
        return l5104;
    }

    public void setL5104(String l5104) {
        this.l5104 = l5104;
    }

    @Override
    public Object clone() {
        mgcoa5530S0DTOSub0 copy = new mgcoa5530S0DTOSub0();
        copy.clone(this);
        return copy;
    }

    public void clone(DataObject src) {
        if (this == src) {
            return;
        }
        mgcoa5530S0DTOSub0 in = (mgcoa5530S0DTOSub0) src;
        this.l5101 = in.l5101;
        this.l5102 = in.l5102;
        this.l5103 = in.l5103;
        this.l5104 = in.l5104;
    }

    @Override
    public String toString() {
        return "L5101 : " + l5101 + " L5102 : " + l5102
                + " L5103 : " + l5103 + " L5104 : " + l5104;
    }

    private static final Map<String, FieldProperty> fieldPropertyMap;

    static {
        fieldPropertyMap = new LinkedHashMap<>();
        putString("l5101");
        putString("l5102");
        putString("l5103");
        putString("l5104");
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

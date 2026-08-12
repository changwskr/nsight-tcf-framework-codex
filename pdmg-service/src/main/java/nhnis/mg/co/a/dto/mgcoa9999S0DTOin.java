package nhnis.mg.co.a.dto;

import com.ims.superspring.dto.DataObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Collections;
import java.util.Map;
import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;
import java.util.stream.Collectors;

@jakarta.annotation.Generated(
        value = "com.tmaxsoft.sts4.codegen.dto.DtoGenerator",
        date = "26. 7. 24. 오전 10:08",
        comments = "mgcoa9999S0DTOin"
)
public class mgcoa9999S0DTOin extends DataObject
{
    private static final long serialVersionUID = 1L;

    private String salzTipKdc = null;

    public String getSalzTipKdc() {
        return salzTipKdc;
    }

    public void setSalzTipKdc(String salzTipKdc) {
        if(salzTipKdc == null) {
            this.salzTipKdc = null;
        } else {
            this.salzTipKdc = salzTipKdc;
        }
    }

    public Object clone() {
        mgcoa9999S0DTOin copyObj = new mgcoa9999S0DTOin();
        copyObj.clone(this);
        return copyObj;
    }

    public void clone(DataObject _mgcoa9999S0DTOin) {
        if(this == _mgcoa9999S0DTOin)
            return;

        mgcoa9999S0DTOin __mgcoa9999S0DTOin =
                (mgcoa9999S0DTOin) _mgcoa9999S0DTOin;
        this.setSalzTipKdc(__mgcoa9999S0DTOin.getSalzTipKdc());
    }

    public String toString() {
        StringBuilder buffer = new StringBuilder();

        buffer.append("salzTipKdc : ").append(salzTipKdc).append("\n");
        return buffer.toString();
    }

    private static final Map<String, FieldProperty> fieldPropertyMap;

    static {
        fieldPropertyMap = new java.util.LinkedHashMap<String, FieldProperty>(1);
        fieldPropertyMap.put("salzTipKdc", FieldProperty.builder()
                .setPhysicalName("salzTipKdc")
                .setLogicalName("salzTipKdc")
                .setType(FieldProperty.TYPE_OBJECT_STRING)
                .setDecimal(-1)
                .setIsNullable(true)
                .setIsEncrypt(false)
                .build());
    }

    public Map<String, FieldProperty> getFieldPropertyMap() {
        return Collections.unmodifiableMap(fieldPropertyMap);
    }
}

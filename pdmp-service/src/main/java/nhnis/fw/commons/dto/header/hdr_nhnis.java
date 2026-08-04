package nhnis.fw.commons.dto.header;

import java.util.Collections;
import java.util.Map;

import com.ims.superspring.dto.DataObject;
import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;

@jakarta.annotation.Generated(
        value = "com.imssoft.sts4.codegen.dto.DtoGenerator",
        date = "26. 7. 21. 오전 11:15",
        comments = "공통 헤더"
)
public class hdr_nhnis extends DataObject {
    private static final long serialVersionUID = 1L;

    private nhnis.fw.commons.dto.header.sys_comm sys_comm = null;

    public nhnis.fw.commons.dto.header.sys_comm getSys_comm() {
        return sys_comm;
    }

    public void setSys_comm(nhnis.fw.commons.dto.header.sys_comm sys_comm) {
        if (sys_comm == null) {
            this.sys_comm = null;
        } else {
            this.sys_comm = sys_comm;
        }
    }

    public Object clone() {
        hdr_nhnis copyObj = new hdr_nhnis();
        copyObj.clone(this);
        return copyObj;
    }

    public void clone(DataObject _hdr_nhnis) {
        if (this == _hdr_nhnis)
            return;

        hdr_nhnis __hdr_nhnis = (hdr_nhnis) _hdr_nhnis;
        nhnis.fw.commons.dto.header.sys_comm _value0 = __hdr_nhnis.getSys_comm();
        if (_value0 == null) {
            this.setSys_comm(null);
        } else {
            this.setSys_comm((nhnis.fw.commons.dto.header.sys_comm) _value0.clone());
        }
    }

    public String toString() {
        StringBuilder buffer = new StringBuilder();

        buffer.append("sys_comm : ").append(sys_comm).append("\n");
        return buffer.toString();
    }

    private static final Map<String, FieldProperty> fieldPropertyMap;

    static {
        fieldPropertyMap = new java.util.LinkedHashMap<String, FieldProperty>(1);
        fieldPropertyMap.put("sys_comm", FieldProperty.builder()
                .setPhysicalName("sys_comm")
                .setLogicalName("sys_comm")
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

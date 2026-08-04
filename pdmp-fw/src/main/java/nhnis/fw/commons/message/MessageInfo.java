package nhnis.fw.commons.message;

import com.ims.superspring.dto.DataObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Collections;
import java.util.Map;
import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;
import java.util.stream.Collectors;

@jakarta.annotation.Generated(
        value = "com.imssoft.sts4.codegen.dto.DtoGenerator",
        date = "26. 7. 21. 오전 11:21",
        comments = "MessageInfo"
)
public class MessageInfo extends DataObject {
    private static final long serialVersionUID = 1L;

    private String code = null;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        if (code == null) {
            this.code = null;
        } else {
            this.code = code;
        }
    }

    private String message = null;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        if (message == null) {
            this.message = null;
        } else {
            this.message = message;
        }
    }

    public Object clone() {
        MessageInfo copyObj = new MessageInfo();
        copyObj.clone(this);
        return copyObj;
    }

    public void clone(DataObject _messageInfo) {
        if (this == _messageInfo) {
            return;
        }

        MessageInfo __messageInfo = (MessageInfo) _messageInfo;
        this.setCode(__messageInfo.getCode());
        this.setMessage(__messageInfo.getMessage());
    }

    public String toString() {
        StringBuilder buffer = new StringBuilder();

        buffer.append("code : ").append(code).append("\n");
        buffer.append("message : ").append(message).append("\n");
        return buffer.toString();
    }

    private static final Map<String, FieldProperty> fieldPropertyMap;

    static {
        fieldPropertyMap = new java.util.LinkedHashMap<String, FieldProperty>(2);
        fieldPropertyMap.put("code", FieldProperty.builder()
                .setPhysicalName("code")
                .setLogicalName("코드")
                .setType(FieldProperty.TYPE_OBJECT_STRING)
                .setDecimal(-1)
                .setIsNullable(true)
                .setIsEncrypt(false)
                .build());
        fieldPropertyMap.put("message", FieldProperty.builder()
                .setPhysicalName("message")
                .setLogicalName("메시지 내용")
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

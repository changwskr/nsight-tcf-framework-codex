package nhnis.mg.co.a.dto;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.ims.superspring.dto.DataObject;
import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;

/**
 * 거래 파라미터 수정 입력 (mgcoa9000U0).
 */
public class mgcoa9000U0DTOin extends DataObject {

    private static final long serialVersionUID = 1L;

    private String txId;
    private String txName;
    private String appId;
    private String pathUrl;
    private String httpMethod;
    private String chgUserId;

    public String getTxId() {
        return txId;
    }

    public void setTxId(String txId) {
        this.txId = txId;
    }

    public String getTxName() {
        return txName;
    }

    public void setTxName(String txName) {
        this.txName = txName;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getPathUrl() {
        return pathUrl;
    }

    public void setPathUrl(String pathUrl) {
        this.pathUrl = pathUrl;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public String getChgUserId() {
        return chgUserId;
    }

    public void setChgUserId(String chgUserId) {
        this.chgUserId = chgUserId;
    }

    @Override
    public Object clone() {
        mgcoa9000U0DTOin copy = new mgcoa9000U0DTOin();
        copy.clone(this);
        return copy;
    }

    public void clone(DataObject src) {
        if (this == src) {
            return;
        }
        mgcoa9000U0DTOin in = (mgcoa9000U0DTOin) src;
        this.txId = in.txId;
        this.txName = in.txName;
        this.appId = in.appId;
        this.pathUrl = in.pathUrl;
        this.httpMethod = in.httpMethod;
        this.chgUserId = in.chgUserId;
    }

    @Override
    public String toString() {
        return "txId : " + txId + " txName : " + txName + "\n";
    }

    private static final Map<String, FieldProperty> fieldPropertyMap;

    static {
        fieldPropertyMap = new LinkedHashMap<>();
        putString("txId");
        putString("txName");
        putString("appId");
        putString("pathUrl");
        putString("httpMethod");
        putString("chgUserId");
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

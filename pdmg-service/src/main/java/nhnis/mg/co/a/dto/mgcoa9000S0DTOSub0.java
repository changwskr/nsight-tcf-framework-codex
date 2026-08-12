package nhnis.mg.co.a.dto;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.ims.superspring.dto.DataObject;
import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;

/**
 * 거래 파라미터 목록 Sub DTO (mgcoa9000S0).
 */
public class mgcoa9000S0DTOSub0 extends DataObject {

    private static final long serialVersionUID = 1L;

    private String txId;
    private String txName;
    private String appId;
    private String pathUrl;
    private String httpMethod;
    private String regUserId;
    private String regDtm;
    private String chgUserId;
    private String chgDtm;

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

    public String getRegUserId() {
        return regUserId;
    }

    public void setRegUserId(String regUserId) {
        this.regUserId = regUserId;
    }

    public String getRegDtm() {
        return regDtm;
    }

    public void setRegDtm(String regDtm) {
        this.regDtm = regDtm;
    }

    public String getChgUserId() {
        return chgUserId;
    }

    public void setChgUserId(String chgUserId) {
        this.chgUserId = chgUserId;
    }

    public String getChgDtm() {
        return chgDtm;
    }

    public void setChgDtm(String chgDtm) {
        this.chgDtm = chgDtm;
    }

    @Override
    public Object clone() {
        mgcoa9000S0DTOSub0 copy = new mgcoa9000S0DTOSub0();
        copy.clone(this);
        return copy;
    }

    public void clone(DataObject src) {
        if (this == src) {
            return;
        }
        mgcoa9000S0DTOSub0 in = (mgcoa9000S0DTOSub0) src;
        this.txId = in.txId;
        this.txName = in.txName;
        this.appId = in.appId;
        this.pathUrl = in.pathUrl;
        this.httpMethod = in.httpMethod;
        this.regUserId = in.regUserId;
        this.regDtm = in.regDtm;
        this.chgUserId = in.chgUserId;
        this.chgDtm = in.chgDtm;
    }

    @Override
    public String toString() {
        return "txId : " + txId + " txName : " + txName + " appId : " + appId
                + " pathUrl : " + pathUrl + " httpMethod : " + httpMethod;
    }

    private static final Map<String, FieldProperty> fieldPropertyMap;

    static {
        fieldPropertyMap = new LinkedHashMap<>();
        putString("txId");
        putString("txName");
        putString("appId");
        putString("pathUrl");
        putString("httpMethod");
        putString("regUserId");
        putString("regDtm");
        putString("chgUserId");
        putString("chgDtm");
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

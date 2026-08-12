package nhnis.mg.co.a.dto;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.ims.superspring.dto.DataObject;
import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;

/**
 * 거래 파라미터 조회 입력 (mgcoa9000S0).
 */
public class mgcoa9000S0DTOin extends DataObject {

    private static final long serialVersionUID = 1L;

    private String keyword;
    private String txId;
    private String txName;
    private String appId;
    private String httpMethod;
    private Integer pageNo;
    private Integer pageSize;

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

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

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public Integer getPageNo() {
        return pageNo;
    }

    public void setPageNo(Integer pageNo) {
        this.pageNo = pageNo;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    @Override
    public Object clone() {
        mgcoa9000S0DTOin copy = new mgcoa9000S0DTOin();
        copy.clone(this);
        return copy;
    }

    public void clone(DataObject src) {
        if (this == src) {
            return;
        }
        mgcoa9000S0DTOin in = (mgcoa9000S0DTOin) src;
        this.keyword = in.keyword;
        this.txId = in.txId;
        this.txName = in.txName;
        this.appId = in.appId;
        this.httpMethod = in.httpMethod;
        this.pageNo = in.pageNo;
        this.pageSize = in.pageSize;
    }

    @Override
    public String toString() {
        return "keyword : " + keyword + " txId : " + txId + " pageNo : " + pageNo
                + " pageSize : " + pageSize + "\n";
    }

    private static final Map<String, FieldProperty> fieldPropertyMap;

    static {
        fieldPropertyMap = new LinkedHashMap<>();
        putString("keyword");
        putString("txId");
        putString("txName");
        putString("appId");
        putString("httpMethod");
        putInt("pageNo");
        putInt("pageSize");
    }

    private static void putString(String name) {
        fieldPropertyMap.put(name, FieldProperty.builder()
                .setPhysicalName(name).setLogicalName(name)
                .setType(FieldProperty.TYPE_OBJECT_STRING).setDecimal(-1)
                .setIsNullable(true).setIsEncrypt(false).build());
    }

    private static void putInt(String name) {
        fieldPropertyMap.put(name, FieldProperty.builder()
                .setPhysicalName(name).setLogicalName(name)
                .setType(FieldProperty.TYPE_OBJECT_INT).setDecimal(-1)
                .setIsNullable(true).setIsEncrypt(false).build());
    }

    public Map<String, FieldProperty> getFieldPropertyMap() {
        return Collections.unmodifiableMap(fieldPropertyMap);
    }
}

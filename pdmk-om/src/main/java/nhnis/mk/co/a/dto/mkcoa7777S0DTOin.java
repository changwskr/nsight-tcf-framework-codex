package nhnis.mk.co.a.dto;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.ims.superspring.dto.DataObject;
import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;

/**
 * 이미지로그 조회 입력 (mkcoa7777S0).
 */
public class mkcoa7777S0DTOin extends DataObject {

    private static final long serialVersionUID = 1L;

    private String guid;
    private String serviceId;
    private String screenId;
    private String optrEno;
    private Boolean exceptionOnly;
    /** 현재시각 기준 N초 이내 요청만 (문제 거래 추적). null/0이면 미적용 */
    private Integer withinSeconds;
    private Integer pageNo;
    private Integer pageSize;

    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public String getScreenId() {
        return screenId;
    }

    public void setScreenId(String screenId) {
        this.screenId = screenId;
    }

    public String getOptrEno() {
        return optrEno;
    }

    public void setOptrEno(String optrEno) {
        this.optrEno = optrEno;
    }

    public Boolean getExceptionOnly() {
        return exceptionOnly;
    }

    public void setExceptionOnly(Boolean exceptionOnly) {
        this.exceptionOnly = exceptionOnly;
    }

    public Integer getWithinSeconds() {
        return withinSeconds;
    }

    public void setWithinSeconds(Integer withinSeconds) {
        this.withinSeconds = withinSeconds;
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
        mkcoa7777S0DTOin copy = new mkcoa7777S0DTOin();
        copy.clone(this);
        return copy;
    }

    public void clone(DataObject src) {
        if (this == src) {
            return;
        }
        mkcoa7777S0DTOin in = (mkcoa7777S0DTOin) src;
        this.guid = in.guid;
        this.serviceId = in.serviceId;
        this.screenId = in.screenId;
        this.optrEno = in.optrEno;
        this.exceptionOnly = in.exceptionOnly;
        this.withinSeconds = in.withinSeconds;
        this.pageNo = in.pageNo;
        this.pageSize = in.pageSize;
    }

    @Override
    public String toString() {
        return "guid : " + guid + "\n"
                + "serviceId : " + serviceId + "\n"
                + "screenId : " + screenId + "\n"
                + "optrEno : " + optrEno + "\n"
                + "exceptionOnly : " + exceptionOnly + "\n"
                + "withinSeconds : " + withinSeconds + "\n"
                + "pageNo : " + pageNo + "\n"
                + "pageSize : " + pageSize + "\n";
    }

    private static final Map<String, FieldProperty> fieldPropertyMap;

    static {
        fieldPropertyMap = new LinkedHashMap<>();
        fieldPropertyMap.put("guid", FieldProperty.builder()
                .setPhysicalName("guid").setLogicalName("guid")
                .setType(FieldProperty.TYPE_OBJECT_STRING).setDecimal(-1)
                .setIsNullable(true).setIsEncrypt(false).build());
        fieldPropertyMap.put("serviceId", FieldProperty.builder()
                .setPhysicalName("serviceId").setLogicalName("serviceId")
                .setType(FieldProperty.TYPE_OBJECT_STRING).setDecimal(-1)
                .setIsNullable(true).setIsEncrypt(false).build());
        fieldPropertyMap.put("screenId", FieldProperty.builder()
                .setPhysicalName("screenId").setLogicalName("screenId")
                .setType(FieldProperty.TYPE_OBJECT_STRING).setDecimal(-1)
                .setIsNullable(true).setIsEncrypt(false).build());
        fieldPropertyMap.put("optrEno", FieldProperty.builder()
                .setPhysicalName("optrEno").setLogicalName("optrEno")
                .setType(FieldProperty.TYPE_OBJECT_STRING).setDecimal(-1)
                .setIsNullable(true).setIsEncrypt(false).build());
        fieldPropertyMap.put("exceptionOnly", FieldProperty.builder()
                .setPhysicalName("exceptionOnly").setLogicalName("exceptionOnly")
                .setType(FieldProperty.TYPE_OBJECT_INT).setDecimal(-1)
                .setIsNullable(true).setIsEncrypt(false).build());
        fieldPropertyMap.put("withinSeconds", FieldProperty.builder()
                .setPhysicalName("withinSeconds").setLogicalName("withinSeconds")
                .setType(FieldProperty.TYPE_OBJECT_INT).setDecimal(-1)
                .setIsNullable(true).setIsEncrypt(false).build());
        fieldPropertyMap.put("pageNo", FieldProperty.builder()
                .setPhysicalName("pageNo").setLogicalName("pageNo")
                .setType(FieldProperty.TYPE_OBJECT_INT).setDecimal(-1)
                .setIsNullable(true).setIsEncrypt(false).build());
        fieldPropertyMap.put("pageSize", FieldProperty.builder()
                .setPhysicalName("pageSize").setLogicalName("pageSize")
                .setType(FieldProperty.TYPE_OBJECT_INT).setDecimal(-1)
                .setIsNullable(true).setIsEncrypt(false).build());
    }

    public Map<String, FieldProperty> getFieldPropertyMap() {
        return Collections.unmodifiableMap(fieldPropertyMap);
    }
}

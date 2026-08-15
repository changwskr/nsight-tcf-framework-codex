package nhnis.infra.in.a.dto;

import java.util.*;
import com.ims.superspring.dto.DataObject;
import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;

public class ifina5200C0DTOin extends DataObject {
    private static final long serialVersionUID = 1L;
    private String interfaceId, fromAppId, toAppId, toExternalName, protocolCd, directionCd, criticalYn, remark;
    public String getInterfaceId(){return interfaceId;} public void setInterfaceId(String v){interfaceId=v;}
    public String getFromAppId(){return fromAppId;} public void setFromAppId(String v){fromAppId=v;}
    public String getToAppId(){return toAppId;} public void setToAppId(String v){toAppId=v;}
    public String getToExternalName(){return toExternalName;} public void setToExternalName(String v){toExternalName=v;}
    public String getProtocolCd(){return protocolCd;} public void setProtocolCd(String v){protocolCd=v;}
    public String getDirectionCd(){return directionCd;} public void setDirectionCd(String v){directionCd=v;}
    public String getCriticalYn(){return criticalYn;} public void setCriticalYn(String v){criticalYn=v;}
    public String getRemark(){return remark;} public void setRemark(String v){remark=v;}
    @Override public Object clone(){ ifina5200C0DTOin c=new ifina5200C0DTOin(); c.clone(this); return c; }
    public void clone(DataObject src){ if(this==src)return; ifina5200C0DTOin in=(ifina5200C0DTOin)src;
      interfaceId=in.interfaceId; fromAppId=in.fromAppId; toAppId=in.toAppId; toExternalName=in.toExternalName;
      protocolCd=in.protocolCd; directionCd=in.directionCd; criticalYn=in.criticalYn; remark=in.remark; }
    private static final Map<String, FieldProperty> fieldPropertyMap = new LinkedHashMap<>();
    static { fieldPropertyMap.put("interfaceId", FieldProperty.builder().setPhysicalName("interfaceId").setLogicalName("interfaceId").setType(FieldProperty.TYPE_OBJECT_STRING).setDecimal(-1).setIsNullable(true).setIsEncrypt(false).build()); }
    public Map<String, FieldProperty> getFieldPropertyMap(){ return Collections.unmodifiableMap(fieldPropertyMap); }
}

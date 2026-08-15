package nhnis.infra.in.a.dto;

import java.util.*;
import com.ims.superspring.dto.DataObject;
import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;

public class ifina2300C0DTOin extends DataObject {
    private static final long serialVersionUID = 1L;
    private String mapId, appId, mapTypeCd, refId, roleCd, remark, regUserId;
    public String getMapId(){return mapId;} public void setMapId(String v){mapId=v;}
    public String getAppId(){return appId;} public void setAppId(String v){appId=v;}
    public String getMapTypeCd(){return mapTypeCd;} public void setMapTypeCd(String v){mapTypeCd=v;}
    public String getRefId(){return refId;} public void setRefId(String v){refId=v;}
    public String getRoleCd(){return roleCd;} public void setRoleCd(String v){roleCd=v;}
    public String getRemark(){return remark;} public void setRemark(String v){remark=v;}
    public String getRegUserId(){return regUserId;} public void setRegUserId(String v){regUserId=v;}
    @Override public Object clone(){ ifina2300C0DTOin c=new ifina2300C0DTOin(); c.clone(this); return c; }
    public void clone(DataObject src){ if(this==src)return; ifina2300C0DTOin in=(ifina2300C0DTOin)src;
      mapId=in.mapId; appId=in.appId; mapTypeCd=in.mapTypeCd; refId=in.refId; roleCd=in.roleCd;
      remark=in.remark; regUserId=in.regUserId; }
    private static final Map<String, FieldProperty> fieldPropertyMap = new LinkedHashMap<>();
    static { fieldPropertyMap.put("mapId", FieldProperty.builder().setPhysicalName("mapId").setLogicalName("mapId").setType(FieldProperty.TYPE_OBJECT_STRING).setDecimal(-1).setIsNullable(true).setIsEncrypt(false).build()); }
    public Map<String, FieldProperty> getFieldPropertyMap(){ return Collections.unmodifiableMap(fieldPropertyMap); }
}

package nhnis.infra.in.a.dto;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import com.ims.superspring.dto.DataObject;
import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;

public class ifina5300C0DTOin extends DataObject {
    private static final long serialVersionUID = 1L;
    private String relationId, fromTypeCd, fromId, toTypeCd, toId, relationTypeCd, criticalYn, remark, regUserId;
    public String getRelationId(){return relationId;} public void setRelationId(String v){relationId=v;}
    public String getFromTypeCd(){return fromTypeCd;} public void setFromTypeCd(String v){fromTypeCd=v;}
    public String getFromId(){return fromId;} public void setFromId(String v){fromId=v;}
    public String getToTypeCd(){return toTypeCd;} public void setToTypeCd(String v){toTypeCd=v;}
    public String getToId(){return toId;} public void setToId(String v){toId=v;}
    public String getRelationTypeCd(){return relationTypeCd;} public void setRelationTypeCd(String v){relationTypeCd=v;}
    public String getCriticalYn(){return criticalYn;} public void setCriticalYn(String v){criticalYn=v;}
    public String getRemark(){return remark;} public void setRemark(String v){remark=v;}
    public String getRegUserId(){return regUserId;} public void setRegUserId(String v){regUserId=v;}
    @Override public Object clone(){ ifina5300C0DTOin c=new ifina5300C0DTOin(); c.clone(this); return c; }
    public void clone(DataObject src){ if(this==src)return; ifina5300C0DTOin in=(ifina5300C0DTOin)src;
      relationId=in.relationId; fromTypeCd=in.fromTypeCd; fromId=in.fromId; toTypeCd=in.toTypeCd; toId=in.toId;
      relationTypeCd=in.relationTypeCd; criticalYn=in.criticalYn; remark=in.remark; regUserId=in.regUserId; }
    private static final Map<String, FieldProperty> fieldPropertyMap = new LinkedHashMap<>();
    static { fieldPropertyMap.put("relationId", FieldProperty.builder().setPhysicalName("relationId").setLogicalName("relationId").setType(FieldProperty.TYPE_OBJECT_STRING).setDecimal(-1).setIsNullable(true).setIsEncrypt(false).build()); }
    public Map<String, FieldProperty> getFieldPropertyMap(){ return Collections.unmodifiableMap(fieldPropertyMap); }
}

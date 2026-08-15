package nhnis.infra.in.a.dto;

import java.util.*;
import com.ims.superspring.dto.DataObject;
import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;

public class ifina9200S0DTOin extends DataObject {
    private static final long serialVersionUID = 1L;
    private String targetTypeCd, targetId, gateId;
    public String getTargetTypeCd(){return targetTypeCd;} public void setTargetTypeCd(String v){targetTypeCd=v;}
    public String getTargetId(){return targetId;} public void setTargetId(String v){targetId=v;}
    public String getGateId(){return gateId;} public void setGateId(String v){gateId=v;}
    @Override public Object clone(){ ifina9200S0DTOin c=new ifina9200S0DTOin(); c.clone(this); return c; }
    public void clone(DataObject src){ if(this==src)return; ifina9200S0DTOin in=(ifina9200S0DTOin)src;
      targetTypeCd=in.targetTypeCd; targetId=in.targetId; gateId=in.gateId; }
    private static final Map<String, FieldProperty> fieldPropertyMap = new LinkedHashMap<>();
    static { fieldPropertyMap.put("targetId", FieldProperty.builder().setPhysicalName("targetId").setLogicalName("targetId").setType(FieldProperty.TYPE_OBJECT_STRING).setDecimal(-1).setIsNullable(true).setIsEncrypt(false).build()); }
    public Map<String, FieldProperty> getFieldPropertyMap(){ return Collections.unmodifiableMap(fieldPropertyMap); }
}

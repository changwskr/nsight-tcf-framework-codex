package nhnis.infra.in.a.dto;

import java.util.*;
import com.ims.superspring.dto.DataObject;
import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;

public class ifina8200S0DTOin extends DataObject {
    private static final long serialVersionUID = 1L;
    private String waveId;
    public String getWaveId(){return waveId;} public void setWaveId(String v){waveId=v;}
    @Override public Object clone(){ ifina8200S0DTOin c=new ifina8200S0DTOin(); c.clone(this); return c; }
    public void clone(DataObject src){ if(this==src)return; ifina8200S0DTOin in=(ifina8200S0DTOin)src; waveId=in.waveId; }
    private static final Map<String, FieldProperty> fieldPropertyMap = new LinkedHashMap<>();
    static { fieldPropertyMap.put("waveId", FieldProperty.builder().setPhysicalName("waveId").setLogicalName("waveId").setType(FieldProperty.TYPE_OBJECT_STRING).setDecimal(-1).setIsNullable(true).setIsEncrypt(false).build()); }
    public Map<String, FieldProperty> getFieldPropertyMap(){ return Collections.unmodifiableMap(fieldPropertyMap); }
}

package nhnis.infra.in.a.dto;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.ims.superspring.dto.DataObject;
import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;

public class ifina0100S0DTOin extends DataObject {
    private static final long serialVersionUID = 1L;
    private Integer recentLimit;
    public Integer getRecentLimit(){return recentLimit;} public void setRecentLimit(Integer v){recentLimit=v;}
    @Override public Object clone(){ ifina0100S0DTOin c=new ifina0100S0DTOin(); c.clone(this); return c; }
    public void clone(DataObject src){ if(this==src)return; ifina0100S0DTOin in=(ifina0100S0DTOin)src; recentLimit=in.recentLimit; }
    private static final Map<String, FieldProperty> fieldPropertyMap = new LinkedHashMap<>();
    static { fieldPropertyMap.put("recentLimit", FieldProperty.builder().setPhysicalName("recentLimit").setLogicalName("recentLimit").setType(FieldProperty.TYPE_OBJECT_INT).setDecimal(-1).setIsNullable(true).setIsEncrypt(false).build()); }
    public Map<String, FieldProperty> getFieldPropertyMap(){ return Collections.unmodifiableMap(fieldPropertyMap); }
}

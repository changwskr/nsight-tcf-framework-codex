package nhnis.infra.in.a.dto;

import java.util.*;
import com.ims.superspring.dto.DataObject;
import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;

public class ifina9400S0DTOin extends DataObject {
    private static final long serialVersionUID = 1L;
    private Integer tableId;
    public Integer getTableId(){return tableId;} public void setTableId(Integer v){tableId=v;}
    @Override public Object clone(){ ifina9400S0DTOin c=new ifina9400S0DTOin(); c.clone(this); return c; }
    public void clone(DataObject src){ if(this==src)return; ifina9400S0DTOin in=(ifina9400S0DTOin)src; tableId=in.tableId; }
    private static final Map<String, FieldProperty> fieldPropertyMap = new LinkedHashMap<>();
    static { fieldPropertyMap.put("tableId", FieldProperty.builder().setPhysicalName("tableId").setLogicalName("tableId").setType(FieldProperty.TYPE_PRIMITIVE_INT).setDecimal(-1).setIsNullable(true).setIsEncrypt(false).build()); }
    public Map<String, FieldProperty> getFieldPropertyMap(){ return Collections.unmodifiableMap(fieldPropertyMap); }
}

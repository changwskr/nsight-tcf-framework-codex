package nhnis.infra.in.a.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.ims.superspring.dto.DataObject;
import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;

public class ifina5300D0DTOin extends DataObject {
    private static final long serialVersionUID = 1L;
    private List<String> relationIdList = new ArrayList<>();
    public List<String> getRelationIdList(){return relationIdList;} public void setRelationIdList(List<String> v){relationIdList=v!=null?v:new ArrayList<>();}
    @Override public Object clone(){ ifina5300D0DTOin c=new ifina5300D0DTOin(); c.clone(this); return c; }
    public void clone(DataObject src){ if(this==src)return; ifina5300D0DTOin in=(ifina5300D0DTOin)src;
      relationIdList=in.relationIdList==null?new ArrayList<>():new ArrayList<>(in.relationIdList); }
    private static final Map<String, FieldProperty> fieldPropertyMap = new LinkedHashMap<>();
    static { fieldPropertyMap.put("relationIdList", FieldProperty.builder().setPhysicalName("relationIdList").setLogicalName("relationIdList").setType(FieldProperty.TYPE_OBJECT_STRING).setDecimal(-1).setIsNullable(true).setIsEncrypt(false).build()); }
    public Map<String, FieldProperty> getFieldPropertyMap(){ return Collections.unmodifiableMap(fieldPropertyMap); }
}

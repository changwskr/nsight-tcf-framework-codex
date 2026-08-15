package nhnis.infra.in.a.dto;

import java.util.*;
import com.ims.superspring.dto.DataObject;
import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;

public class ifina2300D0DTOin extends DataObject {
    private static final long serialVersionUID = 1L;
    private List<String> mapIdList = new ArrayList<>();
    public List<String> getMapIdList(){return mapIdList;} public void setMapIdList(List<String> v){mapIdList=v!=null?v:new ArrayList<>();}
    @Override public Object clone(){ ifina2300D0DTOin c=new ifina2300D0DTOin(); c.clone(this); return c; }
    public void clone(DataObject src){ if(this==src)return; ifina2300D0DTOin in=(ifina2300D0DTOin)src;
      mapIdList=in.mapIdList==null?new ArrayList<>():new ArrayList<>(in.mapIdList); }
    private static final Map<String, FieldProperty> fieldPropertyMap = new LinkedHashMap<>();
    static { fieldPropertyMap.put("mapIdList", FieldProperty.builder().setPhysicalName("mapIdList").setLogicalName("mapIdList").setType(FieldProperty.TYPE_OBJECT_STRING).setDecimal(-1).setIsNullable(true).setIsEncrypt(false).build()); }
    public Map<String, FieldProperty> getFieldPropertyMap(){ return Collections.unmodifiableMap(fieldPropertyMap); }
}

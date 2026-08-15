package nhnis.infra.in.a.dto;

import java.util.*;
import com.ims.superspring.dto.DataObject;
import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;

public class ifina8100D0DTOin extends DataObject {
    private static final long serialVersionUID = 1L;
    private List<String> planIdList = new ArrayList<>();
    public List<String> getPlanIdList(){return planIdList;} public void setPlanIdList(List<String> v){planIdList=v!=null?v:new ArrayList<>();}
    @Override public Object clone(){ ifina8100D0DTOin c=new ifina8100D0DTOin(); c.clone(this); return c; }
    public void clone(DataObject src){ if(this==src)return; ifina8100D0DTOin in=(ifina8100D0DTOin)src;
      planIdList=in.planIdList==null?new ArrayList<>():new ArrayList<>(in.planIdList); }
    private static final Map<String, FieldProperty> fieldPropertyMap = new LinkedHashMap<>();
    static { fieldPropertyMap.put("planIdList", FieldProperty.builder().setPhysicalName("planIdList").setLogicalName("planIdList").setType(FieldProperty.TYPE_OBJECT_STRING).setDecimal(-1).setIsNullable(true).setIsEncrypt(false).build()); }
    public Map<String, FieldProperty> getFieldPropertyMap(){ return Collections.unmodifiableMap(fieldPropertyMap); }
}

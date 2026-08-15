package nhnis.infra.in.a.dto;

import java.util.*;
import com.ims.superspring.dto.DataObject;
import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;

public class ifina5200D0DTOin extends DataObject {
    private static final long serialVersionUID = 1L;
    private List<String> interfaceIdList = new ArrayList<>();
    public List<String> getInterfaceIdList(){return interfaceIdList;} public void setInterfaceIdList(List<String> v){interfaceIdList=v!=null?v:new ArrayList<>();}
    @Override public Object clone(){ ifina5200D0DTOin c=new ifina5200D0DTOin(); c.clone(this); return c; }
    public void clone(DataObject src){ if(this==src)return; ifina5200D0DTOin in=(ifina5200D0DTOin)src;
      interfaceIdList=in.interfaceIdList==null?new ArrayList<>():new ArrayList<>(in.interfaceIdList); }
    private static final Map<String, FieldProperty> fieldPropertyMap = new LinkedHashMap<>();
    static { fieldPropertyMap.put("interfaceIdList", FieldProperty.builder().setPhysicalName("interfaceIdList").setLogicalName("interfaceIdList").setType(FieldProperty.TYPE_ABSTRACT_INCLUDE).setDecimal(-1).setIsNullable(true).setIsEncrypt(false).build()); }
    public Map<String, FieldProperty> getFieldPropertyMap(){ return Collections.unmodifiableMap(fieldPropertyMap); }
}

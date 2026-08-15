package nhnis.infra.in.a.dto;

import java.util.*;
import com.ims.superspring.dto.DataObject;
import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;

public class ifina7200U0DTOin extends DataObject {
    private static final long serialVersionUID = 1L;
    private String licenseId;
    private List<Map<String, Object>> allocations = new ArrayList<>();
    public String getLicenseId(){return licenseId;} public void setLicenseId(String v){licenseId=v;}
    public List<Map<String, Object>> getAllocations(){return allocations;} public void setAllocations(List<Map<String, Object>> v){allocations=v!=null?v:new ArrayList<>();}
    @Override public Object clone(){ ifina7200U0DTOin c=new ifina7200U0DTOin(); c.clone(this); return c; }
    public void clone(DataObject src){ if(this==src)return; ifina7200U0DTOin in=(ifina7200U0DTOin)src;
      licenseId=in.licenseId; allocations=in.allocations==null?new ArrayList<>():new ArrayList<>(in.allocations); }
    private static final Map<String, FieldProperty> fieldPropertyMap = new LinkedHashMap<>();
    static { fieldPropertyMap.put("licenseId", FieldProperty.builder().setPhysicalName("licenseId").setLogicalName("licenseId").setType(FieldProperty.TYPE_OBJECT_STRING).setDecimal(-1).setIsNullable(true).setIsEncrypt(false).build()); }
    public Map<String, FieldProperty> getFieldPropertyMap(){ return Collections.unmodifiableMap(fieldPropertyMap); }
}

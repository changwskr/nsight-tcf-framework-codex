package nhnis.infra.in.a.dto;

import java.util.*;
import com.ims.superspring.dto.DataObject;
import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;

public class ifina7100D0DTOin extends DataObject {
    private static final long serialVersionUID = 1L;
    private List<String> licenseIdList = new ArrayList<>();
    public List<String> getLicenseIdList(){return licenseIdList;} public void setLicenseIdList(List<String> v){licenseIdList=v!=null?v:new ArrayList<>();}
    @Override public Object clone(){ ifina7100D0DTOin c=new ifina7100D0DTOin(); c.clone(this); return c; }
    public void clone(DataObject src){ if(this==src)return; ifina7100D0DTOin in=(ifina7100D0DTOin)src;
      licenseIdList=in.licenseIdList==null?new ArrayList<>():new ArrayList<>(in.licenseIdList); }
    private static final Map<String, FieldProperty> fieldPropertyMap = new LinkedHashMap<>();
    static { fieldPropertyMap.put("licenseIdList", FieldProperty.builder().setPhysicalName("licenseIdList").setLogicalName("licenseIdList").setType(FieldProperty.TYPE_OBJECT_STRING).setDecimal(-1).setIsNullable(true).setIsEncrypt(false).build()); }
    public Map<String, FieldProperty> getFieldPropertyMap(){ return Collections.unmodifiableMap(fieldPropertyMap); }
}

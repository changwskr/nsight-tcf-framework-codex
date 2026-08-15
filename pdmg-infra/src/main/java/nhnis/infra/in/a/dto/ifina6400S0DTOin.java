package nhnis.infra.in.a.dto;

import java.util.*;
import com.ims.superspring.dto.DataObject;
import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;

public class ifina6400S0DTOin extends DataObject {
    private static final long serialVersionUID = 1L;
    private String targetTypeCd, metricScopeCd;
    private List<String> targetIdList = new ArrayList<>();
    public String getTargetTypeCd(){return targetTypeCd;} public void setTargetTypeCd(String v){targetTypeCd=v;}
    public String getMetricScopeCd(){return metricScopeCd;} public void setMetricScopeCd(String v){metricScopeCd=v;}
    public List<String> getTargetIdList(){return targetIdList;} public void setTargetIdList(List<String> v){targetIdList=v!=null?v:new ArrayList<>();}
    @Override public Object clone(){ ifina6400S0DTOin c=new ifina6400S0DTOin(); c.clone(this); return c; }
    public void clone(DataObject src){ if(this==src)return; ifina6400S0DTOin in=(ifina6400S0DTOin)src;
      targetTypeCd=in.targetTypeCd; metricScopeCd=in.metricScopeCd;
      targetIdList=in.targetIdList==null?new ArrayList<>():new ArrayList<>(in.targetIdList); }
    private static final Map<String, FieldProperty> fieldPropertyMap = new LinkedHashMap<>();
    static { fieldPropertyMap.put("metricScopeCd", FieldProperty.builder().setPhysicalName("metricScopeCd").setLogicalName("metricScopeCd").setType(FieldProperty.TYPE_OBJECT_STRING).setDecimal(-1).setIsNullable(true).setIsEncrypt(false).build()); }
    public Map<String, FieldProperty> getFieldPropertyMap(){ return Collections.unmodifiableMap(fieldPropertyMap); }
}

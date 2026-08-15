package nhnis.infra.in.a.dto;

import java.util.*;
import com.ims.superspring.dto.DataObject;
import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;

public class ifina6400S0DTOout extends DataObject {
    private static final long serialVersionUID = 1L;
    private String targetTypeCd, metricScopeCd, RSLT_CD, RSLT_MSG;
    private List<String> targetIds = new ArrayList<>();
    private List<Map<String, Object>> metrics = new ArrayList<>();
    private List<Map<String, Object>> rows = new ArrayList<>();
    public String getTargetTypeCd(){return targetTypeCd;} public void setTargetTypeCd(String v){targetTypeCd=v;}
    public String getMetricScopeCd(){return metricScopeCd;} public void setMetricScopeCd(String v){metricScopeCd=v;}
    public List<String> getTargetIds(){return targetIds;} public void setTargetIds(List<String> v){targetIds=v!=null?v:new ArrayList<>();}
    public List<Map<String, Object>> getMetrics(){return metrics;} public void setMetrics(List<Map<String, Object>> v){metrics=v!=null?v:new ArrayList<>();}
    public List<Map<String, Object>> getRows(){return rows;} public void setRows(List<Map<String, Object>> v){rows=v!=null?v:new ArrayList<>();}
    public String getRSLT_CD(){return RSLT_CD;} public void setRSLT_CD(String v){RSLT_CD=v;}
    public String getRSLT_MSG(){return RSLT_MSG;} public void setRSLT_MSG(String v){RSLT_MSG=v;}
    @Override public Object clone(){ ifina6400S0DTOout c=new ifina6400S0DTOout(); c.clone(this); return c; }
    public void clone(DataObject src){ if(this==src)return; ifina6400S0DTOout in=(ifina6400S0DTOout)src;
      targetTypeCd=in.targetTypeCd; metricScopeCd=in.metricScopeCd;
      targetIds=in.targetIds==null?new ArrayList<>():new ArrayList<>(in.targetIds);
      metrics=in.metrics==null?new ArrayList<>():new ArrayList<>(in.metrics);
      rows=in.rows==null?new ArrayList<>():new ArrayList<>(in.rows);
      RSLT_CD=in.RSLT_CD; RSLT_MSG=in.RSLT_MSG; }
    private static final Map<String, FieldProperty> fieldPropertyMap = new LinkedHashMap<>();
    static { fieldPropertyMap.put("RSLT_CD", FieldProperty.builder().setPhysicalName("RSLT_CD").setLogicalName("RSLT_CD").setType(FieldProperty.TYPE_OBJECT_STRING).setDecimal(-1).setIsNullable(true).setIsEncrypt(false).build()); }
    public Map<String, FieldProperty> getFieldPropertyMap(){ return Collections.unmodifiableMap(fieldPropertyMap); }
}

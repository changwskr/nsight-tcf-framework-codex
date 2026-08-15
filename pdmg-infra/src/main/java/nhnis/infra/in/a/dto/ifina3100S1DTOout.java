package nhnis.infra.in.a.dto;
import java.util.ArrayList; import java.util.Collections; import java.util.LinkedHashMap; import java.util.List; import java.util.Map;
import com.ims.superspring.dto.DataObject; import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;
public class ifina3100S1DTOout extends DataObject {
    private static final long serialVersionUID = 1L;
    private ifina3100S0DTOSub0 base;
    private List<Map<String, Object>> endpoints = new ArrayList<>();
    private List<Map<String, Object>> attrs = new ArrayList<>();
    private int endpointCount; private int mwCount; private int dbCount; private int attrCount;
    private List<String> warnings = new ArrayList<>();
    private String RSLT_CD, RSLT_MSG;
    public ifina3100S0DTOSub0 getBase(){return base;} public void setBase(ifina3100S0DTOSub0 v){base=v;}
    public List<Map<String, Object>> getEndpoints(){return endpoints;} public void setEndpoints(List<Map<String, Object>> v){endpoints=v!=null?v:new ArrayList<>();}
    public List<Map<String, Object>> getAttrs(){return attrs;} public void setAttrs(List<Map<String, Object>> v){attrs=v!=null?v:new ArrayList<>();}
    public int getEndpointCount(){return endpointCount;} public void setEndpointCount(int v){endpointCount=v;}
    public int getMwCount(){return mwCount;} public void setMwCount(int v){mwCount=v;}
    public int getDbCount(){return dbCount;} public void setDbCount(int v){dbCount=v;}
    public int getAttrCount(){return attrCount;} public void setAttrCount(int v){attrCount=v;}
    public List<String> getWarnings(){return warnings;} public void setWarnings(List<String> v){warnings=v!=null?v:new ArrayList<>();}
    public String getRSLT_CD(){return RSLT_CD;} public void setRSLT_CD(String v){RSLT_CD=v;}
    public String getRSLT_MSG(){return RSLT_MSG;} public void setRSLT_MSG(String v){RSLT_MSG=v;}
    @Override public Object clone(){ ifina3100S1DTOout c=new ifina3100S1DTOout(); c.clone(this); return c; }
    public void clone(DataObject src){ if(this==src)return; ifina3100S1DTOout in=(ifina3100S1DTOout)src;
      base=in.base==null?null:(ifina3100S0DTOSub0)in.base.clone();
      endpoints=in.endpoints==null?new ArrayList<>():new ArrayList<>(in.endpoints);
      attrs=in.attrs==null?new ArrayList<>():new ArrayList<>(in.attrs);
      warnings=in.warnings==null?new ArrayList<>():new ArrayList<>(in.warnings);
      endpointCount=in.endpointCount; mwCount=in.mwCount; dbCount=in.dbCount; attrCount=in.attrCount;
      RSLT_CD=in.RSLT_CD; RSLT_MSG=in.RSLT_MSG; }
    private static final Map<String, FieldProperty> fieldPropertyMap = new LinkedHashMap<>();
    static {
      fieldPropertyMap.put("RSLT_CD", FieldProperty.builder().setPhysicalName("RSLT_CD").setLogicalName("RSLT_CD").setType(FieldProperty.TYPE_OBJECT_STRING).setDecimal(-1).setIsNullable(true).setIsEncrypt(false).build());
    }
    public Map<String, FieldProperty> getFieldPropertyMap(){ return Collections.unmodifiableMap(fieldPropertyMap); }
}

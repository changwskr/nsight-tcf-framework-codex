package nhnis.infra.in.a.dto;

import java.util.*;
import com.ims.superspring.dto.DataObject;
import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;

public class ifina8200S0DTOout extends DataObject {
    private static final long serialVersionUID = 1L;
    private List<Map<String, Object>> rows = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();
    private String RSLT_CD, RSLT_MSG;
    public List<Map<String, Object>> getRows(){return rows;} public void setRows(List<Map<String, Object>> v){rows=v!=null?v:new ArrayList<>();}
    public List<String> getWarnings(){return warnings;} public void setWarnings(List<String> v){warnings=v!=null?v:new ArrayList<>();}
    public String getRSLT_CD(){return RSLT_CD;} public void setRSLT_CD(String v){RSLT_CD=v;}
    public String getRSLT_MSG(){return RSLT_MSG;} public void setRSLT_MSG(String v){RSLT_MSG=v;}
    @Override public Object clone(){ ifina8200S0DTOout c=new ifina8200S0DTOout(); c.clone(this); return c; }
    public void clone(DataObject src){ if(this==src)return; ifina8200S0DTOout in=(ifina8200S0DTOout)src;
      rows=in.rows==null?new ArrayList<>():new ArrayList<>(in.rows); warnings=new ArrayList<>(in.warnings);
      RSLT_CD=in.RSLT_CD; RSLT_MSG=in.RSLT_MSG; }
    private static final Map<String, FieldProperty> fieldPropertyMap = new LinkedHashMap<>();
    static { fieldPropertyMap.put("RSLT_CD", FieldProperty.builder().setPhysicalName("RSLT_CD").setLogicalName("RSLT_CD").setType(FieldProperty.TYPE_OBJECT_STRING).setDecimal(-1).setIsNullable(true).setIsEncrypt(false).build()); }
    public Map<String, FieldProperty> getFieldPropertyMap(){ return Collections.unmodifiableMap(fieldPropertyMap); }
}

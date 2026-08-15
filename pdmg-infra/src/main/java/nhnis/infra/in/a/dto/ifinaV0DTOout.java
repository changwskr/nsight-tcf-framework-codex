package nhnis.infra.in.a.dto;

import java.util.*;
import com.ims.superspring.dto.DataObject;
import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;

/** 공통 V0 검증 응답. */
public class ifinaV0DTOout extends DataObject {
    private static final long serialVersionUID = 1L;
    private List<Map<String, Object>> violations = new ArrayList<>();
    private int warnCount, errorCount;
    private String RSLT_CD, RSLT_MSG;
    public List<Map<String, Object>> getViolations(){return violations;}
    public void setViolations(List<Map<String, Object>> v){violations=v!=null?v:new ArrayList<>();}
    public int getWarnCount(){return warnCount;} public void setWarnCount(int v){warnCount=v;}
    public int getErrorCount(){return errorCount;} public void setErrorCount(int v){errorCount=v;}
    public String getRSLT_CD(){return RSLT_CD;} public void setRSLT_CD(String v){RSLT_CD=v;}
    public String getRSLT_MSG(){return RSLT_MSG;} public void setRSLT_MSG(String v){RSLT_MSG=v;}
    @Override public Object clone(){ ifinaV0DTOout c=new ifinaV0DTOout(); c.clone(this); return c; }
    public void clone(DataObject src){ if(this==src)return; ifinaV0DTOout in=(ifinaV0DTOout)src;
      violations=in.violations==null?new ArrayList<>():new ArrayList<>(in.violations);
      warnCount=in.warnCount; errorCount=in.errorCount; RSLT_CD=in.RSLT_CD; RSLT_MSG=in.RSLT_MSG; }
    private static final Map<String, FieldProperty> fieldPropertyMap = new LinkedHashMap<>();
    static { fieldPropertyMap.put("RSLT_CD", FieldProperty.builder().setPhysicalName("RSLT_CD").setLogicalName("RSLT_CD").setType(FieldProperty.TYPE_OBJECT_STRING).setDecimal(-1).setIsNullable(true).setIsEncrypt(false).build()); }
    public Map<String, FieldProperty> getFieldPropertyMap(){ return Collections.unmodifiableMap(fieldPropertyMap); }
}

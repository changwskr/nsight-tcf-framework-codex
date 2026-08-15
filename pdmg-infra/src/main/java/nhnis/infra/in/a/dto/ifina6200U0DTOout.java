package nhnis.infra.in.a.dto;

import java.util.*;
import com.ims.superspring.dto.DataObject;
import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;

public class ifina6200U0DTOout extends DataObject {
    private static final long serialVersionUID = 1L;
    private int PROC_CNT; private String RSLT_CD, RSLT_MSG; private List<String> warnings = new ArrayList<>();
    public int getPROC_CNT(){return PROC_CNT;} public void setPROC_CNT(int v){PROC_CNT=v;}
    public String getRSLT_CD(){return RSLT_CD;} public void setRSLT_CD(String v){RSLT_CD=v;}
    public String getRSLT_MSG(){return RSLT_MSG;} public void setRSLT_MSG(String v){RSLT_MSG=v;}
    public List<String> getWarnings(){return warnings;} public void setWarnings(List<String> v){warnings=v!=null?v:new ArrayList<>();}
    @Override public Object clone(){ ifina6200U0DTOout c=new ifina6200U0DTOout(); c.clone(this); return c; }
    public void clone(DataObject src){ if(this==src)return; ifina6200U0DTOout in=(ifina6200U0DTOout)src;
      PROC_CNT=in.PROC_CNT; RSLT_CD=in.RSLT_CD; RSLT_MSG=in.RSLT_MSG; warnings=new ArrayList<>(in.warnings); }
    private static final Map<String, FieldProperty> fieldPropertyMap = new LinkedHashMap<>();
    static { fieldPropertyMap.put("RSLT_CD", FieldProperty.builder().setPhysicalName("RSLT_CD").setLogicalName("RSLT_CD").setType(FieldProperty.TYPE_OBJECT_STRING).setDecimal(-1).setIsNullable(true).setIsEncrypt(false).build()); }
    public Map<String, FieldProperty> getFieldPropertyMap(){ return Collections.unmodifiableMap(fieldPropertyMap); }
}

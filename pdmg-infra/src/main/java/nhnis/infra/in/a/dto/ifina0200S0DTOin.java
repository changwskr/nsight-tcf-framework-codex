package nhnis.infra.in.a.dto;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import com.ims.superspring.dto.DataObject;
import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;

public class ifina0200S0DTOin extends DataObject {
    private static final long serialVersionUID = 1L;
    private String keyword, severityCd, riskType; private Integer maxDaysLeft;
    public String getKeyword(){return keyword;} public void setKeyword(String v){keyword=v;}
    public String getSeverityCd(){return severityCd;} public void setSeverityCd(String v){severityCd=v;}
    public String getRiskType(){return riskType;} public void setRiskType(String v){riskType=v;}
    public Integer getMaxDaysLeft(){return maxDaysLeft;} public void setMaxDaysLeft(Integer v){maxDaysLeft=v;}
    @Override public Object clone(){ ifina0200S0DTOin c=new ifina0200S0DTOin(); c.clone(this); return c; }
    public void clone(DataObject src){ if(this==src)return; ifina0200S0DTOin in=(ifina0200S0DTOin)src;
      keyword=in.keyword; severityCd=in.severityCd; riskType=in.riskType; maxDaysLeft=in.maxDaysLeft; }
    private static final Map<String, FieldProperty> fieldPropertyMap = new LinkedHashMap<>();
    static { fieldPropertyMap.put("keyword", FieldProperty.builder().setPhysicalName("keyword").setLogicalName("keyword").setType(FieldProperty.TYPE_OBJECT_STRING).setDecimal(-1).setIsNullable(true).setIsEncrypt(false).build()); }
    public Map<String, FieldProperty> getFieldPropertyMap(){ return Collections.unmodifiableMap(fieldPropertyMap); }
}

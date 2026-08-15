package nhnis.infra.in.a.dto;

import java.util.*;
import com.ims.superspring.dto.DataObject;
import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;

public class ifina7300S0DTOin extends DataObject {
    private static final long serialVersionUID = 1L;
    private String targetTypeCd, targetId, periodYm, scenarioCd;
    private Integer years;
    public String getTargetTypeCd(){return targetTypeCd;} public void setTargetTypeCd(String v){targetTypeCd=v;}
    public String getTargetId(){return targetId;} public void setTargetId(String v){targetId=v;}
    public String getPeriodYm(){return periodYm;} public void setPeriodYm(String v){periodYm=v;}
    public String getScenarioCd(){return scenarioCd;} public void setScenarioCd(String v){scenarioCd=v;}
    public Integer getYears(){return years;} public void setYears(Integer v){years=v;}
    @Override public Object clone(){ ifina7300S0DTOin c=new ifina7300S0DTOin(); c.clone(this); return c; }
    public void clone(DataObject src){ if(this==src)return; ifina7300S0DTOin in=(ifina7300S0DTOin)src;
      targetTypeCd=in.targetTypeCd; targetId=in.targetId; periodYm=in.periodYm; scenarioCd=in.scenarioCd; years=in.years; }
    private static final Map<String, FieldProperty> fieldPropertyMap = new LinkedHashMap<>();
    static { fieldPropertyMap.put("targetId", FieldProperty.builder().setPhysicalName("targetId").setLogicalName("targetId").setType(FieldProperty.TYPE_OBJECT_STRING).setDecimal(-1).setIsNullable(true).setIsEncrypt(false).build()); }
    public Map<String, FieldProperty> getFieldPropertyMap(){ return Collections.unmodifiableMap(fieldPropertyMap); }
}

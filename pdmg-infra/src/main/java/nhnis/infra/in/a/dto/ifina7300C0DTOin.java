package nhnis.infra.in.a.dto;

import java.math.BigDecimal;
import java.util.*;
import com.ims.superspring.dto.DataObject;
import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;

public class ifina7300C0DTOin extends DataObject {
    private static final long serialVersionUID = 1L;
    private String costId, targetTypeCd, targetId, periodYm, scenarioCd, costTypeCd, currencyCd, remark;
    private BigDecimal amount;
    public String getCostId(){return costId;} public void setCostId(String v){costId=v;}
    public String getTargetTypeCd(){return targetTypeCd;} public void setTargetTypeCd(String v){targetTypeCd=v;}
    public String getTargetId(){return targetId;} public void setTargetId(String v){targetId=v;}
    public String getPeriodYm(){return periodYm;} public void setPeriodYm(String v){periodYm=v;}
    public String getScenarioCd(){return scenarioCd;} public void setScenarioCd(String v){scenarioCd=v;}
    public String getCostTypeCd(){return costTypeCd;} public void setCostTypeCd(String v){costTypeCd=v;}
    public BigDecimal getAmount(){return amount;} public void setAmount(BigDecimal v){amount=v;}
    public String getCurrencyCd(){return currencyCd;} public void setCurrencyCd(String v){currencyCd=v;}
    public String getRemark(){return remark;} public void setRemark(String v){remark=v;}
    @Override public Object clone(){ ifina7300C0DTOin c=new ifina7300C0DTOin(); c.clone(this); return c; }
    public void clone(DataObject src){ if(this==src)return; ifina7300C0DTOin in=(ifina7300C0DTOin)src;
      costId=in.costId; targetTypeCd=in.targetTypeCd; targetId=in.targetId; periodYm=in.periodYm;
      scenarioCd=in.scenarioCd; costTypeCd=in.costTypeCd; amount=in.amount; currencyCd=in.currencyCd; remark=in.remark; }
    private static final Map<String, FieldProperty> fieldPropertyMap = new LinkedHashMap<>();
    static { fieldPropertyMap.put("costId", FieldProperty.builder().setPhysicalName("costId").setLogicalName("costId").setType(FieldProperty.TYPE_OBJECT_STRING).setDecimal(-1).setIsNullable(true).setIsEncrypt(false).build()); }
    public Map<String, FieldProperty> getFieldPropertyMap(){ return Collections.unmodifiableMap(fieldPropertyMap); }
}

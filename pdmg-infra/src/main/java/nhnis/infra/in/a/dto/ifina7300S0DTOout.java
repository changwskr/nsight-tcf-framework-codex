package nhnis.infra.in.a.dto;

import java.math.BigDecimal;
import java.util.*;
import com.ims.superspring.dto.DataObject;
import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;

public class ifina7300S0DTOout extends DataObject {
    private static final long serialVersionUID = 1L;
    private String targetTypeCd, targetId, periodYm, RSLT_CD, RSLT_MSG;
    private Integer years;
    private List<Map<String, Object>> rows = new ArrayList<>();
    private Map<String, Object> tcoSummary = new LinkedHashMap<>();
    private BigDecimal asisAnnual, tobeAnnual, migrationOnce, asisTco, tobeTco, deltaTco;
    public String getTargetTypeCd(){return targetTypeCd;} public void setTargetTypeCd(String v){targetTypeCd=v;}
    public String getTargetId(){return targetId;} public void setTargetId(String v){targetId=v;}
    public String getPeriodYm(){return periodYm;} public void setPeriodYm(String v){periodYm=v;}
    public Integer getYears(){return years;} public void setYears(Integer v){years=v;}
    public List<Map<String, Object>> getRows(){return rows;} public void setRows(List<Map<String, Object>> v){rows=v!=null?v:new ArrayList<>();}
    public Map<String, Object> getTcoSummary(){return tcoSummary;} public void setTcoSummary(Map<String, Object> v){tcoSummary=v!=null?v:new LinkedHashMap<>();}
    public BigDecimal getAsisAnnual(){return asisAnnual;} public void setAsisAnnual(BigDecimal v){asisAnnual=v;}
    public BigDecimal getTobeAnnual(){return tobeAnnual;} public void setTobeAnnual(BigDecimal v){tobeAnnual=v;}
    public BigDecimal getMigrationOnce(){return migrationOnce;} public void setMigrationOnce(BigDecimal v){migrationOnce=v;}
    public BigDecimal getAsisTco(){return asisTco;} public void setAsisTco(BigDecimal v){asisTco=v;}
    public BigDecimal getTobeTco(){return tobeTco;} public void setTobeTco(BigDecimal v){tobeTco=v;}
    public BigDecimal getDeltaTco(){return deltaTco;} public void setDeltaTco(BigDecimal v){deltaTco=v;}
    public String getRSLT_CD(){return RSLT_CD;} public void setRSLT_CD(String v){RSLT_CD=v;}
    public String getRSLT_MSG(){return RSLT_MSG;} public void setRSLT_MSG(String v){RSLT_MSG=v;}
    @Override public Object clone(){ ifina7300S0DTOout c=new ifina7300S0DTOout(); c.clone(this); return c; }
    public void clone(DataObject src){ if(this==src)return; ifina7300S0DTOout in=(ifina7300S0DTOout)src;
      targetTypeCd=in.targetTypeCd; targetId=in.targetId; periodYm=in.periodYm; years=in.years;
      rows=in.rows==null?new ArrayList<>():new ArrayList<>(in.rows);
      tcoSummary=in.tcoSummary==null?new LinkedHashMap<>():new LinkedHashMap<>(in.tcoSummary);
      asisAnnual=in.asisAnnual; tobeAnnual=in.tobeAnnual; migrationOnce=in.migrationOnce;
      asisTco=in.asisTco; tobeTco=in.tobeTco; deltaTco=in.deltaTco; RSLT_CD=in.RSLT_CD; RSLT_MSG=in.RSLT_MSG; }
    private static final Map<String, FieldProperty> fieldPropertyMap = new LinkedHashMap<>();
    static { fieldPropertyMap.put("RSLT_CD", FieldProperty.builder().setPhysicalName("RSLT_CD").setLogicalName("RSLT_CD").setType(FieldProperty.TYPE_OBJECT_STRING).setDecimal(-1).setIsNullable(true).setIsEncrypt(false).build()); }
    public Map<String, FieldProperty> getFieldPropertyMap(){ return Collections.unmodifiableMap(fieldPropertyMap); }
}

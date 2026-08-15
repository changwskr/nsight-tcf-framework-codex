package nhnis.infra.in.a.dto;
import java.util.Collections; import java.util.LinkedHashMap; import java.util.Map;
import com.ims.superspring.dto.DataObject; import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;
public class ifina1500S0DTOSub0 extends DataObject {
    private static final long serialVersionUID = 1L;
    private String entityType, orgId, orgName, parentOrgId, orgTypeCd, personId, personName, email, roleCd, activeYn, remark, regUserId, regDtm, chgUserId, chgDtm;
    public String getEntityType(){return entityType;} public void setEntityType(String v){entityType=v;}
    public String getOrgId(){return orgId;} public void setOrgId(String v){orgId=v;}
    public String getOrgName(){return orgName;} public void setOrgName(String v){orgName=v;}
    public String getParentOrgId(){return parentOrgId;} public void setParentOrgId(String v){parentOrgId=v;}
    public String getOrgTypeCd(){return orgTypeCd;} public void setOrgTypeCd(String v){orgTypeCd=v;}
    public String getPersonId(){return personId;} public void setPersonId(String v){personId=v;}
    public String getPersonName(){return personName;} public void setPersonName(String v){personName=v;}
    public String getEmail(){return email;} public void setEmail(String v){email=v;}
    public String getRoleCd(){return roleCd;} public void setRoleCd(String v){roleCd=v;}
    public String getActiveYn(){return activeYn;} public void setActiveYn(String v){activeYn=v;}
    public String getRemark(){return remark;} public void setRemark(String v){remark=v;}
    public String getRegUserId(){return regUserId;} public void setRegUserId(String v){regUserId=v;}
    public String getRegDtm(){return regDtm;} public void setRegDtm(String v){regDtm=v;}
    public String getChgUserId(){return chgUserId;} public void setChgUserId(String v){chgUserId=v;}
    public String getChgDtm(){return chgDtm;} public void setChgDtm(String v){chgDtm=v;}
    @Override public Object clone(){ ifina1500S0DTOSub0 c=new ifina1500S0DTOSub0(); c.clone(this); return c; }
    public void clone(DataObject src){ if(this==src)return; ifina1500S0DTOSub0 in=(ifina1500S0DTOSub0)src; entityType=in.entityType; orgId=in.orgId; orgName=in.orgName; parentOrgId=in.parentOrgId; orgTypeCd=in.orgTypeCd; personId=in.personId; personName=in.personName; email=in.email; roleCd=in.roleCd; activeYn=in.activeYn; remark=in.remark; regUserId=in.regUserId; regDtm=in.regDtm; chgUserId=in.chgUserId; chgDtm=in.chgDtm; }
    private static final Map<String, FieldProperty> fieldPropertyMap = new LinkedHashMap<>();
    static { for(String n: new String[]{"entityType", "orgId", "orgName", "parentOrgId", "orgTypeCd", "personId", "personName", "email", "roleCd", "activeYn", "remark", "regUserId", "regDtm", "chgUserId", "chgDtm"})
      fieldPropertyMap.put(n, FieldProperty.builder().setPhysicalName(n).setLogicalName(n).setType(FieldProperty.TYPE_OBJECT_STRING).setDecimal(-1).setIsNullable(true).setIsEncrypt(false).build()); }
    public Map<String, FieldProperty> getFieldPropertyMap(){ return Collections.unmodifiableMap(fieldPropertyMap); }
}

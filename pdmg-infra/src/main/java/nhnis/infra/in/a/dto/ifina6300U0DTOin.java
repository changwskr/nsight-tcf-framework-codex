package nhnis.infra.in.a.dto;

import java.util.*;
import com.ims.superspring.dto.DataObject;
import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;

public class ifina6300U0DTOin extends DataObject {
    private static final long serialVersionUID = 1L;
    private String profileId, targetTypeCd, targetId, securityGradeCd;
    private String personalInfoYn, creditInfoYn, financialTxnYn, adminInfoYn;
    private String externalConnYn, internetConnYn, encryptionYn, kmsHsmYn;
    private String pamYn, edrYn, auditLogYn, authMethodCd, networkZoneCd, remark;
    public String getProfileId(){return profileId;} public void setProfileId(String v){profileId=v;}
    public String getTargetTypeCd(){return targetTypeCd;} public void setTargetTypeCd(String v){targetTypeCd=v;}
    public String getTargetId(){return targetId;} public void setTargetId(String v){targetId=v;}
    public String getSecurityGradeCd(){return securityGradeCd;} public void setSecurityGradeCd(String v){securityGradeCd=v;}
    public String getPersonalInfoYn(){return personalInfoYn;} public void setPersonalInfoYn(String v){personalInfoYn=v;}
    public String getCreditInfoYn(){return creditInfoYn;} public void setCreditInfoYn(String v){creditInfoYn=v;}
    public String getFinancialTxnYn(){return financialTxnYn;} public void setFinancialTxnYn(String v){financialTxnYn=v;}
    public String getAdminInfoYn(){return adminInfoYn;} public void setAdminInfoYn(String v){adminInfoYn=v;}
    public String getExternalConnYn(){return externalConnYn;} public void setExternalConnYn(String v){externalConnYn=v;}
    public String getInternetConnYn(){return internetConnYn;} public void setInternetConnYn(String v){internetConnYn=v;}
    public String getEncryptionYn(){return encryptionYn;} public void setEncryptionYn(String v){encryptionYn=v;}
    public String getKmsHsmYn(){return kmsHsmYn;} public void setKmsHsmYn(String v){kmsHsmYn=v;}
    public String getPamYn(){return pamYn;} public void setPamYn(String v){pamYn=v;}
    public String getEdrYn(){return edrYn;} public void setEdrYn(String v){edrYn=v;}
    public String getAuditLogYn(){return auditLogYn;} public void setAuditLogYn(String v){auditLogYn=v;}
    public String getAuthMethodCd(){return authMethodCd;} public void setAuthMethodCd(String v){authMethodCd=v;}
    public String getNetworkZoneCd(){return networkZoneCd;} public void setNetworkZoneCd(String v){networkZoneCd=v;}
    public String getRemark(){return remark;} public void setRemark(String v){remark=v;}
    @Override public Object clone(){ ifina6300U0DTOin c=new ifina6300U0DTOin(); c.clone(this); return c; }
    public void clone(DataObject src){ if(this==src)return; ifina6300U0DTOin in=(ifina6300U0DTOin)src;
      profileId=in.profileId; targetTypeCd=in.targetTypeCd; targetId=in.targetId; securityGradeCd=in.securityGradeCd;
      personalInfoYn=in.personalInfoYn; creditInfoYn=in.creditInfoYn; financialTxnYn=in.financialTxnYn; adminInfoYn=in.adminInfoYn;
      externalConnYn=in.externalConnYn; internetConnYn=in.internetConnYn; encryptionYn=in.encryptionYn; kmsHsmYn=in.kmsHsmYn;
      pamYn=in.pamYn; edrYn=in.edrYn; auditLogYn=in.auditLogYn; authMethodCd=in.authMethodCd; networkZoneCd=in.networkZoneCd;
      remark=in.remark; }
    private static final Map<String, FieldProperty> fieldPropertyMap = new LinkedHashMap<>();
    static { fieldPropertyMap.put("targetId", FieldProperty.builder().setPhysicalName("targetId").setLogicalName("targetId").setType(FieldProperty.TYPE_OBJECT_STRING).setDecimal(-1).setIsNullable(true).setIsEncrypt(false).build()); }
    public Map<String, FieldProperty> getFieldPropertyMap(){ return Collections.unmodifiableMap(fieldPropertyMap); }
}

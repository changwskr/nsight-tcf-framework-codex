package nhnis.infra.in.a.dto;

import java.math.BigDecimal;
import java.util.*;
import com.ims.superspring.dto.DataObject;
import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;

public class ifina7100C0DTOin extends DataObject {
    private static final long serialVersionUID = 1L;
    private String licenseId, productName, vendorName, licenseModelCd, currencyCd, contractEndDt, mobilityYn, remark;
    private BigDecimal qty, annualMaintAmt;
    public String getLicenseId(){return licenseId;} public void setLicenseId(String v){licenseId=v;}
    public String getProductName(){return productName;} public void setProductName(String v){productName=v;}
    public String getVendorName(){return vendorName;} public void setVendorName(String v){vendorName=v;}
    public String getLicenseModelCd(){return licenseModelCd;} public void setLicenseModelCd(String v){licenseModelCd=v;}
    public BigDecimal getQty(){return qty;} public void setQty(BigDecimal v){qty=v;}
    public BigDecimal getAnnualMaintAmt(){return annualMaintAmt;} public void setAnnualMaintAmt(BigDecimal v){annualMaintAmt=v;}
    public String getCurrencyCd(){return currencyCd;} public void setCurrencyCd(String v){currencyCd=v;}
    public String getContractEndDt(){return contractEndDt;} public void setContractEndDt(String v){contractEndDt=v;}
    public String getMobilityYn(){return mobilityYn;} public void setMobilityYn(String v){mobilityYn=v;}
    public String getRemark(){return remark;} public void setRemark(String v){remark=v;}
    @Override public Object clone(){ ifina7100C0DTOin c=new ifina7100C0DTOin(); c.clone(this); return c; }
    public void clone(DataObject src){ if(this==src)return; ifina7100C0DTOin in=(ifina7100C0DTOin)src;
      licenseId=in.licenseId; productName=in.productName; vendorName=in.vendorName; licenseModelCd=in.licenseModelCd;
      qty=in.qty; annualMaintAmt=in.annualMaintAmt; currencyCd=in.currencyCd; contractEndDt=in.contractEndDt;
      mobilityYn=in.mobilityYn; remark=in.remark; }
    private static final Map<String, FieldProperty> fieldPropertyMap = new LinkedHashMap<>();
    static { fieldPropertyMap.put("licenseId", FieldProperty.builder().setPhysicalName("licenseId").setLogicalName("licenseId").setType(FieldProperty.TYPE_OBJECT_STRING).setDecimal(-1).setIsNullable(true).setIsEncrypt(false).build()); }
    public Map<String, FieldProperty> getFieldPropertyMap(){ return Collections.unmodifiableMap(fieldPropertyMap); }
}

package nhnis.infra.in.a.dto;

import java.math.BigDecimal;
import java.util.*;
import com.ims.superspring.dto.DataObject;
import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;

public class ifina7200S0DTOout extends DataObject {
    private static final long serialVersionUID = 1L;
    private String licenseId, productName, vendorName, licenseModelCd, RSLT_CD, RSLT_MSG;
    private BigDecimal contractQty, allocatedSum, remainingQty;
    private List<Map<String, Object>> allocations = new ArrayList<>();
    public String getLicenseId(){return licenseId;} public void setLicenseId(String v){licenseId=v;}
    public String getProductName(){return productName;} public void setProductName(String v){productName=v;}
    public String getVendorName(){return vendorName;} public void setVendorName(String v){vendorName=v;}
    public String getLicenseModelCd(){return licenseModelCd;} public void setLicenseModelCd(String v){licenseModelCd=v;}
    public BigDecimal getContractQty(){return contractQty;} public void setContractQty(BigDecimal v){contractQty=v;}
    public BigDecimal getAllocatedSum(){return allocatedSum;} public void setAllocatedSum(BigDecimal v){allocatedSum=v;}
    public BigDecimal getRemainingQty(){return remainingQty;} public void setRemainingQty(BigDecimal v){remainingQty=v;}
    public List<Map<String, Object>> getAllocations(){return allocations;} public void setAllocations(List<Map<String, Object>> v){allocations=v!=null?v:new ArrayList<>();}
    public String getRSLT_CD(){return RSLT_CD;} public void setRSLT_CD(String v){RSLT_CD=v;}
    public String getRSLT_MSG(){return RSLT_MSG;} public void setRSLT_MSG(String v){RSLT_MSG=v;}
    @Override public Object clone(){ ifina7200S0DTOout c=new ifina7200S0DTOout(); c.clone(this); return c; }
    public void clone(DataObject src){ if(this==src)return; ifina7200S0DTOout in=(ifina7200S0DTOout)src;
      licenseId=in.licenseId; productName=in.productName; vendorName=in.vendorName; licenseModelCd=in.licenseModelCd;
      contractQty=in.contractQty; allocatedSum=in.allocatedSum; remainingQty=in.remainingQty;
      allocations=in.allocations==null?new ArrayList<>():new ArrayList<>(in.allocations);
      RSLT_CD=in.RSLT_CD; RSLT_MSG=in.RSLT_MSG; }
    private static final Map<String, FieldProperty> fieldPropertyMap = new LinkedHashMap<>();
    static { fieldPropertyMap.put("RSLT_CD", FieldProperty.builder().setPhysicalName("RSLT_CD").setLogicalName("RSLT_CD").setType(FieldProperty.TYPE_OBJECT_STRING).setDecimal(-1).setIsNullable(true).setIsEncrypt(false).build()); }
    public Map<String, FieldProperty> getFieldPropertyMap(){ return Collections.unmodifiableMap(fieldPropertyMap); }
}

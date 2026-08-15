package nhnis.infra.in.a.dto;

import java.util.*;
import com.ims.superspring.dto.DataObject;
import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;

public class ifina7100S0DTOin extends DataObject {
    private static final long serialVersionUID = 1L;
    private String keyword, vendorName, licenseModelCd, licenseId;
    private Integer pageNo, pageSize;
    public String getKeyword(){return keyword;} public void setKeyword(String v){keyword=v;}
    public String getVendorName(){return vendorName;} public void setVendorName(String v){vendorName=v;}
    public String getLicenseModelCd(){return licenseModelCd;} public void setLicenseModelCd(String v){licenseModelCd=v;}
    public String getLicenseId(){return licenseId;} public void setLicenseId(String v){licenseId=v;}
    public Integer getPageNo(){return pageNo;} public void setPageNo(Integer v){pageNo=v;}
    public Integer getPageSize(){return pageSize;} public void setPageSize(Integer v){pageSize=v;}
    @Override public Object clone(){ ifina7100S0DTOin c=new ifina7100S0DTOin(); c.clone(this); return c; }
    public void clone(DataObject src){ if(this==src)return; ifina7100S0DTOin in=(ifina7100S0DTOin)src;
      keyword=in.keyword; vendorName=in.vendorName; licenseModelCd=in.licenseModelCd; licenseId=in.licenseId;
      pageNo=in.pageNo; pageSize=in.pageSize; }
    private static final Map<String, FieldProperty> fieldPropertyMap = new LinkedHashMap<>();
    static { fieldPropertyMap.put("keyword", FieldProperty.builder().setPhysicalName("keyword").setLogicalName("keyword").setType(FieldProperty.TYPE_OBJECT_STRING).setDecimal(-1).setIsNullable(true).setIsEncrypt(false).build()); }
    public Map<String, FieldProperty> getFieldPropertyMap(){ return Collections.unmodifiableMap(fieldPropertyMap); }
}

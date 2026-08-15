package nhnis.infra.in.a.dto;

import java.util.*;
import com.ims.superspring.dto.DataObject;
import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;

public class ifina8100S0DTOin extends DataObject {
    private static final long serialVersionUID = 1L;
    private String keyword, waveId, waveUnassigned, targetTypeCd, targetId, strategy7rCd, statusCd;
    private Integer pageNo, pageSize;
    public String getKeyword(){return keyword;} public void setKeyword(String v){keyword=v;}
    public String getWaveId(){return waveId;} public void setWaveId(String v){waveId=v;}
    public String getWaveUnassigned(){return waveUnassigned;} public void setWaveUnassigned(String v){waveUnassigned=v;}
    public String getTargetTypeCd(){return targetTypeCd;} public void setTargetTypeCd(String v){targetTypeCd=v;}
    public String getTargetId(){return targetId;} public void setTargetId(String v){targetId=v;}
    public String getStrategy7rCd(){return strategy7rCd;} public void setStrategy7rCd(String v){strategy7rCd=v;}
    public String getStatusCd(){return statusCd;} public void setStatusCd(String v){statusCd=v;}
    public Integer getPageNo(){return pageNo;} public void setPageNo(Integer v){pageNo=v;}
    public Integer getPageSize(){return pageSize;} public void setPageSize(Integer v){pageSize=v;}
    @Override public Object clone(){ ifina8100S0DTOin c=new ifina8100S0DTOin(); c.clone(this); return c; }
    public void clone(DataObject src){ if(this==src)return; ifina8100S0DTOin in=(ifina8100S0DTOin)src;
      keyword=in.keyword; waveId=in.waveId; waveUnassigned=in.waveUnassigned; targetTypeCd=in.targetTypeCd;
      targetId=in.targetId; strategy7rCd=in.strategy7rCd; statusCd=in.statusCd; pageNo=in.pageNo; pageSize=in.pageSize; }
    private static final Map<String, FieldProperty> fieldPropertyMap = new LinkedHashMap<>();
    static { fieldPropertyMap.put("keyword", FieldProperty.builder().setPhysicalName("keyword").setLogicalName("keyword").setType(FieldProperty.TYPE_OBJECT_STRING).setDecimal(-1).setIsNullable(true).setIsEncrypt(false).build()); }
    public Map<String, FieldProperty> getFieldPropertyMap(){ return Collections.unmodifiableMap(fieldPropertyMap); }
}

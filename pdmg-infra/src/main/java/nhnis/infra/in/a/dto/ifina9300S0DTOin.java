package nhnis.infra.in.a.dto;

import java.util.*;
import com.ims.superspring.dto.DataObject;
import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;

public class ifina9300S0DTOin extends DataObject {
    private static final long serialVersionUID = 1L;
    private String keyword, severityCd, gapType;
    public String getKeyword(){return keyword;} public void setKeyword(String v){keyword=v;}
    public String getSeverityCd(){return severityCd;} public void setSeverityCd(String v){severityCd=v;}
    public String getGapType(){return gapType;} public void setGapType(String v){gapType=v;}
    @Override public Object clone(){ ifina9300S0DTOin c=new ifina9300S0DTOin(); c.clone(this); return c; }
    public void clone(DataObject src){ if(this==src)return; ifina9300S0DTOin in=(ifina9300S0DTOin)src;
      keyword=in.keyword; severityCd=in.severityCd; gapType=in.gapType; }
    private static final Map<String, FieldProperty> fieldPropertyMap = new LinkedHashMap<>();
    static { fieldPropertyMap.put("keyword", FieldProperty.builder().setPhysicalName("keyword").setLogicalName("keyword").setType(FieldProperty.TYPE_OBJECT_STRING).setDecimal(-1).setIsNullable(true).setIsEncrypt(false).build()); }
    public Map<String, FieldProperty> getFieldPropertyMap(){ return Collections.unmodifiableMap(fieldPropertyMap); }
}

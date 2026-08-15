package nhnis.infra.in.a.dto;

import java.util.*;
import com.ims.superspring.dto.DataObject;
import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;

public class ifina1400C0DTOin extends DataObject {
    private static final long serialVersionUID = 1L;
    private String gateId, nameKo, description, activeYn;
    private Integer sortNo;
    public String getGateId(){return gateId;} public void setGateId(String v){gateId=v;}
    public String getNameKo(){return nameKo;} public void setNameKo(String v){nameKo=v;}
    public String getDescription(){return description;} public void setDescription(String v){description=v;}
    public String getActiveYn(){return activeYn;} public void setActiveYn(String v){activeYn=v;}
    public Integer getSortNo(){return sortNo;} public void setSortNo(Integer v){sortNo=v;}
    @Override public Object clone(){ ifina1400C0DTOin c=new ifina1400C0DTOin(); c.clone(this); return c; }
    public void clone(DataObject src){ if(this==src)return; ifina1400C0DTOin in=(ifina1400C0DTOin)src;
      gateId=in.gateId; nameKo=in.nameKo; description=in.description; activeYn=in.activeYn; sortNo=in.sortNo; }
    private static final Map<String, FieldProperty> fieldPropertyMap = new LinkedHashMap<>();
    static { fieldPropertyMap.put("gateId", FieldProperty.builder().setPhysicalName("gateId").setLogicalName("gateId").setType(FieldProperty.TYPE_OBJECT_STRING).setDecimal(-1).setIsNullable(true).setIsEncrypt(false).build()); }
    public Map<String, FieldProperty> getFieldPropertyMap(){ return Collections.unmodifiableMap(fieldPropertyMap); }
}

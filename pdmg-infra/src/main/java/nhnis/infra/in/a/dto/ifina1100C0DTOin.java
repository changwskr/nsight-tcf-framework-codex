package nhnis.infra.in.a.dto;
import java.util.Collections; import java.util.LinkedHashMap; import java.util.Map;
import com.ims.superspring.dto.DataObject; import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;
public class ifina1100C0DTOin extends DataObject {
    private static final long serialVersionUID = 1L;
    private String codeSetId, codeValue, nameKo, activeYn, remark, regUserId;
    private Integer sortOrder;
    public String getCodeSetId(){return codeSetId;} public void setCodeSetId(String v){codeSetId=v;}
    public String getCodeValue(){return codeValue;} public void setCodeValue(String v){codeValue=v;}
    public String getNameKo(){return nameKo;} public void setNameKo(String v){nameKo=v;}
    public String getActiveYn(){return activeYn;} public void setActiveYn(String v){activeYn=v;}
    public String getRemark(){return remark;} public void setRemark(String v){remark=v;}
    public String getRegUserId(){return regUserId;} public void setRegUserId(String v){regUserId=v;}
    public Integer getSortOrder(){return sortOrder;} public void setSortOrder(Integer v){sortOrder=v;}
    @Override public Object clone(){ ifina1100C0DTOin c=new ifina1100C0DTOin(); c.clone(this); return c; }
    public void clone(DataObject src){ if(this==src)return; ifina1100C0DTOin in=(ifina1100C0DTOin)src; codeSetId=in.codeSetId; codeValue=in.codeValue; nameKo=in.nameKo; activeYn=in.activeYn; remark=in.remark; regUserId=in.regUserId; sortOrder=in.sortOrder; }
    private static final Map<String, FieldProperty> fieldPropertyMap = new LinkedHashMap<>();
    static { for(String n: new String[]{"codeSetId", "codeValue", "nameKo", "activeYn", "remark", "regUserId"})
      fieldPropertyMap.put(n, FieldProperty.builder().setPhysicalName(n).setLogicalName(n).setType(FieldProperty.TYPE_OBJECT_STRING).setDecimal(-1).setIsNullable(true).setIsEncrypt(false).build());
      for(String n: new String[]{"sortOrder"}) fieldPropertyMap.put(n, FieldProperty.builder().setPhysicalName(n).setLogicalName(n).setType(FieldProperty.TYPE_OBJECT_INT).setDecimal(-1).setIsNullable(true).setIsEncrypt(false).build()); }
    public Map<String, FieldProperty> getFieldPropertyMap(){ return Collections.unmodifiableMap(fieldPropertyMap); }
}

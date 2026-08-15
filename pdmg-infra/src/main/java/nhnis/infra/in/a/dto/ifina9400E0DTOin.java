package nhnis.infra.in.a.dto;

import java.util.*;
import com.ims.superspring.dto.DataObject;
import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;

public class ifina9400E0DTOin extends DataObject {
    private static final long serialVersionUID = 1L;
    private Integer tableId;
    private String formatCd;
    private String allTablesYn;
    private List<Integer> tableIdList;
    public Integer getTableId(){return tableId;} public void setTableId(Integer v){tableId=v;}
    public String getFormatCd(){return formatCd;} public void setFormatCd(String v){formatCd=v;}
    public String getAllTablesYn(){return allTablesYn;} public void setAllTablesYn(String v){allTablesYn=v;}
    public List<Integer> getTableIdList(){return tableIdList;} public void setTableIdList(List<Integer> v){tableIdList=v;}
    @Override public Object clone(){ ifina9400E0DTOin c=new ifina9400E0DTOin(); c.clone(this); return c; }
    public void clone(DataObject src){ if(this==src)return; ifina9400E0DTOin in=(ifina9400E0DTOin)src;
      tableId=in.tableId; formatCd=in.formatCd; allTablesYn=in.allTablesYn;
      tableIdList=in.tableIdList==null?null:new ArrayList<>(in.tableIdList); }
    private static final Map<String, FieldProperty> fieldPropertyMap = new LinkedHashMap<>();
    static {
        fieldPropertyMap.put("tableId", FieldProperty.builder().setPhysicalName("tableId").setLogicalName("tableId").setType(FieldProperty.TYPE_PRIMITIVE_INT).setDecimal(-1).setIsNullable(true).setIsEncrypt(false).build());
        fieldPropertyMap.put("formatCd", FieldProperty.builder().setPhysicalName("formatCd").setLogicalName("formatCd").setType(FieldProperty.TYPE_OBJECT_STRING).setDecimal(-1).setIsNullable(true).setIsEncrypt(false).build());
        fieldPropertyMap.put("allTablesYn", FieldProperty.builder().setPhysicalName("allTablesYn").setLogicalName("allTablesYn").setType(FieldProperty.TYPE_OBJECT_STRING).setDecimal(-1).setIsNullable(true).setIsEncrypt(false).build());
    }
    public Map<String, FieldProperty> getFieldPropertyMap(){ return Collections.unmodifiableMap(fieldPropertyMap); }
}

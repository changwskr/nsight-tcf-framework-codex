package nhnis.infra.in.a.dto;

import java.util.*;
import com.ims.superspring.dto.DataObject;
import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;

public class ifina9400S0DTOout extends DataObject {
    private static final long serialVersionUID = 1L;
    private int tableId; private String tableName, RSLT_CD, RSLT_MSG;
    private List<String> columns = new ArrayList<>();
    private List<Map<String, Object>> rows = new ArrayList<>();
    public int getTableId(){return tableId;} public void setTableId(int v){tableId=v;}
    public String getTableName(){return tableName;} public void setTableName(String v){tableName=v;}
    public List<String> getColumns(){return columns;} public void setColumns(List<String> v){columns=v!=null?v:new ArrayList<>();}
    public List<Map<String, Object>> getRows(){return rows;} public void setRows(List<Map<String, Object>> v){rows=v!=null?v:new ArrayList<>();}
    public String getRSLT_CD(){return RSLT_CD;} public void setRSLT_CD(String v){RSLT_CD=v;}
    public String getRSLT_MSG(){return RSLT_MSG;} public void setRSLT_MSG(String v){RSLT_MSG=v;}
    @Override public Object clone(){ ifina9400S0DTOout c=new ifina9400S0DTOout(); c.clone(this); return c; }
    public void clone(DataObject src){ if(this==src)return; ifina9400S0DTOout in=(ifina9400S0DTOout)src;
      tableId=in.tableId; tableName=in.tableName; columns=new ArrayList<>(in.columns);
      rows=in.rows==null?new ArrayList<>():new ArrayList<>(in.rows); RSLT_CD=in.RSLT_CD; RSLT_MSG=in.RSLT_MSG; }
    private static final Map<String, FieldProperty> fieldPropertyMap = new LinkedHashMap<>();
    static { fieldPropertyMap.put("RSLT_CD", FieldProperty.builder().setPhysicalName("RSLT_CD").setLogicalName("RSLT_CD").setType(FieldProperty.TYPE_OBJECT_STRING).setDecimal(-1).setIsNullable(true).setIsEncrypt(false).build()); }
    public Map<String, FieldProperty> getFieldPropertyMap(){ return Collections.unmodifiableMap(fieldPropertyMap); }
}

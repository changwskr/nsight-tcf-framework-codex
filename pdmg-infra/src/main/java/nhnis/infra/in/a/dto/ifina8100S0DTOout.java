package nhnis.infra.in.a.dto;

import java.util.*;
import com.ims.superspring.dto.DataObject;
import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;

public class ifina8100S0DTOout extends DataObject {
    private static final long serialVersionUID = 1L;
    private List<Map<String, Object>> rows = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();
    private int size, pageNo, pageSize, totalPages; private long totalCount;
    public List<Map<String, Object>> getRows(){return rows;} public void setRows(List<Map<String, Object>> v){rows=v!=null?v:new ArrayList<>();}
    public List<String> getWarnings(){return warnings;} public void setWarnings(List<String> v){warnings=v!=null?v:new ArrayList<>();}
    public int getSize(){return size;} public void setSize(int v){size=v;}
    public int getPageNo(){return pageNo;} public void setPageNo(int v){pageNo=v;}
    public int getPageSize(){return pageSize;} public void setPageSize(int v){pageSize=v;}
    public long getTotalCount(){return totalCount;} public void setTotalCount(long v){totalCount=v;}
    public int getTotalPages(){return totalPages;} public void setTotalPages(int v){totalPages=v;}
    @Override public Object clone(){ ifina8100S0DTOout c=new ifina8100S0DTOout(); c.clone(this); return c; }
    public void clone(DataObject src){ if(this==src)return; ifina8100S0DTOout in=(ifina8100S0DTOout)src;
      rows=in.rows==null?new ArrayList<>():new ArrayList<>(in.rows); warnings=new ArrayList<>(in.warnings);
      size=in.size; pageNo=in.pageNo; pageSize=in.pageSize; totalCount=in.totalCount; totalPages=in.totalPages; }
    private static final Map<String, FieldProperty> fieldPropertyMap = new LinkedHashMap<>();
    static { fieldPropertyMap.put("size", FieldProperty.builder().setPhysicalName("size").setLogicalName("size").setType(FieldProperty.TYPE_PRIMITIVE_INT).setDecimal(-1).setIsNullable(true).setIsEncrypt(false).build()); }
    public Map<String, FieldProperty> getFieldPropertyMap(){ return Collections.unmodifiableMap(fieldPropertyMap); }
}

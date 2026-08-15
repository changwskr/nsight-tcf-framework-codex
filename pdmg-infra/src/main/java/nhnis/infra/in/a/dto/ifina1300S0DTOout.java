package nhnis.infra.in.a.dto;

import java.util.*;
import com.ims.superspring.dto.DataObject;
import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;

public class ifina1300S0DTOout extends DataObject {
    private static final long serialVersionUID = 1L;
    private List<Map<String, Object>> rows = new ArrayList<>();
    private int size, pageNo, pageSize, totalCount, totalPages;
    private String RSLT_CD, RSLT_MSG;
    public List<Map<String, Object>> getRows(){return rows;} public void setRows(List<Map<String, Object>> v){rows=v!=null?v:new ArrayList<>();}
    public int getSize(){return size;} public void setSize(int v){size=v;}
    public int getPageNo(){return pageNo;} public void setPageNo(int v){pageNo=v;}
    public int getPageSize(){return pageSize;} public void setPageSize(int v){pageSize=v;}
    public int getTotalCount(){return totalCount;} public void setTotalCount(int v){totalCount=v;}
    public int getTotalPages(){return totalPages;} public void setTotalPages(int v){totalPages=v;}
    public String getRSLT_CD(){return RSLT_CD;} public void setRSLT_CD(String v){RSLT_CD=v;}
    public String getRSLT_MSG(){return RSLT_MSG;} public void setRSLT_MSG(String v){RSLT_MSG=v;}
    @Override public Object clone(){ ifina1300S0DTOout c=new ifina1300S0DTOout(); c.clone(this); return c; }
    public void clone(DataObject src){ if(this==src)return; ifina1300S0DTOout in=(ifina1300S0DTOout)src;
      rows=in.rows==null?new ArrayList<>():new ArrayList<>(in.rows);
      size=in.size; pageNo=in.pageNo; pageSize=in.pageSize; totalCount=in.totalCount; totalPages=in.totalPages;
      RSLT_CD=in.RSLT_CD; RSLT_MSG=in.RSLT_MSG; }
    private static final Map<String, FieldProperty> fieldPropertyMap = new LinkedHashMap<>();
    static { fieldPropertyMap.put("RSLT_CD", FieldProperty.builder().setPhysicalName("RSLT_CD").setLogicalName("RSLT_CD").setType(FieldProperty.TYPE_OBJECT_STRING).setDecimal(-1).setIsNullable(true).setIsEncrypt(false).build()); }
    public Map<String, FieldProperty> getFieldPropertyMap(){ return Collections.unmodifiableMap(fieldPropertyMap); }
}

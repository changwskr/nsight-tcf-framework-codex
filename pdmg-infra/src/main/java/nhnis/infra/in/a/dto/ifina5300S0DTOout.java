package nhnis.infra.in.a.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.ims.superspring.dto.DataObject;
import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;

public class ifina5300S0DTOout extends DataObject {
    private static final long serialVersionUID = 1L;
    private List<Map<String, Object>> nodes = new ArrayList<>();
    private List<Map<String, Object>> edges = new ArrayList<>();
    private List<Map<String, Object>> rows = new ArrayList<>();
    private int size, pageNo, pageSize, totalPages; private long totalCount;
    private String rootType, rootId; private int depth;
    public List<Map<String, Object>> getNodes(){return nodes;} public void setNodes(List<Map<String, Object>> v){nodes=v!=null?v:new ArrayList<>();}
    public List<Map<String, Object>> getEdges(){return edges;} public void setEdges(List<Map<String, Object>> v){edges=v!=null?v:new ArrayList<>();}
    public List<Map<String, Object>> getRows(){return rows;} public void setRows(List<Map<String, Object>> v){rows=v!=null?v:new ArrayList<>();}
    public int getSize(){return size;} public void setSize(int v){size=v;}
    public int getPageNo(){return pageNo;} public void setPageNo(int v){pageNo=v;}
    public int getPageSize(){return pageSize;} public void setPageSize(int v){pageSize=v;}
    public long getTotalCount(){return totalCount;} public void setTotalCount(long v){totalCount=v;}
    public int getTotalPages(){return totalPages;} public void setTotalPages(int v){totalPages=v;}
    public String getRootType(){return rootType;} public void setRootType(String v){rootType=v;}
    public String getRootId(){return rootId;} public void setRootId(String v){rootId=v;}
    public int getDepth(){return depth;} public void setDepth(int v){depth=v;}
    @Override public Object clone(){ ifina5300S0DTOout c=new ifina5300S0DTOout(); c.clone(this); return c; }
    public void clone(DataObject src){ if(this==src)return; ifina5300S0DTOout in=(ifina5300S0DTOout)src;
      nodes=in.nodes==null?new ArrayList<>():new ArrayList<>(in.nodes);
      edges=in.edges==null?new ArrayList<>():new ArrayList<>(in.edges);
      rows=in.rows==null?new ArrayList<>():new ArrayList<>(in.rows);
      size=in.size; pageNo=in.pageNo; pageSize=in.pageSize; totalCount=in.totalCount; totalPages=in.totalPages;
      rootType=in.rootType; rootId=in.rootId; depth=in.depth; }
    private static final Map<String, FieldProperty> fieldPropertyMap = new LinkedHashMap<>();
    static { fieldPropertyMap.put("size", FieldProperty.builder().setPhysicalName("size").setLogicalName("size").setType(FieldProperty.TYPE_PRIMITIVE_INT).setDecimal(-1).setIsNullable(true).setIsEncrypt(false).build()); }
    public Map<String, FieldProperty> getFieldPropertyMap(){ return Collections.unmodifiableMap(fieldPropertyMap); }
}

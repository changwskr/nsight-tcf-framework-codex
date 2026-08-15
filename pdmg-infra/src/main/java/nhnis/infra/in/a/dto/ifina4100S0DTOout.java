package nhnis.infra.in.a.dto;
import java.util.ArrayList; import java.util.Collections; import java.util.LinkedHashMap; import java.util.List; import java.util.Map;
import com.ims.superspring.dto.DataObject; import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;
public class ifina4100S0DTOout extends DataObject {
    private static final long serialVersionUID = 1L;
    private List<ifina4100S0DTOSub0> ifina4100S0DTOSub0;
    private int size, pageNo, pageSize, totalPages; private long totalCount;
    public int sizeifina4100S0DTOSub0(){ return ifina4100S0DTOSub0==null?0:ifina4100S0DTOSub0.size(); }
    public List<ifina4100S0DTOSub0> getifina4100S0DTOSub0(){ return ifina4100S0DTOSub0; }
    public void setifina4100S0DTOSub0(List<ifina4100S0DTOSub0> list){ this.ifina4100S0DTOSub0=list; }
    public void addifina4100S0DTOSub0(ifina4100S0DTOSub0 item){ if(ifina4100S0DTOSub0==null)ifina4100S0DTOSub0=new ArrayList<>(); ifina4100S0DTOSub0.add(item); }
    public int getSize(){return size;} public void setSize(int v){size=v;}
    public int getPageNo(){return pageNo;} public void setPageNo(int v){pageNo=v;}
    public int getPageSize(){return pageSize;} public void setPageSize(int v){pageSize=v;}
    public long getTotalCount(){return totalCount;} public void setTotalCount(long v){totalCount=v;}
    public int getTotalPages(){return totalPages;} public void setTotalPages(int v){totalPages=v;}
    @Override public Object clone(){ ifina4100S0DTOout c=new ifina4100S0DTOout(); c.clone(this); return c; }
    public void clone(DataObject src){ if(this==src)return; ifina4100S0DTOout in=(ifina4100S0DTOout)src;
      ifina4100S0DTOSub0=null; if(in.ifina4100S0DTOSub0!=null) for(ifina4100S0DTOSub0 x: in.ifina4100S0DTOSub0) addifina4100S0DTOSub0(x==null?null:(ifina4100S0DTOSub0)x.clone());
      size=in.size; pageNo=in.pageNo; pageSize=in.pageSize; totalCount=in.totalCount; totalPages=in.totalPages; }
    private static final Map<String, FieldProperty> fieldPropertyMap = new LinkedHashMap<>();
    static {
      fieldPropertyMap.put("ifina4100S0DTOSub0", FieldProperty.builder().setPhysicalName("ifina4100S0DTOSub0").setLogicalName("ifina4100S0DTOSub0").setType(FieldProperty.TYPE_ABSTRACT_INCLUDE).setDecimal(-1).setArray("size").setReference("nhnis.infra.in.a.dto.ifina4100S0DTOSub0").setIsNullable(true).setIsEncrypt(false).build());
      fieldPropertyMap.put("size", FieldProperty.builder().setPhysicalName("size").setLogicalName("size").setType(FieldProperty.TYPE_PRIMITIVE_INT).setDecimal(-1).setIsNullable(true).setIsEncrypt(false).build());
    }
    public Map<String, FieldProperty> getFieldPropertyMap(){ return Collections.unmodifiableMap(fieldPropertyMap); }
}

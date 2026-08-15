package nhnis.infra.in.a.dto;
import java.util.ArrayList; import java.util.Collections; import java.util.LinkedHashMap; import java.util.List; import java.util.Map;
import com.ims.superspring.dto.DataObject; import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;
public class ifina3110S0DTOout extends DataObject {
    private static final long serialVersionUID = 1L;
    private List<ifina3110S0DTOSub0> ifina3110S0DTOSub0;
    private int size; private int pageNo; private int pageSize; private long totalCount; private int totalPages;
    public int sizeifina3110S0DTOSub0(){ return ifina3110S0DTOSub0==null?0:ifina3110S0DTOSub0.size(); }
    public List<ifina3110S0DTOSub0> getifina3110S0DTOSub0(){ return ifina3110S0DTOSub0; }
    public void setifina3110S0DTOSub0(List<ifina3110S0DTOSub0> list){ this.ifina3110S0DTOSub0=list; }
    public void addifina3110S0DTOSub0(ifina3110S0DTOSub0 item){ if(ifina3110S0DTOSub0==null)ifina3110S0DTOSub0=new ArrayList<>(); ifina3110S0DTOSub0.add(item); }
    public int getSize(){return size;} public void setSize(int v){size=v;}
    public int getPageNo(){return pageNo;} public void setPageNo(int v){pageNo=v;}
    public int getPageSize(){return pageSize;} public void setPageSize(int v){pageSize=v;}
    public long getTotalCount(){return totalCount;} public void setTotalCount(long v){totalCount=v;}
    public int getTotalPages(){return totalPages;} public void setTotalPages(int v){totalPages=v;}
    @Override public Object clone(){ ifina3110S0DTOout c=new ifina3110S0DTOout(); c.clone(this); return c; }
    public void clone(DataObject src){ if(this==src)return; ifina3110S0DTOout in=(ifina3110S0DTOout)src;
      ifina3110S0DTOSub0=null; if(in.ifina3110S0DTOSub0!=null) for(ifina3110S0DTOSub0 x: in.ifina3110S0DTOSub0) addifina3110S0DTOSub0(x==null?null:(ifina3110S0DTOSub0)x.clone());
      size=in.size; pageNo=in.pageNo; pageSize=in.pageSize; totalCount=in.totalCount; totalPages=in.totalPages; }
    private static final Map<String, FieldProperty> fieldPropertyMap = new LinkedHashMap<>();
    static {
      fieldPropertyMap.put("ifina3110S0DTOSub0", FieldProperty.builder().setPhysicalName("ifina3110S0DTOSub0").setLogicalName("ifina3110S0DTOSub0").setType(FieldProperty.TYPE_ABSTRACT_INCLUDE).setDecimal(-1).setArray("size").setReference("nhnis.infra.in.a.dto.ifina3110S0DTOSub0").setIsNullable(true).setIsEncrypt(false).build());
      fieldPropertyMap.put("size", FieldProperty.builder().setPhysicalName("size").setLogicalName("size").setType(FieldProperty.TYPE_PRIMITIVE_INT).setDecimal(-1).setIsNullable(true).setIsEncrypt(false).build());
    }
    public Map<String, FieldProperty> getFieldPropertyMap(){ return Collections.unmodifiableMap(fieldPropertyMap); }
}

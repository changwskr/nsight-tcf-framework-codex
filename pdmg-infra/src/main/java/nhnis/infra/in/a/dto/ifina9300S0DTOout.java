package nhnis.infra.in.a.dto;

import java.util.*;
import com.ims.superspring.dto.DataObject;
import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;

public class ifina9300S0DTOout extends DataObject {
    private static final long serialVersionUID = 1L;
    private List<Map<String, Object>> rows = new ArrayList<>();
    private int size; private long checklistCount, haCount, capacityCount, waveCount, statusCount;
    private String RSLT_CD, RSLT_MSG;
    public List<Map<String, Object>> getRows(){return rows;} public void setRows(List<Map<String, Object>> v){rows=v!=null?v:new ArrayList<>();}
    public int getSize(){return size;} public void setSize(int v){size=v;}
    public long getChecklistCount(){return checklistCount;} public void setChecklistCount(long v){checklistCount=v;}
    public long getHaCount(){return haCount;} public void setHaCount(long v){haCount=v;}
    public long getCapacityCount(){return capacityCount;} public void setCapacityCount(long v){capacityCount=v;}
    public long getWaveCount(){return waveCount;} public void setWaveCount(long v){waveCount=v;}
    public long getStatusCount(){return statusCount;} public void setStatusCount(long v){statusCount=v;}
    public String getRSLT_CD(){return RSLT_CD;} public void setRSLT_CD(String v){RSLT_CD=v;}
    public String getRSLT_MSG(){return RSLT_MSG;} public void setRSLT_MSG(String v){RSLT_MSG=v;}
    @Override public Object clone(){ ifina9300S0DTOout c=new ifina9300S0DTOout(); c.clone(this); return c; }
    public void clone(DataObject src){ if(this==src)return; ifina9300S0DTOout in=(ifina9300S0DTOout)src;
      rows=in.rows==null?new ArrayList<>():new ArrayList<>(in.rows); size=in.size;
      checklistCount=in.checklistCount; haCount=in.haCount; capacityCount=in.capacityCount;
      waveCount=in.waveCount; statusCount=in.statusCount; RSLT_CD=in.RSLT_CD; RSLT_MSG=in.RSLT_MSG; }
    private static final Map<String, FieldProperty> fieldPropertyMap = new LinkedHashMap<>();
    static { fieldPropertyMap.put("RSLT_CD", FieldProperty.builder().setPhysicalName("RSLT_CD").setLogicalName("RSLT_CD").setType(FieldProperty.TYPE_OBJECT_STRING).setDecimal(-1).setIsNullable(true).setIsEncrypt(false).build()); }
    public Map<String, FieldProperty> getFieldPropertyMap(){ return Collections.unmodifiableMap(fieldPropertyMap); }
}

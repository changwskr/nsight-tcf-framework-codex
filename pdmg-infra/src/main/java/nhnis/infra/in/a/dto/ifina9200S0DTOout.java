package nhnis.infra.in.a.dto;

import java.util.*;
import com.ims.superspring.dto.DataObject;
import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;

public class ifina9200S0DTOout extends DataObject {
    private static final long serialVersionUID = 1L;
    private List<Map<String, Object>> gates = new ArrayList<>();
    private List<Map<String, Object>> results = new ArrayList<>();
    private List<String> hints = new ArrayList<>();
    private String targetTypeCd, targetId; private int passCount, conditionalCount, failCount, notReadyCount;
    public List<Map<String, Object>> getGates(){return gates;} public void setGates(List<Map<String, Object>> v){gates=v!=null?v:new ArrayList<>();}
    public List<Map<String, Object>> getResults(){return results;} public void setResults(List<Map<String, Object>> v){results=v!=null?v:new ArrayList<>();}
    public List<String> getHints(){return hints;} public void setHints(List<String> v){hints=v!=null?v:new ArrayList<>();}
    public String getTargetTypeCd(){return targetTypeCd;} public void setTargetTypeCd(String v){targetTypeCd=v;}
    public String getTargetId(){return targetId;} public void setTargetId(String v){targetId=v;}
    public int getPassCount(){return passCount;} public void setPassCount(int v){passCount=v;}
    public int getConditionalCount(){return conditionalCount;} public void setConditionalCount(int v){conditionalCount=v;}
    public int getFailCount(){return failCount;} public void setFailCount(int v){failCount=v;}
    public int getNotReadyCount(){return notReadyCount;} public void setNotReadyCount(int v){notReadyCount=v;}
    @Override public Object clone(){ ifina9200S0DTOout c=new ifina9200S0DTOout(); c.clone(this); return c; }
    public void clone(DataObject src){ if(this==src)return; ifina9200S0DTOout in=(ifina9200S0DTOout)src;
      gates=new ArrayList<>(in.gates); results=new ArrayList<>(in.results); hints=new ArrayList<>(in.hints);
      targetTypeCd=in.targetTypeCd; targetId=in.targetId; passCount=in.passCount; conditionalCount=in.conditionalCount; failCount=in.failCount; notReadyCount=in.notReadyCount; }
    private static final Map<String, FieldProperty> fieldPropertyMap = new LinkedHashMap<>();
    static { fieldPropertyMap.put("passCount", FieldProperty.builder().setPhysicalName("passCount").setLogicalName("passCount").setType(FieldProperty.TYPE_PRIMITIVE_INT).setDecimal(-1).setIsNullable(true).setIsEncrypt(false).build()); }
    public Map<String, FieldProperty> getFieldPropertyMap(){ return Collections.unmodifiableMap(fieldPropertyMap); }
}

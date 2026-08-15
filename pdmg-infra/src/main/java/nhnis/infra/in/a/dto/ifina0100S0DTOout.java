package nhnis.infra.in.a.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.ims.superspring.dto.DataObject;
import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;

public class ifina0100S0DTOout extends DataObject {
    private static final long serialVersionUID = 1L;
    private long assetCount, pilotAssetCount, systemCount, groupCount, appCount, mwCount, dbCount, endpointCount, changeLogCount;
    private long confirmedCount, retiredCount;
    private List<Map<String, Object>> recentAssets = new ArrayList<>();
    private String RSLT_CD, RSLT_MSG;
    public long getAssetCount(){return assetCount;} public void setAssetCount(long v){assetCount=v;}
    public long getPilotAssetCount(){return pilotAssetCount;} public void setPilotAssetCount(long v){pilotAssetCount=v;}
    public long getSystemCount(){return systemCount;} public void setSystemCount(long v){systemCount=v;}
    public long getGroupCount(){return groupCount;} public void setGroupCount(long v){groupCount=v;}
    public long getAppCount(){return appCount;} public void setAppCount(long v){appCount=v;}
    public long getMwCount(){return mwCount;} public void setMwCount(long v){mwCount=v;}
    public long getDbCount(){return dbCount;} public void setDbCount(long v){dbCount=v;}
    public long getEndpointCount(){return endpointCount;} public void setEndpointCount(long v){endpointCount=v;}
    public long getChangeLogCount(){return changeLogCount;} public void setChangeLogCount(long v){changeLogCount=v;}
    public long getConfirmedCount(){return confirmedCount;} public void setConfirmedCount(long v){confirmedCount=v;}
    public long getRetiredCount(){return retiredCount;} public void setRetiredCount(long v){retiredCount=v;}
    public List<Map<String, Object>> getRecentAssets(){return recentAssets;} public void setRecentAssets(List<Map<String, Object>> v){recentAssets=v!=null?v:new ArrayList<>();}
    public String getRSLT_CD(){return RSLT_CD;} public void setRSLT_CD(String v){RSLT_CD=v;}
    public String getRSLT_MSG(){return RSLT_MSG;} public void setRSLT_MSG(String v){RSLT_MSG=v;}
    @Override public Object clone(){ ifina0100S0DTOout c=new ifina0100S0DTOout(); c.clone(this); return c; }
    public void clone(DataObject src){ if(this==src)return; ifina0100S0DTOout in=(ifina0100S0DTOout)src;
      assetCount=in.assetCount; pilotAssetCount=in.pilotAssetCount; systemCount=in.systemCount; groupCount=in.groupCount;
      appCount=in.appCount; mwCount=in.mwCount; dbCount=in.dbCount; endpointCount=in.endpointCount; changeLogCount=in.changeLogCount;
      confirmedCount=in.confirmedCount; retiredCount=in.retiredCount;
      recentAssets=in.recentAssets==null?new ArrayList<>():new ArrayList<>(in.recentAssets);
      RSLT_CD=in.RSLT_CD; RSLT_MSG=in.RSLT_MSG; }
    private static final Map<String, FieldProperty> fieldPropertyMap = new LinkedHashMap<>();
    static { fieldPropertyMap.put("RSLT_CD", FieldProperty.builder().setPhysicalName("RSLT_CD").setLogicalName("RSLT_CD").setType(FieldProperty.TYPE_OBJECT_STRING).setDecimal(-1).setIsNullable(true).setIsEncrypt(false).build()); }
    public Map<String, FieldProperty> getFieldPropertyMap(){ return Collections.unmodifiableMap(fieldPropertyMap); }
}

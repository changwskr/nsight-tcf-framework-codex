package nhnis.infra.in.a.dto;

import java.math.BigDecimal;
import java.util.*;
import com.ims.superspring.dto.DataObject;
import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;

public class ifina6200U0DTOin extends DataObject {
    private static final long serialVersionUID = 1L;
    private String snapshotId, targetTypeCd, targetId, metricScopeCd, capturedAt, remark;
    private BigDecimal cpuPct, memPct, tps, respP95Ms;
    private Integer dbConnPeak;
    public String getSnapshotId(){return snapshotId;} public void setSnapshotId(String v){snapshotId=v;}
    public String getTargetTypeCd(){return targetTypeCd;} public void setTargetTypeCd(String v){targetTypeCd=v;}
    public String getTargetId(){return targetId;} public void setTargetId(String v){targetId=v;}
    public String getMetricScopeCd(){return metricScopeCd;} public void setMetricScopeCd(String v){metricScopeCd=v;}
    public String getCapturedAt(){return capturedAt;} public void setCapturedAt(String v){capturedAt=v;}
    public BigDecimal getCpuPct(){return cpuPct;} public void setCpuPct(BigDecimal v){cpuPct=v;}
    public BigDecimal getMemPct(){return memPct;} public void setMemPct(BigDecimal v){memPct=v;}
    public BigDecimal getTps(){return tps;} public void setTps(BigDecimal v){tps=v;}
    public BigDecimal getRespP95Ms(){return respP95Ms;} public void setRespP95Ms(BigDecimal v){respP95Ms=v;}
    public Integer getDbConnPeak(){return dbConnPeak;} public void setDbConnPeak(Integer v){dbConnPeak=v;}
    public String getRemark(){return remark;} public void setRemark(String v){remark=v;}
    @Override public Object clone(){ ifina6200U0DTOin c=new ifina6200U0DTOin(); c.clone(this); return c; }
    public void clone(DataObject src){ if(this==src)return; ifina6200U0DTOin in=(ifina6200U0DTOin)src;
      snapshotId=in.snapshotId; targetTypeCd=in.targetTypeCd; targetId=in.targetId; metricScopeCd=in.metricScopeCd;
      capturedAt=in.capturedAt; cpuPct=in.cpuPct; memPct=in.memPct; tps=in.tps; respP95Ms=in.respP95Ms;
      dbConnPeak=in.dbConnPeak; remark=in.remark; }
    private static final Map<String, FieldProperty> fieldPropertyMap = new LinkedHashMap<>();
    static { fieldPropertyMap.put("targetId", FieldProperty.builder().setPhysicalName("targetId").setLogicalName("targetId").setType(FieldProperty.TYPE_OBJECT_STRING).setDecimal(-1).setIsNullable(true).setIsEncrypt(false).build()); }
    public Map<String, FieldProperty> getFieldPropertyMap(){ return Collections.unmodifiableMap(fieldPropertyMap); }
}

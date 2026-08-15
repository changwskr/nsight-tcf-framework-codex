package nhnis.infra.in.a.dto;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import com.ims.superspring.dto.DataObject;
import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;

public class ifina1999S0DTOSub0 extends DataObject {
    private static final long serialVersionUID = 1L;
    private String serverId, serverName, techRole, envCd, tierCd, statusCd, remark, regUserId, regDtm, chgUserId, chgDtm;
    public String getServerId() { return serverId; } public void setServerId(String v) { serverId = v; }
    public String getServerName() { return serverName; } public void setServerName(String v) { serverName = v; }
    public String getTechRole() { return techRole; } public void setTechRole(String v) { techRole = v; }
    public String getEnvCd() { return envCd; } public void setEnvCd(String v) { envCd = v; }
    public String getTierCd() { return tierCd; } public void setTierCd(String v) { tierCd = v; }
    public String getStatusCd() { return statusCd; } public void setStatusCd(String v) { statusCd = v; }
    public String getRemark() { return remark; } public void setRemark(String v) { remark = v; }
    public String getRegUserId() { return regUserId; } public void setRegUserId(String v) { regUserId = v; }
    public String getRegDtm() { return regDtm; } public void setRegDtm(String v) { regDtm = v; }
    public String getChgUserId() { return chgUserId; } public void setChgUserId(String v) { chgUserId = v; }
    public String getChgDtm() { return chgDtm; } public void setChgDtm(String v) { chgDtm = v; }
    @Override public Object clone() { ifina1999S0DTOSub0 c = new ifina1999S0DTOSub0(); c.clone(this); return c; }
    public void clone(DataObject src) {
        if (this == src) return;
        ifina1999S0DTOSub0 in = (ifina1999S0DTOSub0) src;
        serverId=in.serverId; serverName=in.serverName; techRole=in.techRole; envCd=in.envCd; tierCd=in.tierCd;
        statusCd=in.statusCd; remark=in.remark; regUserId=in.regUserId; regDtm=in.regDtm; chgUserId=in.chgUserId; chgDtm=in.chgDtm;
    }
    private static final Map<String, FieldProperty> fieldPropertyMap = new LinkedHashMap<>();
    static {
        for (String n : new String[]{"serverId","serverName","techRole","envCd","tierCd","statusCd","remark","regUserId","regDtm","chgUserId","chgDtm"}) {
            fieldPropertyMap.put(n, FieldProperty.builder().setPhysicalName(n).setLogicalName(n)
                .setType(FieldProperty.TYPE_OBJECT_STRING).setDecimal(-1).setIsNullable(true).setIsEncrypt(false).build());
        }
    }
    public Map<String, FieldProperty> getFieldPropertyMap() { return Collections.unmodifiableMap(fieldPropertyMap); }
}

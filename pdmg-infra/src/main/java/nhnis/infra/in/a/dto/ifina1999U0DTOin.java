package nhnis.infra.in.a.dto;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.ims.superspring.dto.DataObject;
import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;

public class ifina1999U0DTOin extends DataObject {

    private static final long serialVersionUID = 1L;

    private String serverId;
    private String serverName;
    private String techRole;
    private String envCd;
    private String tierCd;
    private String statusCd;
    private String remark;
    private String chgUserId;

    public String getServerId() { return serverId; }
    public void setServerId(String serverId) { this.serverId = serverId; }
    public String getServerName() { return serverName; }
    public void setServerName(String serverName) { this.serverName = serverName; }
    public String getTechRole() { return techRole; }
    public void setTechRole(String techRole) { this.techRole = techRole; }
    public String getEnvCd() { return envCd; }
    public void setEnvCd(String envCd) { this.envCd = envCd; }
    public String getTierCd() { return tierCd; }
    public void setTierCd(String tierCd) { this.tierCd = tierCd; }
    public String getStatusCd() { return statusCd; }
    public void setStatusCd(String statusCd) { this.statusCd = statusCd; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public String getChgUserId() { return chgUserId; }
    public void setChgUserId(String chgUserId) { this.chgUserId = chgUserId; }

    @Override
    public Object clone() {
        ifina1999U0DTOin copy = new ifina1999U0DTOin();
        copy.clone(this);
        return copy;
    }

    public void clone(DataObject src) {
        if (this == src) {
            return;
        }
        ifina1999U0DTOin in = (ifina1999U0DTOin) src;
        this.serverId = in.serverId;
        this.serverName = in.serverName;
        this.techRole = in.techRole;
        this.envCd = in.envCd;
        this.tierCd = in.tierCd;
        this.statusCd = in.statusCd;
        this.remark = in.remark;
        this.chgUserId = in.chgUserId;
    }

    private static final Map<String, FieldProperty> fieldPropertyMap = new LinkedHashMap<>();

    static {
        for (String n : new String[] {
                "serverId", "serverName", "techRole", "envCd", "tierCd", "statusCd", "remark", "chgUserId"
        }) {
            fieldPropertyMap.put(n, FieldProperty.builder()
                    .setPhysicalName(n).setLogicalName(n)
                    .setType(FieldProperty.TYPE_OBJECT_STRING).setDecimal(-1)
                    .setIsNullable(true).setIsEncrypt(false).build());
        }
    }

    public Map<String, FieldProperty> getFieldPropertyMap() {
        return Collections.unmodifiableMap(fieldPropertyMap);
    }
}

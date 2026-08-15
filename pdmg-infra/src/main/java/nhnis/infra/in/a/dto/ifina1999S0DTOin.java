package nhnis.infra.in.a.dto;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import com.ims.superspring.dto.DataObject;
import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;

public class ifina1999S0DTOin extends DataObject {
    private static final long serialVersionUID = 1L;
    private String keyword, serverId, serverName, techRole, envCd, statusCd;
    private Integer pageNo, pageSize;
    public String getKeyword() { return keyword; } public void setKeyword(String v) { keyword = v; }
    public String getServerId() { return serverId; } public void setServerId(String v) { serverId = v; }
    public String getServerName() { return serverName; } public void setServerName(String v) { serverName = v; }
    public String getTechRole() { return techRole; } public void setTechRole(String v) { techRole = v; }
    public String getEnvCd() { return envCd; } public void setEnvCd(String v) { envCd = v; }
    public String getStatusCd() { return statusCd; } public void setStatusCd(String v) { statusCd = v; }
    public Integer getPageNo() { return pageNo; } public void setPageNo(Integer v) { pageNo = v; }
    public Integer getPageSize() { return pageSize; } public void setPageSize(Integer v) { pageSize = v; }
    @Override public Object clone() { ifina1999S0DTOin c = new ifina1999S0DTOin(); c.clone(this); return c; }
    public void clone(DataObject src) {
        if (this == src) return;
        ifina1999S0DTOin in = (ifina1999S0DTOin) src;
        keyword=in.keyword; serverId=in.serverId; serverName=in.serverName; techRole=in.techRole;
        envCd=in.envCd; statusCd=in.statusCd; pageNo=in.pageNo; pageSize=in.pageSize;
    }
    private static final Map<String, FieldProperty> fieldPropertyMap = new LinkedHashMap<>();
    static {
        for (String n : new String[]{"keyword","serverId","serverName","techRole","envCd","statusCd"}) {
            fieldPropertyMap.put(n, FieldProperty.builder().setPhysicalName(n).setLogicalName(n)
                .setType(FieldProperty.TYPE_OBJECT_STRING).setDecimal(-1).setIsNullable(true).setIsEncrypt(false).build());
        }
        for (String n : new String[]{"pageNo","pageSize"}) {
            fieldPropertyMap.put(n, FieldProperty.builder().setPhysicalName(n).setLogicalName(n)
                .setType(FieldProperty.TYPE_OBJECT_INT).setDecimal(-1).setIsNullable(true).setIsEncrypt(false).build());
        }
    }
    public Map<String, FieldProperty> getFieldPropertyMap() { return Collections.unmodifiableMap(fieldPropertyMap); }
}

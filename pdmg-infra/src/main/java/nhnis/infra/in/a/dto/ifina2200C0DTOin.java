package nhnis.infra.in.a.dto;
import java.util.Collections; import java.util.LinkedHashMap; import java.util.Map;
import com.ims.superspring.dto.DataObject; import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;
public class ifina2200C0DTOin extends DataObject {
    private static final long serialVersionUID = 1L;
    private String appId, appName, systemId, langCd, statusCd, remark, regUserId;
    public String getAppId(){return appId;} public void setAppId(String v){appId=v;}
    public String getAppName(){return appName;} public void setAppName(String v){appName=v;}
    public String getSystemId(){return systemId;} public void setSystemId(String v){systemId=v;}
    public String getLangCd(){return langCd;} public void setLangCd(String v){langCd=v;}
    public String getStatusCd(){return statusCd;} public void setStatusCd(String v){statusCd=v;}
    public String getRemark(){return remark;} public void setRemark(String v){remark=v;}
    public String getRegUserId(){return regUserId;} public void setRegUserId(String v){ regUserId=v; }
    @Override public Object clone(){ ifina2200C0DTOin c=new ifina2200C0DTOin(); c.clone(this); return c; }
    public void clone(DataObject src){ if(this==src)return; ifina2200C0DTOin in=(ifina2200C0DTOin)src;
      appId=in.appId; appName=in.appName; systemId=in.systemId; langCd=in.langCd; statusCd=in.statusCd; remark=in.remark; regUserId=in.regUserId; }
    private static final Map<String, FieldProperty> fieldPropertyMap = new LinkedHashMap<>();
    static { for(String n: new String[]{"appId","appName","systemId","langCd","statusCd","remark","regUserId"})
      fieldPropertyMap.put(n, FieldProperty.builder().setPhysicalName(n).setLogicalName(n).setType(FieldProperty.TYPE_OBJECT_STRING).setDecimal(-1).setIsNullable(true).setIsEncrypt(false).build()); }
    public Map<String, FieldProperty> getFieldPropertyMap(){ return Collections.unmodifiableMap(fieldPropertyMap); }
}

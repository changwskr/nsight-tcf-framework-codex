package nhnis.infra.in.a.dto;

import java.util.*;
import com.ims.superspring.dto.DataObject;
import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;

public class ifina1600C0DTOin extends DataObject {
    private static final long serialVersionUID = 1L;
    private String evidenceId, targetTypeCd, targetId, gateId, fileName, fileUri, url, remark;
    /** 실파일 Base64 (data URL prefix 허용). 없으면 메타만 등록. */
    private String fileContentBase64;
    public String getEvidenceId(){return evidenceId;} public void setEvidenceId(String v){evidenceId=v;}
    public String getTargetTypeCd(){return targetTypeCd;} public void setTargetTypeCd(String v){targetTypeCd=v;}
    public String getTargetId(){return targetId;} public void setTargetId(String v){targetId=v;}
    public String getGateId(){return gateId;} public void setGateId(String v){gateId=v;}
    public String getFileName(){return fileName;} public void setFileName(String v){fileName=v;}
    public String getFileUri(){return fileUri;} public void setFileUri(String v){fileUri=v;}
    public String getUrl(){return url;} public void setUrl(String v){url=v;}
    public String getRemark(){return remark;} public void setRemark(String v){remark=v;}
    public String getFileContentBase64(){return fileContentBase64;} public void setFileContentBase64(String v){fileContentBase64=v;}
    @Override public Object clone(){ ifina1600C0DTOin c=new ifina1600C0DTOin(); c.clone(this); return c; }
    public void clone(DataObject src){ if(this==src)return; ifina1600C0DTOin in=(ifina1600C0DTOin)src;
      evidenceId=in.evidenceId; targetTypeCd=in.targetTypeCd; targetId=in.targetId; gateId=in.gateId;
      fileName=in.fileName; fileUri=in.fileUri; url=in.url; remark=in.remark;
      fileContentBase64=in.fileContentBase64; }
    private static final Map<String, FieldProperty> fieldPropertyMap = new LinkedHashMap<>();
    static { fieldPropertyMap.put("evidenceId", FieldProperty.builder().setPhysicalName("evidenceId").setLogicalName("evidenceId").setType(FieldProperty.TYPE_OBJECT_STRING).setDecimal(-1).setIsNullable(true).setIsEncrypt(false).build()); }
    public Map<String, FieldProperty> getFieldPropertyMap(){ return Collections.unmodifiableMap(fieldPropertyMap); }
}

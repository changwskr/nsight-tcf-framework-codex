package nhnis.infra.in.a.dto;

import java.util.*;
import com.ims.superspring.dto.DataObject;
import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;

public class ifina1600C0DTOout extends DataObject {
    private static final long serialVersionUID = 1L;
    private Integer PROC_CNT;
    private Long sizeBytes;
    private String evidenceId, fileName, fileUri, downloadUri, RSLT_CD, RSLT_MSG;
    public Integer getPROC_CNT(){return PROC_CNT;} public void setPROC_CNT(Integer v){PROC_CNT=v;}
    public Long getSizeBytes(){return sizeBytes;} public void setSizeBytes(Long v){sizeBytes=v;}
    public String getEvidenceId(){return evidenceId;} public void setEvidenceId(String v){evidenceId=v;}
    public String getFileName(){return fileName;} public void setFileName(String v){fileName=v;}
    public String getFileUri(){return fileUri;} public void setFileUri(String v){fileUri=v;}
    public String getDownloadUri(){return downloadUri;} public void setDownloadUri(String v){downloadUri=v;}
    public String getRSLT_CD(){return RSLT_CD;} public void setRSLT_CD(String v){RSLT_CD=v;}
    public String getRSLT_MSG(){return RSLT_MSG;} public void setRSLT_MSG(String v){RSLT_MSG=v;}
    @Override public Object clone(){ ifina1600C0DTOout c=new ifina1600C0DTOout(); c.clone(this); return c; }
    public void clone(DataObject src){ if(this==src)return; ifina1600C0DTOout in=(ifina1600C0DTOout)src;
      PROC_CNT=in.PROC_CNT; sizeBytes=in.sizeBytes; evidenceId=in.evidenceId; fileName=in.fileName;
      fileUri=in.fileUri; downloadUri=in.downloadUri; RSLT_CD=in.RSLT_CD; RSLT_MSG=in.RSLT_MSG; }
    private static final Map<String, FieldProperty> fieldPropertyMap = new LinkedHashMap<>();
    static { fieldPropertyMap.put("RSLT_CD", FieldProperty.builder().setPhysicalName("RSLT_CD").setLogicalName("RSLT_CD").setType(FieldProperty.TYPE_OBJECT_STRING).setDecimal(-1).setIsNullable(true).setIsEncrypt(false).build()); }
    public Map<String, FieldProperty> getFieldPropertyMap(){ return Collections.unmodifiableMap(fieldPropertyMap); }
}

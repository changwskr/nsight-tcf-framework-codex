package nhnis.infra.in.a.dto;

import java.util.*;
import com.ims.superspring.dto.DataObject;
import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;

public class ifina9400E0DTOout extends DataObject {
    private static final long serialVersionUID = 1L;
    private Integer tableId, rowCount, tableCount;
    private String tableName, formatCd, fileName, downloadUri, RSLT_CD, RSLT_MSG;
    private List<Integer> exportedTableIds;
    public Integer getTableId(){return tableId;} public void setTableId(Integer v){tableId=v;}
    public String getTableName(){return tableName;} public void setTableName(String v){tableName=v;}
    public String getFormatCd(){return formatCd;} public void setFormatCd(String v){formatCd=v;}
    public String getFileName(){return fileName;} public void setFileName(String v){fileName=v;}
    public String getDownloadUri(){return downloadUri;} public void setDownloadUri(String v){downloadUri=v;}
    public Integer getRowCount(){return rowCount;} public void setRowCount(Integer v){rowCount=v;}
    public Integer getTableCount(){return tableCount;} public void setTableCount(Integer v){tableCount=v;}
    public List<Integer> getExportedTableIds(){return exportedTableIds;} public void setExportedTableIds(List<Integer> v){exportedTableIds=v;}
    public String getRSLT_CD(){return RSLT_CD;} public void setRSLT_CD(String v){RSLT_CD=v;}
    public String getRSLT_MSG(){return RSLT_MSG;} public void setRSLT_MSG(String v){RSLT_MSG=v;}
    @Override public Object clone(){ ifina9400E0DTOout c=new ifina9400E0DTOout(); c.clone(this); return c; }
    public void clone(DataObject src){ if(this==src)return; ifina9400E0DTOout in=(ifina9400E0DTOout)src;
      tableId=in.tableId; tableName=in.tableName; formatCd=in.formatCd; fileName=in.fileName;
      downloadUri=in.downloadUri; rowCount=in.rowCount; tableCount=in.tableCount;
      exportedTableIds=in.exportedTableIds==null?null:new ArrayList<>(in.exportedTableIds);
      RSLT_CD=in.RSLT_CD; RSLT_MSG=in.RSLT_MSG; }
    private static final Map<String, FieldProperty> fieldPropertyMap = new LinkedHashMap<>();
    static { fieldPropertyMap.put("RSLT_CD", FieldProperty.builder().setPhysicalName("RSLT_CD").setLogicalName("RSLT_CD").setType(FieldProperty.TYPE_OBJECT_STRING).setDecimal(-1).setIsNullable(true).setIsEncrypt(false).build()); }
    public Map<String, FieldProperty> getFieldPropertyMap(){ return Collections.unmodifiableMap(fieldPropertyMap); }
}

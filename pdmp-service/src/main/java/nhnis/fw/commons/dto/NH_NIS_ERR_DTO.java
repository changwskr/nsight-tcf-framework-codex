package nhnis.fw.commons.dto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.ims.superspring.dto.DataObject;
import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;

@jakarta.annotation.Generated(
        value = "com.imssoft.sts4.codegen.dto.DtoGenerator",
        date = "26. 7. 27. 오후 3:03",
        comments = "NHBASE EXCEPTION DTO"
)
public class NH_NIS_ERR_DTO extends DataObject {
    private static final long serialVersionUID = 1L;

    private String stdErrCode = null;

    public String getStdErrCode() {
        return stdErrCode;
    }

    public void setStdErrCode(String stdErrCode) {
        if (stdErrCode == null) {
            this.stdErrCode = null;
        } else {
            this.stdErrCode = stdErrCode;
        }
    }

    private String stdErrMsgCntn = null;

    public String getStdErrMsgCntn() {
        return stdErrMsgCntn;
    }

    public void setStdErrMsgCntn(String stdErrMsgCntn) {
        if (stdErrMsgCntn == null) {
            this.stdErrMsgCntn = null;
        } else {
            this.stdErrMsgCntn = stdErrMsgCntn;
        }
    }

    private String addMsgContents = null;

    public String getAddMsgContents() {
        return addMsgContents;
    }

    public void setAddMsgContents(String addMsgContents) {
        if (addMsgContents == null) {
            this.addMsgContents = null;
        } else {
            this.addMsgContents = addMsgContents;
        }
    }

    private String errClassName = null;

    public String getErrClassName() {
        return errClassName;
    }

    public void setErrClassName(String errClassName) {
        if (errClassName == null) {
            this.errClassName = null;
        } else {
            this.errClassName = errClassName;
        }
    }

    private String errFileName = null;

    public String getErrFileName() {
        return errFileName;
    }

    public void setErrFileName(String errFileName) {
        if (errFileName == null) {
            this.errFileName = null;
        } else {
            this.errFileName = errFileName;
        }
    }

    private String errMethodName = null;

    public String getErrMethodName() {
        return errMethodName;
    }

    public void setErrMethodName(String errMethodName) {
        if (errMethodName == null) {
            this.errMethodName = null;
        } else {
            this.errMethodName = errMethodName;
        }
    }

    private int errLineNo = 0;

    public int getErrLineNo() {
        return errLineNo;
    }

    public void setErrLineNo(int errLineNo) {
        this.errLineNo = errLineNo;
    }

    public void setErrLineNo(Integer errLineNo) {
        if (errLineNo == null) {
            this.errLineNo = 0;
        } else {
            this.errLineNo = errLineNo.intValue();
        }
    }

    public void setErrLineNo(String errLineNo) {
        if (errLineNo == null || errLineNo.length() == 0) {
            this.errLineNo = 0;
        } else {
            this.errLineNo = Integer.parseInt(errLineNo);
        }
    }

    private String errType = null;

    public String getErrType() {
        return errType;
    }

    public void setErrType(String errType) {
        if (errType == null) {
            this.errType = null;
        } else {
            this.errType = errType;
        }
    }

    private List<String> stackTrace = null;

    public List<String> getStackTraceList() {
        if (stackTrace == null)
            return null;
        return this.stackTrace;
    }

    public void clearStackTrace() {
        if (stackTrace != null)
            stackTrace.clear();
    }

    public void ensureCapacityStackTrace(int minCapacity) {
        if (stackTrace == null)
            stackTrace = new ArrayList<String>(minCapacity);
        else
            ((ArrayList<String>) stackTrace).ensureCapacity(minCapacity);
    }

    public NH_NIS_ERR_DTO fillStackTrace(int _index) {
        ensureCapacityStackTrace(_index + 1);
        for (int i = sizeStackTrace(); i < _index + 1; i++) {
            this.stackTrace.add(i, null);
        }
        return this;
    }

    public int sizeStackTrace() {
        if (stackTrace == null)
            return 0;

        return stackTrace.size();
    }

    public List<String> getStackTrace() {
        if (this.stackTrace == null)
            return null;

        return this.stackTrace;
    }

    public String[] getStackTraceArray() {
        if (this.stackTrace == null)
            return null;

        return this.stackTrace.toArray(new String[stackTrace.size()]);
    }

    public String getStackTrace(int _index) {
        return stackTrace.get(_index);
    }

    public void setStackTrace(List<String> stackTrace) {
        if (stackTrace == null) {
            this.stackTrace = null;
        } else {
            this.stackTrace = stackTrace;
        }
    }

    public void setStackTraceArray(String[] stackTraceArray) {
        if (stackTraceArray == null) {
            this.stackTrace = null;
        } else {
            this.stackTrace = Arrays.asList(stackTraceArray);
        }
    }

    public void addStackTrace(int _index, String stackTrace) {
        if (this.stackTrace == null)
            this.stackTrace = new ArrayList<String>((int) 15);
        if (stackTrace == null) {
            this.stackTrace.add(_index, null);
        } else {
            this.stackTrace.add(_index, stackTrace);
        }
    }

    public void addStackTrace(String stackTrace) {
        if (this.stackTrace == null)
            this.stackTrace = new ArrayList<String>((int) 15);
        if (stackTrace == null) {
            this.stackTrace.add(null);
        } else {
            this.stackTrace.add(stackTrace);
        }
    }

    public Object clone() {
        NH_NIS_ERR_DTO copyObj = new NH_NIS_ERR_DTO();
        copyObj.clone(this);
        return copyObj;
    }

    public void clone(DataObject _nH_NIS_ERR_DTO) {
        if (this == _nH_NIS_ERR_DTO)
            return;

        NH_NIS_ERR_DTO __nH_NIS_ERR_DTO = (NH_NIS_ERR_DTO) _nH_NIS_ERR_DTO;
        this.setStdErrCode(__nH_NIS_ERR_DTO.getStdErrCode());
        this.setStdErrMsgCntn(__nH_NIS_ERR_DTO.getStdErrMsgCntn());
        this.setAddMsgContents(__nH_NIS_ERR_DTO.getAddMsgContents());
        this.setErrClassName(__nH_NIS_ERR_DTO.getErrClassName());
        this.setErrFileName(__nH_NIS_ERR_DTO.getErrFileName());
        this.setErrMethodName(__nH_NIS_ERR_DTO.getErrMethodName());
        this.setErrLineNo(__nH_NIS_ERR_DTO.getErrLineNo());
        this.setErrType(__nH_NIS_ERR_DTO.getErrType());
        this.clearStackTrace();
        if (this.getStackTrace() == null && __nH_NIS_ERR_DTO.getStackTrace() != null)
            this.ensureCapacityStackTrace(__nH_NIS_ERR_DTO.sizeStackTrace());

        for (int index = 0; index < __nH_NIS_ERR_DTO.sizeStackTrace(); index++) {
            this.addStackTrace(index, __nH_NIS_ERR_DTO.getStackTrace(index));
        }
    }

    public String toString() {
        StringBuilder buffer = new StringBuilder();

        buffer.append("stdErrCode : ").append(stdErrCode).append("\n");
        buffer.append("stdErrMsgCntn : ").append(stdErrMsgCntn).append("\n");
        buffer.append("addMsgContents : ").append(addMsgContents).append("\n");
        buffer.append("errClassName : ").append(errClassName).append("\n");
        buffer.append("errFileName : ").append(errFileName).append("\n");
        buffer.append("errMethodName : ").append(errMethodName).append("\n");
        buffer.append("errLineNo : ").append(errLineNo).append("\n");
        buffer.append("errType : ").append(errType).append("\n");
        buffer.append("stackTrace[");
        for (int index = 0; index < sizeStackTrace(); index++) {
            buffer.append("(").append(index).append(")").append(getStackTrace(index)).append(" ");
        }
        buffer.append("]");
        buffer.append("\n");
        return buffer.toString();
    }

    private static Map<String, FieldProperty> fieldPropertyMap;

    static {
        fieldPropertyMap = new java.util.LinkedHashMap<String, FieldProperty>(9);
        fieldPropertyMap.put("stdErrCode", FieldProperty.builder()
                .setPhysicalName("stdErrCode")
                .setLogicalName("stdErrCode")
                .setType(FieldProperty.TYPE_OBJECT_STRING)
                .setDecimal(-1)
                .setIsNullable(true)
                .setIsEncrypt(false)
                .build());
        fieldPropertyMap.put("stdErrMsgCntn", FieldProperty.builder()
                .setPhysicalName("stdErrMsgCntn")
                .setLogicalName("stdErrMsgCntn")
                .setType(FieldProperty.TYPE_OBJECT_STRING)
                .setDecimal(-1)
                .setIsNullable(true)
                .setIsEncrypt(false)
                .build());
        fieldPropertyMap.put("addMsgContents", FieldProperty.builder()
                .setPhysicalName("addMsgContents")
                .setLogicalName("addMsgContents")
                .setType(FieldProperty.TYPE_OBJECT_STRING)
                .setDecimal(-1)
                .setIsNullable(true)
                .setIsEncrypt(false)
                .build());
        fieldPropertyMap.put("errClassName", FieldProperty.builder()
                .setPhysicalName("errClassName")
                .setLogicalName("errClassName")
                .setType(FieldProperty.TYPE_OBJECT_STRING)
                .setDecimal(-1)
                .setIsNullable(true)
                .setIsEncrypt(false)
                .build());
        fieldPropertyMap.put("errFileName", FieldProperty.builder()
                .setPhysicalName("errFileName")
                .setLogicalName("errFileName")
                .setType(FieldProperty.TYPE_OBJECT_STRING)
                .setDecimal(-1)
                .setIsNullable(true)
                .setIsEncrypt(false)
                .build());
        fieldPropertyMap.put("errMethodName", FieldProperty.builder()
                .setPhysicalName("errMethodName")
                .setLogicalName("errMethodName")
                .setType(FieldProperty.TYPE_OBJECT_STRING)
                .setDecimal(-1)
                .setIsNullable(true)
                .setIsEncrypt(false)
                .build());
        fieldPropertyMap.put("errLineNo", FieldProperty.builder()
                .setPhysicalName("errLineNo")
                .setLogicalName("errLineNo")
                .setType(FieldProperty.TYPE_OBJECT_INT)
                .setDecimal(-1)
                .setIsNullable(true)
                .setIsEncrypt(false)
                .build());
        fieldPropertyMap.put("errType", FieldProperty.builder()
                .setPhysicalName("errType")
                .setLogicalName("errType")
                .setType(FieldProperty.TYPE_OBJECT_STRING)
                .setDecimal(-1)
                .setIsNullable(true)
                .setIsEncrypt(false)
                .build());
        fieldPropertyMap.put("stackTrace", FieldProperty.builder()
                .setPhysicalName("stackTrace")
                .setLogicalName("stackTrace")
                .setType(FieldProperty.TYPE_OBJECT_STRING)
                .setDecimal(-1)
                .setIsNullable(true)
                .setIsEncrypt(false)
                .build());
    }

    public Map<String, FieldProperty> getFieldPropertyMap() {
        return Collections.unmodifiableMap(fieldPropertyMap);
    }
}

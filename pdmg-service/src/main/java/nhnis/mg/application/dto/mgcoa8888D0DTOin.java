package nhnis.mg.application.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ims.superspring.dto.DataObject;
import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;

/**
 * 이미지로그 삭제 입력 (mgcoa8888D0).
 */
public class mgcoa8888D0DTOin extends DataObject {

    private static final long serialVersionUID = 1L;

    @JsonProperty("guidList")
    @JsonAlias({ "GUID_LIST", "guid_LIST", "guid_list" })
    private List<String> guidList = new ArrayList<>();

    public List<String> getGuidList() {
        return guidList;
    }

    public void setGuidList(List<String> guidList) {
        this.guidList = guidList != null ? guidList : new ArrayList<>();
    }

    /** 기존 전문 키 호환 (Jackson 직렬화에서는 제외). */
    @JsonIgnore
    public List<String> getGUID_LIST() {
        return getGuidList();
    }

    @JsonIgnore
    public void setGUID_LIST(List<String> guidList) {
        setGuidList(guidList);
    }

    @Override
    public Object clone() {
        mgcoa8888D0DTOin copy = new mgcoa8888D0DTOin();
        copy.clone(this);
        return copy;
    }

    public void clone(DataObject src) {
        if (this == src) {
            return;
        }
        mgcoa8888D0DTOin in = (mgcoa8888D0DTOin) src;
        this.guidList = in.guidList == null ? new ArrayList<>() : new ArrayList<>(in.guidList);
    }

    @Override
    public String toString() {
        return "guidList : " + guidList + "\n";
    }

    private static final Map<String, FieldProperty> fieldPropertyMap;

    static {
        fieldPropertyMap = new LinkedHashMap<>();
        fieldPropertyMap.put("guidList", FieldProperty.builder()
                .setPhysicalName("guidList").setLogicalName("guidList")
                .setType(FieldProperty.TYPE_OBJECT_STRING).setDecimal(-1)
                .setIsNullable(true).setIsEncrypt(false).build());
    }

    public Map<String, FieldProperty> getFieldPropertyMap() {
        return Collections.unmodifiableMap(fieldPropertyMap);
    }
}

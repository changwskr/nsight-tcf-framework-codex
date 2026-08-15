package nhnis.infra.in.a.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ims.superspring.dto.DataObject;
import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;

public class ifina1999D0DTOin extends DataObject {

    private static final long serialVersionUID = 1L;

    @JsonProperty("serverIdList")
    @JsonAlias({ "SERVER_ID_LIST", "server_id_list" })
    private List<String> serverIdList = new ArrayList<>();

    public List<String> getServerIdList() {
        return serverIdList;
    }

    public void setServerIdList(List<String> serverIdList) {
        this.serverIdList = serverIdList != null ? serverIdList : new ArrayList<>();
    }

    @Override
    public Object clone() {
        ifina1999D0DTOin copy = new ifina1999D0DTOin();
        copy.clone(this);
        return copy;
    }

    public void clone(DataObject src) {
        if (this == src) {
            return;
        }
        ifina1999D0DTOin in = (ifina1999D0DTOin) src;
        this.serverIdList = in.serverIdList == null ? new ArrayList<>() : new ArrayList<>(in.serverIdList);
    }

    private static final Map<String, FieldProperty> fieldPropertyMap = new LinkedHashMap<>();

    static {
        fieldPropertyMap.put("serverIdList", FieldProperty.builder()
                .setPhysicalName("serverIdList").setLogicalName("serverIdList")
                .setType(FieldProperty.TYPE_OBJECT_STRING).setDecimal(-1)
                .setIsNullable(true).setIsEncrypt(false).build());
    }

    public Map<String, FieldProperty> getFieldPropertyMap() {
        return Collections.unmodifiableMap(fieldPropertyMap);
    }
}

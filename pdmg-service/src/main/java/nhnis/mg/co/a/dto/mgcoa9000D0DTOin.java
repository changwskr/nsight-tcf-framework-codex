package nhnis.mg.co.a.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ims.superspring.dto.DataObject;
import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;

/**
 * 거래 파라미터 삭제 입력 (mgcoa9000D0).
 */
public class mgcoa9000D0DTOin extends DataObject {

    private static final long serialVersionUID = 1L;

    @JsonProperty("txIdList")
    @JsonAlias({ "TX_ID_LIST", "txId_LIST", "tx_id_list" })
    private List<String> txIdList = new ArrayList<>();

    public List<String> getTxIdList() {
        return txIdList;
    }

    public void setTxIdList(List<String> txIdList) {
        this.txIdList = txIdList != null ? txIdList : new ArrayList<>();
    }

    @Override
    public Object clone() {
        mgcoa9000D0DTOin copy = new mgcoa9000D0DTOin();
        copy.clone(this);
        return copy;
    }

    public void clone(DataObject src) {
        if (this == src) {
            return;
        }
        mgcoa9000D0DTOin in = (mgcoa9000D0DTOin) src;
        this.txIdList = in.txIdList == null ? new ArrayList<>() : new ArrayList<>(in.txIdList);
    }

    @Override
    public String toString() {
        return "txIdList : " + txIdList + "\n";
    }

    private static final Map<String, FieldProperty> fieldPropertyMap;

    static {
        fieldPropertyMap = new LinkedHashMap<>();
        fieldPropertyMap.put("txIdList", FieldProperty.builder()
                .setPhysicalName("txIdList").setLogicalName("txIdList")
                .setType(FieldProperty.TYPE_OBJECT_STRING).setDecimal(-1)
                .setIsNullable(true).setIsEncrypt(false).build());
    }

    public Map<String, FieldProperty> getFieldPropertyMap() {
        return Collections.unmodifiableMap(fieldPropertyMap);
    }
}

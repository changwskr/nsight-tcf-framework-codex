package nhnis.infra.in.a.dto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.ims.superspring.dto.DataObject;
import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;

public class ifina1999S0DTOout extends DataObject {

    private static final long serialVersionUID = 1L;

    private List<ifina1999S0DTOSub0> ifina1999S0DTOSub0 = null;
    private int size = 0;
    private int pageNo;
    private int pageSize;
    private long totalCount;
    private int totalPages;

    public int sizeifina1999S0DTOSub0() {
        return ifina1999S0DTOSub0 == null ? 0 : ifina1999S0DTOSub0.size();
    }

    public List<ifina1999S0DTOSub0> getifina1999S0DTOSub0() {
        return ifina1999S0DTOSub0;
    }

    public ifina1999S0DTOSub0 getifina1999S0DTOSub0(int index) {
        return ifina1999S0DTOSub0.get(index);
    }

    public void setifina1999S0DTOSub0(List<ifina1999S0DTOSub0> list) {
        this.ifina1999S0DTOSub0 = list;
    }

    public void addifina1999S0DTOSub0(ifina1999S0DTOSub0 item) {
        if (ifina1999S0DTOSub0 == null) {
            ifina1999S0DTOSub0 = new ArrayList<>();
        }
        ifina1999S0DTOSub0.add(item);
    }

    public void clearifina1999S0DTOSub0() {
        if (ifina1999S0DTOSub0 != null) {
            ifina1999S0DTOSub0.clear();
        }
    }

    public void ensureCapacityifina1999S0DTOSub0(int minCapacity) {
        if (ifina1999S0DTOSub0 == null) {
            ifina1999S0DTOSub0 = new ArrayList<>(minCapacity);
        } else {
            ((ArrayList<ifina1999S0DTOSub0>) ifina1999S0DTOSub0).ensureCapacity(minCapacity);
        }
    }

    public ifina1999S0DTOSub0[] getifina1999S0DTOSub0Array() {
        return ifina1999S0DTOSub0 == null ? null : ifina1999S0DTOSub0.toArray(new ifina1999S0DTOSub0[0]);
    }

    public void setifina1999S0DTOSub0Array(ifina1999S0DTOSub0[] array) {
        this.ifina1999S0DTOSub0 = array == null ? null : Arrays.asList(array);
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public void setSize(Integer size) {
        this.size = size == null ? 0 : size;
    }

    public int getPageNo() {
        return pageNo;
    }

    public void setPageNo(int pageNo) {
        this.pageNo = pageNo;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(long totalCount) {
        this.totalCount = totalCount;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    @Override
    public Object clone() {
        ifina1999S0DTOout copy = new ifina1999S0DTOout();
        copy.clone(this);
        return copy;
    }

    public void clone(DataObject src) {
        if (this == src) {
            return;
        }
        ifina1999S0DTOout in = (ifina1999S0DTOout) src;
        this.clearifina1999S0DTOSub0();
        if (in.getifina1999S0DTOSub0() != null) {
            this.ensureCapacityifina1999S0DTOSub0(in.sizeifina1999S0DTOSub0());
            for (int i = 0; i < in.sizeifina1999S0DTOSub0(); i++) {
                ifina1999S0DTOSub0 value = in.getifina1999S0DTOSub0(i);
                this.addifina1999S0DTOSub0(value == null ? null : (ifina1999S0DTOSub0) value.clone());
            }
        }
        this.setSize(in.getSize());
        this.pageNo = in.pageNo;
        this.pageSize = in.pageSize;
        this.totalCount = in.totalCount;
        this.totalPages = in.totalPages;
    }

    private static final Map<String, FieldProperty> fieldPropertyMap;

    static {
        fieldPropertyMap = new LinkedHashMap<>();
        fieldPropertyMap.put("ifina1999S0DTOSub0", FieldProperty.builder()
                .setPhysicalName("ifina1999S0DTOSub0").setLogicalName("ifina1999S0DTOSub0")
                .setType(FieldProperty.TYPE_ABSTRACT_INCLUDE).setDecimal(-1).setArray("size")
                .setReference("nhnis.infra.in.a.dto.ifina1999S0DTOSub0")
                .setIsNullable(true).setIsEncrypt(false).build());
        fieldPropertyMap.put("size", FieldProperty.builder()
                .setPhysicalName("size").setLogicalName("size")
                .setType(FieldProperty.TYPE_PRIMITIVE_INT).setDecimal(-1)
                .setIsNullable(true).setIsEncrypt(false).build());
    }

    public Map<String, FieldProperty> getFieldPropertyMap() {
        return Collections.unmodifiableMap(fieldPropertyMap);
    }
}

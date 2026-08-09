package nhnis.mg.application.dto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.ims.superspring.dto.DataObject;
import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;

/**
 * 마케팅희망고객 조회 출력 (mgcoa5530S0).
 */
public class mgcoa5530S0DTOout extends DataObject {

    private static final long serialVersionUID = 1L;

    private List<mgcoa5530S0DTOSub0> mgcoa5530S0DTOSub0 = null;
    private int size = 0;
    private int pageNo;
    private int pageSize;
    private long totalCount;
    private int totalPages;

    public List<mgcoa5530S0DTOSub0> getmgcoa5530S0DTOSub0List() {
        return mgcoa5530S0DTOSub0;
    }

    public void clearmgcoa5530S0DTOSub0() {
        if (mgcoa5530S0DTOSub0 != null) {
            mgcoa5530S0DTOSub0.clear();
        }
    }

    public void ensureCapacitymgcoa5530S0DTOSub0(int minCapacity) {
        if (mgcoa5530S0DTOSub0 == null) {
            mgcoa5530S0DTOSub0 = new ArrayList<>(minCapacity);
        } else {
            ((ArrayList<mgcoa5530S0DTOSub0>) mgcoa5530S0DTOSub0).ensureCapacity(minCapacity);
        }
    }

    public int sizemgcoa5530S0DTOSub0() {
        return mgcoa5530S0DTOSub0 == null ? 0 : mgcoa5530S0DTOSub0.size();
    }

    public List<mgcoa5530S0DTOSub0> getmgcoa5530S0DTOSub0() {
        return mgcoa5530S0DTOSub0;
    }

    public mgcoa5530S0DTOSub0[] getmgcoa5530S0DTOSub0Array() {
        if (mgcoa5530S0DTOSub0 == null) {
            return null;
        }
        return mgcoa5530S0DTOSub0.toArray(new mgcoa5530S0DTOSub0[0]);
    }

    public mgcoa5530S0DTOSub0 getmgcoa5530S0DTOSub0(int index) {
        return mgcoa5530S0DTOSub0.get(index);
    }

    public void setmgcoa5530S0DTOSub0(List<mgcoa5530S0DTOSub0> list) {
        this.mgcoa5530S0DTOSub0 = list;
    }

    public void setmgcoa5530S0DTOSub0Array(mgcoa5530S0DTOSub0[] array) {
        this.mgcoa5530S0DTOSub0 = array == null ? null : Arrays.asList(array);
    }

    public void addmgcoa5530S0DTOSub0(mgcoa5530S0DTOSub0 item) {
        if (mgcoa5530S0DTOSub0 == null) {
            mgcoa5530S0DTOSub0 = new ArrayList<>();
        }
        mgcoa5530S0DTOSub0.add(item);
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
        mgcoa5530S0DTOout copy = new mgcoa5530S0DTOout();
        copy.clone(this);
        return copy;
    }

    public void clone(DataObject src) {
        if (this == src) {
            return;
        }
        mgcoa5530S0DTOout in = (mgcoa5530S0DTOout) src;
        this.clearmgcoa5530S0DTOSub0();
        if (in.getmgcoa5530S0DTOSub0() != null) {
            this.ensureCapacitymgcoa5530S0DTOSub0(in.sizemgcoa5530S0DTOSub0());
            for (int i = 0; i < in.sizemgcoa5530S0DTOSub0(); i++) {
                mgcoa5530S0DTOSub0 value = in.getmgcoa5530S0DTOSub0(i);
                this.addmgcoa5530S0DTOSub0(
                        value == null ? null : (mgcoa5530S0DTOSub0) value.clone());
            }
        }
        this.setSize(in.getSize());
        this.pageNo = in.pageNo;
        this.pageSize = in.pageSize;
        this.totalCount = in.totalCount;
        this.totalPages = in.totalPages;
    }

    @Override
    public String toString() {
        return "mgcoa5530S0DTOSub0 size=" + size
                + " pageNo=" + pageNo + " totalCount=" + totalCount;
    }

    private static final Map<String, FieldProperty> fieldPropertyMap;

    static {
        fieldPropertyMap = new LinkedHashMap<>();
        fieldPropertyMap.put("mgcoa5530S0DTOSub0", FieldProperty.builder()
                .setPhysicalName("mgcoa5530S0DTOSub0")
                .setLogicalName("mgcoa5530S0DTOSub0")
                .setType(FieldProperty.TYPE_ABSTRACT_INCLUDE)
                .setDecimal(-1)
                .setArray("size")
                .setReference("nhnis.mg.application.dto.mgcoa5530S0DTOSub0")
                .setIsNullable(true)
                .setIsEncrypt(false)
                .build());
        fieldPropertyMap.put("size", FieldProperty.builder()
                .setPhysicalName("size")
                .setLogicalName("size")
                .setType(FieldProperty.TYPE_PRIMITIVE_INT)
                .setDecimal(-1)
                .setIsNullable(true)
                .setIsEncrypt(false)
                .build());
    }

    public Map<String, FieldProperty> getFieldPropertyMap() {
        return Collections.unmodifiableMap(fieldPropertyMap);
    }
}

package nhnis.mg.co.a.dto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.ims.superspring.dto.DataObject;
import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;

/**
 * 이미지로그 조회 출력 (mgcoa8888S0).
 */
public class mgcoa8888S0DTOout extends DataObject {

    private static final long serialVersionUID = 1L;

    private List<mgcoa8888S0DTOSub0> mgcoa8888S0DTOSub0 = null;
    private int size = 0;
    private int pageNo;
    private int pageSize;
    private long totalCount;
    private int totalPages;

    public List<mgcoa8888S0DTOSub0> getmgcoa8888S0DTOSub0List() {
        return mgcoa8888S0DTOSub0;
    }

    public void clearmgcoa8888S0DTOSub0() {
        if (mgcoa8888S0DTOSub0 != null) {
            mgcoa8888S0DTOSub0.clear();
        }
    }

    public void ensureCapacitymgcoa8888S0DTOSub0(int minCapacity) {
        if (mgcoa8888S0DTOSub0 == null) {
            mgcoa8888S0DTOSub0 = new ArrayList<>(minCapacity);
        } else {
            ((ArrayList<mgcoa8888S0DTOSub0>) mgcoa8888S0DTOSub0).ensureCapacity(minCapacity);
        }
    }

    public int sizemgcoa8888S0DTOSub0() {
        return mgcoa8888S0DTOSub0 == null ? 0 : mgcoa8888S0DTOSub0.size();
    }

    public List<mgcoa8888S0DTOSub0> getmgcoa8888S0DTOSub0() {
        return mgcoa8888S0DTOSub0;
    }

    public mgcoa8888S0DTOSub0[] getmgcoa8888S0DTOSub0Array() {
        if (mgcoa8888S0DTOSub0 == null) {
            return null;
        }
        return mgcoa8888S0DTOSub0.toArray(new mgcoa8888S0DTOSub0[0]);
    }

    public mgcoa8888S0DTOSub0 getmgcoa8888S0DTOSub0(int index) {
        return mgcoa8888S0DTOSub0.get(index);
    }

    public void setmgcoa8888S0DTOSub0(List<mgcoa8888S0DTOSub0> list) {
        this.mgcoa8888S0DTOSub0 = list;
    }

    public void setmgcoa8888S0DTOSub0Array(mgcoa8888S0DTOSub0[] array) {
        this.mgcoa8888S0DTOSub0 = array == null ? null : Arrays.asList(array);
    }

    public void addmgcoa8888S0DTOSub0(mgcoa8888S0DTOSub0 item) {
        if (mgcoa8888S0DTOSub0 == null) {
            mgcoa8888S0DTOSub0 = new ArrayList<>();
        }
        mgcoa8888S0DTOSub0.add(item);
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
        mgcoa8888S0DTOout copy = new mgcoa8888S0DTOout();
        copy.clone(this);
        return copy;
    }

    public void clone(DataObject src) {
        if (this == src) {
            return;
        }
        mgcoa8888S0DTOout in = (mgcoa8888S0DTOout) src;
        this.clearmgcoa8888S0DTOSub0();
        if (in.getmgcoa8888S0DTOSub0() != null) {
            this.ensureCapacitymgcoa8888S0DTOSub0(in.sizemgcoa8888S0DTOSub0());
            for (int i = 0; i < in.sizemgcoa8888S0DTOSub0(); i++) {
                mgcoa8888S0DTOSub0 value = in.getmgcoa8888S0DTOSub0(i);
                this.addmgcoa8888S0DTOSub0(
                        value == null ? null : (mgcoa8888S0DTOSub0) value.clone());
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
        StringBuilder buffer = new StringBuilder();
        buffer.append("mgcoa8888S0DTOSub0[");
        for (int i = 0; i < sizemgcoa8888S0DTOSub0(); i++) {
            buffer.append("[").append(i).append(" : ")
                    .append(getmgcoa8888S0DTOSub0(i)).append("]");
        }
        buffer.append("]\nsize : ").append(size)
                .append(" pageNo : ").append(pageNo)
                .append(" pageSize : ").append(pageSize)
                .append(" totalCount : ").append(totalCount)
                .append(" totalPages : ").append(totalPages)
                .append("\n");
        return buffer.toString();
    }

    private static final Map<String, FieldProperty> fieldPropertyMap;

    static {
        fieldPropertyMap = new LinkedHashMap<>();
        fieldPropertyMap.put("mgcoa8888S0DTOSub0", FieldProperty.builder()
                .setPhysicalName("mgcoa8888S0DTOSub0")
                .setLogicalName("mgcoa8888S0DTOSub0")
                .setType(FieldProperty.TYPE_ABSTRACT_INCLUDE)
                .setDecimal(-1)
                .setArray("size")
                .setReference("nhnis.mg.co.a.dto.mgcoa8888S0DTOSub0")
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

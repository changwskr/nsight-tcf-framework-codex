package nhnis.mk.co.a.dto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.ims.superspring.dto.DataObject;
import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;

/**
 * 마케팅희망고객 조회 출력 (mkcoa5530S0).
 */
public class mkcoa5530S0DTOout extends DataObject {

    private static final long serialVersionUID = 1L;

    private List<mkcoa5530S0DTOSub0> mkcoa5530S0DTOSub0 = null;
    private int size = 0;
    private int pageNo;
    private int pageSize;
    private long totalCount;
    private int totalPages;

    public List<mkcoa5530S0DTOSub0> getmkcoa5530S0DTOSub0List() {
        return mkcoa5530S0DTOSub0;
    }

    public void clearmkcoa5530S0DTOSub0() {
        if (mkcoa5530S0DTOSub0 != null) {
            mkcoa5530S0DTOSub0.clear();
        }
    }

    public void ensureCapacitymkcoa5530S0DTOSub0(int minCapacity) {
        if (mkcoa5530S0DTOSub0 == null) {
            mkcoa5530S0DTOSub0 = new ArrayList<>(minCapacity);
        } else {
            ((ArrayList<mkcoa5530S0DTOSub0>) mkcoa5530S0DTOSub0).ensureCapacity(minCapacity);
        }
    }

    public int sizemkcoa5530S0DTOSub0() {
        return mkcoa5530S0DTOSub0 == null ? 0 : mkcoa5530S0DTOSub0.size();
    }

    public List<mkcoa5530S0DTOSub0> getmkcoa5530S0DTOSub0() {
        return mkcoa5530S0DTOSub0;
    }

    public mkcoa5530S0DTOSub0[] getmkcoa5530S0DTOSub0Array() {
        if (mkcoa5530S0DTOSub0 == null) {
            return null;
        }
        return mkcoa5530S0DTOSub0.toArray(new mkcoa5530S0DTOSub0[0]);
    }

    public mkcoa5530S0DTOSub0 getmkcoa5530S0DTOSub0(int index) {
        return mkcoa5530S0DTOSub0.get(index);
    }

    public void setmkcoa5530S0DTOSub0(List<mkcoa5530S0DTOSub0> list) {
        this.mkcoa5530S0DTOSub0 = list;
    }

    public void setmkcoa5530S0DTOSub0Array(mkcoa5530S0DTOSub0[] array) {
        this.mkcoa5530S0DTOSub0 = array == null ? null : Arrays.asList(array);
    }

    public void addmkcoa5530S0DTOSub0(mkcoa5530S0DTOSub0 item) {
        if (mkcoa5530S0DTOSub0 == null) {
            mkcoa5530S0DTOSub0 = new ArrayList<>();
        }
        mkcoa5530S0DTOSub0.add(item);
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
        mkcoa5530S0DTOout copy = new mkcoa5530S0DTOout();
        copy.clone(this);
        return copy;
    }

    public void clone(DataObject src) {
        if (this == src) {
            return;
        }
        mkcoa5530S0DTOout in = (mkcoa5530S0DTOout) src;
        this.clearmkcoa5530S0DTOSub0();
        if (in.getmkcoa5530S0DTOSub0() != null) {
            this.ensureCapacitymkcoa5530S0DTOSub0(in.sizemkcoa5530S0DTOSub0());
            for (int i = 0; i < in.sizemkcoa5530S0DTOSub0(); i++) {
                mkcoa5530S0DTOSub0 value = in.getmkcoa5530S0DTOSub0(i);
                this.addmkcoa5530S0DTOSub0(
                        value == null ? null : (mkcoa5530S0DTOSub0) value.clone());
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
        return "mkcoa5530S0DTOSub0 size=" + size
                + " pageNo=" + pageNo + " totalCount=" + totalCount;
    }

    private static final Map<String, FieldProperty> fieldPropertyMap;

    static {
        fieldPropertyMap = new LinkedHashMap<>();
        fieldPropertyMap.put("mkcoa5530S0DTOSub0", FieldProperty.builder()
                .setPhysicalName("mkcoa5530S0DTOSub0")
                .setLogicalName("mkcoa5530S0DTOSub0")
                .setType(FieldProperty.TYPE_ABSTRACT_INCLUDE)
                .setDecimal(-1)
                .setArray("size")
                .setReference("nhnis.mk.co.a.dto.mkcoa5530S0DTOSub0")
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

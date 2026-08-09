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
 * 이미지로그 조회 출력 (mkcoa8888S0).
 */
public class mkcoa8888S0DTOout extends DataObject {

    private static final long serialVersionUID = 1L;

    private List<mkcoa8888S0DTOSub0> mkcoa8888S0DTOSub0 = null;
    private int size = 0;
    private int pageNo;
    private int pageSize;
    private long totalCount;
    private int totalPages;

    public List<mkcoa8888S0DTOSub0> getmkcoa8888S0DTOSub0List() {
        return mkcoa8888S0DTOSub0;
    }

    public void clearmkcoa8888S0DTOSub0() {
        if (mkcoa8888S0DTOSub0 != null) {
            mkcoa8888S0DTOSub0.clear();
        }
    }

    public void ensureCapacitymkcoa8888S0DTOSub0(int minCapacity) {
        if (mkcoa8888S0DTOSub0 == null) {
            mkcoa8888S0DTOSub0 = new ArrayList<>(minCapacity);
        } else {
            ((ArrayList<mkcoa8888S0DTOSub0>) mkcoa8888S0DTOSub0).ensureCapacity(minCapacity);
        }
    }

    public int sizemkcoa8888S0DTOSub0() {
        return mkcoa8888S0DTOSub0 == null ? 0 : mkcoa8888S0DTOSub0.size();
    }

    public List<mkcoa8888S0DTOSub0> getmkcoa8888S0DTOSub0() {
        return mkcoa8888S0DTOSub0;
    }

    public mkcoa8888S0DTOSub0[] getmkcoa8888S0DTOSub0Array() {
        if (mkcoa8888S0DTOSub0 == null) {
            return null;
        }
        return mkcoa8888S0DTOSub0.toArray(new mkcoa8888S0DTOSub0[0]);
    }

    public mkcoa8888S0DTOSub0 getmkcoa8888S0DTOSub0(int index) {
        return mkcoa8888S0DTOSub0.get(index);
    }

    public void setmkcoa8888S0DTOSub0(List<mkcoa8888S0DTOSub0> list) {
        this.mkcoa8888S0DTOSub0 = list;
    }

    public void setmkcoa8888S0DTOSub0Array(mkcoa8888S0DTOSub0[] array) {
        this.mkcoa8888S0DTOSub0 = array == null ? null : Arrays.asList(array);
    }

    public void addmkcoa8888S0DTOSub0(mkcoa8888S0DTOSub0 item) {
        if (mkcoa8888S0DTOSub0 == null) {
            mkcoa8888S0DTOSub0 = new ArrayList<>();
        }
        mkcoa8888S0DTOSub0.add(item);
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
        mkcoa8888S0DTOout copy = new mkcoa8888S0DTOout();
        copy.clone(this);
        return copy;
    }

    public void clone(DataObject src) {
        if (this == src) {
            return;
        }
        mkcoa8888S0DTOout in = (mkcoa8888S0DTOout) src;
        this.clearmkcoa8888S0DTOSub0();
        if (in.getmkcoa8888S0DTOSub0() != null) {
            this.ensureCapacitymkcoa8888S0DTOSub0(in.sizemkcoa8888S0DTOSub0());
            for (int i = 0; i < in.sizemkcoa8888S0DTOSub0(); i++) {
                mkcoa8888S0DTOSub0 value = in.getmkcoa8888S0DTOSub0(i);
                this.addmkcoa8888S0DTOSub0(
                        value == null ? null : (mkcoa8888S0DTOSub0) value.clone());
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
        buffer.append("mkcoa8888S0DTOSub0[");
        for (int i = 0; i < sizemkcoa8888S0DTOSub0(); i++) {
            buffer.append("[").append(i).append(" : ")
                    .append(getmkcoa8888S0DTOSub0(i)).append("]");
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
        fieldPropertyMap.put("mkcoa8888S0DTOSub0", FieldProperty.builder()
                .setPhysicalName("mkcoa8888S0DTOSub0")
                .setLogicalName("mkcoa8888S0DTOSub0")
                .setType(FieldProperty.TYPE_ABSTRACT_INCLUDE)
                .setDecimal(-1)
                .setArray("size")
                .setReference("nhnis.mk.co.a.dto.mkcoa8888S0DTOSub0")
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

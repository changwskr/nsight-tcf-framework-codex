package nhnis.mk.co.a.dto;

import com.ims.superspring.dto.DataObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Collections;
import java.util.Map;
import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;
import java.util.stream.Collectors;

@jakarta.annotation.Generated(
        value = "com.tmaxsoft.sts4.codegen.dto.DtoGenerator",
        date = "26. 7. 24. 오전 10:09",
        comments = "mkcoa9999S0DTOout"
)
public class mkcoa9999S0DTOout extends DataObject
{
    private static final long serialVersionUID = 1L;

    private List<nhnis.mk.co.a.dto.mkcoa9999S0DTOSub0> mkcoa9999S0DTOSub0 = null;

    public List<nhnis.mk.co.a.dto.mkcoa9999S0DTOSub0> getmkcoa9999S0DTOSub0List() {
        if(mkcoa9999S0DTOSub0 == null)
            return null;
        return this.mkcoa9999S0DTOSub0;
    }

    public void clearmkcoa9999S0DTOSub0() {
        if(mkcoa9999S0DTOSub0 != null)
            mkcoa9999S0DTOSub0.clear();
    }

    public void ensureCapacitymkcoa9999S0DTOSub0(int minCapacity) {
        if(mkcoa9999S0DTOSub0 == null)
            mkcoa9999S0DTOSub0 =
                    new ArrayList<nhnis.mk.co.a.dto.mkcoa9999S0DTOSub0>(minCapacity);
        else
            ((ArrayList<nhnis.mk.co.a.dto.mkcoa9999S0DTOSub0>)mkcoa9999S0DTOSub0)
                    .ensureCapacity(minCapacity);
    }

    public mkcoa9999S0DTOout fillmkcoa9999S0DTOSub0(int _index) {
        ensureCapacitymkcoa9999S0DTOSub0(_index + 1);
        for (int i = sizemkcoa9999S0DTOSub0(); i < _index + 1; i++) {
            this.mkcoa9999S0DTOSub0.add(i,
                    new nhnis.mk.co.a.dto.mkcoa9999S0DTOSub0());
        }
        return this;
    }

    public int sizemkcoa9999S0DTOSub0() {
        if(mkcoa9999S0DTOSub0 == null)
            return 0;

        return mkcoa9999S0DTOSub0.size();
    }

    public List<nhnis.mk.co.a.dto.mkcoa9999S0DTOSub0> getmkcoa9999S0DTOSub0() {
        if(this.mkcoa9999S0DTOSub0 == null)
            return null;

        return this.mkcoa9999S0DTOSub0;
    }

    public nhnis.mk.co.a.dto.mkcoa9999S0DTOSub0[] getmkcoa9999S0DTOSub0Array() {
        if(this.mkcoa9999S0DTOSub0 == null)
            return null;

        return this.mkcoa9999S0DTOSub0.toArray(
                new nhnis.mk.co.a.dto.mkcoa9999S0DTOSub0[mkcoa9999S0DTOSub0.size()]);
    }

    public nhnis.mk.co.a.dto.mkcoa9999S0DTOSub0 getmkcoa9999S0DTOSub0(int _index) {
        return (nhnis.mk.co.a.dto.mkcoa9999S0DTOSub0)
                mkcoa9999S0DTOSub0.get(_index);
    }

    public void setmkcoa9999S0DTOSub0(
            List<nhnis.mk.co.a.dto.mkcoa9999S0DTOSub0> mkcoa9999S0DTOSub0) {

        if (mkcoa9999S0DTOSub0 == null) {
            this.mkcoa9999S0DTOSub0 = null;
        } else {
            this.mkcoa9999S0DTOSub0 = mkcoa9999S0DTOSub0;
        }
    }

    public void setmkcoa9999S0DTOSub0Array(
            nhnis.mk.co.a.dto.mkcoa9999S0DTOSub0[] mkcoa9999S0DTOSub0Array) {
        if (mkcoa9999S0DTOSub0Array == null) {
            this.mkcoa9999S0DTOSub0 = null;
        } else {
            this.mkcoa9999S0DTOSub0 = Arrays.asList(mkcoa9999S0DTOSub0Array);
        }
    }

    public void addmkcoa9999S0DTOSub0(
            int _index,
            nhnis.mk.co.a.dto.mkcoa9999S0DTOSub0 mkcoa9999S0DTOSub0) {
        if(this.mkcoa9999S0DTOSub0 == null)
            this.mkcoa9999S0DTOSub0 =
                    new ArrayList<nhnis.mk.co.a.dto.mkcoa9999S0DTOSub0>((int)size);
        if(mkcoa9999S0DTOSub0 == null) {
            this.mkcoa9999S0DTOSub0.add(_index, null);
        } else {
            this.mkcoa9999S0DTOSub0.add(_index, mkcoa9999S0DTOSub0);
        }
    }

    public void addmkcoa9999S0DTOSub0(
            nhnis.mk.co.a.dto.mkcoa9999S0DTOSub0 mkcoa9999S0DTOSub0) {
        if(this.mkcoa9999S0DTOSub0 == null)
            this.mkcoa9999S0DTOSub0 =
                    new ArrayList<nhnis.mk.co.a.dto.mkcoa9999S0DTOSub0>((int)size);
        if(mkcoa9999S0DTOSub0 == null) {
            this.mkcoa9999S0DTOSub0.add(null);
        } else {
            this.mkcoa9999S0DTOSub0.add(mkcoa9999S0DTOSub0);
        }
    }

    private int size = 0;

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public void setSize(Integer size) {
        if(size == null) {
            this.size = 0;
        } else {
            this.size = size.intValue();
        }
    }

    public void setSize(String size) {
        if (size == null || size.length() == 0) {
            this.size = 0;
        } else {
            this.size = Integer.parseInt(size);
        }
    }

    public Object clone() {
        mkcoa9999S0DTOout copyObj = new mkcoa9999S0DTOout();
        copyObj.clone(this);
        return copyObj;
    }

    public void clone(DataObject _mkcoa9999S0DTOout) {
        if(this == _mkcoa9999S0DTOout)
            return;

        mkcoa9999S0DTOout __mkcoa9999S0DTOout =
                (mkcoa9999S0DTOout) _mkcoa9999S0DTOout;
        this.clearmkcoa9999S0DTOSub0();
        if(this.getmkcoa9999S0DTOSub0() == null &&
                __mkcoa9999S0DTOout.getmkcoa9999S0DTOSub0() != null)
            this.ensureCapacitymkcoa9999S0DTOSub0(
                    __mkcoa9999S0DTOout.sizemkcoa9999S0DTOSub0());

        for(int index = 0;
                index < __mkcoa9999S0DTOout.sizemkcoa9999S0DTOSub0();
                index++) {
            nhnis.mk.co.a.dto.mkcoa9999S0DTOSub0 _value0 =
                    __mkcoa9999S0DTOout.getmkcoa9999S0DTOSub0(index);
            if(_value0 == null) {
                this.addmkcoa9999S0DTOSub0(index, null);
            } else {
                this.addmkcoa9999S0DTOSub0(index,
                        (nhnis.mk.co.a.dto.mkcoa9999S0DTOSub0)_value0.clone());
            }
        }

        this.setSize(__mkcoa9999S0DTOout.getSize());
    }

    public String toString() {
        StringBuilder buffer = new StringBuilder();

        buffer.append("mkcoa9999S0DTOSub0[");
        for (int index = 0; index < sizemkcoa9999S0DTOSub0(); index++) {
            buffer.append("[").append(index).append(" : ")
                    .append(getmkcoa9999S0DTOSub0(index)).append("]");
        }
        buffer.append("]");
        buffer.append("\n");
        buffer.append("size : ").append(size).append("\n");
        return buffer.toString();
    }

    private static final Map<String, FieldProperty> fieldPropertyMap;

    static {
        fieldPropertyMap = new java.util.LinkedHashMap<String, FieldProperty>(2);
        fieldPropertyMap.put("mkcoa9999S0DTOSub0", FieldProperty.builder()
                .setPhysicalName("mkcoa9999S0DTOSub0")
                .setLogicalName("mkcoa9999S0DTOSub0")
                .setType(FieldProperty.TYPE_ABSTRACT_INCLUDE)
                .setDecimal(-1)
                .setArray("size")
                .setReference("nhnis.mk.co.a.dto.mkcoa9999S0DTOSub0")
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

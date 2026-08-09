package nhnis.mg.application.dto;

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
        comments = "mgcoa9999S0DTOout"
)
public class mgcoa9999S0DTOout extends DataObject
{
    private static final long serialVersionUID = 1L;

    private List<nhnis.mg.application.dto.mgcoa9999S0DTOSub0> mgcoa9999S0DTOSub0 = null;

    public List<nhnis.mg.application.dto.mgcoa9999S0DTOSub0> getmgcoa9999S0DTOSub0List() {
        if(mgcoa9999S0DTOSub0 == null)
            return null;
        return this.mgcoa9999S0DTOSub0;
    }

    public void clearmgcoa9999S0DTOSub0() {
        if(mgcoa9999S0DTOSub0 != null)
            mgcoa9999S0DTOSub0.clear();
    }

    public void ensureCapacitymgcoa9999S0DTOSub0(int minCapacity) {
        if(mgcoa9999S0DTOSub0 == null)
            mgcoa9999S0DTOSub0 =
                    new ArrayList<nhnis.mg.application.dto.mgcoa9999S0DTOSub0>(minCapacity);
        else
            ((ArrayList<nhnis.mg.application.dto.mgcoa9999S0DTOSub0>)mgcoa9999S0DTOSub0)
                    .ensureCapacity(minCapacity);
    }

    public mgcoa9999S0DTOout fillmgcoa9999S0DTOSub0(int _index) {
        ensureCapacitymgcoa9999S0DTOSub0(_index + 1);
        for (int i = sizemgcoa9999S0DTOSub0(); i < _index + 1; i++) {
            this.mgcoa9999S0DTOSub0.add(i,
                    new nhnis.mg.application.dto.mgcoa9999S0DTOSub0());
        }
        return this;
    }

    public int sizemgcoa9999S0DTOSub0() {
        if(mgcoa9999S0DTOSub0 == null)
            return 0;

        return mgcoa9999S0DTOSub0.size();
    }

    public List<nhnis.mg.application.dto.mgcoa9999S0DTOSub0> getmgcoa9999S0DTOSub0() {
        if(this.mgcoa9999S0DTOSub0 == null)
            return null;

        return this.mgcoa9999S0DTOSub0;
    }

    public nhnis.mg.application.dto.mgcoa9999S0DTOSub0[] getmgcoa9999S0DTOSub0Array() {
        if(this.mgcoa9999S0DTOSub0 == null)
            return null;

        return this.mgcoa9999S0DTOSub0.toArray(
                new nhnis.mg.application.dto.mgcoa9999S0DTOSub0[mgcoa9999S0DTOSub0.size()]);
    }

    public nhnis.mg.application.dto.mgcoa9999S0DTOSub0 getmgcoa9999S0DTOSub0(int _index) {
        return (nhnis.mg.application.dto.mgcoa9999S0DTOSub0)
                mgcoa9999S0DTOSub0.get(_index);
    }

    public void setmgcoa9999S0DTOSub0(
            List<nhnis.mg.application.dto.mgcoa9999S0DTOSub0> mgcoa9999S0DTOSub0) {

        if (mgcoa9999S0DTOSub0 == null) {
            this.mgcoa9999S0DTOSub0 = null;
        } else {
            this.mgcoa9999S0DTOSub0 = mgcoa9999S0DTOSub0;
        }
    }

    public void setmgcoa9999S0DTOSub0Array(
            nhnis.mg.application.dto.mgcoa9999S0DTOSub0[] mgcoa9999S0DTOSub0Array) {
        if (mgcoa9999S0DTOSub0Array == null) {
            this.mgcoa9999S0DTOSub0 = null;
        } else {
            this.mgcoa9999S0DTOSub0 = Arrays.asList(mgcoa9999S0DTOSub0Array);
        }
    }

    public void addmgcoa9999S0DTOSub0(
            int _index,
            nhnis.mg.application.dto.mgcoa9999S0DTOSub0 mgcoa9999S0DTOSub0) {
        if(this.mgcoa9999S0DTOSub0 == null)
            this.mgcoa9999S0DTOSub0 =
                    new ArrayList<nhnis.mg.application.dto.mgcoa9999S0DTOSub0>((int)size);
        if(mgcoa9999S0DTOSub0 == null) {
            this.mgcoa9999S0DTOSub0.add(_index, null);
        } else {
            this.mgcoa9999S0DTOSub0.add(_index, mgcoa9999S0DTOSub0);
        }
    }

    public void addmgcoa9999S0DTOSub0(
            nhnis.mg.application.dto.mgcoa9999S0DTOSub0 mgcoa9999S0DTOSub0) {
        if(this.mgcoa9999S0DTOSub0 == null)
            this.mgcoa9999S0DTOSub0 =
                    new ArrayList<nhnis.mg.application.dto.mgcoa9999S0DTOSub0>((int)size);
        if(mgcoa9999S0DTOSub0 == null) {
            this.mgcoa9999S0DTOSub0.add(null);
        } else {
            this.mgcoa9999S0DTOSub0.add(mgcoa9999S0DTOSub0);
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
        mgcoa9999S0DTOout copyObj = new mgcoa9999S0DTOout();
        copyObj.clone(this);
        return copyObj;
    }

    public void clone(DataObject _mgcoa9999S0DTOout) {
        if(this == _mgcoa9999S0DTOout)
            return;

        mgcoa9999S0DTOout __mgcoa9999S0DTOout =
                (mgcoa9999S0DTOout) _mgcoa9999S0DTOout;
        this.clearmgcoa9999S0DTOSub0();
        if(this.getmgcoa9999S0DTOSub0() == null &&
                __mgcoa9999S0DTOout.getmgcoa9999S0DTOSub0() != null)
            this.ensureCapacitymgcoa9999S0DTOSub0(
                    __mgcoa9999S0DTOout.sizemgcoa9999S0DTOSub0());

        for(int index = 0;
                index < __mgcoa9999S0DTOout.sizemgcoa9999S0DTOSub0();
                index++) {
            nhnis.mg.application.dto.mgcoa9999S0DTOSub0 _value0 =
                    __mgcoa9999S0DTOout.getmgcoa9999S0DTOSub0(index);
            if(_value0 == null) {
                this.addmgcoa9999S0DTOSub0(index, null);
            } else {
                this.addmgcoa9999S0DTOSub0(index,
                        (nhnis.mg.application.dto.mgcoa9999S0DTOSub0)_value0.clone());
            }
        }

        this.setSize(__mgcoa9999S0DTOout.getSize());
    }

    public String toString() {
        StringBuilder buffer = new StringBuilder();

        buffer.append("mgcoa9999S0DTOSub0[");
        for (int index = 0; index < sizemgcoa9999S0DTOSub0(); index++) {
            buffer.append("[").append(index).append(" : ")
                    .append(getmgcoa9999S0DTOSub0(index)).append("]");
        }
        buffer.append("]");
        buffer.append("\n");
        buffer.append("size : ").append(size).append("\n");
        return buffer.toString();
    }

    private static final Map<String, FieldProperty> fieldPropertyMap;

    static {
        fieldPropertyMap = new java.util.LinkedHashMap<String, FieldProperty>(2);
        fieldPropertyMap.put("mgcoa9999S0DTOSub0", FieldProperty.builder()
                .setPhysicalName("mgcoa9999S0DTOSub0")
                .setLogicalName("mgcoa9999S0DTOSub0")
                .setType(FieldProperty.TYPE_ABSTRACT_INCLUDE)
                .setDecimal(-1)
                .setArray("size")
                .setReference("nhnis.mg.application.dto.mgcoa9999S0DTOSub0")
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

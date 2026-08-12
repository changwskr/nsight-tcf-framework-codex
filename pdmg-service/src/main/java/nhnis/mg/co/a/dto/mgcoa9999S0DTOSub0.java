package nhnis.mg.co.a.dto;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.ims.superspring.dto.DataObject;
import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;

/**
 * 영업팁 실적 목록 Sub DTO (mgcoa9999S0).
 */
public class mgcoa9999S0DTOSub0 extends DataObject {

    private static final long serialVersionUID = 1L;

    private String trtBrc;
    private String trtmnEno;
    private String salzTipKdc;
    private String basDt;
    private String prtoCn;
    private String inqCn;
    private String inpCn;

    public String getTrtBrc() {
        return trtBrc;
    }

    public void setTrtBrc(String trtBrc) {
        this.trtBrc = trtBrc;
    }

    public String getTrtmnEno() {
        return trtmnEno;
    }

    public void setTrtmnEno(String trtmnEno) {
        this.trtmnEno = trtmnEno;
    }

    public String getSalzTipKdc() {
        return salzTipKdc;
    }

    public void setSalzTipKdc(String salzTipKdc) {
        this.salzTipKdc = salzTipKdc;
    }

    public String getBasDt() {
        return basDt;
    }

    public void setBasDt(String basDt) {
        this.basDt = basDt;
    }

    public String getPrtoCn() {
        return prtoCn;
    }

    public void setPrtoCn(String prtoCn) {
        this.prtoCn = prtoCn;
    }

    public String getInqCn() {
        return inqCn;
    }

    public void setInqCn(String inqCn) {
        this.inqCn = inqCn;
    }

    public String getInpCn() {
        return inpCn;
    }

    public void setInpCn(String inpCn) {
        this.inpCn = inpCn;
    }

    @Override
    public Object clone() {
        mgcoa9999S0DTOSub0 copy = new mgcoa9999S0DTOSub0();
        copy.clone(this);
        return copy;
    }

    public void clone(DataObject src) {
        if (this == src) {
            return;
        }
        mgcoa9999S0DTOSub0 in = (mgcoa9999S0DTOSub0) src;
        this.trtBrc = in.trtBrc;
        this.trtmnEno = in.trtmnEno;
        this.salzTipKdc = in.salzTipKdc;
        this.basDt = in.basDt;
        this.prtoCn = in.prtoCn;
        this.inqCn = in.inqCn;
        this.inpCn = in.inpCn;
    }

    @Override
    public String toString() {
        return "trtBrc : " + trtBrc
                + " trtmnEno : " + trtmnEno
                + " salzTipKdc : " + salzTipKdc
                + " basDt : " + basDt;
    }

    private static final Map<String, FieldProperty> fieldPropertyMap;

    static {
        fieldPropertyMap = new LinkedHashMap<>();
        putString("trtBrc");
        putString("trtmnEno");
        putString("salzTipKdc");
        putString("basDt");
        putString("prtoCn");
        putString("inqCn");
        putString("inpCn");
    }

    private static void putString(String name) {
        fieldPropertyMap.put(name, FieldProperty.builder()
                .setPhysicalName(name).setLogicalName(name)
                .setType(FieldProperty.TYPE_OBJECT_STRING).setDecimal(-1)
                .setIsNullable(true).setIsEncrypt(false).build());
    }

    public Map<String, FieldProperty> getFieldPropertyMap() {
        return Collections.unmodifiableMap(fieldPropertyMap);
    }
}

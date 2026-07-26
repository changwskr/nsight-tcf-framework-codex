package com.nh.nsight.marketing.ln.persistence.dto.customercontact;

import java.util.LinkedHashMap;
import java.util.Map;

public class CustomerContactRow {

    private String contactId;
    private String customerNo;
    private String contactType;
    private String contactValue;
    private String useYn;
    private String regDtm;
    private String updDtm;

    public String getContactId() {
        return contactId;
    }

    public void setContactId(String contactId) {
        this.contactId = contactId;
    }

    public String getCustomerNo() {
        return customerNo;
    }

    public void setCustomerNo(String customerNo) {
        this.customerNo = customerNo;
    }

    public String getContactType() {
        return contactType;
    }

    public void setContactType(String contactType) {
        this.contactType = contactType;
    }

    public String getContactValue() {
        return contactValue;
    }

    public void setContactValue(String contactValue) {
        this.contactValue = contactValue;
    }

    public String getUseYn() {
        return useYn;
    }

    public void setUseYn(String useYn) {
        this.useYn = useYn;
    }

    public String getRegDtm() {
        return regDtm;
    }

    public void setRegDtm(String regDtm) {
        this.regDtm = regDtm;
    }

    public String getUpdDtm() {
        return updDtm;
    }

    public void setUpdDtm(String updDtm) {
        this.updDtm = updDtm;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("contactId", contactId);
        map.put("customerNo", customerNo);
        map.put("contactType", contactType);
        map.put("contactValue", contactValue);
        map.put("useYn", useYn);
        map.put("regDtm", regDtm);
        map.put("updDtm", updDtm);
        return map;
    }
}

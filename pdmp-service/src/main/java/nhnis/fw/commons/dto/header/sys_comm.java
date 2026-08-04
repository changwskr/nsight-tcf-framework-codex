package nhnis.fw.commons.dto.header;

import java.util.Collections;
import java.util.Map;

import com.ims.superspring.dto.DataObject;
import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;

@jakarta.annotation.Generated(
        value = "com.imssoft.sts4.codegen.dto.DtoGenerator",
        date = "26. 7. 21. 오후 2:27",
        comments = "NH 시스템 공통 헤더"
)
public class sys_comm extends DataObject {
    private static final long serialVersionUID = 1L;

    private String std_gbl_id = null;

    public String getStd_gbl_id() {
        return std_gbl_id;
    }

    public void setStd_gbl_id(String std_gbl_id) {
        if (std_gbl_id == null) {
            this.std_gbl_id = null;
        } else {
            this.std_gbl_id = std_gbl_id;
        }
    }

    private String rms_svc_c = null;

    public String getRms_svc_c() {
        return rms_svc_c;
    }

    public void setRms_svc_c(String rms_svc_c) {
        if (rms_svc_c == null) {
            this.rms_svc_c = null;
        } else {
            this.rms_svc_c = rms_svc_c;
        }
    }

    private String orgtr_gbl_id = null;

    public String getOrgtr_gbl_id() {
        return orgtr_gbl_id;
    }

    public void setOrgtr_gbl_id(String orgtr_gbl_id) {
        if (orgtr_gbl_id == null) {
            this.orgtr_gbl_id = null;
        } else {
            this.orgtr_gbl_id = orgtr_gbl_id;
        }
    }

    private String trz_gbl_id = null;

    public String getTrz_gbl_id() {
        return trz_gbl_id;
    }

    public void setTrz_gbl_id(String trz_gbl_id) {
        if (trz_gbl_id == null) {
            this.trz_gbl_id = null;
        } else {
            this.trz_gbl_id = trz_gbl_id;
        }
    }

    private String sync_dsc = null;

    public String getSync_dsc() {
        return sync_dsc;
    }

    public void setSync_dsc(String sync_dsc) {
        if (sync_dsc == null) {
            this.sync_dsc = null;
        } else {
            this.sync_dsc = sync_dsc;
        }
    }

    private String async_attr_c = null;

    public String getAsync_attr_c() {
        return async_attr_c;
    }

    public void setAsync_attr_c(String async_attr_c) {
        if (async_attr_c == null) {
            this.async_attr_c = null;
        } else {
            this.async_attr_c = async_attr_c;
        }
    }

    private String tr_sysid = null;

    public String getTr_sysid() {
        return tr_sysid;
    }

    public void setTr_sysid(String tr_sysid) {
        if (tr_sysid == null) {
            this.tr_sysid = null;
        } else {
            this.tr_sysid = tr_sysid;
        }
    }

    private int ttl_ug_ync = 0;

    public int getTtl_ug_ync() {
        return ttl_ug_ync;
    }

    public void setTtl_ug_ync(int ttl_ug_ync) {
        this.ttl_ug_ync = ttl_ug_ync;
    }

    public void setTtl_ug_ync(Integer ttl_ug_ync) {
        if (ttl_ug_ync == null) {
            this.ttl_ug_ync = 0;
        } else {
            this.ttl_ug_ync = ttl_ug_ync.intValue();
        }
    }

    public void setTtl_ug_ync(String ttl_ug_ync) {
        if (ttl_ug_ync == null || ttl_ug_ync.length() == 0) {
            this.ttl_ug_ync = 0;
        } else {
            this.ttl_ug_ync = Integer.parseInt(ttl_ug_ync);
        }
    }

    private String std_tgrm_rqr_rsp_dsc = null;

    public String getStd_tgrm_rqr_rsp_dsc() {
        return std_tgrm_rqr_rsp_dsc;
    }

    public void setStd_tgrm_rqr_rsp_dsc(String std_tgrm_rqr_rsp_dsc) {
        if (std_tgrm_rqr_rsp_dsc == null) {
            this.std_tgrm_rqr_rsp_dsc = null;
        } else {
            this.std_tgrm_rqr_rsp_dsc = std_tgrm_rqr_rsp_dsc;
        }
    }

    private String std_tgrm_lclc = null;

    public String getStd_tgrm_lclc() {
        return std_tgrm_lclc;
    }

    public void setStd_tgrm_lclc(String std_tgrm_lclc) {
        if (std_tgrm_lclc == null) {
            this.std_tgrm_lclc = null;
        } else {
            this.std_tgrm_lclc = std_tgrm_lclc;
        }
    }

    private String tr_trm_ipadr = null;

    public String getTr_trm_ipadr() {
        return tr_trm_ipadr;
    }

    public void setTr_trm_ipadr(String tr_trm_ipadr) {
        if (tr_trm_ipadr == null) {
            this.tr_trm_ipadr = null;
        } else {
            this.tr_trm_ipadr = tr_trm_ipadr;
        }
    }

    private String tr_dtm = null;

    public String getTr_dtm() {
        return tr_dtm;
    }

    public void setTr_dtm(String tr_dtm) {
        if (tr_dtm == null) {
            this.tr_dtm = null;
        } else {
            this.tr_dtm = tr_dtm;
        }
    }

    private String tr_brc = null;

    public String getTr_brc() {
        return tr_brc;
    }

    public void setTr_brc(String tr_brc) {
        if (tr_brc == null) {
            this.tr_brc = null;
        } else {
            this.tr_brc = tr_brc;
        }
    }

    private String naac_dsc = null;

    public String getNaac_dsc() {
        return naac_dsc;
    }

    public void setNaac_dsc(String naac_dsc) {
        if (naac_dsc == null) {
            this.naac_dsc = null;
        } else {
            this.naac_dsc = naac_dsc;
        }
    }

    private String trmn_naac_dsc = null;

    public String getTrmn_naac_dsc() {
        return trmn_naac_dsc;
    }

    public void setTrmn_naac_dsc(String trmn_naac_dsc) {
        if (trmn_naac_dsc == null) {
            this.trmn_naac_dsc = null;
        } else {
            this.trmn_naac_dsc = trmn_naac_dsc;
        }
    }

    private String trmno = null;

    public String getTrmno() {
        return trmno;
    }

    public void setTrmno(String trmno) {
        if (trmno == null) {
            this.trmno = null;
        } else {
            this.trmno = trmno;
        }
    }

    private String trm_kdc = null;

    public String getTrm_kdc() {
        return trm_kdc;
    }

    public void setTrm_kdc(String trm_kdc) {
        if (trm_kdc == null) {
            this.trm_kdc = null;
        } else {
            this.trm_kdc = trm_kdc;
        }
    }

    private String scid = null;

    public String getScid() {
        return scid;
    }

    public void setScid(String scid) {
        if (scid == null) {
            this.scid = null;
        } else {
            this.scid = scid;
        }
    }

    private String optr_eno = null;

    public String getOptr_eno() {
        return optr_eno;
    }

    public void setOptr_eno(String optr_eno) {
        if (optr_eno == null) {
            this.optr_eno = null;
        } else {
            this.optr_eno = optr_eno;
        }
    }

    private String tr_optrnm = null;

    public String getTr_optrnm() {
        return tr_optrnm;
    }

    public void setTr_optrnm(String tr_optrnm) {
        if (tr_optrnm == null) {
            this.tr_optrnm = null;
        } else {
            this.tr_optrnm = tr_optrnm;
        }
    }

    private String optr_pzcc = null;

    public String getOptr_pzcc() {
        return optr_pzcc;
    }

    public void setOptr_pzcc(String optr_pzcc) {
        if (optr_pzcc == null) {
            this.optr_pzcc = null;
        } else {
            this.optr_pzcc = optr_pzcc;
        }
    }

    public Object clone() {
        sys_comm copyObj = new sys_comm();
        copyObj.clone(this);
        return copyObj;
    }

    public void clone(DataObject _sys_comm) {
        if (this == _sys_comm)
            return;

        sys_comm __sys_comm = (sys_comm) _sys_comm;
        this.setStd_gbl_id(__sys_comm.getStd_gbl_id());
        this.setRms_svc_c(__sys_comm.getRms_svc_c());
        this.setOrgtr_gbl_id(__sys_comm.getOrgtr_gbl_id());
        this.setTrz_gbl_id(__sys_comm.getTrz_gbl_id());
        this.setSync_dsc(__sys_comm.getSync_dsc());
        this.setAsync_attr_c(__sys_comm.getAsync_attr_c());
        this.setTr_sysid(__sys_comm.getTr_sysid());
        this.setTtl_ug_ync(__sys_comm.getTtl_ug_ync());
        this.setStd_tgrm_rqr_rsp_dsc(__sys_comm.getStd_tgrm_rqr_rsp_dsc());
        this.setStd_tgrm_lclc(__sys_comm.getStd_tgrm_lclc());
        this.setTr_trm_ipadr(__sys_comm.getTr_trm_ipadr());
        this.setTr_dtm(__sys_comm.getTr_dtm());
        this.setTr_brc(__sys_comm.getTr_brc());
        this.setNaac_dsc(__sys_comm.getNaac_dsc());
        this.setTrmn_naac_dsc(__sys_comm.getTrmn_naac_dsc());
        this.setTrmno(__sys_comm.getTrmno());
        this.setTrm_kdc(__sys_comm.getTrm_kdc());
        this.setScid(__sys_comm.getScid());
        this.setOptr_eno(__sys_comm.getOptr_eno());
        this.setTr_optrnm(__sys_comm.getTr_optrnm());
        this.setOptr_pzcc(__sys_comm.getOptr_pzcc());
    }

    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("std_gbl_id : ").append(std_gbl_id).append("\n");
        buffer.append("rms_svc_c : ").append(rms_svc_c).append("\n");
        buffer.append("orgtr_gbl_id : ").append(orgtr_gbl_id).append("\n");
        buffer.append("trz_gbl_id : ").append(trz_gbl_id).append("\n");
        buffer.append("sync_dsc : ").append(sync_dsc).append("\n");
        buffer.append("async_attr_c : ").append(async_attr_c).append("\n");
        buffer.append("tr_sysid : ").append(tr_sysid).append("\n");
        buffer.append("ttl_ug_ync : ").append(ttl_ug_ync).append("\n");
        buffer.append("std_tgrm_rqr_rsp_dsc : ").append(std_tgrm_rqr_rsp_dsc).append("\n");
        buffer.append("std_tgrm_lclc : ").append(std_tgrm_lclc).append("\n");
        buffer.append("tr_trm_ipadr : ").append(tr_trm_ipadr).append("\n");
        buffer.append("tr_dtm : ").append(tr_dtm).append("\n");
        buffer.append("tr_brc : ").append(tr_brc).append("\n");
        buffer.append("naac_dsc : ").append(naac_dsc).append("\n");
        buffer.append("trmn_naac_dsc : ").append(trmn_naac_dsc).append("\n");
        buffer.append("trmno : ").append(trmno).append("\n");
        buffer.append("trm_kdc : ").append(trm_kdc).append("\n");
        buffer.append("scid : ").append(scid).append("\n");
        buffer.append("optr_eno : ").append(optr_eno).append("\n");
        buffer.append("tr_optrnm : ").append(tr_optrnm).append("\n");
        buffer.append("optr_pzcc : ").append(optr_pzcc).append("\n");
        return buffer.toString();
    }

    private static final Map<String, FieldProperty> fieldPropertyMap;

    static {
        fieldPropertyMap = new java.util.LinkedHashMap<String, FieldProperty>(21);
        putStringField("std_gbl_id");
        putStringField("rms_svc_c");
        putStringField("orgtr_gbl_id");
        putStringField("trz_gbl_id");
        putStringField("sync_dsc");
        putStringField("async_attr_c");
        putStringField("tr_sysid");
        fieldPropertyMap.put("ttl_ug_ync", FieldProperty.builder()
                .setPhysicalName("ttl_ug_ync")
                .setLogicalName("ttl_ug_ync")
                .setType(FieldProperty.TYPE_OBJECT_INT)
                .setDecimal(-1)
                .setIsNullable(true)
                .setIsEncrypt(false)
                .build());
        putStringField("std_tgrm_rqr_rsp_dsc");
        putStringField("std_tgrm_lclc");
        putStringField("tr_trm_ipadr");
        putStringField("tr_dtm");
        putStringField("tr_brc");
        putStringField("naac_dsc");
        putStringField("trmn_naac_dsc");
        putStringField("trmno");
        putStringField("trm_kdc");
        putStringField("scid");
        putStringField("optr_eno");
        putStringField("tr_optrnm");
        putStringField("optr_pzcc");
    }

    private static void putStringField(String name) {
        fieldPropertyMap.put(name, FieldProperty.builder()
                .setPhysicalName(name)
                .setLogicalName(name)
                .setType(FieldProperty.TYPE_OBJECT_STRING)
                .setDecimal(-1)
                .setIsNullable(true)
                .setIsEncrypt(false)
                .build());
    }

    public Map<String, FieldProperty> getFieldPropertyMap() {
        return Collections.unmodifiableMap(fieldPropertyMap);
    }
}

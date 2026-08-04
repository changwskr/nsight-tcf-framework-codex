package nhnis.fw.commons.dto.header;

import com.ims.superspring.dto.engine.exception.UnmarshalException;
import com.ims.superspring.dto.engine.exception.MarshalException;
import com.ims.superspring.dto.DataObject;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.google.gson.stream.JsonToken;
import org.w3c.dom.Node;

import com.ims.superspring.dto.engine.base.JsonMessage;

@jakarta.annotation.Generated(
        value = "com.imssoft.sts4.codegen.message.MessageGenerator",
        date = "26. 7. 21. 오후 2:27",
        comments = "NH 시스템 공통 헤더MsgJson"
)
public class sys_commMsgJson extends JsonMessage {

    public byte[] marshal(DataObject obj) throws MarshalException {
        sys_comm _sys_comm = (sys_comm) obj;
        if (_sys_comm == null) {
            return null;
        }

        ByteArrayOutputStream out = null;
        OutputStreamWriter osw = null;
        BufferedWriter bw = null;
        JsonWriter jw = null;

        try {
            out = new ByteArrayOutputStream();
            osw = new OutputStreamWriter(out, this.encoding);
            bw = new BufferedWriter(osw);
            jw = new JsonWriter(bw);

            jw.beginObject();
            marshal(_sys_comm, jw);
            jw.endObject();
            jw.close();

            return out.toByteArray();
        } catch (Exception e) {
            throw new MarshalException(e);
        } finally {
            try {
                if (jw != null) {
                    try {
                        jw.close();
                    } catch (IOException e) {
                        throw new MarshalException(e);
                    }
                }
            } finally {
                try {
                    if (bw != null) {
                        try {
                            bw.close();
                        } catch (IOException e) {
                            throw new MarshalException(e);
                        }
                    }
                } finally {
                    try {
                        if (osw != null) {
                            try {
                                osw.close();
                            } catch (IOException e) {
                                throw new MarshalException(e);
                            }
                        }
                    } finally {
                        if (out != null) {
                            try {
                                out.close();
                            } catch (IOException e) {
                                throw new MarshalException(e);
                            }
                        }
                    }
                }
            }
        }
    }

    public void marshal(nhnis.fw.commons.dto.header.sys_comm _sys_comm, JsonWriter writer)
            throws IOException {
        writer.name("std_gbl_id");
        if (_sys_comm.getStd_gbl_id() != null) {
            writer.value(_sys_comm.getStd_gbl_id());
        } else {
            writer.nullValue();
        }
        writer.name("rms_svc_c");
        if (_sys_comm.getRms_svc_c() != null) {
            writer.value(_sys_comm.getRms_svc_c());
        } else {
            writer.nullValue();
        }
        writer.name("orgtr_gbl_id");
        if (_sys_comm.getOrgtr_gbl_id() != null) {
            writer.value(_sys_comm.getOrgtr_gbl_id());
        } else {
            writer.nullValue();
        }
        writer.name("trz_gbl_id");
        if (_sys_comm.getTrz_gbl_id() != null) {
            writer.value(_sys_comm.getTrz_gbl_id());
        } else {
            writer.nullValue();
        }
        writer.name("sync_dsc");
        if (_sys_comm.getSync_dsc() != null) {
            writer.value(_sys_comm.getSync_dsc());
        } else {
            writer.nullValue();
        }
        writer.name("async_attr_c");
        if (_sys_comm.getAsync_attr_c() != null) {
            writer.value(_sys_comm.getAsync_attr_c());
        } else {
            writer.nullValue();
        }
        writer.name("tr_sysid");
        if (_sys_comm.getTr_sysid() != null) {
            writer.value(_sys_comm.getTr_sysid());
        } else {
            writer.nullValue();
        }
        writer.name("ttl_ug_ync");
        writer.value(_sys_comm.getTtl_ug_ync());
        writer.name("std_tgrm_rqr_rsp_dsc");
        if (_sys_comm.getStd_tgrm_rqr_rsp_dsc() != null) {
            writer.value(_sys_comm.getStd_tgrm_rqr_rsp_dsc());
        } else {
            writer.nullValue();
        }
        writer.name("std_tgrm_lclc");
        if (_sys_comm.getStd_tgrm_lclc() != null) {
            writer.value(_sys_comm.getStd_tgrm_lclc());
        } else {
            writer.nullValue();
        }
        writer.name("tr_trm_ipadr");
        if (_sys_comm.getTr_trm_ipadr() != null) {
            writer.value(_sys_comm.getTr_trm_ipadr());
        } else {
            writer.nullValue();
        }
        writer.name("tr_dtm");
        if (_sys_comm.getTr_dtm() != null) {
            writer.value(_sys_comm.getTr_dtm());
        } else {
            writer.nullValue();
        }
        writer.name("tr_brc");
        if (_sys_comm.getTr_brc() != null) {
            writer.value(_sys_comm.getTr_brc());
        } else {
            writer.nullValue();
        }
        writer.name("naac_dsc");
        if (_sys_comm.getNaac_dsc() != null) {
            writer.value(_sys_comm.getNaac_dsc());
        } else {
            writer.nullValue();
        }
        writer.name("trmn_naac_dsc");
        if (_sys_comm.getTrmn_naac_dsc() != null) {
            writer.value(_sys_comm.getTrmn_naac_dsc());
        } else {
            writer.nullValue();
        }
        writer.name("trmno");
        if (_sys_comm.getTrmno() != null) {
            writer.value(_sys_comm.getTrmno());
        } else {
            writer.nullValue();
        }
        writer.name("trm_kdc");
        if (_sys_comm.getTrm_kdc() != null) {
            writer.value(_sys_comm.getTrm_kdc());
        } else {
            writer.nullValue();
        }
        writer.name("scid");
        if (_sys_comm.getScid() != null) {
            writer.value(_sys_comm.getScid());
        } else {
            writer.nullValue();
        }
        writer.name("optr_eno");
        if (_sys_comm.getOptr_eno() != null) {
            writer.value(_sys_comm.getOptr_eno());
        } else {
            writer.nullValue();
        }
        writer.name("tr_optrnm");
        if (_sys_comm.getTr_optrnm() != null) {
            writer.value(_sys_comm.getTr_optrnm());
        } else {
            writer.nullValue();
        }
        writer.name("optr_pzcc");
        if (_sys_comm.getOptr_pzcc() != null) {
            writer.value(_sys_comm.getOptr_pzcc());
        } else {
            writer.nullValue();
        }
    }

    public String removeNullChar(String charString) {
        if (charString == null)
            return "";

        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < charString.length(); i++) {
            if (charString.charAt(i) == (char) 0) {
                sb.append("");
            } else {
                sb.append(charString.charAt(i));
            }
        }
        return sb.toString();
    }

    @Override
    public void marshal(DataObject arg0, Node arg1) throws MarshalException {
    }

    public sys_comm unmarshal(byte[] bytes, int i) throws UnmarshalException {
        sys_comm _sys_comm = new sys_comm();
        ByteArrayInputStream in = null;
        InputStreamReader isr = null;
        BufferedReader reader = null;
        JsonReader jr = null;

        if (bytes.length <= 0)
            return new sys_comm();

        try {
            in = new ByteArrayInputStream(bytes);
            isr = new InputStreamReader(in, this.encoding);
            reader = new BufferedReader(isr);
            jr = new JsonReader(reader);
            jr.beginObject();

            _sys_comm = (sys_comm) unmarshal(jr, _sys_comm);

            jr.endObject();
            jr.close();

        } catch (Exception e) {
            throw new UnmarshalException(e);
        } finally {
            try {
                if (jr != null)
                    try {
                        jr.close();
                    } catch (IOException e) {
                        throw new UnmarshalException(e);
                    }
            } finally {
                try {
                    if (reader != null)
                        try {
                            reader.close();
                        } catch (IOException e) {
                            throw new UnmarshalException(e);
                        }
                } finally {
                    try {
                        if (isr != null)
                            try {
                                isr.close();
                            } catch (IOException e) {
                                throw new UnmarshalException(e);
                            }
                    } finally {
                        if (in != null)
                            try {
                                in.close();
                            } catch (IOException e) {
                                throw new UnmarshalException(e);
                            }
                    }
                }
            }
        }
        return _sys_comm;
    }

    public DataObject unmarshal(byte[] bytes, sys_comm dto) throws UnmarshalException {
        sys_comm _sys_comm = (sys_comm) dto;
        ByteArrayInputStream in = null;
        InputStreamReader isr = null;
        BufferedReader reader = null;
        JsonReader jr = null;

        if (bytes.length <= 0)
            return new sys_comm();

        try {
            in = new ByteArrayInputStream(bytes);
            isr = new InputStreamReader(in, this.encoding);
            reader = new BufferedReader(isr);
            jr = new JsonReader(reader);
            jr.beginObject();

            _sys_comm = (sys_comm) unmarshal(jr, _sys_comm);

            jr.endObject();
            jr.close();

        } catch (Exception e) {
            throw new UnmarshalException(e);
        } finally {
            try {
                if (in != null)
                    try {
                        in.close();
                    } catch (IOException e) {
                        throw new UnmarshalException(e);
                    }
            } finally {
                try {
                    if (isr != null)
                        try {
                            isr.close();
                        } catch (IOException e) {
                            throw new UnmarshalException(e);
                        }
                } finally {
                    try {
                        if (jr != null)
                            try {
                                jr.close();
                            } catch (IOException e) {
                                throw new UnmarshalException(e);
                            }
                    } finally {
                        if (reader != null)
                            try {
                                reader.close();
                            } catch (IOException e) {
                                throw new UnmarshalException(e);
                            }
                    }
                }
            }
        }
        return _sys_comm;
    }

    public DataObject unmarshal(JsonReader reader, DataObject dto) throws IOException {
        while (reader.hasNext()) {
            String name = reader.nextName();
            setField((sys_comm) dto, reader, name);
        }

        validField((sys_comm) dto);

        return dto;
    }

    protected void validField(sys_comm dto) throws IOException {
    }

    protected void setField(sys_comm dto, JsonReader reader, String name) throws IOException {
        switch (name) {
            case "std_gbl_id": {
                if (reader.peek() != JsonToken.NULL) {
                    dto.setStd_gbl_id(reader.nextString());
                } else {
                    reader.nextNull();
                }
                break;
            }
            case "rms_svc_c": {
                if (reader.peek() != JsonToken.NULL) {
                    dto.setRms_svc_c(reader.nextString());
                } else {
                    reader.nextNull();
                }
                break;
            }
            case "orgtr_gbl_id": {
                if (reader.peek() != JsonToken.NULL) {
                    dto.setOrgtr_gbl_id(reader.nextString());
                } else {
                    reader.nextNull();
                }
                break;
            }
            case "trz_gbl_id": {
                if (reader.peek() != JsonToken.NULL) {
                    dto.setTrz_gbl_id(reader.nextString());
                } else {
                    reader.nextNull();
                }
                break;
            }
            case "sync_dsc": {
                if (reader.peek() != JsonToken.NULL) {
                    dto.setSync_dsc(reader.nextString());
                } else {
                    reader.nextNull();
                }
                break;
            }
            case "async_attr_c": {
                if (reader.peek() != JsonToken.NULL) {
                    dto.setAsync_attr_c(reader.nextString());
                } else {
                    reader.nextNull();
                }
                break;
            }
            case "tr_sysid": {
                if (reader.peek() != JsonToken.NULL) {
                    dto.setTr_sysid(reader.nextString());
                } else {
                    reader.nextNull();
                }
                break;
            }
            case "ttl_ug_ync": {
                if (reader.peek() != JsonToken.NULL) {
                    dto.setTtl_ug_ync(reader.nextInt());
                } else {
                    reader.nextNull();
                }
                break;
            }
            case "std_tgrm_rqr_rsp_dsc": {
                if (reader.peek() != JsonToken.NULL) {
                    dto.setStd_tgrm_rqr_rsp_dsc(reader.nextString());
                } else {
                    reader.nextNull();
                }
                break;
            }
            case "std_tgrm_lclc": {
                if (reader.peek() != JsonToken.NULL) {
                    dto.setStd_tgrm_lclc(reader.nextString());
                } else {
                    reader.nextNull();
                }
                break;
            }
            case "tr_trm_ipadr": {
                if (reader.peek() != JsonToken.NULL) {
                    dto.setTr_trm_ipadr(reader.nextString());
                } else {
                    reader.nextNull();
                }
                break;
            }
            case "tr_dtm": {
                if (reader.peek() != JsonToken.NULL) {
                    dto.setTr_dtm(reader.nextString());
                } else {
                    reader.nextNull();
                }
                break;
            }
            case "tr_brc": {
                if (reader.peek() != JsonToken.NULL) {
                    dto.setTr_brc(reader.nextString());
                } else {
                    reader.nextNull();
                }
                break;
            }
            case "naac_dsc": {
                if (reader.peek() != JsonToken.NULL) {
                    dto.setNaac_dsc(reader.nextString());
                } else {
                    reader.nextNull();
                }
                break;
            }
            case "trmn_naac_dsc": {
                if (reader.peek() != JsonToken.NULL) {
                    dto.setTrmn_naac_dsc(reader.nextString());
                } else {
                    reader.nextNull();
                }
                break;
            }
            case "trmno": {
                if (reader.peek() != JsonToken.NULL) {
                    dto.setTrmno(reader.nextString());
                } else {
                    reader.nextNull();
                }
                break;
            }
            case "trm_kdc": {
                if (reader.peek() != JsonToken.NULL) {
                    dto.setTrm_kdc(reader.nextString());
                } else {
                    reader.nextNull();
                }
                break;
            }
            case "scid": {
                if (reader.peek() != JsonToken.NULL) {
                    dto.setScid(reader.nextString());
                } else {
                    reader.nextNull();
                }
                break;
            }
            case "optr_eno": {
                if (reader.peek() != JsonToken.NULL) {
                    dto.setOptr_eno(reader.nextString());
                } else {
                    reader.nextNull();
                }
                break;
            }
            case "tr_optrnm": {
                if (reader.peek() != JsonToken.NULL) {
                    dto.setTr_optrnm(reader.nextString());
                } else {
                    reader.nextNull();
                }
                break;
            }
            case "optr_pzcc": {
                if (reader.peek() != JsonToken.NULL) {
                    dto.setOptr_pzcc(reader.nextString());
                } else {
                    reader.nextNull();
                }
                break;
            }
            default: {
                reader.skipValue();
                break;
            }
        }
    }

    @Override
    public DataObject unmarshal(Node arg0) throws UnmarshalException {
        return null;
    }

    @Override
    public int unmarshal(byte[] arg0, int arg1, DataObject arg2) throws Exception {
        return 0;
    }
}

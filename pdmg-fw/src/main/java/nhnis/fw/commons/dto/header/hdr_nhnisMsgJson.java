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
        date = "26. 7. 21. 오전 11:15",
        comments = "공통 헤더MsgJson"
)
public class hdr_nhnisMsgJson extends JsonMessage {

    public byte[] marshal(DataObject obj) throws MarshalException {
        hdr_nhnis _hdr_nhnis = (hdr_nhnis) obj;

        if (_hdr_nhnis == null)
            return null;

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

            marshal(_hdr_nhnis, jw);
            jw.endObject();
            jw.close();

            return out.toByteArray();
        } catch (Exception e) {
            throw new MarshalException(e);
        } finally {
            try {
                if (jw != null)
                    try {
                        jw.close();
                    } catch (IOException e) {
                        throw new MarshalException(e);
                    }
            } finally {
                try {
                    if (bw != null)
                        try {
                            bw.close();
                        } catch (IOException e) {
                            throw new MarshalException(e);
                        }
                } finally {
                    try {
                        if (osw != null)
                            try {
                                osw.close();
                            } catch (IOException e) {
                                throw new MarshalException(e);
                            }
                    } finally {
                        if (out != null)
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

    public void marshal(nhnis.fw.commons.dto.header.hdr_nhnis _hdr_nhnis, JsonWriter writer)
            throws IOException {

        nhnis.fw.commons.dto.header.sys_commMsgJson __sys_comm =
                new nhnis.fw.commons.dto.header.sys_commMsgJson();
        writer.name("sys_comm");
        if (_hdr_nhnis.getSys_comm() != null) {
            writer.beginObject();
            __sys_comm.marshal(
                    (nhnis.fw.commons.dto.header.sys_comm) _hdr_nhnis.getSys_comm(), writer);
            writer.endObject();
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

    public hdr_nhnis unmarshal(byte[] bytes, int i) throws UnmarshalException {
        hdr_nhnis _hdr_nhnis = new hdr_nhnis();
        ByteArrayInputStream in = null;
        InputStreamReader isr = null;
        BufferedReader reader = null;
        JsonReader jr = null;

        if (bytes.length <= 0)
            return new hdr_nhnis();

        try {
            in = new ByteArrayInputStream(bytes);
            isr = new InputStreamReader(in, this.encoding);
            reader = new BufferedReader(isr);
            jr = new JsonReader(reader);
            jr.beginObject();

            _hdr_nhnis = (hdr_nhnis) unmarshal(jr, _hdr_nhnis);

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
        return _hdr_nhnis;
    }

    public DataObject unmarshal(byte[] bytes, hdr_nhnis dto) throws UnmarshalException {
        hdr_nhnis _hdr_nhnis = (hdr_nhnis) dto;
        ByteArrayInputStream in = null;
        InputStreamReader isr = null;
        BufferedReader reader = null;
        JsonReader jr = null;

        if (bytes.length <= 0)
            return new hdr_nhnis();

        try {
            in = new ByteArrayInputStream(bytes);
            isr = new InputStreamReader(in, this.encoding);
            reader = new BufferedReader(isr);
            jr = new JsonReader(reader);
            jr.beginObject();

            _hdr_nhnis = (hdr_nhnis) unmarshal(jr, _hdr_nhnis);

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
        return _hdr_nhnis;
    }

    public DataObject unmarshal(JsonReader reader, DataObject dto) throws IOException {
        while (reader.hasNext()) {
            String name = reader.nextName();
            setField((hdr_nhnis) dto, reader, name);
        }

        validField((hdr_nhnis) dto);

        return dto;
    }

    protected void validField(hdr_nhnis dto) throws IOException {
    }

    protected void setField(hdr_nhnis dto, JsonReader reader, String name) throws IOException {
        switch (name) {
            case "sys_comm": {
                if (reader.peek() == JsonToken.NULL) {
                    reader.nextNull();
                } else {
                    nhnis.fw.commons.dto.header.sys_commMsgJson __sys_comm =
                            new nhnis.fw.commons.dto.header.sys_commMsgJson();
                    nhnis.fw.commons.dto.header.sys_comm ___sys_comm =
                            new nhnis.fw.commons.dto.header.sys_comm();
                    reader.beginObject();
                    dto.setSys_comm(
                            (nhnis.fw.commons.dto.header.sys_comm) __sys_comm.unmarshal(reader,
                                    ___sys_comm));
                    reader.endObject();
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

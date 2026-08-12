package nhnis.mg.co.a.dto;

import com.ims.superspring.dto.engine.exception.UnmarshalException;
import com.ims.superspring.dto.engine.exception.MarshalException;
import com.ims.superspring.dto.DataObject;
import java.util.ArrayList;
import java.util.List;
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
        value = "com.tmaxsoft.sts4.codegen.message.MessageGenerator",
        date = "26. 7. 24. 오전 10:09",
        comments = "mgcoa9999S0DTOSub0MsgJson"
)
public class mgcoa9999S0DTOSub0MsgJson extends JsonMessage
{
    public byte[] marshal(DataObject obj) throws MarshalException {
        mgcoa9999S0DTOSub0 _mgcoa9999S0DTOSub0 =
                (mgcoa9999S0DTOSub0)obj;

        if (_mgcoa9999S0DTOSub0 == null)
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

            marshal(_mgcoa9999S0DTOSub0, jw);

            jw.endObject();
            jw.close();
            return out.toByteArray();
        } catch(Exception e) {
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

    public void marshal(
            nhnis.mg.co.a.dto.mgcoa9999S0DTOSub0 _mgcoa9999S0DTOSub0,
            JsonWriter writer) throws IOException {

        writer.name("trtBrc");
        if (_mgcoa9999S0DTOSub0.getTrtBrc() != null) {
            writer.value(_mgcoa9999S0DTOSub0.getTrtBrc());
        } else {
            writer.nullValue();
        }
        writer.name("trtmnEno");
        if (_mgcoa9999S0DTOSub0.getTrtmnEno() != null) {
            writer.value(_mgcoa9999S0DTOSub0.getTrtmnEno());
        } else {
            writer.nullValue();
        }
        writer.name("salzTipKdc");
        if (_mgcoa9999S0DTOSub0.getSalzTipKdc() != null) {
            writer.value(_mgcoa9999S0DTOSub0.getSalzTipKdc());
        } else {
            writer.nullValue();
        }
        writer.name("basDt");
        if (_mgcoa9999S0DTOSub0.getBasDt() != null) {
            writer.value(_mgcoa9999S0DTOSub0.getBasDt());
        } else {
            writer.nullValue();
        }
        writer.name("prtoCn");
        if (_mgcoa9999S0DTOSub0.getPrtoCn() != null) {
            writer.value(_mgcoa9999S0DTOSub0.getPrtoCn());
        } else {
            writer.nullValue();
        }
        writer.name("inqCn");
        if (_mgcoa9999S0DTOSub0.getInqCn() != null) {
            writer.value(_mgcoa9999S0DTOSub0.getInqCn());
        } else {
            writer.nullValue();
        }
        writer.name("inpCn");
        if (_mgcoa9999S0DTOSub0.getInpCn() != null) {
            writer.value(_mgcoa9999S0DTOSub0.getInpCn());
        } else {
            writer.nullValue();
        }
    }

    public String removeNullChar(String charString) {
        if (charString == null)
            return "";

        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < charString.length(); i++) {
            if (charString.charAt(i) == (char)0) {
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

    public mgcoa9999S0DTOSub0 unmarshal(byte[] bytes, int i)
            throws UnmarshalException {
        mgcoa9999S0DTOSub0 _mgcoa9999S0DTOSub0 =
                new mgcoa9999S0DTOSub0();
        ByteArrayInputStream in = null;
        InputStreamReader isr = null;
        BufferedReader reader = null;
        JsonReader jr = null;

        if (bytes.length <= 0)
            return new mgcoa9999S0DTOSub0();

        try {
            in = new ByteArrayInputStream(bytes);
            isr = new InputStreamReader(in, this.encoding);
            reader = new BufferedReader(isr);
            jr = new JsonReader(reader);
            jr.beginObject();

            _mgcoa9999S0DTOSub0 =
                    (mgcoa9999S0DTOSub0)unmarshal(
                            jr, _mgcoa9999S0DTOSub0);

            jr.endObject();
            jr.close();

        } catch(Exception e) {
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
        return _mgcoa9999S0DTOSub0;
    }

    public DataObject unmarshal(
            byte[] bytes,
            mgcoa9999S0DTOSub0 dto) throws UnmarshalException {
        mgcoa9999S0DTOSub0 _mgcoa9999S0DTOSub0 =
                (mgcoa9999S0DTOSub0) dto;
        ByteArrayInputStream in = null;
        InputStreamReader isr = null;
        BufferedReader reader = null;
        JsonReader jr = null;

        if (bytes.length <= 0)
            return new mgcoa9999S0DTOSub0();

        try {
            in = new ByteArrayInputStream(bytes);
            isr = new InputStreamReader(in, this.encoding);
            reader = new BufferedReader(isr);
            jr = new JsonReader(reader);
            jr.beginObject();

            _mgcoa9999S0DTOSub0 =
                    (mgcoa9999S0DTOSub0)unmarshal(
                            jr, _mgcoa9999S0DTOSub0);

            jr.endObject();
            jr.close();

        } catch(Exception e) {
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
        return _mgcoa9999S0DTOSub0;
    }

    public DataObject unmarshal(JsonReader reader, DataObject dto)
            throws IOException {

        while (reader.hasNext()) {
            String name = reader.nextName();
            setField((mgcoa9999S0DTOSub0)dto, reader, name);
        }

        validField((mgcoa9999S0DTOSub0)dto);

        return dto;
    }

    protected void validField(mgcoa9999S0DTOSub0 dto)
            throws IOException {
    }

    protected void setField(
            mgcoa9999S0DTOSub0 dto,
            JsonReader reader,
            String name) throws IOException {

        switch(name) {
        case "trtBrc":
        {
            if(reader.peek() != JsonToken.NULL) {
                dto.setTrtBrc(reader.nextString());
            } else {
                reader.nextNull();
            }
            break;
        }
        case "trtmnEno":
        {
            if(reader.peek() != JsonToken.NULL) {
                dto.setTrtmnEno(reader.nextString());
            } else {
                reader.nextNull();
            }
            break;
        }
        case "salzTipKdc":
        {
            if(reader.peek() != JsonToken.NULL) {
                dto.setSalzTipKdc(reader.nextString());
            } else {
                reader.nextNull();
            }
            break;
        }
        case "basDt":
        {
            if(reader.peek() != JsonToken.NULL) {
                dto.setBasDt(reader.nextString());
            } else {
                reader.nextNull();
            }
            break;
        }
        case "prtoCn":
        {
            if(reader.peek() != JsonToken.NULL) {
                dto.setPrtoCn(reader.nextString());
            } else {
                reader.nextNull();
            }
            break;
        }
        case "inqCn":
        {
            if(reader.peek() != JsonToken.NULL) {
                dto.setInqCn(reader.nextString());
            } else {
                reader.nextNull();
            }
            break;
        }
        case "inpCn":
        {
            if(reader.peek() != JsonToken.NULL) {
                dto.setInpCn(reader.nextString());
            } else {
                reader.nextNull();
            }
            break;
        }
        default:
            reader.skipValue();
            break;
        }
    }

    @Override
    public DataObject unmarshal(Node arg0) throws UnmarshalException {
        return null;
    }

    @Override
    public int unmarshal(byte[] arg0, int arg1, DataObject arg2)
            throws Exception {
        return 0;
    }
}

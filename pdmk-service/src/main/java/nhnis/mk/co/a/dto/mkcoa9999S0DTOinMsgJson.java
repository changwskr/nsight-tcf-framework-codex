package nhnis.mk.co.a.dto;

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
        date = "26. 7. 24. 오전 10:08",
        comments = "mkcoa9999S0DTOinMsgJson"
)
public class mkcoa9999S0DTOinMsgJson extends JsonMessage
{
    public byte[] marshal(DataObject obj) throws MarshalException {
        mkcoa9999S0DTOin _mkcoa9999S0DTOin = (mkcoa9999S0DTOin)obj;

        if (_mkcoa9999S0DTOin == null)
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

            marshal(_mkcoa9999S0DTOin, jw);

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

    public void marshal(nhnis.mk.co.a.dto.mkcoa9999S0DTOin _mkcoa9999S0DTOin,
            JsonWriter writer) throws IOException {

        writer.name("salzTipKdc");
        if (_mkcoa9999S0DTOin.getSalzTipKdc() != null) {
            writer.value(_mkcoa9999S0DTOin.getSalzTipKdc());
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

    public mkcoa9999S0DTOin unmarshal(byte[] bytes, int i) throws UnmarshalException {
        mkcoa9999S0DTOin _mkcoa9999S0DTOin = new mkcoa9999S0DTOin();
        ByteArrayInputStream in = null;
        InputStreamReader isr = null;
        BufferedReader reader = null;
        JsonReader jr = null;

        if (bytes.length <= 0)
            return new mkcoa9999S0DTOin();

        try {
            in = new ByteArrayInputStream(bytes);
            isr = new InputStreamReader(in, this.encoding);
            reader = new BufferedReader(isr);
            jr = new JsonReader(reader);
            jr.beginObject();

            _mkcoa9999S0DTOin =
                    (mkcoa9999S0DTOin)unmarshal(jr, _mkcoa9999S0DTOin);

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
        return _mkcoa9999S0DTOin;
    }

    public DataObject unmarshal(byte[] bytes, mkcoa9999S0DTOin dto)
            throws UnmarshalException {
        mkcoa9999S0DTOin _mkcoa9999S0DTOin = (mkcoa9999S0DTOin) dto;
        ByteArrayInputStream in = null;
        InputStreamReader isr = null;
        BufferedReader reader = null;
        JsonReader jr = null;

        if (bytes.length <= 0)
            return new mkcoa9999S0DTOin();

        try {
            in = new ByteArrayInputStream(bytes);
            isr = new InputStreamReader(in, this.encoding);
            reader = new BufferedReader(isr);
            jr = new JsonReader(reader);
            jr.beginObject();

            _mkcoa9999S0DTOin =
                    (mkcoa9999S0DTOin)unmarshal(jr, _mkcoa9999S0DTOin);

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
        return _mkcoa9999S0DTOin;
    }

    public DataObject unmarshal(JsonReader reader, DataObject dto) throws IOException {

        while (reader.hasNext()) {
            String name = reader.nextName();
            setField((mkcoa9999S0DTOin)dto, reader, name);
        }

        validField((mkcoa9999S0DTOin)dto);

        return dto;
    }

    protected void validField(mkcoa9999S0DTOin dto) throws IOException {
    }

    protected void setField(mkcoa9999S0DTOin dto, JsonReader reader, String name)
            throws IOException {

        switch(name) {
        case "salzTipKdc":
        {
            if(reader.peek() != JsonToken.NULL) {
                dto.setSalzTipKdc(reader.nextString());
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
    public int unmarshal(byte[] arg0, int arg1, DataObject arg2) throws Exception {
        return 0;
    }
}

package nhnis.fw.commons.dto;

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
        date = "26. 7. 27. 오후 3:03",
        comments = "NH_NIS_ERR_DTOMsgJson"
)
public class NH_NIS_ERR_DTOMsgJson extends JsonMessage {

    public byte[] marshal(DataObject obj) throws MarshalException {
        NH_NIS_ERR_DTO _NH_NIS_ERR_DTO = (NH_NIS_ERR_DTO) obj;
        if (_NH_NIS_ERR_DTO == null) {
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
            marshal(_NH_NIS_ERR_DTO, jw);
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
                        try {
                            if (out != null) {
                                try {
                                    out.close();
                                } catch (IOException e) {
                                    throw new MarshalException(e);
                                }
                            }
                        } catch (MarshalException e) {
                            throw e;
                        }
                    }
                }
            }
        }
    }

    public void marshal(NH_NIS_ERR_DTO _NH_NIS_ERR_DTO, JsonWriter writer) throws IOException {
        writer.name("stdErrCode");
        if (_NH_NIS_ERR_DTO.getStdErrCode() != null) {
            writer.value(_NH_NIS_ERR_DTO.getStdErrCode());
        } else {
            writer.nullValue();
        }

        writer.name("stdErrMsgCntn");
        if (_NH_NIS_ERR_DTO.getStdErrMsgCntn() != null) {
            writer.value(_NH_NIS_ERR_DTO.getStdErrMsgCntn());
        } else {
            writer.nullValue();
        }

        writer.name("addMsgContents");
        if (_NH_NIS_ERR_DTO.getAddMsgContents() != null) {
            writer.value(_NH_NIS_ERR_DTO.getAddMsgContents());
        } else {
            writer.nullValue();
        }

        writer.name("errClassName");
        if (_NH_NIS_ERR_DTO.getErrClassName() != null) {
            writer.value(_NH_NIS_ERR_DTO.getErrClassName());
        } else {
            writer.nullValue();
        }

        writer.name("errFileName");
        if (_NH_NIS_ERR_DTO.getErrFileName() != null) {
            writer.value(_NH_NIS_ERR_DTO.getErrFileName());
        } else {
            writer.nullValue();
        }

        writer.name("errMethodName");
        if (_NH_NIS_ERR_DTO.getErrMethodName() != null) {
            writer.value(_NH_NIS_ERR_DTO.getErrMethodName());
        } else {
            writer.nullValue();
        }

        writer.name("errLineNo");
        writer.value(_NH_NIS_ERR_DTO.getErrLineNo());

        writer.name("errType");
        if (_NH_NIS_ERR_DTO.getErrType() != null) {
            writer.value(_NH_NIS_ERR_DTO.getErrType());
        } else {
            writer.nullValue();
        }

        writer.name("stackTrace");
        if (_NH_NIS_ERR_DTO.getStackTrace() != null) {
            writer.beginArray();
            for (int i = 0; i < _NH_NIS_ERR_DTO.sizeStackTrace(); i++) {
                String stackTrace = _NH_NIS_ERR_DTO.getStackTrace(i);
                if (stackTrace != null) {
                    writer.value(stackTrace);
                } else {
                    writer.nullValue();
                }
            }
            writer.endArray();
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

    public NH_NIS_ERR_DTO unmarshal(byte[] bytes, int i) throws UnmarshalException {
        NH_NIS_ERR_DTO _NH_NIS_ERR_DTO = new NH_NIS_ERR_DTO();
        ByteArrayInputStream in = null;
        InputStreamReader isr = null;
        BufferedReader reader = null;
        JsonReader jr = null;

        if (bytes.length <= 0)
            return new NH_NIS_ERR_DTO();

        try {
            in = new ByteArrayInputStream(bytes);
            isr = new InputStreamReader(in, this.encoding);
            reader = new BufferedReader(isr);
            jr = new JsonReader(reader);
            jr.beginObject();

            _NH_NIS_ERR_DTO = (NH_NIS_ERR_DTO) unmarshal(jr, _NH_NIS_ERR_DTO);

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
        return _NH_NIS_ERR_DTO;
    }

    public DataObject unmarshal(byte[] bytes, NH_NIS_ERR_DTO dto) throws UnmarshalException {
        NH_NIS_ERR_DTO _NH_NIS_ERR_DTO = (NH_NIS_ERR_DTO) dto;
        ByteArrayInputStream in = null;
        InputStreamReader isr = null;
        BufferedReader reader = null;
        JsonReader jr = null;

        if (bytes.length <= 0)
            return new NH_NIS_ERR_DTO();

        try {
            in = new ByteArrayInputStream(bytes);
            isr = new InputStreamReader(in, this.encoding);
            reader = new BufferedReader(isr);
            jr = new JsonReader(reader);
            jr.beginObject();

            _NH_NIS_ERR_DTO = (NH_NIS_ERR_DTO) unmarshal(jr, _NH_NIS_ERR_DTO);

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
        return _NH_NIS_ERR_DTO;
    }

    public DataObject unmarshal(JsonReader reader, DataObject dto) throws IOException {
        while (reader.hasNext()) {
            String name = reader.nextName();
            setField((NH_NIS_ERR_DTO) dto, reader, name);
        }

        validField((NH_NIS_ERR_DTO) dto);

        return dto;
    }

    protected void validField(NH_NIS_ERR_DTO dto) throws IOException {
        int compareSize8 = 15;
        int dtoArraySize8 = Integer.valueOf(15);
        if (compareSize8 > dtoArraySize8) {
            throw new IOException("UnmarshalException");
        }
    }

    protected void setField(NH_NIS_ERR_DTO dto, JsonReader reader, String name) throws IOException {
        switch (name) {
            case "stdErrCode": {
                if (reader.peek() != JsonToken.NULL) {
                    dto.setStdErrCode(reader.nextString());
                } else {
                    reader.nextNull();
                }
                break;
            }
            case "stdErrMsgCntn": {
                if (reader.peek() != JsonToken.NULL) {
                    dto.setStdErrMsgCntn(reader.nextString());
                } else {
                    reader.nextNull();
                }
                break;
            }
            case "addMsgContents": {
                if (reader.peek() != JsonToken.NULL) {
                    dto.setAddMsgContents(reader.nextString());
                } else {
                    reader.nextNull();
                }
                break;
            }
            case "errClassName": {
                if (reader.peek() != JsonToken.NULL) {
                    dto.setErrClassName(reader.nextString());
                } else {
                    reader.nextNull();
                }
                break;
            }
            case "errFileName": {
                if (reader.peek() != JsonToken.NULL) {
                    dto.setErrFileName(reader.nextString());
                } else {
                    reader.nextNull();
                }
                break;
            }
            case "errMethodName": {
                if (reader.peek() != JsonToken.NULL) {
                    dto.setErrMethodName(reader.nextString());
                } else {
                    reader.nextNull();
                }
                break;
            }
            case "errLineNo": {
                if (reader.peek() != JsonToken.NULL) {
                    dto.setErrLineNo(reader.nextInt());
                } else {
                    reader.nextNull();
                }
                break;
            }
            case "errType": {
                if (reader.peek() != JsonToken.NULL) {
                    dto.setErrType(reader.nextString());
                } else {
                    reader.nextNull();
                }
                break;
            }
            case "stackTrace": {
                if (reader.peek() != JsonToken.NULL) {
                    reader.beginArray();
                    while (reader.hasNext()) {
                        if (reader.peek() != JsonToken.NULL) {
                            dto.addStackTrace(reader.nextString());
                        } else {
                            dto.addStackTrace((String) null);
                            reader.nextNull();
                        }
                    }
                    reader.endArray();
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

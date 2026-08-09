package nhnis.fw.commons.dto.imagelog;

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
        date = "26. 7. 28. 오후 4:34",
        comments = "ImageLogDTOMsgJson"
)
public class ImageLogDTOMsgJson extends JsonMessage {

    public byte[] marshal(DataObject obj) throws MarshalException {
        ImageLogDTO _ImageLogDTO = (ImageLogDTO) obj;
        if (_ImageLogDTO == null) {
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
            marshal(_ImageLogDTO, jw);
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

    public void marshal(nhnis.fw.commons.dto.imagelog.ImageLogDTO _ImageLogDTO, JsonWriter writer)
            throws IOException {
        writer.name("guid");
        if (_ImageLogDTO.getGuid() != null) {
            writer.value(_ImageLogDTO.getGuid());
        } else {
            writer.nullValue();
        }

        writer.name("serviceId");
        if (_ImageLogDTO.getServiceId() != null) {
            writer.value(_ImageLogDTO.getServiceId());
        } else {
            writer.nullValue();
        }

        writer.name("screenId");
        if (_ImageLogDTO.getScreenId() != null) {
            writer.value(_ImageLogDTO.getScreenId());
        } else {
            writer.nullValue();
        }

        writer.name("optrEno");
        if (_ImageLogDTO.getOptrEno() != null) {
            writer.value(_ImageLogDTO.getOptrEno());
        } else {
            writer.nullValue();
        }

        writer.name("clientIp");
        if (_ImageLogDTO.getClientIp() != null) {
            writer.value(_ImageLogDTO.getClientIp());
        } else {
            writer.nullValue();
        }

        writer.name("requestTime");
        if (_ImageLogDTO.getRequestTime() != null) {
            writer.value(_ImageLogDTO.getRequestTime());
        } else {
            writer.nullValue();
        }

        writer.name("responseTime");
        if (_ImageLogDTO.getResponseTime() != null) {
            writer.value(_ImageLogDTO.getResponseTime());
        } else {
            writer.nullValue();
        }

        writer.name("exceptionType");
        if (_ImageLogDTO.getExceptionType() != null) {
            writer.value(_ImageLogDTO.getExceptionType());
        } else {
            writer.nullValue();
        }

        writer.name("exceptionCode");
        if (_ImageLogDTO.getExceptionCode() != null) {
            writer.value(_ImageLogDTO.getExceptionCode());
        } else {
            writer.nullValue();
        }

        writer.name("exceptionMsg");
        if (_ImageLogDTO.getExceptionMsg() != null) {
            writer.value(_ImageLogDTO.getExceptionMsg());
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

    public ImageLogDTO unmarshal(byte[] bytes, int i) throws UnmarshalException {
        ImageLogDTO _ImageLogDTO = new ImageLogDTO();
        ByteArrayInputStream in = null;
        InputStreamReader isr = null;
        BufferedReader reader = null;
        JsonReader jr = null;

        if (bytes.length <= 0)
            return new ImageLogDTO();

        try {
            in = new ByteArrayInputStream(bytes);
            isr = new InputStreamReader(in, this.encoding);
            reader = new BufferedReader(isr);
            jr = new JsonReader(reader);
            jr.beginObject();

            _ImageLogDTO = (ImageLogDTO) unmarshal(jr, _ImageLogDTO);

            jr.endObject();
            jr.close();

        } catch (Exception e) {
            throw new UnmarshalException(e);
        } finally {
            try {
                if (jr != null) {
                    try {
                        jr.close();
                    } catch (IOException e) {
                        throw new UnmarshalException(e);
                    }
                }
            } finally {
                try {
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (IOException e) {
                            throw new UnmarshalException(e);
                        }
                    }
                } finally {
                    try {
                        if (isr != null) {
                            try {
                                isr.close();
                            } catch (IOException e) {
                                throw new UnmarshalException(e);
                            }
                        }
                    } finally {
                        if (in != null) {
                            try {
                                in.close();
                            } catch (IOException e) {
                                throw new UnmarshalException(e);
                            }
                        }
                    }
                }
            }
        }
        return _ImageLogDTO;
    }

    public DataObject unmarshal(byte[] bytes, ImageLogDTO dto) throws UnmarshalException {
        ImageLogDTO _ImageLogDTO = (ImageLogDTO) dto;
        ByteArrayInputStream in = null;
        InputStreamReader isr = null;
        BufferedReader reader = null;
        JsonReader jr = null;

        if (bytes.length <= 0)
            return new ImageLogDTO();

        try {
            in = new ByteArrayInputStream(bytes);
            isr = new InputStreamReader(in, this.encoding);
            reader = new BufferedReader(isr);
            jr = new JsonReader(reader);
            jr.beginObject();

            _ImageLogDTO = (ImageLogDTO) unmarshal(jr, _ImageLogDTO);

            jr.endObject();
            jr.close();

        } catch (Exception e) {
            throw new UnmarshalException(e);
        } finally {
            try {
                if (jr != null) {
                    try {
                        jr.close();
                    } catch (IOException e) {
                        throw new UnmarshalException(e);
                    }
                }
            } finally {
                try {
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (IOException e) {
                            throw new UnmarshalException(e);
                        }
                    }
                } finally {
                    try {
                        if (isr != null) {
                            try {
                                isr.close();
                            } catch (IOException e) {
                                throw new UnmarshalException(e);
                            }
                        }
                    } finally {
                        if (in != null) {
                            try {
                                in.close();
                            } catch (IOException e) {
                                throw new UnmarshalException(e);
                            }
                        }
                    }
                }
            }
        }
        return _ImageLogDTO;
    }

    public DataObject unmarshal(JsonReader reader, DataObject dto) throws IOException {
        while (reader.hasNext()) {
            String name = reader.nextName();
            setField((ImageLogDTO) dto, reader, name);
        }

        validField((ImageLogDTO) dto);

        return dto;
    }

    protected void validField(ImageLogDTO dto) throws IOException {
    }

    protected void setField(ImageLogDTO dto, JsonReader reader, String name) throws IOException {
        switch (name) {
            case "guid": {
                if (reader.peek() != JsonToken.NULL) {
                    dto.setGuid(reader.nextString());
                } else {
                    reader.nextNull();
                }
                break;
            }
            case "serviceId": {
                if (reader.peek() != JsonToken.NULL) {
                    dto.setServiceId(reader.nextString());
                } else {
                    reader.nextNull();
                }
                break;
            }
            case "screenId": {
                if (reader.peek() != JsonToken.NULL) {
                    dto.setScreenId(reader.nextString());
                } else {
                    reader.nextNull();
                }
                break;
            }
            case "optrEno": {
                if (reader.peek() != JsonToken.NULL) {
                    dto.setOptrEno(reader.nextString());
                } else {
                    reader.nextNull();
                }
                break;
            }
            case "clientIp": {
                if (reader.peek() != JsonToken.NULL) {
                    dto.setClientIp(reader.nextString());
                } else {
                    reader.nextNull();
                }
                break;
            }
            case "requestTime": {
                if (reader.peek() != JsonToken.NULL) {
                    dto.setRequestTime(reader.nextString());
                } else {
                    reader.nextNull();
                }
                break;
            }
            case "responseTime": {
                if (reader.peek() != JsonToken.NULL) {
                    dto.setResponseTime(reader.nextString());
                } else {
                    reader.nextNull();
                }
                break;
            }
            case "exceptionType": {
                if (reader.peek() != JsonToken.NULL) {
                    dto.setExceptionType(reader.nextString());
                } else {
                    reader.nextNull();
                }
                break;
            }
            case "exceptionCode": {
                if (reader.peek() != JsonToken.NULL) {
                    dto.setExceptionCode(reader.nextString());
                } else {
                    reader.nextNull();
                }
                break;
            }
            case "exceptionMsg": {
                if (reader.peek() != JsonToken.NULL) {
                    dto.setExceptionMsg(reader.nextString());
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

package nhnis.fw.commons.apigw.dto;

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
        comments = "ApiGatewayDtoMsgJson"
)
public class ApiGatewayDtoMsgJson extends JsonMessage {

    public byte[] marshal(DataObject obj) throws MarshalException {
        ApiGatewayDto _ApiGatewayDto = (ApiGatewayDto) obj;
        if (_ApiGatewayDto == null) {
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
            marshal(_ApiGatewayDto, jw);
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

    public void marshal(nhnis.fw.commons.apigw.dto.ApiGatewayDto _ApiGatewayDto, JsonWriter writer)
            throws IOException {
        writer.name("url");
        if (_ApiGatewayDto.getUrl() != null) {
            writer.value(_ApiGatewayDto.getUrl());
        } else {
            writer.nullValue();
        }

        writer.name("contentType");
        if (_ApiGatewayDto.getContentType() != null) {
            writer.value(_ApiGatewayDto.getContentType());
        } else {
            writer.nullValue();
        }

        writer.name("interfaceId");
        if (_ApiGatewayDto.getInterfaceId() != null) {
            writer.value(_ApiGatewayDto.getInterfaceId());
        } else {
            writer.nullValue();
        }

        writer.name("recvTrxName");
        if (_ApiGatewayDto.getRecvTrxName() != null) {
            writer.value(_ApiGatewayDto.getRecvTrxName());
        } else {
            writer.nullValue();
        }

        writer.name("recvType");
        if (_ApiGatewayDto.getRecvType() != null) {
            writer.value(_ApiGatewayDto.getRecvType());
        } else {
            writer.nullValue();
        }

        writer.name("replyType");
        if (_ApiGatewayDto.getReplyType() != null) {
            writer.value(_ApiGatewayDto.getReplyType());
        } else {
            writer.nullValue();
        }

        writer.name("body");
        if (_ApiGatewayDto.getBody() != null) {
            writer.value(_ApiGatewayDto.getBody());
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

    public ApiGatewayDto unmarshal(byte[] bytes, int i) throws UnmarshalException {
        ApiGatewayDto _ApiGatewayDto = new ApiGatewayDto();
        ByteArrayInputStream in = null;
        InputStreamReader isr = null;
        BufferedReader reader = null;
        JsonReader jr = null;

        if (bytes.length <= 0)
            return new ApiGatewayDto();

        try {
            in = new ByteArrayInputStream(bytes);
            isr = new InputStreamReader(in, this.encoding);
            reader = new BufferedReader(isr);
            jr = new JsonReader(reader);
            jr.beginObject();

            _ApiGatewayDto = (ApiGatewayDto) unmarshal(jr, _ApiGatewayDto);

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
        return _ApiGatewayDto;
    }

    public DataObject unmarshal(byte[] bytes, ApiGatewayDto dto) throws UnmarshalException {
        ApiGatewayDto _ApiGatewayDto = (ApiGatewayDto) dto;
        ByteArrayInputStream in = null;
        InputStreamReader isr = null;
        BufferedReader reader = null;
        JsonReader jr = null;

        if (bytes.length <= 0)
            return new ApiGatewayDto();

        try {
            in = new ByteArrayInputStream(bytes);
            isr = new InputStreamReader(in, this.encoding);
            reader = new BufferedReader(isr);
            jr = new JsonReader(reader);
            jr.beginObject();

            _ApiGatewayDto = (ApiGatewayDto) unmarshal(jr, _ApiGatewayDto);

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
        return _ApiGatewayDto;
    }

    public DataObject unmarshal(JsonReader reader, DataObject dto) throws IOException {
        while (reader.hasNext()) {
            String name = reader.nextName();
            setField((ApiGatewayDto) dto, reader, name);
        }

        validField((ApiGatewayDto) dto);

        return dto;
    }

    protected void validField(ApiGatewayDto dto) throws IOException {
    }

    protected void setField(ApiGatewayDto dto, JsonReader reader, String name) throws IOException {
        switch (name) {
            case "url": {
                if (reader.peek() != JsonToken.NULL) {
                    dto.setUrl(reader.nextString());
                } else {
                    reader.nextNull();
                }
                break;
            }
            case "contentType": {
                if (reader.peek() != JsonToken.NULL) {
                    dto.setContentType(reader.nextString());
                } else {
                    reader.nextNull();
                }
                break;
            }
            case "interfaceId": {
                if (reader.peek() != JsonToken.NULL) {
                    dto.setInterfaceId(reader.nextString());
                } else {
                    reader.nextNull();
                }
                break;
            }
            case "recvTrxName": {
                if (reader.peek() != JsonToken.NULL) {
                    dto.setRecvTrxName(reader.nextString());
                } else {
                    reader.nextNull();
                }
                break;
            }
            case "recvType": {
                if (reader.peek() != JsonToken.NULL) {
                    dto.setRecvType(reader.nextString());
                } else {
                    reader.nextNull();
                }
                break;
            }
            case "replyType": {
                if (reader.peek() != JsonToken.NULL) {
                    dto.setReplyType(reader.nextString());
                } else {
                    reader.nextNull();
                }
                break;
            }
            case "body": {
                if (reader.peek() != JsonToken.NULL) {
                    dto.setBody(reader.nextString());
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

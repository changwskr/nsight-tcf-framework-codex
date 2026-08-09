package nhnis.fw.commons.fos;

import java.io.InputStream;

import org.springframework.http.MediaType;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ObjectStorageDto {

    private boolean isSuccess = false;
    private int httpStatusCode = -1;
    private InputStream inputSteram;
    private MediaType contentType;
}

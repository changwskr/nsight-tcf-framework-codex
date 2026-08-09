package nhnis.fw.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import nhnis.mg.PdmgApplication;

@SpringBootTest(classes = PdmgApplication.class)
class ExceptionCodePropertiesTest {

    @Autowired
    private ExceptionCodeProperties properties;

    @Test
    void containsBusinessMessages() {
        assertThat(properties.message("MP0404"))
                .isEqualTo("요청한 영업팁 실적을 찾을 수 없습니다.");
        assertThat(properties.message("MP0409"))
                .isEqualTo("동일한 기본키의 영업팁 실적이 이미 존재합니다.");
    }
}

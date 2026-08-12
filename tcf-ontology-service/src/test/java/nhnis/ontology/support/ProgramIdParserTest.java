package nhnis.ontology.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ProgramIdParserTest {

    @Test
    void accepts_mgcoa8888_as_program() {
        assertThat(ProgramIdParser.isValid("mgcoa8888")).isTrue();
        assertThat(ProgramIdParser.canonical("MGCOA8888")).isEqualTo("mgcoa8888");
    }

    @Test
    void rejects_serviceId_shape() {
        assertThat(ProgramIdParser.isValid("mgcoa8888S0")).isFalse();
        assertThat(ServiceIdParser.isValid("mgcoa8888S0")).isTrue();
        assertThat(ServiceIdParser.isValid("mgcoa8888")).isFalse();
    }

    @Test
    void canonical_rejects_invalid() {
        assertThatThrownBy(() -> ProgramIdParser.canonical("mgcoa8888S0"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

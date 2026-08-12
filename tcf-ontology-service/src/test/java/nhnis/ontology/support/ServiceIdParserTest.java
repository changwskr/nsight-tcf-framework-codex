package nhnis.ontology.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import nhnis.ontology.domain.concept.ServiceIdParts;

class ServiceIdParserTest {

    @Test
    void parse_mgcoa8888S0() {
        ServiceIdParts parts = ServiceIdParser.parse("mgcoa8888S0");
        assertThat(parts.getGroupCode()).isEqualTo("mg");
        assertThat(parts.getBusinessCode()).isEqualTo("co");
        assertThat(parts.getFunctionCode()).isEqualTo("a");
        assertThat(parts.getProgramNo()).isEqualTo("8888");
        assertThat(parts.getOperationType()).isEqualTo("S");
        assertThat(parts.getSequence()).isEqualTo("0");
        assertThat(parts.getFullServiceId()).isEqualTo("mgcoa8888S0");
        assertThat(parts.programId()).isEqualTo("mgcoa8888");
    }

    @Test
    void parse_normalizesCase() {
        assertThat(ServiceIdParser.canonical("MGCOA8888s0")).isEqualTo("mgcoa8888S0");
    }

    @Test
    void parse_rejectsInvalid() {
        assertThatThrownBy(() -> ServiceIdParser.parse("mgcoa8888"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(ServiceIdParser.isValid("mgcoa8888S0")).isTrue();
        assertThat(ServiceIdParser.isValid("bad")).isFalse();
    }
}

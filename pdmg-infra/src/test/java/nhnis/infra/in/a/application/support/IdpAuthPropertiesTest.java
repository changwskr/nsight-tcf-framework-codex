package nhnis.infra.in.a.application.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class IdpAuthPropertiesTest {

    @Test
    void mapsIdpAliasesAndDirectRaciCodes() {
        IdpAuthProperties props = new IdpAuthProperties();
        assertThat(props.resolveRaciRole("infra-ops")).isEqualTo("OPS");
        assertThat(props.resolveRaciRole("DBA")).isEqualTo("DBA");
        assertThat(props.resolveRaciRole("middleware")).isEqualTo("MW");
        assertThat(props.resolveRaciRole("unknown-group")).isNull();
        assertThat(props.resolveRaciRole("")).isNull();
    }
}

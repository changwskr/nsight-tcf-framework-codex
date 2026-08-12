package nhnis.fw.tcf.timeout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import org.junit.jupiter.api.Test;

class OnlineTimeoutPropertiesTest {

    @Test
    void resolveUsesOverrideWhenPresent() {
        OnlineTimeoutProperties properties = new OnlineTimeoutProperties();
        properties.setMilliseconds(5000);
        properties.setOverrides(Map.of("mgcoa5530S0", 10000L));

        assertThat(properties.resolveMilliseconds("mgcoa5530S0")).isEqualTo(10000L);
        assertThat(properties.resolveMilliseconds("mgcoa8888S0")).isEqualTo(5000L);
        assertThat(properties.resolveMilliseconds(null)).isEqualTo(5000L);
    }

    @Test
    void validateRejectsInvalidOverride() {
        OnlineTimeoutProperties properties = new OnlineTimeoutProperties();
        properties.setMilliseconds(5000);
        properties.setOverrides(Map.of("mgcoa5530S0", 0L));

        assertThatThrownBy(properties::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("overrides.mgcoa5530S0");
    }
}

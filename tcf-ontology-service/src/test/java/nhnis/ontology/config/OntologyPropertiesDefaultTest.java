package nhnis.ontology.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("prod")
class OntologyPropertiesDefaultTest {

    @Autowired
    private OntologyProperties properties;

    @Test
    void prod_profile_keeps_admin_mutations_disabled() {
        assertThat(properties.isAdminMutationsEnabled()).isFalse();
    }

    @Test
    void java_bean_default_is_disabled() {
        assertThat(new OntologyProperties().isAdminMutationsEnabled()).isFalse();
    }
}

package nhnis.ontology.prompt;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class PromptRuntimeDynamicTest {

    @Autowired
    private PromptContextExporter exporter;

    @Test
    void runtime_summary_comes_from_tx_runtime_yaml_steps() {
        String md = exporter.asMarkdown("mgcoa8888");
        assertThat(md).contains("source: ontology/technical/tx-runtime.yml steps (dynamic)");
        assertThat(md).contains("DefaultFilter");
        assertThat(md).doesNotContain("RequestThread(TX밖): Filter → Interceptor.pre → Controller → TcfFacade → Future.get");
    }
}

package nhnis.ontology.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RuntimeTxChainQueryTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void runtime_tx_chain_from_yaml() {
        ResponseEntity<String> response =
                restTemplate.getForEntity("/api/ontology/query/runtime/tx-chain", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("RUNTIME");
        assertThat(response.getBody()).contains("DefaultFilter");
        assertThat(response.getBody()).contains("OnlineTimeoutExecutor");
        assertThat(response.getBody()).contains("TransactionDispatcher");
        assertThat(response.getBody()).contains("FLOWS_TO");
        assertThat(response.getBody()).contains("STARTS_TRANSACTION");
        assertThat(response.getBody()).contains("DISPATCHES_TO");
        assertThat(response.getBody()).contains("rdw-TransactionTemplate");
    }

    @Test
    void facade_runtime_includes_yaml() {
        ResponseEntity<String> response =
                restTemplate.getForEntity("/api/ontology/runtime/tx-chain", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("yamlRuntime");
        assertThat(response.getBody()).contains("nhnis.fw.timeout.enabled");
    }
}

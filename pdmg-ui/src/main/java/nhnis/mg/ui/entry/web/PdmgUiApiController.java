package nhnis.mg.ui.entry.web;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import nhnis.mg.ui.application.service.TransactionCatalog;
import nhnis.mg.ui.client.TransactionRelayService;
import nhnis.mg.ui.config.PdmgUiProperties;
import nhnis.mg.ui.support.RelayResult;
import nhnis.mg.ui.support.TransactionInfo;

@RestController
@RequestMapping("/api")
public class PdmgUiApiController {

    private final TransactionCatalog catalog;
    private final TransactionRelayService relayService;
    private final PdmgUiProperties properties;

    public PdmgUiApiController(TransactionCatalog catalog, TransactionRelayService relayService,
            PdmgUiProperties properties) {
        this.catalog = catalog;
        this.relayService = relayService;
        this.properties = properties;
    }

    @GetMapping("/config")
    public Map<String, Object> config() {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("targetBaseUrl", properties.getTargetBaseUrl());
        config.put("timeoutMs", properties.getTimeoutMs());
        return config;
    }

    @GetMapping("/transactions")
    public List<TransactionInfo> transactions() {
        return catalog.findAll();
    }

    @GetMapping("/transactions/{id}")
    public TransactionInfo transaction(@PathVariable("id") String id) {
        return catalog.findById(id);
    }

    @GetMapping("/transactions/{id}/target-url")
    public Map<String, String> targetUrl(@PathVariable("id") String id,
            @RequestParam(value = "baseUrl", required = false) String baseUrl) {
        return Map.of("targetUrl", relayService.resolveTargetUrl(id, baseUrl));
    }

    @PostMapping("/relay/{id}")
    public RelayResult relay(@PathVariable("id") String id,
            @RequestBody(required = false) String requestBody,
            @RequestParam(value = "baseUrl", required = false) String baseUrl) {
        return relayService.relay(id, requestBody, baseUrl);
    }

    @PostMapping("/imagelog/list")
    public RelayResult imageLogList(@RequestBody(required = false) String requestBody,
            @RequestParam(value = "baseUrl", required = false) String baseUrl) {
        return relayService.relay("mgcoa8888S0", requestBody, baseUrl);
    }

    @PostMapping("/imagelog/delete")
    public RelayResult imageLogDelete(@RequestBody(required = false) String requestBody,
            @RequestParam(value = "baseUrl", required = false) String baseUrl) {
        return relayService.relay("mgcoa8888D0", requestBody, baseUrl);
    }
}

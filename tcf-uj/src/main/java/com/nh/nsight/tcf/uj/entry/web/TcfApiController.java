package com.nh.nsight.tcf.uj.entry.web;

import com.nh.nsight.tcf.uj.config.TcfUjProperties;
import com.nh.nsight.tcf.uj.support.BusinessModuleInfo;
import com.nh.nsight.tcf.uj.support.BusinessModuleTransactions;
import com.nh.nsight.tcf.uj.support.BusinessTransactionInfo;
import com.nh.nsight.tcf.uj.support.RelayResult;
import com.nh.nsight.tcf.uj.application.service.BusinessModuleCatalog;
import com.nh.nsight.tcf.uj.application.service.BusinessTransactionCatalog;
import com.nh.nsight.tcf.uj.client.GatewayRelayService;
import com.nh.nsight.tcf.uj.client.TransactionRelayService;
import com.nh.nsight.tcf.uj.client.TransactionRelayService.RelayOptions;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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

@RestController
@RequestMapping("/api")
public class TcfApiController {
    private final BusinessModuleCatalog catalog;
    private final BusinessTransactionCatalog transactionCatalog;
    private final TransactionRelayService relayService;
    private final GatewayRelayService gatewayRelayService;
    private final TcfUjProperties properties;

    public TcfApiController(BusinessModuleCatalog catalog, BusinessTransactionCatalog transactionCatalog,
                             TransactionRelayService relayService, GatewayRelayService gatewayRelayService,
                             TcfUjProperties properties) {
        this.catalog = catalog;
        this.transactionCatalog = transactionCatalog;
        this.relayService = relayService;
        this.gatewayRelayService = gatewayRelayService;
        this.properties = properties;
    }

    @GetMapping("/business-modules")
    public List<BusinessModuleInfo> businessModules() {
        return catalog.findAll();
    }

    @GetMapping("/business-modules/{code}")
    public BusinessModuleInfo businessModule(@PathVariable("code") String code) {
        return catalog.findByCode(code);
    }

    @GetMapping("/multi/business-modules")
    public List<BusinessModuleTransactions> multiBusinessModules() {
        return transactionCatalog.findAll();
    }

    @GetMapping("/multi/business-modules/{code}")
    public BusinessModuleTransactions multiBusinessModule(@PathVariable("code") String code) {
        return transactionCatalog.findByCode(code);
    }

    @GetMapping("/multi/business-modules/{code}/transactions/{transactionId}")
    public BusinessTransactionInfo multiTransaction(
            @PathVariable("code") String code,
            @PathVariable("transactionId") String transactionId) {
        return transactionCatalog.findTransaction(code, transactionId);
    }

    @GetMapping("/multi/business-modules/{code}/target-url")
    public Map<String, String> multiTargetUrl(
            @PathVariable("code") String code,
            @RequestParam(value = "deploymentMode", required = false) String deploymentMode,
            @RequestParam(value = "bootrunHost", required = false) String bootrunHost,
            @RequestParam(value = "tomcatGatewayUrl", required = false) String tomcatGatewayUrl) {
        RelayOptions options = new RelayOptions(deploymentMode, bootrunHost, tomcatGatewayUrl);
        return Map.of("targetUrl", relayService.resolveTargetUrl(code, options));
    }

    @PostMapping("/multi/relay/{code}/online")
    public RelayResult multiRelay(
            @PathVariable("code") String code,
            @RequestBody String requestBody,
            @RequestParam(value = "deploymentMode", required = false) String deploymentMode,
            @RequestParam(value = "bootrunHost", required = false) String bootrunHost,
            @RequestParam(value = "tomcatGatewayUrl", required = false) String tomcatGatewayUrl,
            HttpServletRequest request,
            HttpServletResponse response) {
        RelayOptions options = new RelayOptions(deploymentMode, bootrunHost, tomcatGatewayUrl);
        RelayResult result = relayService.relay(code, requestBody, options, request.getHeader("Cookie"));
        applySetCookies(response, result);
        return result;
    }

    @GetMapping("/config")
    public Map<String, Object> config() {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("deploymentMode", properties.getDeploymentMode().name());
        config.put("tomcatGatewayUrl", properties.getTomcatGatewayUrl());
        config.put("bootrunHost", properties.getBootrunHost());
        config.put("gatewayEnabled", true);
        config.put("gatewayOmUrl", gatewayRelayService.resolveGatewayOmUrl(
                new RelayOptions(properties.getDeploymentMode().name(), properties.getBootrunHost(),
                        properties.getTomcatGatewayUrl())));
        config.put("omGatewayEnabled", properties.isOmGatewayEnabled());
        return config;
    }

    @GetMapping("/gateway/om/target-url")
    public Map<String, String> gatewayOmTargetUrl(
            @RequestParam(value = "deploymentMode", required = false) String deploymentMode,
            @RequestParam(value = "bootrunHost", required = false) String bootrunHost,
            @RequestParam(value = "tomcatGatewayUrl", required = false) String tomcatGatewayUrl) {
        RelayOptions options = new RelayOptions(deploymentMode, bootrunHost, tomcatGatewayUrl);
        return Map.of("targetUrl", gatewayRelayService.resolveGatewayOmUrl(options));
    }

    @PostMapping("/gateway/om/online")
    public RelayResult gatewayOmRelay(
            @RequestBody String requestBody,
            @RequestParam(value = "deploymentMode", required = false) String deploymentMode,
            @RequestParam(value = "bootrunHost", required = false) String bootrunHost,
            @RequestParam(value = "tomcatGatewayUrl", required = false) String tomcatGatewayUrl,
            HttpServletRequest request,
            HttpServletResponse response) {
        RelayOptions options = new RelayOptions(deploymentMode, bootrunHost, tomcatGatewayUrl);
        RelayResult result = relayService.relay("OM", requestBody, options, request.getHeader("Cookie"));
        applySetCookies(response, result);
        return result;
    }

    @GetMapping("/business-modules/{code}/target-url")
    public Map<String, String> targetUrl(
            @PathVariable("code") String code,
            @RequestParam(value = "deploymentMode", required = false) String deploymentMode,
            @RequestParam(value = "bootrunHost", required = false) String bootrunHost,
            @RequestParam(value = "tomcatGatewayUrl", required = false) String tomcatGatewayUrl) {
        RelayOptions options = new RelayOptions(deploymentMode, bootrunHost, tomcatGatewayUrl);
        return Map.of("targetUrl", relayService.resolveTargetUrl(code, options));
    }

    @PostMapping("/relay/{code}/online")
    public RelayResult relay(
            @PathVariable("code") String code,
            @RequestBody String requestBody,
            @RequestParam(value = "deploymentMode", required = false) String deploymentMode,
            @RequestParam(value = "bootrunHost", required = false) String bootrunHost,
            @RequestParam(value = "tomcatGatewayUrl", required = false) String tomcatGatewayUrl,
            HttpServletRequest request,
            HttpServletResponse response) {
        RelayOptions options = new RelayOptions(deploymentMode, bootrunHost, tomcatGatewayUrl);
        RelayResult result = relayService.relay(code, requestBody, options, request.getHeader("Cookie"));
        applySetCookies(response, result);
        return result;
    }

    private void applySetCookies(HttpServletResponse response, RelayResult result) {
        if (result.setCookies() == null) {
            return;
        }
        for (String setCookie : result.setCookies()) {
            response.addHeader("Set-Cookie", setCookie);
        }
    }
}

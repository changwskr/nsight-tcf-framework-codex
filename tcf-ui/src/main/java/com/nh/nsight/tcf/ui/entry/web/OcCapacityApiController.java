package com.nh.nsight.tcf.ui.entry.web;

import com.nh.nsight.tcf.ui.client.OcRelayService;
import com.nh.nsight.tcf.ui.client.TransactionRelayService.RelayOptions;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/oc/capacity")
public class OcCapacityApiController {
    private final OcRelayService relayService;

    public OcCapacityApiController(OcRelayService relayService) {
        this.relayService = relayService;
    }

    @GetMapping("/base-url")
    public Map<String, String> baseUrl(
            @RequestParam(value = "deploymentMode", required = false) String deploymentMode,
            @RequestParam(value = "bootrunHost", required = false) String bootrunHost,
            @RequestParam(value = "tomcatGatewayUrl", required = false) String tomcatGatewayUrl) {
        RelayOptions options = new RelayOptions(deploymentMode, bootrunHost, tomcatGatewayUrl);
        return Map.of("baseUrl", relayService.resolveBaseUrl(options));
    }

    @GetMapping(value = "/defaults", produces = MediaType.APPLICATION_JSON_VALUE)
    public String defaults(
            @RequestParam(value = "deploymentMode", required = false) String deploymentMode,
            @RequestParam(value = "bootrunHost", required = false) String bootrunHost,
            @RequestParam(value = "tomcatGatewayUrl", required = false) String tomcatGatewayUrl) {
        RelayOptions options = new RelayOptions(deploymentMode, bootrunHost, tomcatGatewayUrl);
        return relayService.relayGet("/defaults", options);
    }

    @PostMapping(value = "/calculate", produces = MediaType.APPLICATION_JSON_VALUE)
    public String calculate(
            @RequestBody String requestBody,
            @RequestParam(value = "deploymentMode", required = false) String deploymentMode,
            @RequestParam(value = "bootrunHost", required = false) String bootrunHost,
            @RequestParam(value = "tomcatGatewayUrl", required = false) String tomcatGatewayUrl) {
        RelayOptions options = new RelayOptions(deploymentMode, bootrunHost, tomcatGatewayUrl);
        return relayService.relayPost("/calculate", requestBody, options);
    }

    @PostMapping(value = "/calculate-step", produces = MediaType.APPLICATION_JSON_VALUE)
    public String calculateStep(
            @RequestBody String requestBody,
            @RequestParam(value = "deploymentMode", required = false) String deploymentMode,
            @RequestParam(value = "bootrunHost", required = false) String bootrunHost,
            @RequestParam(value = "tomcatGatewayUrl", required = false) String tomcatGatewayUrl) {
        RelayOptions options = new RelayOptions(deploymentMode, bootrunHost, tomcatGatewayUrl);
        return relayService.relayPost("/calculate-step", requestBody, options);
    }

    @PostMapping(value = "/was-thread/calculate", produces = MediaType.APPLICATION_JSON_VALUE)
    public String wasThreadCalculate(
            @RequestBody String requestBody,
            @RequestParam(value = "deploymentMode", required = false) String deploymentMode,
            @RequestParam(value = "bootrunHost", required = false) String bootrunHost,
            @RequestParam(value = "tomcatGatewayUrl", required = false) String tomcatGatewayUrl) {
        RelayOptions options = new RelayOptions(deploymentMode, bootrunHost, tomcatGatewayUrl);
        return relayService.relayPost("/was-thread/calculate", requestBody, options);
    }
}

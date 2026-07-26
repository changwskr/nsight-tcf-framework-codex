package com.nh.nsight.tcf.ui.entry.web;

import com.nh.nsight.tcf.ui.client.OcRelayService;
import com.nh.nsight.tcf.ui.client.TransactionRelayService.RelayOptions;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/oc/cap-new")
public class OcCapNewApiController {

    private final OcRelayService relayService;

    public OcCapNewApiController(OcRelayService relayService) {
        this.relayService = relayService;
    }

    @GetMapping(value = "/defaults", produces = MediaType.APPLICATION_JSON_VALUE)
    public String defaults(
            @RequestParam(value = "deploymentMode", required = false) String deploymentMode,
            @RequestParam(value = "bootrunHost", required = false) String bootrunHost,
            @RequestParam(value = "tomcatGatewayUrl", required = false) String tomcatGatewayUrl) {
        RelayOptions options = new RelayOptions(deploymentMode, bootrunHost, tomcatGatewayUrl);
        return relayService.relayCapNewGet("/defaults", options);
    }

    @GetMapping(value = "/templates", produces = MediaType.APPLICATION_JSON_VALUE)
    public String listTemplates(
            @RequestParam(value = "deploymentMode", required = false) String deploymentMode,
            @RequestParam(value = "bootrunHost", required = false) String bootrunHost,
            @RequestParam(value = "tomcatGatewayUrl", required = false) String tomcatGatewayUrl) {
        RelayOptions options = new RelayOptions(deploymentMode, bootrunHost, tomcatGatewayUrl);
        return relayService.relayCapNewGet("/templates", options);
    }

    @GetMapping(value = "/templates/{code}", produces = MediaType.APPLICATION_JSON_VALUE)
    public String getTemplate(
            @PathVariable String code,
            @RequestParam(value = "deploymentMode", required = false) String deploymentMode,
            @RequestParam(value = "bootrunHost", required = false) String bootrunHost,
            @RequestParam(value = "tomcatGatewayUrl", required = false) String tomcatGatewayUrl) {
        RelayOptions options = new RelayOptions(deploymentMode, bootrunHost, tomcatGatewayUrl);
        return relayService.relayCapNewGet("/templates/" + code, options);
    }

    @GetMapping(value = "/scenarios", produces = MediaType.APPLICATION_JSON_VALUE)
    public String listScenarios(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "deploymentMode", required = false) String deploymentMode,
            @RequestParam(value = "bootrunHost", required = false) String bootrunHost,
            @RequestParam(value = "tomcatGatewayUrl", required = false) String tomcatGatewayUrl) {
        RelayOptions options = new RelayOptions(deploymentMode, bootrunHost, tomcatGatewayUrl);
        String path = status != null && !status.isBlank() ? "/scenarios?status=" + status : "/scenarios";
        return relayService.relayCapNewGet(path, options);
    }

    @PostMapping(value = "/compare", produces = MediaType.APPLICATION_JSON_VALUE)
    public String compare(
            @RequestBody String requestBody,
            @RequestParam(value = "deploymentMode", required = false) String deploymentMode,
            @RequestParam(value = "bootrunHost", required = false) String bootrunHost,
            @RequestParam(value = "tomcatGatewayUrl", required = false) String tomcatGatewayUrl) {
        RelayOptions options = new RelayOptions(deploymentMode, bootrunHost, tomcatGatewayUrl);
        return relayService.relayCapNewPost("/compare", requestBody, options);
    }

    @PostMapping(value = "/scenarios", produces = MediaType.APPLICATION_JSON_VALUE)
    public String createScenario(
            @RequestBody String requestBody,
            @RequestParam(value = "deploymentMode", required = false) String deploymentMode,
            @RequestParam(value = "bootrunHost", required = false) String bootrunHost,
            @RequestParam(value = "tomcatGatewayUrl", required = false) String tomcatGatewayUrl) {
        RelayOptions options = new RelayOptions(deploymentMode, bootrunHost, tomcatGatewayUrl);
        return relayService.relayCapNewPost("/scenarios", requestBody, options);
    }

    @GetMapping(value = "/scenarios/{scenarioId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public String getScenario(
            @PathVariable String scenarioId,
            @RequestParam(value = "deploymentMode", required = false) String deploymentMode,
            @RequestParam(value = "bootrunHost", required = false) String bootrunHost,
            @RequestParam(value = "tomcatGatewayUrl", required = false) String tomcatGatewayUrl) {
        RelayOptions options = new RelayOptions(deploymentMode, bootrunHost, tomcatGatewayUrl);
        return relayService.relayCapNewGet("/scenarios/" + scenarioId, options);
    }

    @PutMapping(value = "/scenarios/{scenarioId}/step/{stepNumber}", produces = MediaType.APPLICATION_JSON_VALUE)
    public String saveStep(
            @PathVariable String scenarioId,
            @PathVariable int stepNumber,
            @RequestBody String requestBody,
            @RequestParam(value = "deploymentMode", required = false) String deploymentMode,
            @RequestParam(value = "bootrunHost", required = false) String bootrunHost,
            @RequestParam(value = "tomcatGatewayUrl", required = false) String tomcatGatewayUrl) {
        RelayOptions options = new RelayOptions(deploymentMode, bootrunHost, tomcatGatewayUrl);
        return relayService.relayCapNewPut("/scenarios/" + scenarioId + "/step/" + stepNumber, requestBody, options);
    }

    @DeleteMapping(value = "/scenarios/{scenarioId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public String deleteScenario(
            @PathVariable String scenarioId,
            @RequestParam(value = "deploymentMode", required = false) String deploymentMode,
            @RequestParam(value = "bootrunHost", required = false) String bootrunHost,
            @RequestParam(value = "tomcatGatewayUrl", required = false) String tomcatGatewayUrl) {
        RelayOptions options = new RelayOptions(deploymentMode, bootrunHost, tomcatGatewayUrl);
        return relayService.relayCapNewDelete("/scenarios/" + scenarioId, options);
    }

    @GetMapping(value = "/approvals", produces = MediaType.APPLICATION_JSON_VALUE)
    public String listApprovals(
            @RequestParam(value = "deploymentMode", required = false) String deploymentMode,
            @RequestParam(value = "bootrunHost", required = false) String bootrunHost,
            @RequestParam(value = "tomcatGatewayUrl", required = false) String tomcatGatewayUrl) {
        RelayOptions options = new RelayOptions(deploymentMode, bootrunHost, tomcatGatewayUrl);
        return relayService.relayCapNewGet("/approvals", options);
    }

    @PostMapping(value = "/scenarios/{scenarioId}/approve", produces = MediaType.APPLICATION_JSON_VALUE)
    public String approve(
            @PathVariable String scenarioId,
            @RequestBody String requestBody,
            @RequestParam(value = "deploymentMode", required = false) String deploymentMode,
            @RequestParam(value = "bootrunHost", required = false) String bootrunHost,
            @RequestParam(value = "tomcatGatewayUrl", required = false) String tomcatGatewayUrl) {
        RelayOptions options = new RelayOptions(deploymentMode, bootrunHost, tomcatGatewayUrl);
        return relayService.relayCapNewPost("/scenarios/" + scenarioId + "/approve", requestBody, options);
    }

    @PostMapping(value = "/scenarios/{scenarioId}/revoke", produces = MediaType.APPLICATION_JSON_VALUE)
    public String revoke(
            @PathVariable String scenarioId,
            @RequestBody String requestBody,
            @RequestParam(value = "deploymentMode", required = false) String deploymentMode,
            @RequestParam(value = "bootrunHost", required = false) String bootrunHost,
            @RequestParam(value = "tomcatGatewayUrl", required = false) String tomcatGatewayUrl) {
        RelayOptions options = new RelayOptions(deploymentMode, bootrunHost, tomcatGatewayUrl);
        return relayService.relayCapNewPost("/scenarios/" + scenarioId + "/revoke", requestBody, options);
    }

    @PostMapping(value = "/scenarios/{scenarioId}/clone", produces = MediaType.APPLICATION_JSON_VALUE)
    public String cloneVersion(
            @PathVariable String scenarioId,
            @RequestBody(required = false) String requestBody,
            @RequestParam(value = "deploymentMode", required = false) String deploymentMode,
            @RequestParam(value = "bootrunHost", required = false) String bootrunHost,
            @RequestParam(value = "tomcatGatewayUrl", required = false) String tomcatGatewayUrl) {
        RelayOptions options = new RelayOptions(deploymentMode, bootrunHost, tomcatGatewayUrl);
        return relayService.relayCapNewPost("/scenarios/" + scenarioId + "/clone", requestBody == null ? "{}" : requestBody, options);
    }

    @GetMapping(value = "/scenarios/{scenarioId}/env-handoff", produces = MediaType.APPLICATION_JSON_VALUE)
    public String envHandoff(
            @PathVariable String scenarioId,
            @RequestParam(value = "deploymentMode", required = false) String deploymentMode,
            @RequestParam(value = "bootrunHost", required = false) String bootrunHost,
            @RequestParam(value = "tomcatGatewayUrl", required = false) String tomcatGatewayUrl) {
        RelayOptions options = new RelayOptions(deploymentMode, bootrunHost, tomcatGatewayUrl);
        return relayService.relayCapNewGet("/scenarios/" + scenarioId + "/env-handoff", options);
    }

    @GetMapping(value = "/scenarios/{scenarioId}/legacy-compare", produces = MediaType.APPLICATION_JSON_VALUE)
    public String legacyCompare(
            @PathVariable String scenarioId,
            @RequestParam(value = "deploymentMode", required = false) String deploymentMode,
            @RequestParam(value = "bootrunHost", required = false) String bootrunHost,
            @RequestParam(value = "tomcatGatewayUrl", required = false) String tomcatGatewayUrl) {
        RelayOptions options = new RelayOptions(deploymentMode, bootrunHost, tomcatGatewayUrl);
        return relayService.relayCapNewGet("/scenarios/" + scenarioId + "/legacy-compare", options);
    }

    @GetMapping(value = "/scenarios/{scenarioId}/vm-compare", produces = MediaType.APPLICATION_JSON_VALUE)
    public String vmCompare(
            @PathVariable String scenarioId,
            @RequestParam(value = "profiles", required = false) String profiles,
            @RequestParam(value = "deploymentMode", required = false) String deploymentMode,
            @RequestParam(value = "bootrunHost", required = false) String bootrunHost,
            @RequestParam(value = "tomcatGatewayUrl", required = false) String tomcatGatewayUrl) {
        RelayOptions options = new RelayOptions(deploymentMode, bootrunHost, tomcatGatewayUrl);
        String path = "/scenarios/" + scenarioId + "/vm-compare";
        if (profiles != null && !profiles.isBlank()) {
            path += "?profiles=" + java.net.URLEncoder.encode(profiles, java.nio.charset.StandardCharsets.UTF_8);
        }
        return relayService.relayCapNewGet(path, options);
    }

    @PostMapping(value = "/scenarios/{scenarioId}/export/excel",
            produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> exportScenarioExcel(
            @PathVariable String scenarioId,
            @RequestParam(value = "deploymentMode", required = false) String deploymentMode,
            @RequestParam(value = "bootrunHost", required = false) String bootrunHost,
            @RequestParam(value = "tomcatGatewayUrl", required = false) String tomcatGatewayUrl) {
        RelayOptions options = new RelayOptions(deploymentMode, bootrunHost, tomcatGatewayUrl);
        byte[] body = relayService.relayCapNewPostBinary(
                "/scenarios/" + scenarioId + "/export/excel", "{}", options);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(body);
    }

    @PostMapping(value = "/export/excel",
            produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> exportExcel(
            @RequestBody String requestBody,
            @RequestParam(value = "deploymentMode", required = false) String deploymentMode,
            @RequestParam(value = "bootrunHost", required = false) String bootrunHost,
            @RequestParam(value = "tomcatGatewayUrl", required = false) String tomcatGatewayUrl) {
        RelayOptions options = new RelayOptions(deploymentMode, bootrunHost, tomcatGatewayUrl);
        byte[] body = relayService.relayCapNewPostBinary("/export/excel", requestBody, options);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(body);
    }
}

package com.nh.nsight.tcf.ui.entry.web;

import com.nh.nsight.tcf.ui.client.OcRelayService;
import com.nh.nsight.tcf.ui.client.TransactionRelayService.RelayOptions;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/oc/env")
public class OcEnvApiController {

    private final OcRelayService relayService;

    public OcEnvApiController(OcRelayService relayService) {
        this.relayService = relayService;
    }

    @GetMapping(value = "/capacity-design/defaults", produces = MediaType.APPLICATION_JSON_VALUE)
    public String capacityDesignDefaults(
            @RequestParam(value = "deploymentMode", required = false) String deploymentMode,
            @RequestParam(value = "bootrunHost", required = false) String bootrunHost,
            @RequestParam(value = "tomcatGatewayUrl", required = false) String tomcatGatewayUrl) {
        return relayService.relayEnvGet("/capacity-design/defaults",
                new RelayOptions(deploymentMode, bootrunHost, tomcatGatewayUrl));
    }

    @PostMapping(value = "/capacity-design/analyze", produces = MediaType.APPLICATION_JSON_VALUE)
    public String analyzeCapacityDesign(
            @RequestBody String requestBody,
            @RequestParam(value = "deploymentMode", required = false) String deploymentMode,
            @RequestParam(value = "bootrunHost", required = false) String bootrunHost,
            @RequestParam(value = "tomcatGatewayUrl", required = false) String tomcatGatewayUrl) {
        return relayService.relayEnvPost("/capacity-design/analyze", requestBody,
                new RelayOptions(deploymentMode, bootrunHost, tomcatGatewayUrl));
    }

    @GetMapping(value = "/settings", produces = MediaType.APPLICATION_JSON_VALUE)
    public String settings(
            @RequestParam(value = "deploymentMode", required = false) String deploymentMode,
            @RequestParam(value = "bootrunHost", required = false) String bootrunHost,
            @RequestParam(value = "tomcatGatewayUrl", required = false) String tomcatGatewayUrl) {
        return relayService.relayEnvGet("/settings", new RelayOptions(deploymentMode, bootrunHost, tomcatGatewayUrl));
    }

    @GetMapping(value = "/projects/baseline", produces = MediaType.APPLICATION_JSON_VALUE)
    public String baseline(
            @RequestParam(value = "projectId", required = false) String projectId,
            @RequestParam(value = "envCode", required = false) String envCode,
            @RequestParam(value = "deploymentMode", required = false) String deploymentMode,
            @RequestParam(value = "bootrunHost", required = false) String bootrunHost,
            @RequestParam(value = "tomcatGatewayUrl", required = false) String tomcatGatewayUrl) {
        String query = "?projectId=" + encode(projectId) + "&envCode=" + encode(envCode);
        return relayService.relayEnvGet("/projects/baseline" + query,
                new RelayOptions(deploymentMode, bootrunHost, tomcatGatewayUrl));
    }

    @PostMapping(value = "/assessments", produces = MediaType.APPLICATION_JSON_VALUE)
    public String runAssessment(
            @RequestParam(value = "projectId", required = false) String projectId,
            @RequestParam(value = "envCode", required = false) String envCode,
            @RequestParam(value = "mergeUploaded", defaultValue = "true") boolean mergeUploaded,
            @RequestParam(value = "deploymentMode", required = false) String deploymentMode,
            @RequestParam(value = "bootrunHost", required = false) String bootrunHost,
            @RequestParam(value = "tomcatGatewayUrl", required = false) String tomcatGatewayUrl) {
        String query = "?projectId=" + encode(projectId)
                + "&envCode=" + encode(envCode)
                + "&mergeUploaded=" + mergeUploaded;
        return relayService.relayEnvPost("/assessments" + query, "{}",
                new RelayOptions(deploymentMode, bootrunHost, tomcatGatewayUrl));
    }

    @GetMapping(value = "/assessments/{runId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public String getAssessment(
            @PathVariable String runId,
            @RequestParam(value = "deploymentMode", required = false) String deploymentMode,
            @RequestParam(value = "bootrunHost", required = false) String bootrunHost,
            @RequestParam(value = "tomcatGatewayUrl", required = false) String tomcatGatewayUrl) {
        return relayService.relayEnvGet("/assessments/" + encode(runId),
                new RelayOptions(deploymentMode, bootrunHost, tomcatGatewayUrl));
    }

    @PostMapping(value = "/config-files/upload", produces = MediaType.APPLICATION_JSON_VALUE)
    public String uploadConfigFiles(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(value = "deploymentMode", required = false) String deploymentMode,
            @RequestParam(value = "bootrunHost", required = false) String bootrunHost,
            @RequestParam(value = "tomcatGatewayUrl", required = false) String tomcatGatewayUrl) {
        return relayService.relayEnvUpload(files, new RelayOptions(deploymentMode, bootrunHost, tomcatGatewayUrl));
    }

    @PostMapping(value = "/export/excel", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> exportExcel(
            @RequestBody String requestBody,
            @RequestParam(value = "deploymentMode", required = false) String deploymentMode,
            @RequestParam(value = "bootrunHost", required = false) String bootrunHost,
            @RequestParam(value = "tomcatGatewayUrl", required = false) String tomcatGatewayUrl) {
        byte[] body = relayService.relayEnvPostBinary("/export/excel", requestBody,
                new RelayOptions(deploymentMode, bootrunHost, tomcatGatewayUrl));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(body);
    }

    private static String encode(String value) {
        if (value == null) {
            return "";
        }
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }
}

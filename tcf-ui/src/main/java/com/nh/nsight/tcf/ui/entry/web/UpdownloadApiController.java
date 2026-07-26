package com.nh.nsight.tcf.ui.entry.web;

import com.nh.nsight.tcf.ui.client.TransactionRelayService.RelayOptions;
import com.nh.nsight.tcf.ui.client.UpdownloadRelayService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/updownload")
public class UpdownloadApiController {
    private final UpdownloadRelayService relayService;

    public UpdownloadApiController(UpdownloadRelayService relayService) {
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

    @PostMapping("/upload")
    public String upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "userId", required = false, defaultValue = "U123456") String userId,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "businessCode", required = false) String businessCode,
            @RequestParam(value = "deploymentMode", required = false) String deploymentMode,
            @RequestParam(value = "bootrunHost", required = false) String bootrunHost,
            @RequestParam(value = "tomcatGatewayUrl", required = false) String tomcatGatewayUrl) {
        RelayOptions options = new RelayOptions(deploymentMode, bootrunHost, tomcatGatewayUrl);
        return relayService.relayUpload(file, userId, description, businessCode, options);
    }

    @GetMapping("/files")
    public String list(
            @RequestParam(value = "originalName", required = false) String originalName,
            @RequestParam(value = "uploadUser", required = false) String uploadUser,
            @RequestParam(value = "fromDate", required = false) String fromDate,
            @RequestParam(value = "toDate", required = false) String toDate,
            @RequestParam(value = "pageNo", required = false, defaultValue = "1") String pageNo,
            @RequestParam(value = "pageSize", required = false, defaultValue = "20") String pageSize,
            @RequestParam(value = "deploymentMode", required = false) String deploymentMode,
            @RequestParam(value = "bootrunHost", required = false) String bootrunHost,
            @RequestParam(value = "tomcatGatewayUrl", required = false) String tomcatGatewayUrl) {
        RelayOptions options = new RelayOptions(deploymentMode, bootrunHost, tomcatGatewayUrl);
        Map<String, String> query = new LinkedHashMap<>();
        query.put("originalName", originalName);
        query.put("uploadUser", uploadUser);
        query.put("fromDate", fromDate);
        query.put("toDate", toDate);
        query.put("pageNo", pageNo);
        query.put("pageSize", pageSize);
        return relayService.relayList(options, query);
    }

    @GetMapping("/files/{fileId}")
    public String detail(
            @PathVariable("fileId") String fileId,
            @RequestParam(value = "deploymentMode", required = false) String deploymentMode,
            @RequestParam(value = "bootrunHost", required = false) String bootrunHost,
            @RequestParam(value = "tomcatGatewayUrl", required = false) String tomcatGatewayUrl) {
        RelayOptions options = new RelayOptions(deploymentMode, bootrunHost, tomcatGatewayUrl);
        return relayService.relayDetail(fileId, options);
    }

    @PutMapping("/files/{fileId}")
    public String update(
            @PathVariable("fileId") String fileId,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "deploymentMode", required = false) String deploymentMode,
            @RequestParam(value = "bootrunHost", required = false) String bootrunHost,
            @RequestParam(value = "tomcatGatewayUrl", required = false) String tomcatGatewayUrl) {
        RelayOptions options = new RelayOptions(deploymentMode, bootrunHost, tomcatGatewayUrl);
        return relayService.relayUpdate(fileId, description, options);
    }

    @DeleteMapping("/files/{fileId}")
    public String delete(
            @PathVariable("fileId") String fileId,
            @RequestParam(value = "deploymentMode", required = false) String deploymentMode,
            @RequestParam(value = "bootrunHost", required = false) String bootrunHost,
            @RequestParam(value = "tomcatGatewayUrl", required = false) String tomcatGatewayUrl) {
        RelayOptions options = new RelayOptions(deploymentMode, bootrunHost, tomcatGatewayUrl);
        return relayService.relayDelete(fileId, options);
    }

    @GetMapping("/files/{fileId}/download")
    public ResponseEntity<ByteArrayResource> download(
            @PathVariable("fileId") String fileId,
            @RequestParam(value = "deploymentMode", required = false) String deploymentMode,
            @RequestParam(value = "bootrunHost", required = false) String bootrunHost,
            @RequestParam(value = "tomcatGatewayUrl", required = false) String tomcatGatewayUrl) {
        RelayOptions options = new RelayOptions(deploymentMode, bootrunHost, tomcatGatewayUrl);
        return relayService.relayDownload(fileId, options);
    }
}

package com.nh.nsight.tcf.ui.entry.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/etc")
public class EtcApiController {
    private static final String DEPRECATED_MESSAGE = """
            {"body":{"error":"ET(common-etc) 모듈은 제거되었습니다. OM 운영관리 포털의 거래로그 조회(/om/admin/transaction-log.html)를 사용하세요."}}
            """;

    @DeleteMapping("/transaction-logs")
    @ResponseStatus(HttpStatus.GONE)
    public String deleteAllLogs(
            @RequestParam(value = "deploymentMode", required = false) String deploymentMode,
            @RequestParam(value = "bootrunHost", required = false) String bootrunHost,
            @RequestParam(value = "tomcatGatewayUrl", required = false) String tomcatGatewayUrl) {
        return DEPRECATED_MESSAGE;
    }

    @PostMapping("/transaction-logs/delete")
    @ResponseStatus(HttpStatus.GONE)
    public String deleteAllLogsPost(
            @RequestParam(value = "deploymentMode", required = false) String deploymentMode,
            @RequestParam(value = "bootrunHost", required = false) String bootrunHost,
            @RequestParam(value = "tomcatGatewayUrl", required = false) String tomcatGatewayUrl) {
        return DEPRECATED_MESSAGE;
    }
}

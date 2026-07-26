package com.nh.nsight.marketing.oc.entry.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nh.nsight.marketing.oc.application.dto.capacity.CapacityApiResponse;
import com.nh.nsight.marketing.oc.application.dto.capacity.WasThreadOnlyCDTO;
import com.nh.nsight.marketing.oc.application.dto.capacity.WasThreadResultCDTO;
import com.nh.nsight.marketing.oc.application.service.ASMSC72001;
import com.nh.nsight.marketing.oc.support.OcCapacityBizException;

@RestController
@RequestMapping("/api/oc/capacity/was-thread")
public class ACMSC72001 {

    private static final String AC = "ACMSC72001";

    private final ASMSC72001 asmsc72001;

    public ACMSC72001(ASMSC72001 asmsc72001) {
        this.asmsc72001 = asmsc72001;
    }

    @PostMapping("/calculate")
    public ResponseEntity<CapacityApiResponse<WasThreadResultCDTO>> calculate(
            @RequestBody WasThreadOnlyCDTO request) {
        System.out.println("★★★★★ [" + AC + "] calculate START");
        WasThreadResultCDTO result = asmsc72001.calculateWasThread(request);
        System.out.println("★★★★★ [" + AC + "] calculate END");
        return ResponseEntity.ok(CapacityApiResponse.ok(result, "WAS 실행쓰레드 산정 완료"));
    }

    @ExceptionHandler(OcCapacityBizException.class)
    public ResponseEntity<CapacityApiResponse<Void>> handleBiz(OcCapacityBizException ex) {
        return ResponseEntity.badRequest().body(CapacityApiResponse.fail(ex.getMessage()));
    }
}

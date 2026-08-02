package nhnis.mp.co.a.controller;

import java.util.List;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import nhnis.fw.tcf.TcfTransaction;
import nhnis.fw.tcf.dto.ProcessingType;
import nhnis.fw.tcf.dto.StandardRequestDto;
import nhnis.fw.tcf.dto.StandardResponseDto;
import nhnis.mp.co.a.dto.mpcoa9999DtoIn;
import nhnis.mp.co.a.dto.mpcoa9999DtoOut;
import nhnis.mp.co.a.dto.mpcoa9999ListResponseDto;
import nhnis.mp.co.a.service.mpcoa9999Service;

/**
 * 영업팁 실적 전문 컨트롤러.
 *
 * <p>
 * Header 검증·추적·결과 코드는 STF/ETF가 처리하므로 여기서는 Body만 다룬다.
 * 반환 타입이 {@code StandardResponseDto}인 이유는 실패 전문도 같은 자리로 나가야 하기 때문이다.
 */
@RestController
@RequestMapping("/api/mp/co/a/9999")
public class mpcoa9999Controller {

    private final mpcoa9999Service service;

    public mpcoa9999Controller(mpcoa9999Service service) {
        this.service = service;
    }

    @TcfTransaction(serviceId = "MP.SalesTip.list", transactionCode = "MP-INQ-0001", processingType = ProcessingType.INQUIRY, serviceName = "영업팁 실적 목록 조회")
    @PostMapping("/list")
    public StandardResponseDto<mpcoa9999ListResponseDto> selectSalesTipList(
            @RequestBody StandardRequestDto<mpcoa9999DtoIn> request) {
        return StandardResponseDto.of(service.selectSalesTipList(request.getBody()));
    }

    @TcfTransaction(serviceId = "MP.SalesTip.detail", transactionCode = "MP-INQ-0002", processingType = ProcessingType.INQUIRY, serviceName = "영업팁 실적 단건 조회")
    @PostMapping("/detail")
    public StandardResponseDto<mpcoa9999DtoOut> selectSalesTipDetail(
            @RequestBody StandardRequestDto<mpcoa9999DtoIn> request) {
        return StandardResponseDto.of(service.selectSalesTipDetail(request.getBody()));
    }
}

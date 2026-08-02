package nhnis.mp.co.a.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import nhnis.fw.tcf.TcfTransaction;
import nhnis.fw.tcf.dto.ProcessingType;
import nhnis.fw.tcf.dto.StandardRequestDto;
import nhnis.fw.tcf.dto.StandardResponseDto;
import nhnis.mp.co.a.dto.mpcoa8888DtoIn;
import nhnis.mp.co.a.dto.mpcoa8888DtoOut;
import nhnis.mp.co.a.dto.mpcoa8888ListResponseDto;
import nhnis.mp.co.a.service.mpcoa8888Service;

/** 영업팁 실적 CRUD 표준 전문 Controller. */
@RestController
@RequestMapping("/api/mp/co/a/8888")
public class mpcoa8888Controller {

    private final mpcoa8888Service service;

    public mpcoa8888Controller(mpcoa8888Service service) {
        this.service = service;
    }

    @TcfTransaction(serviceId = "MP.SalesTip8888.list", transactionCode = "MP-INQ-8881",
            processingType = ProcessingType.INQUIRY, serviceName = "영업팁 실적 목록 조회")
    @PostMapping("/list")
    public StandardResponseDto<mpcoa8888ListResponseDto> selectSalesTipList(
            @RequestBody StandardRequestDto<mpcoa8888DtoIn> request) {
        return StandardResponseDto.of(service.selectSalesTipList(request.getBody()));
    }

    @TcfTransaction(serviceId = "MP.SalesTip8888.detail", transactionCode = "MP-INQ-8882",
            processingType = ProcessingType.INQUIRY, serviceName = "영업팁 실적 단건 조회")
    @PostMapping("/detail")
    public StandardResponseDto<mpcoa8888DtoOut> selectSalesTipDetail(
            @RequestBody StandardRequestDto<mpcoa8888DtoIn> request) {
        return StandardResponseDto.of(service.selectSalesTipDetail(request.getBody()));
    }

    @TcfTransaction(serviceId = "MP.SalesTip8888.create", transactionCode = "MP-CRT-8883",
            processingType = ProcessingType.CREATE, serviceName = "영업팁 실적 등록")
    @PostMapping("/create")
    public StandardResponseDto<Void> createSalesTip(
            @RequestBody StandardRequestDto<mpcoa8888DtoIn> request) {
        service.createSalesTip(request.getBody());
        return StandardResponseDto.of(null);
    }

    @TcfTransaction(serviceId = "MP.SalesTip8888.update", transactionCode = "MP-UPD-8884",
            processingType = ProcessingType.UPDATE, serviceName = "영업팁 실적 수정")
    @PostMapping("/update")
    public StandardResponseDto<Void> updateSalesTip(
            @RequestBody StandardRequestDto<mpcoa8888DtoIn> request) {
        service.updateSalesTip(request.getBody());
        return StandardResponseDto.of(null);
    }

    @TcfTransaction(serviceId = "MP.SalesTip8888.delete", transactionCode = "MP-DEL-8885",
            processingType = ProcessingType.DELETE, serviceName = "영업팁 실적 삭제")
    @PostMapping("/delete")
    public StandardResponseDto<Void> deleteSalesTip(
            @RequestBody StandardRequestDto<mpcoa8888DtoIn> request) {
        service.deleteSalesTip(request.getBody());
        return StandardResponseDto.of(null);
    }
}

package com.nh.nsight.marketing.oc.application.service;

import org.springframework.stereotype.Service;

import com.nh.nsight.marketing.oc.application.dto.capacity.CapacityCalculationCDTO;
import com.nh.nsight.marketing.oc.application.dto.capacity.CapacityCalculationDDTO;
import com.nh.nsight.marketing.oc.application.dto.capacity.CapacityCalculationResultCDTO;
import com.nh.nsight.marketing.oc.application.rule.CapacityCDtoConverter;
import com.nh.nsight.marketing.oc.support.CapacityCalcStep;

@Service
public class ASMSC71001 {

    private static final String AS = "ASMSC71001";

    private final DCCapacity dcCapacity;

    public ASMSC71001(DCCapacity dcCapacity) {
        this.dcCapacity = dcCapacity;
    }

    public CapacityCalculationCDTO defaults() {
        System.out.println("★★★★★ [" + AS + "] defaults");
        return CapacityCDtoConverter.defaultRequest();
    }

    public CapacityCalculationResultCDTO calculate(CapacityCalculationCDTO request) {
        CapacityCalcStep step = CapacityCalcStep.resolve(request.getCalculationStep());
        System.out.println("★★★★★ [" + AS + "] calculate START step=" + step.getCode());
        CapacityCalculationDDTO domain = CapacityCDtoConverter.toDomain(request);
        CapacityCalculationResultCDTO result = dcCapacity.calculate(domain, step);
        System.out.println("★★★★★ [" + AS + "] calculate END step=" + step.getCode()
                + " scenarioId=" + result.getScenarioId());
        return result;
    }
}

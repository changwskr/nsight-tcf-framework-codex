package com.nh.nsight.marketing.sv.application.rule;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 자동생성 테스트 골격. 실제 필드별 정상/오류 시나리오를 보완한다.
 */
class SvCustomerRuleTest {

    private final SvCustomerRule rule = new SvCustomerRule();

    @Test
    @DisplayName("Rule 인스턴스 생성")
    void createRule() {
        assertNotNull(rule);
    }
}

package com.nh.nsight.marketing.av.application.rule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nh.nsight.marketing.av.application.dto.customercontact.CustomerContactSearchCriteria;
import com.nh.nsight.marketing.av.application.dto.customercontact.CustomerContactSelectDetailRequest;
import com.nh.nsight.marketing.av.application.dto.customercontact.CustomerContactSelectListRequest;
import com.nh.nsight.tcf.core.support.error.BusinessException;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AvCustomerContactRuleTest {

    private final AvCustomerContactRule rule = new AvCustomerContactRule();

    @Test
    void 목록조회조건에기본페이징을적용한다() {
        CustomerContactSelectListRequest request =
                CustomerContactSelectListRequest.fromMap(Map.of("customerNo", "C0001"));

        rule.validateSelectList(request);
        CustomerContactSearchCriteria criteria = rule.buildSearchCriteria(request);

        assertThat(criteria.getCustomerNo()).isEqualTo("C0001");
        assertThat(criteria.getPageNo()).isEqualTo(1);
        assertThat(criteria.getPageSize()).isEqualTo(15);
        assertThat(criteria.getOffset()).isZero();
    }

    @Test
    void 고객번호가없으면목록조회를거부한다() {
        CustomerContactSelectListRequest request =
                CustomerContactSelectListRequest.fromMap(Map.of());

        assertThatThrownBy(() -> rule.validateSelectList(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("customerNo");
    }

    @Test
    void 연락처아이디가없으면상세조회를거부한다() {
        CustomerContactSelectDetailRequest request =
                CustomerContactSelectDetailRequest.fromMap(Map.of());

        assertThatThrownBy(() -> rule.validateSelectDetail(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("contactId");
    }

    @Test
    void 전화번호와이메일을마스킹한다() {
        assertThat(rule.maskContactValue("010-1111-2222")).isEqualTo("010-****-2222");
        assertThat(rule.maskContactValue("customer@example.com")).isEqualTo("c****@example.com");
    }
}

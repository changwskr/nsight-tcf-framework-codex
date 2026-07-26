package com.nh.nsight.tcf.core.support.control;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nh.nsight.tcf.core.config.TcfProperties;
import com.nh.nsight.tcf.core.support.error.BusinessException;
import com.nh.nsight.tcf.core.support.error.ErrorCode;
import com.nh.nsight.tcf.core.support.message.StandardHeader;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class TransactionControlValidatorTest {

    private TcfProperties properties;
    private TransactionControlValidator validator;

    @BeforeEach
    void setUp() {
        properties = new TcfProperties();
        properties.setTransactionControlEnabled(true);
        validator = new TransactionControlValidator(properties);
    }

    @Test
    void validatesRequiredControlFields() {
        validator.validateRequired(fullHeader());
    }

    @Test
    void failsWhenUserMissing() {
        TransactionControlHeader header = fullHeader();
        header.setUser("");
        BusinessException ex = assertThrows(BusinessException.class, () -> validator.validateRequired(header));
        assertEquals(ErrorCode.TXCTRL_HDR_USER, ex.getErrorCode());
    }

    @Test
    void skipsWhenDisabled() {
        properties.setTransactionControlEnabled(false);
        validator.validateRequired(new TransactionControlHeader());
    }

    private TransactionControlHeader fullHeader() {
        TransactionControlHeader header = new TransactionControlHeader();
        header.setServiceId("SV.Sample.inquiry");
        header.setServiceName("SV 샘플 조회");
        header.setTransactionCode("SV-INQ-0001");
        header.setBusinessCode("SV");
        header.setUser("U123456");
        header.setChannelId("WEBTOP");
        header.setBranch("001234");
        header.setClientIp("10.10.10.10");
        return header;
    }
}

class TransactionControlServiceTest {

    private TcfProperties properties;
    private TransactionControlValidator validator;
    private TransactionControlRepository repository;
    private TransactionControlService service;

    @BeforeEach
    void setUp() {
        properties = new TcfProperties();
        properties.setTransactionControlEnabled(true);
        validator = new TransactionControlValidator(properties);
        repository = mock(TransactionControlRepository.class);
        when(repository.isGlobalUnblockActive()).thenReturn(false);
        @SuppressWarnings("unchecked")
        ObjectProvider<TransactionControlRepository> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(repository);
        service = new TransactionControlService(properties, validator, provider);
    }

    @Test
    void blocksEvenWhenGlobalUnblockActive() {
        StandardHeader header = standardHeader();
        when(repository.isGlobalUnblockActive()).thenReturn(true);
        when(repository.findRule(org.mockito.ArgumentMatchers.any()))
                .thenReturn(Optional.of(new TransactionControlRule("BUSINESS", "Y")));
        BusinessException ex = assertThrows(BusinessException.class, () -> service.check(header));
        assertEquals(ErrorCode.TXCTRL_NOT_ALLOWED, ex.getErrorCode());
    }

    @Test
    void allowsWhenGlobalUnblockActiveAndNoBlockRule() {
        StandardHeader header = standardHeader();
        when(repository.isGlobalUnblockActive()).thenReturn(true);
        when(repository.findRule(org.mockito.ArgumentMatchers.any())).thenReturn(Optional.empty());
        assertDoesNotThrow(() -> service.check(header));
    }

    @Test
    void allowsUnregisteredTransaction() {
        StandardHeader header = standardHeader();
        when(repository.findRule(org.mockito.ArgumentMatchers.any())).thenReturn(Optional.empty());
        assertDoesNotThrow(() -> service.check(header));
        verify(repository).findRule(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void blocksWhenServiceRuleMatches() {
        StandardHeader header = standardHeader();
        when(repository.findRule(org.mockito.ArgumentMatchers.any()))
                .thenReturn(Optional.of(new TransactionControlRule("SERVICE", "Y")));
        BusinessException ex = assertThrows(BusinessException.class, () -> service.check(header));
        assertEquals(ErrorCode.TXCTRL_NOT_ALLOWED, ex.getErrorCode());
    }

    @Test
    void skipsAuthBootstrapServices() {
        StandardHeader header = new StandardHeader();
        header.setServiceId("OM.Auth.login");
        service.check(header);
    }

    @Test
    void skipsHealthCheckEvenWhenBlockRuleMatches() {
        StandardHeader header = new StandardHeader();
        header.setServiceId("OM.HealthCheck.inquiry");
        header.setBusinessCode("OM");
        when(repository.findRule(org.mockito.ArgumentMatchers.any()))
                .thenReturn(Optional.of(new TransactionControlRule("BUSINESS", "Y")));
        assertDoesNotThrow(() -> service.check(header));
    }

    @Test
    void skipsDeployHealthCheckEvenWhenBlockRuleMatches() {
        StandardHeader header = new StandardHeader();
        header.setServiceId("OM.Deploy.healthCheck");
        header.setBusinessCode("OM");
        when(repository.findRule(org.mockito.ArgumentMatchers.any()))
                .thenReturn(Optional.of(new TransactionControlRule("BUSINESS", "Y")));
        assertDoesNotThrow(() -> service.check(header));
    }

    @Test
    void skipsTransactionControlAdminEvenWhenBlockRuleMatches() {
        StandardHeader header = new StandardHeader();
        header.setServiceId("OM.TransactionControl.inquiry");
        header.setBusinessCode("OM");
        when(repository.findRule(org.mockito.ArgumentMatchers.any()))
                .thenReturn(Optional.of(new TransactionControlRule("GLOBAL", "Y")));
        assertDoesNotThrow(() -> service.check(header));
        verify(repository, org.mockito.Mockito.never()).findRule(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void skipsTransactionControlPageSupportEvenWhenBlockRuleMatches() {
        StandardHeader header = new StandardHeader();
        header.setServiceId("OM.CommonCode.inquiry");
        header.setBusinessCode("OM");
        when(repository.findRule(org.mockito.ArgumentMatchers.any()))
                .thenReturn(Optional.of(new TransactionControlRule("GLOBAL", "Y")));
        assertDoesNotThrow(() -> service.check(header));
        verify(repository, org.mockito.Mockito.never()).findRule(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void deniesWhenRepositoryUnavailable() {
        @SuppressWarnings("unchecked")
        ObjectProvider<TransactionControlRepository> emptyProvider = mock(ObjectProvider.class);
        when(emptyProvider.getIfAvailable()).thenReturn(null);
        TransactionControlService denyService = new TransactionControlService(
                properties, validator, emptyProvider);
        BusinessException ex = assertThrows(BusinessException.class, () -> denyService.check(standardHeader()));
        assertEquals(ErrorCode.TXCTRL_UNAVAILABLE, ex.getErrorCode());
    }

    private StandardHeader standardHeader() {
        StandardHeader header = new StandardHeader();
        header.setServiceId("SV.Sample.inquiry");
        header.setServiceName("SV 샘플 조회");
        header.setTransactionCode("SV-INQ-0001");
        header.setBusinessCode("SV");
        header.setUserId("U123456");
        header.setChannelId("WEBTOP");
        header.setBranchId("001234");
        header.setClientIp("10.10.10.10");
        return header;
    }
}

class TransactionControlExemptionsTest {

    @Test
    void exemptsHealthCheckPatterns() {
        assertTrue(TransactionControlExemptions.isHealthCheck("OM.HealthCheck.inquiry"));
        assertTrue(TransactionControlExemptions.isHealthCheck("OM.Deploy.healthCheck"));
        assertTrue(TransactionControlExemptions.isHealthCheck("SV.HealthCheck.inquiry"));
        assertFalse(TransactionControlExemptions.isHealthCheck("SV.Sample.inquiry"));
    }

    @Test
    void exemptsAuthAndHealthCheckTogether() {
        assertTrue(TransactionControlExemptions.isExempt("OM.Auth.login"));
        assertTrue(TransactionControlExemptions.isExempt("OM.Auth.ssoLogin"));
        assertTrue(TransactionControlExemptions.isExempt("JWT.Auth.ssoIssue"));
        assertTrue(TransactionControlExemptions.isExempt("OM.HealthCheck.inquiry"));
        assertFalse(TransactionControlExemptions.isExempt("SV.Sample.inquiry"));
    }

    @Test
    void exemptsTransactionControlAdmin() {
        assertTrue(TransactionControlExemptions.isTransactionControlAdmin("OM.TransactionControl.inquiry"));
        assertTrue(TransactionControlExemptions.isExempt("OM.TransactionControl.save"));
        assertTrue(TransactionControlExemptions.isExempt("OM.TransactionControl.update"));
        assertTrue(TransactionControlExemptions.isExempt("OM.TransactionControl.delete"));
        assertFalse(TransactionControlExemptions.isTransactionControlAdmin("SV.Sample.inquiry"));
        assertTrue(TransactionControlExemptions.isTransactionControlPageSupport("OM.CommonCode.inquiry"));
    }
}

class TransactionControlRowSupportTest {

    @Test
    void buildsServiceRuleRow() {
        var row = TransactionControlRowSupport.toStorageRow("SERVICE", "SV.Sample.inquiry", "Y");
        assertEquals("SV.Sample.inquiry", row.get("serviceId"));
        assertEquals("*", row.get("businessCode"));
        assertEquals("SERVICE", row.get("controlType"));
    }

    @Test
    void extractsIpTargetFromServiceNameColumn() {
        var row = TransactionControlRowSupport.toStorageRow("IP", "10.10.10.10", "Y");
        assertEquals("10.10.10.10", TransactionControlRowSupport.extractTarget("IP", row));
    }
}

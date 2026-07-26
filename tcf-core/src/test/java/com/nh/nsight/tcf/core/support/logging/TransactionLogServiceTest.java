package com.nh.nsight.tcf.core.support.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nh.nsight.tcf.core.config.TcfProperties;
import com.nh.nsight.tcf.core.support.context.TransactionContext;
import com.nh.nsight.tcf.core.support.message.StandardHeader;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

@ExtendWith(MockitoExtension.class)
class TransactionLogServiceTest {

    @Mock
    private ObjectProvider<TransactionLogRepository> repositoryProvider;

    @Mock
    private TransactionLogRepository repository;

    private TcfProperties properties;
    private TransactionLogService service;

    @BeforeEach
    void setUp() {
        properties = new TcfProperties();
        service = new TransactionLogService(properties, repositoryProvider);
    }

    @Test
    void end_persistsRecordWhenRepositoryAvailable() {
        properties.setTransactionLogEnabled(true);
        when(repositoryProvider.getIfAvailable()).thenReturn(repository);

        StandardHeader header = sampleHeader();
        TransactionContext context = new TransactionContext(header);

        service.end(context, "S0000", null);

        verify(repository).save(any(TransactionLogRecord.class));
    }

    @Test
    void end_skipsPersistWhenDisabled() {
        properties.setTransactionLogEnabled(false);

        service.end(new TransactionContext(sampleHeader()), "S0000", null);

        verify(repository, never()).save(any());
    }

    @Test
    void end_buildsFailStatusForBusinessError() {
        properties.setTransactionLogEnabled(true);
        List<TransactionLogRecord> saved = new ArrayList<>();
        when(repositoryProvider.getIfAvailable()).thenReturn(record -> saved.add(record));

        service.end(new TransactionContext(sampleHeader()), "E0001", "E-BD-001");

        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).resultStatus()).isEqualTo("FAIL");
        assertThat(saved.get(0).errorCode()).isEqualTo("E-BD-001");
    }

    private StandardHeader sampleHeader() {
        StandardHeader header = new StandardHeader();
        header.setBusinessCode("BD");
        header.setServiceId("BD.Sample.inquiry");
        header.setTransactionCode("BD-INQ-0001");
        header.setGuid("guid-001");
        header.setTraceId("trc-001");
        header.setUserId("U001");
        header.setBranchId("001234");
        return header;
    }
}

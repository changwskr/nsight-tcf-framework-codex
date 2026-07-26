package com.nh.nsight.tcf.core.support.message;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class StandardHeaderEchoTest {

    @Test
    void copyOfPreservesClientValuesBeforeNormalize() {
        StandardHeader header = new StandardHeader();
        header.setBusinessCode("sv");
        header.setProcessingType("inquiry");
        header.setServiceId("SV.Sample.inquiry");
        header.setTransactionCode("SV-INQ-0001");
        header.setChannelId("WEBTOP");

        StandardHeader clientHeader = StandardHeader.copyOf(header);
        header.normalize();

        assertThat(header.getBusinessCode()).isEqualTo("SV");
        assertThat(header.getProcessingType()).isEqualTo("INQUIRY");
        assertThat(clientHeader.getBusinessCode()).isEqualTo("sv");
        assertThat(clientHeader.getProcessingType()).isEqualTo("inquiry");
        assertThat(clientHeader.getSystemId()).isNull();
    }

    @Test
    void applyGeneratedCorrelationIdsOnlyWhenClientDidNotSend() {
        StandardHeader clientHeader = new StandardHeader();
        clientHeader.setGuid("client-guid");
        clientHeader.setTraceId(null);

        StandardHeader processed = new StandardHeader();
        processed.setGuid("server-guid");
        processed.setTraceId("server-trace");

        clientHeader.applyGeneratedCorrelationIdsFrom(processed);

        assertThat(clientHeader.getGuid()).isEqualTo("client-guid");
        assertThat(clientHeader.getTraceId()).isEqualTo("server-trace");
    }
}

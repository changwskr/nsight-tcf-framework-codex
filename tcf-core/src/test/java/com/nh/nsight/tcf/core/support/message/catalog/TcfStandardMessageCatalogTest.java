package com.nh.nsight.tcf.core.support.message.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nh.nsight.tcf.core.support.message.ProcessingType;
import com.nh.nsight.tcf.core.support.message.StandardHeader;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TcfStandardMessageCatalogTest {

    @Test
    void requestHeaderMatchesStandardHeaderPropertyCount() {
        assertEquals(15, TcfStandardMessageCatalog.requestHeaderFields().size());
        assertTrue(TcfStandardMessageCatalog.requestHeaderFields().stream()
                .anyMatch(field -> "serviceName".equals(field.fieldKey())));
    }

    @Test
    void requiredHeaderFieldsAlignWithValidator() {
        Set<String> required = TcfStandardMessageCatalog.requiredRequestHeaderFieldKeys();
        assertEquals(Set.of("businessCode", "serviceId", "transactionCode", "processingType", "channelId"), required);
    }

    @Test
    void processingTypeRuleIncludesAllEnumValues() {
        String rule = TcfStandardMessageCatalog.processingTypeValidationRule();
        for (ProcessingType type : ProcessingType.values()) {
            assertTrue(rule.contains(type.name()));
        }
    }

    @Test
    void readHeaderFieldReturnsBeanValue() {
        StandardHeader header = new StandardHeader();
        header.setServiceId("SV.Sample.inquiry");
        assertEquals("SV.Sample.inquiry", TcfStandardMessageCatalog.readHeaderField(header, "serviceId"));
    }

    @Test
    void templatesExposeFourFrameworkSegments() {
        assertEquals(4, TcfStandardMessageCatalog.templatesAsMaps().size());
        assertTrue(TcfStandardMessageCatalog.findByStructCode(TcfStandardMessageCatalog.STRUCT_STD_REQ_HEADER).isPresent());
    }
}

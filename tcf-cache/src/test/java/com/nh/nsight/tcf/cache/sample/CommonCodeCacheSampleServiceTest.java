package com.nh.nsight.tcf.cache.sample;

import static org.assertj.core.api.Assertions.assertThat;

import com.nh.nsight.tcf.cache.config.TcfCacheAutoConfiguration;
import com.nh.nsight.tcf.cache.config.TcfCacheSupportAutoConfiguration;
import com.nh.nsight.tcf.cache.sample.application.service.CommonCodeCacheSampleService;
import com.nh.nsight.tcf.cache.sample.persistence.InMemoryCommonCodeSampleRepository;
import com.nh.nsight.tcf.cache.sample.model.CommonCodeEntry;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@SpringBootTest(classes = CommonCodeCacheSampleServiceTest.TestConfig.class)
class CommonCodeCacheSampleServiceTest {

    @Autowired
    private CommonCodeCacheSampleService service;

    @Test
    void cachesCodeGroupOnSecondLoad() {
        List<Map<String, Object>> first = service.loadByCodeGroup("CHANNEL_CODE");
        List<Map<String, Object>> second = service.loadByCodeGroup("CHANNEL_CODE");

        assertThat(first).hasSize(3);
        assertThat(second).containsExactlyElementsOf(first);
    }

    @Test
    void evictsGroupAfterSave() {
        service.loadByCodeGroup("SAMPLE_STATUS");
        service.save(new CommonCodeEntry("SAMPLE_STATUS", "PENDING", "대기", 3, "Y", "추가 샘플"));

        List<Map<String, Object>> reloaded = service.loadByCodeGroup("SAMPLE_STATUS");
        assertThat(reloaded).anyMatch(row -> "PENDING".equals(row.get("code")));
    }

    @Test
    void loadsDistinctCodeGroups() {
        assertThat(service.loadAllCodeGroupNames()).contains("CHANNEL_CODE", "SAMPLE_STATUS");
    }

    @Configuration
    @Import({
            CacheAutoConfiguration.class,
            TcfCacheAutoConfiguration.class,
            TcfCacheSupportAutoConfiguration.class
    })
    static class TestConfig {

        @Bean
        InMemoryCommonCodeSampleRepository commonCodeSampleRepository() {
            return new InMemoryCommonCodeSampleRepository();
        }

        @Bean
        CommonCodeCacheSampleService commonCodeCacheSampleService(
                InMemoryCommonCodeSampleRepository repository) {
            return new CommonCodeCacheSampleService(repository);
        }
    }
}

package com.nh.nsight.tcf.cache.sample.application.service;

import com.nh.nsight.tcf.cache.sample.model.CommonCodeEntry;
import com.nh.nsight.tcf.cache.sample.persistence.CommonCodeSampleRepository;
import com.nh.nsight.tcf.cache.sample.support.CommonCodeCacheKeys;
import com.nh.nsight.tcf.cache.support.TcfCacheNames;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;

/**
 * 공통코드 EhCache 저장 샘플 서비스.
 * <p>
 * {@code tcf-om} 의 {@code OmCommonCodeCacheService} 와 동일한 캐시 패턴을 보여 줍니다.
 * 업무 모듈에 복사한 뒤 {@code @Service} 를 붙이고 DAO 를 주입해 사용하세요.
 */
public class CommonCodeCacheSampleService {

    private static final Logger log = LoggerFactory.getLogger(CommonCodeCacheSampleService.class);

    private final CommonCodeSampleRepository repository;

    public CommonCodeCacheSampleService(CommonCodeSampleRepository repository) {
        this.repository = repository;
    }

    @Cacheable(cacheNames = TcfCacheNames.COMMON_CODE, key = "#codeGroup")
    public List<Map<String, Object>> loadByCodeGroup(String codeGroup) {
        log.debug("EhCache miss — sample DB load codeGroup={}", codeGroup);
        return repository.findByCodeGroup(codeGroup).stream()
                .map(CommonCodeEntry::toMap)
                .toList();
    }

    @Cacheable(cacheNames = TcfCacheNames.COMMON_CODE, key = "'" + CommonCodeCacheKeys.ALL_GROUPS_KEY + "'")
    public List<String> loadAllCodeGroupNames() {
        log.debug("EhCache miss — sample DB load distinct code groups");
        return repository.findDistinctCodeGroups();
    }

    public Map<String, Object> findInGroup(String codeGroup, String code) {
        if (codeGroup == null || code == null) {
            return null;
        }
        for (Map<String, Object> row : loadByCodeGroup(codeGroup)) {
            if (code.equals(String.valueOf(row.get("code")))) {
                return row;
            }
        }
        return null;
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = TcfCacheNames.COMMON_CODE, key = "#entry.codeGroup"),
            @CacheEvict(cacheNames = TcfCacheNames.COMMON_CODE, key = "'" + CommonCodeCacheKeys.ALL_GROUPS_KEY + "'")
    })
    public CommonCodeEntry save(CommonCodeEntry entry) {
        log.debug("EhCache evict after save codeGroup={} code={}", entry.codeGroup(), entry.code());
        repository.save(entry);
        return entry;
    }

    @CacheEvict(cacheNames = TcfCacheNames.COMMON_CODE, key = "#codeGroup")
    public void evictCodeGroup(String codeGroup) {
        log.debug("EhCache evict codeGroup={}", codeGroup);
    }
}

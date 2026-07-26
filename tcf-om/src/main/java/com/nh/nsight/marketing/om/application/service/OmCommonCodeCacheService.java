package com.nh.nsight.marketing.om.application.service;

import com.nh.nsight.marketing.om.persistence.dao.OmOperationDao;
import com.nh.nsight.tcf.cache.support.TcfCacheNames;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

@Service
public class OmCommonCodeCacheService {
    static final String ALL_GROUPS_KEY = "__ALL_GROUPS__";

    private static final Logger log = LoggerFactory.getLogger(OmCommonCodeCacheService.class);

    private final OmOperationDao dao;

    public OmCommonCodeCacheService(OmOperationDao dao) {
        this.dao = dao;
    }

    @Cacheable(cacheNames = TcfCacheNames.COMMON_CODE, key = "#codeGroup")
    public List<Map<String, Object>> loadByCodeGroup(String codeGroup) {
        log.debug("EhCache miss — DB load codeGroup={}", codeGroup);
        Map<String, Object> criteria = new HashMap<>();
        criteria.put("codeGroup", codeGroup);
        return dao.searchCommonCodes(criteria);
    }

    @Cacheable(cacheNames = TcfCacheNames.COMMON_CODE, key = "'" + ALL_GROUPS_KEY + "'")
    public List<String> loadAllCodeGroupNames() {
        log.debug("EhCache miss — DB load distinct code groups");
        return dao.selectDistinctCodeGroups();
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
            @CacheEvict(cacheNames = TcfCacheNames.COMMON_CODE, key = "#codeGroup"),
            @CacheEvict(cacheNames = TcfCacheNames.COMMON_CODE, key = "'" + ALL_GROUPS_KEY + "'")
    })
    public void evictCodeGroup(String codeGroup) {
        log.debug("EhCache evict codeGroup={} and group index", codeGroup);
    }
}

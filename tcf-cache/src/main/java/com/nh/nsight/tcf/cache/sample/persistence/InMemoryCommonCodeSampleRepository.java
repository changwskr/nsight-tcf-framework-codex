package com.nh.nsight.tcf.cache.sample.persistence;

import com.nh.nsight.tcf.cache.sample.model.CommonCodeEntry;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 샘플용 인메모리 공통코드 저장소 — 단위 테스트·로컬 데모용.
 */
public class InMemoryCommonCodeSampleRepository implements CommonCodeSampleRepository {

    private final Map<String, Map<String, CommonCodeEntry>> store = new ConcurrentHashMap<>();

    public InMemoryCommonCodeSampleRepository() {
        seedDemoData();
    }

    @Override
    public List<CommonCodeEntry> findByCodeGroup(String codeGroup) {
        Map<String, CommonCodeEntry> group = store.get(codeGroup);
        if (group == null) {
            return List.of();
        }
        return group.values().stream()
                .sorted(Comparator.comparingInt(CommonCodeEntry::sortOrder).thenComparing(CommonCodeEntry::code))
                .toList();
    }

    @Override
    public List<String> findDistinctCodeGroups() {
        return store.keySet().stream().sorted().toList();
    }

    @Override
    public void save(CommonCodeEntry entry) {
        store.computeIfAbsent(entry.codeGroup(), key -> new LinkedHashMap<>())
                .put(entry.code(), entry);
    }

    private void seedDemoData() {
        save(new CommonCodeEntry("CHANNEL_CODE", "WEB", "웹 채널", 1, "Y", "브라우저·모바일 웹"));
        save(new CommonCodeEntry("CHANNEL_CODE", "MOBILE", "모바일 앱", 2, "Y", "네이티브 앱"));
        save(new CommonCodeEntry("CHANNEL_CODE", "BRANCH", "영업점", 3, "Y", "창구 채널"));
        save(new CommonCodeEntry("SAMPLE_STATUS", "ACTIVE", "사용", 1, "Y", "샘플 활성"));
        save(new CommonCodeEntry("SAMPLE_STATUS", "INACTIVE", "중지", 2, "Y", "샘플 비활성"));
    }
}

package com.nh.nsight.tcf.cache.sample.persistence;

import com.nh.nsight.tcf.cache.sample.model.CommonCodeEntry;
import java.util.List;

/**
 * 공통코드 저장소 샘플 인터페이스 — 업무 모듈에서는 MyBatis DAO 로 대체합니다.
 */
public interface CommonCodeSampleRepository {

    List<CommonCodeEntry> findByCodeGroup(String codeGroup);

    List<String> findDistinctCodeGroups();

    void save(CommonCodeEntry entry);
}

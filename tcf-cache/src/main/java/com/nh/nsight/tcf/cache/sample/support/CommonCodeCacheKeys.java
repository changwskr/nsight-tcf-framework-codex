package com.nh.nsight.tcf.cache.sample.support;

/**
 * 공통코드 EhCache 키 규칙 샘플.
 * <p>
 * 실제 OM 구현: {@code OmCommonCodeCacheService} — 그룹 키는 {@code codeGroup},
 * 전체 그룹 목록은 {@link #ALL_GROUPS_KEY}.
 */
public final class CommonCodeCacheKeys {

  /** 그룹 목록 인덱스 캐시 키 (OmCommonCodeCacheService 와 동일) */
  public static final String ALL_GROUPS_KEY = "__ALL_GROUPS__";

  private CommonCodeCacheKeys() {}

  /** 코드그룹 단위 캐시 키 — {@code @Cacheable(key = "#codeGroup")} */
  public static String groupKey(String codeGroup) {
    return codeGroup;
  }

  /** 문서용 논리 키 — CC:CODE:{그룹}:{코드} */
  public static String itemKey(String codeGroup, String code) {
    return "CC:CODE:" + codeGroup + ":" + code;
  }
}

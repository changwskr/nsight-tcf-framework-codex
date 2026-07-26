package com.nh.nsight.tcf.util.meta;

/**
 * 복사 유틸리티 공통 플래그 인터페이스.
 * <p>
 * 구현 클래스는 {@link #COPIED_UTILITY} 및 출처 상수를 반드시 선언한다.
 */
public interface CopiedUtilityFlag {

    /** 이 클래스가 다른 모듈에서 tcf-util로 복사·통합되었음을 나타낸다. */
    boolean COPIED_UTILITY = true;
}
